package models.responses

import com.beust.klaxon.Klaxon
import models.commands.Command

class ServerStateResponse(val state: Array<Command>): Response(type = type){
    companion object{
        val type = "ServerStateResponse"
    }
}