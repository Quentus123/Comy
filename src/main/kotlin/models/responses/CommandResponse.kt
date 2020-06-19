package models.responses

data class CommandResponse(val commandName: String, val result: CommandResult, val authError: AuthentificationResponse?): Response(type = type){
    companion object{
        val type = "CommandResponse"
    }
}