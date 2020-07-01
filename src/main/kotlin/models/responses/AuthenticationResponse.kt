package models.responses

/**
 * A response from the server that tells to the client the result of an authentication.
 *
 * @param token The access token.
 * @param refreshToken The refresh token.
 * @param username The user's username if verification is a success.
 * @param message The authentication's message.
 * @param code The authentication's result code.
 * @param tokenExpiredError True if the token given by client is expired.
 * @param wrongCredentialsError True is username/password givem by client are not valid.
 */
data class AuthenticationResponse(val token: String?, val refreshToken: String?, val username: String?, val message: String, val code: Int, val tokenExpiredError: Boolean, val wrongCredentialsError: Boolean): Response(type = type){
    companion object{
        const val type = "AuthenticationResponse"
    }
}