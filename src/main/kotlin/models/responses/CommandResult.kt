package models.responses

data class CommandResult(val result: String, val status: CommandResultStatus): Response(type = type){
    companion object{
        val type = "CommandResult"
        val DEFAULT_RESULT = CommandResult(result = "", status = CommandResultStatus.DEFAULT_SUCCESS)
    }
}