package models.messages

/**
 * A message from the client that ask the server to refresh access token.
 *
 * @param refreshToken The client's refresh token.
 */
data class RefreshTokenMessage(val refreshToken: String): Message(type = type){
    companion object{
        const val type = "RefreshTokenMessage"
    }
}