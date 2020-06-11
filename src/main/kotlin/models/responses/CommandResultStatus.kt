package models.responses

data class CommandResultStatus(val success: Boolean, val message: String){
    companion object{
        val DEFAULT_SUCCESS = CommandResultStatus(success = true, message = "OK")
        val DEFAULT_FAIL = CommandResultStatus(success = false, message = "Unexpected error")
    }
}