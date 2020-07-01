package models.responses

/**
 * A response from the server to the client with some infos about the server.
 *
 * @param serverName The server's name.
 * @param isSecured Determine if logging is required to access to the server.
 */
data class ServerInfoResponse(val serverName: String, val isSecured: Boolean): Response(type = type){
    companion object{
        const val type = "ServerInfoResponse"
    }
}