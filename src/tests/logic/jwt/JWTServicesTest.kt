package logic.jwt

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import models.users.User
import org.junit.Test

internal class JWTServicesTest {

    val secretKey = "thisIsAnAmazingSecretKey"

    @Test
    fun `test refreshToken with valid token`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val authResult = service.authenticateUser(username = "user", password = "password")
        val token = authResult.token!!
        val refreshToken = authResult.refreshToken!!
        var newToken: String = token
        runBlocking {
            delay(1000)
            newToken = service.refreshToken(refreshToken)!!
        }


        //assert
        assert(token != newToken)
        assert(service.verifyUserToken(token = newToken).user != null)
    }

    @Test
    fun `test refreshToken with invalid token`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val refreshToken = "JHhbchgyeh4514545812EE.dedewW.54dww"
        val newToken = service.refreshToken(refreshToken)

        //assert
        assert(newToken == null)
    }

    @Test
    fun `test refreshToken with valid token but user not found`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val authResult = service.authenticateUser(username = "user", password = "password")
        val refreshToken = authResult.refreshToken!!
        usersDataSource.users.removeAt(0)
        val newToken = service.refreshToken(refreshToken)

        //assert
        assert(newToken == null)
    }

    @Test
    fun `test authenticateUser with valid username password`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val authResult = service.authenticateUser(username = "user", password = "password")

        //assert
        assert(authResult.username == "user")
        assert(authResult.token != null)
        assert(authResult.refreshToken != null)
    }

    @Test
    fun `test authenticateUser with valid username but invalid password`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val authResult = service.authenticateUser(username = "user", password = "wrongpassword")

        //assert
        assert(authResult.wrongLoginPassword)
        assert(authResult.username == null)
        assert(authResult.token == null)
        assert(authResult.refreshToken == null)
    }

    @Test
    fun `test authenticateUser with invalid username`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val authResult = service.authenticateUser(username = "unknownUser", password = "password")

        //assert
        assert(authResult.wrongLoginPassword)
        assert(authResult.username == null)
        assert(authResult.token == null)
        assert(authResult.refreshToken == null)
    }

    @Test
    fun `test verifyUserToken with valid token`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val authResult = service.authenticateUser(username = "user", password = "password")
        val token = authResult.token!!

        //assert
        assert(service.verifyUserToken(token = token).user?.username == "user")
    }

    @Test
    fun `test verifyUserToken with invalid token`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val token = "kjfh.eeEE.4512"

        //assert
        assert(service.verifyUserToken(token = token).verificationError)
    }

    @Test
    fun `test verifyUserToken with valid token but unknown user`() {
        //act
        val usersDataSource = object : UsersDataSource {
            val users = mutableListOf(User(username = "user1", password = "password", securityGroup = null), User(username = "user2", password = "password", securityGroup = null))
            override fun allowedUsers(): MutableList<User> = users
        }
        val service = JWTServices(secret = secretKey, dataSource = usersDataSource)

        //arrange
        val tokenUser1 = service.authenticateUser(username = "user1", password = "password").token!!
        usersDataSource.users.removeAt(0)

        //assert
        assert(service.verifyUserToken(token = tokenUser1).userNotFound)
    }
}