package models.messages

/**
 * A message from the client that ask the server to execute a command.
 *
 * @param commandName The name of the command.
 * @param token The client's access token. Null if the user is not logged.
 * @param params Params to execute with the command.
 */
data class ExecuteCommandMessage(val commandName: String, val token: String? = null, val params: Map<String, Any>): Message(type = type){
    companion object{
        const val type = "ExecuteCommand"
    }
}