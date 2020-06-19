package models.responses

import models.commands.Command
import models.jwt.UserTokenVerificationResult

class ServerStateResponse(val name: String, val commands: Array<Command>, val authError: AuthentificationResponse?): Response(type = type){
    companion object{
        val type = "ServerStateResponse"
    }
}