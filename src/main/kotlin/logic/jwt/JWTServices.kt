package logic.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import com.beust.klaxon.Klaxon
import models.jwt.UserAuthentificationResult
import models.jwt.UserTokenVerificationResult
import models.users.User
import java.lang.Exception
import java.util.*

class JWTServices(private val secret: String, var dataSource: UsersDataSource) {

    class ClaimNotFoundException(val claim: String): Exception("Claim \"$claim\" not found in token")
    class UserNotFoundException(val id: String): Exception("Claim \"$id\" not found")
    class RefreshTokenException: Exception("Unable to refresh token")

    private fun createToken(user: User): String {
        try {
            val algo = Algorithm.HMAC256(secret)
            val nowDate = Date()
            val calendar = Calendar.getInstance()
            calendar.time = nowDate
            calendar.add(Calendar.MINUTE, 10)
            val expDate = calendar.time
            return JWT.create()
                .withIssuer("comy")
                .withClaim("id", user.id)
                .withExpiresAt(expDate)
                .sign(algo)
        } catch (e: JWTCreationException){
            e.printStackTrace()
            TODO(e.stackTrace.toString())
        }
    }

    private fun createRefreshToken(user: User): String {
        try {
            val algo = Algorithm.HMAC256(secret)
            val nowDate = Date()
            val calendar = Calendar.getInstance()
            calendar.time = nowDate
            calendar.add(Calendar.MONTH, 2)
            val expDate = calendar.time
            val refreshKey = generateRefreshKey()
            user.refreshKey = refreshKey
            return JWT.create()
                .withIssuer("comy")
                .withClaim("id", user.id)
                .withClaim("refreshKey", refreshKey)
                .withExpiresAt(expDate)
                .sign(algo)
        } catch (e: JWTCreationException){
            e.printStackTrace()
            TODO(e.stackTrace.toString())
        }
    }

    private fun createTokens(user: User): Pair<String, String> {
        return createToken(user) to createRefreshToken(user)
    }

    private fun generateRefreshKey(currentKeys: Array<String> = arrayOf()): String {

        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        fun generate() = (1 until 20)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")


        var refreshKey = generate()
        while(currentKeys.contains(refreshKey)){
            refreshKey = generate()
        }

        return  refreshKey
    }

    private fun verifyToken(token: String): DecodedJWT {
        val algo = Algorithm.HMAC256(secret)
        val verifier = JWT.require(algo)
            .withIssuer("comy")
            .build()
        return verifier.verify(token)
    }

    fun refreshToken(refreshToken: String): String?{
        try {
            val decodedRefreshToken = verifyToken(refreshToken)
            val id = decodedRefreshToken.getClaim("id")
            val refreshKey = decodedRefreshToken.getClaim("refreshKey")
            if (id.isNull) throw ClaimNotFoundException(claim = "id")
            if (refreshKey.isNull) throw ClaimNotFoundException(claim = "refreshKey")
            val user = dataSource.allowedUsers().firstOrNull { it.id == id.asString() }
            if (user == null) throw UserNotFoundException(id = id.asString())
            if (user.refreshKey != refreshKey.asString()) throw RefreshTokenException()
            return createToken(user)
        } catch (e: Exception){
            if (e is ClaimNotFoundException || e is UserNotFoundException || e is RefreshTokenException || e is TokenExpiredException){
                return null
            } else throw e
        }
    }

    fun authentificateUser(id: String, password: String): UserAuthentificationResult {
        val user = dataSource.allowedUsers().firstOrNull { it.id == id }
        if (user == null || user.password != password) {
            return UserAuthentificationResult(
                token = null,
                refreshToken = null,
                userId = null,
                wrongLoginPassword = true
            )
        }
        val tokens = createTokens(user)
        return UserAuthentificationResult(
            token = tokens.first,
            refreshToken = tokens.second,
            userId = user.id,
            wrongLoginPassword = false
        )

    }

    fun verifyUserToken(token: String): UserTokenVerificationResult {
        try {
            val decodedToken = verifyToken(token)
            val decodedId = decodedToken.getClaim("id")
            if (decodedId.isNull) throw ClaimNotFoundException(claim = "id")
            val user = dataSource.allowedUsers().firstOrNull { it.id == decodedId.asString() }
            if (user == null) throw  UserNotFoundException(id = decodedId.asString())
            return UserTokenVerificationResult(
                user = user,
                tokenExpired = false,
                userNotFound = false,
                verificationError = false
            )
        } catch(e: TokenExpiredException) {
            return UserTokenVerificationResult(
                user = null,
                tokenExpired = true,
                userNotFound = false,
                verificationError = false
            )
        } catch(e: UserNotFoundException) {
            return UserTokenVerificationResult(
                user = null,
                tokenExpired = false,
                userNotFound = true,
                verificationError = false
            )
        } catch(e: JWTVerificationException) {
            return UserTokenVerificationResult(
                user = null,
                tokenExpired = false,
                userNotFound = false,
                verificationError = true
            )
        }
    }

}