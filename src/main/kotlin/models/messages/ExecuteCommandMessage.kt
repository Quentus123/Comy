package models.messages

import models.commands.params.Parameter

data class ExecuteCommandMessage(val commandName: String, val token: String? = null, val params: Map<String, Any>): Message(type = type){
    companion object{
        val type = "ExecuteCommand"
    }
}