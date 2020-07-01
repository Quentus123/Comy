package models.messages

/**
 * A message from the client that ask the server to send his state.
 *
 * @param token The client's access token. Null if the user is not logged.
 */
class NeedStateMessage(val token: String? = null): Message(type = type){
    companion object{
        const val type = "Need refresh"
    }
}