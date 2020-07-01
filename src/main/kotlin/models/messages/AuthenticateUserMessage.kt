package models.messages

/**
 * A message from the client that ask the server to authenticate user with username and password.
 *
 * @param username The client's username.
 * @param password The client's password.
 */
data class AuthenticateUserMessage(val username: String, val password: String): Message(type = type){
    companion object{
        const val type = "AuthenticateUserMessage"
    }
}