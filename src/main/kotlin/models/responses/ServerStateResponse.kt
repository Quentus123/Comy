package models.responses

import models.commands.Command

class ServerStateResponse(val name: String, val commands: Array<Command>, val authError: AuthenticationResponse?): Response(type = type){
    companion object{
        const val type = "ServerStateResponse"
    }
}