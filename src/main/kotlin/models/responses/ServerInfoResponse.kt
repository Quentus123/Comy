package models.responses

data class ServerInfoResponse(val serverName: String, val isSecured: Boolean): Response(type = type){
    companion object{
        const val type = "ServerInfoResponse"
    }
}