package models.commands

data class CommandResult(val result: String, val status: ResultStatus){
    companion object{
        val DEFAULT_RESULT = CommandResult(result = "", status = ResultStatus.DEFAULT_SUCCESS)
    }
}