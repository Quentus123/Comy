package models.messages


/**
 * A message from the client that ask the server to authenticate user with an access token.
 *
 * @param token The client's access token.
 */
data class AuthenticateTokenMessage(val token: String): Message(type = type){
    companion object{
        const val type = "AuthenticateTokenMessage"
    }
}