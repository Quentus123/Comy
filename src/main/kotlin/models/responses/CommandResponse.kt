package models.responses

data class CommandResponse(val commandName: String, val result: CommandResult): Response(type = type){
    companion object{
        val type = "CommandResponse"
    }
}