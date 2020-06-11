package models.commands

data class ResultStatus(val success: Boolean, val message: String){
    companion object{
        val DEFAULT_SUCCESS = ResultStatus(success = true, message = "OK")
        val DEFAULT_FAIL = ResultStatus(success = false, message = "Unexpected error")
    }
}