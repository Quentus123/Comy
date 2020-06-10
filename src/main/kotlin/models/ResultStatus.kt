package models

interface ResultStatus{
    val success: Boolean
    val message: String

    companion object{
        val DEFAULT_SUCCESS = object: ResultStatus{
            override val success: Boolean = true
            override val message: String = "OK"
        }

        val DEFAULT_FAIL = object: ResultStatus{
            override val success: Boolean = false
            override val message: String = "Unexpected error"
        }
    }
}