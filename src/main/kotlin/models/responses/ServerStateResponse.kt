package models.responses

import models.commands.Command

class ServerStateResponse(val name: String, val commands: Array<Command>, val authError: AuthentificationResponse?): Response(type = type){
    companion object{
        val type = "ServerStateResponse"
    }
}