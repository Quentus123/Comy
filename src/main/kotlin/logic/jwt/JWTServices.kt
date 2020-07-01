package logic.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import models.jwt.UserAuthenticationResult
import models.jwt.UserTokenVerificationResult
import models.users.User
import java.util.*

/**
 * Is responsible to create and verify tokens.
 *
 * @param secret The secret key for JSON Web Token.
 * @param dataSource A class that provide users. Useful to manipulate their refreshKey property.
 *
 * @see UsersDataSource
 * @see User
 */
class JWTServices(private val secret: String, var dataSource: UsersDataSource) {

    class ClaimNotFoundException(val claim: String): Exception("Claim \"$claim\" not found in token")
    class UserNotFoundException(val id: String): Exception("Claim \"$id\" not found")
    class RefreshTokenException: Exception("Unable to refresh token")

    /**
     * Create an access token for user. The token generated is valid for the next 10 minutes.
     *
     * @param user The user.
     * @return The user's access token
     *
     * @see User
     */
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
                .withClaim("id", user.username)
                .withExpiresAt(expDate)
                .sign(algo)
        } catch (e: JWTCreationException){
            e.printStackTrace()
            TODO(e.stackTrace.toString())
        }
    }

    /**
     * Create an refresh token for user. The token generated is valid for the next 2 months. IMPORTANT : this function modify user's refreshKey property so it invalidate all previously generated user's refreshToken.
     *
     * @param user The user.
     * @return The user's refresh token
     * @see User
     */
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
                .withClaim("id", user.username)
                .withClaim("refreshKey", refreshKey)
                .withExpiresAt(expDate)
                .sign(algo)
        } catch (e: JWTCreationException){
            e.printStackTrace()
            TODO(e.stackTrace.toString())
        }
    }

    /**
     * Create access token and refresh token for user.
     *
     * @param user The user to create tokens.
     * @return A pair where first value is the access token, the second is the refresh token.
     */
    private fun createTokens(user: User): Pair<String, String> {
        return createToken(user) to createRefreshToken(user)
    }

    /**
     * An helper function that generate an unique new refresh key.
     *
     * @param currentKeys Optional: An array of keys. New generated key will be different of them. Default value : An empty array.
     * @return A refresh key.
     */
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

    /**
     * Verify if token is valid.
     *
     * @throws TokenExpiredException if token is expired.
     * @return a DecodedJWT object that represent the json web token.
     */
    private fun verifyToken(token: String): DecodedJWT {
        val algo = Algorithm.HMAC256(secret)
        val verifier = JWT.require(algo)
            .withIssuer("comy")
            .build()
        return verifier.verify(token)
    }

    fun refreshToken(refreshToken: String): String?{
        return try {
            val decodedRefreshToken = verifyToken(refreshToken)
            val id = decodedRefreshToken.getClaim("id")
            val refreshKey = decodedRefreshToken.getClaim("refreshKey")
            if (id.isNull) throw ClaimNotFoundException(claim = "id")
            if (refreshKey.isNull) throw ClaimNotFoundException(claim = "refreshKey")
            val user = dataSource.allowedUsers().firstOrNull { it.username == id.asString() }
                    ?: throw UserNotFoundException(id = id.asString())
            if (user.refreshKey != refreshKey.asString()) throw RefreshTokenException()
            createToken(user)
        } catch (e: Exception){
            null
        }
    }

    /**
     * Authenticate an user and generated tokens.
     *
     * @param username The user's username.
     * @param password The user's password.
     * @return An UserAuthenticationResult object that tell if authentication is a success or not. If success, result will contains the user's access token, refresh token and username.
     *
     * @see UserAuthenticationResult
     */
    fun authenticateUser(username: String, password: String): UserAuthenticationResult {
        val user = dataSource.allowedUsers().firstOrNull { it.username == username }
        if (user == null || user.password != password) {
            return UserAuthenticationResult(
                token = null,
                refreshToken = null,
                username = null,
                wrongLoginPassword = true
            )
        }
        val tokens = createTokens(user)
        return UserAuthenticationResult(
            token = tokens.first,
            refreshToken = tokens.second,
            username = user.username,
            wrongLoginPassword = false
        )

    }

    /**
     * Verify is an access token is valid.
     *
     * @param token The user's access token.
     * @return An UserTokenVerificationResult that tell if token is valid. If token is valid, the result will contains the user. Else, the result will contains the reason why verification failed.
     *
     * @see UserTokenVerificationResult
     */
    fun verifyUserToken(token: String): UserTokenVerificationResult {
        try {
            val decodedToken = verifyToken(token)
            val decodedId = decodedToken.getClaim("id")
            if (decodedId.isNull) throw ClaimNotFoundException(claim = "id")
            val user = dataSource.allowedUsers().firstOrNull { it.username == decodedId.asString() }
                    ?: throw  UserNotFoundException(id = decodedId.asString())
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