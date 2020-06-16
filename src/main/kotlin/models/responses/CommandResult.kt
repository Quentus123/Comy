package models.responses

data class CommandResult(val message: String, val status: CommandResultStatus){
    companion object{
        val DEFAULT_RESULT = CommandResult(message = "", status = CommandResultStatus.DEFAULT_SUCCESS)
    }
}