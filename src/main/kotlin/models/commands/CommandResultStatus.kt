package models.commands

/**
 * Represent a command execution's status.
 *
 * @param success determine if command has been executed successfully.
 * @param message The status message.
 *
 *
 * @see CommandResultStatus
 */
data class CommandResultStatus(val success: Boolean, val message: String){
    companion object{
        val DEFAULT_SUCCESS = CommandResultStatus(success = true, message = "OK")
        val DEFAULT_FAIL = CommandResultStatus(success = false, message = "Unexpected error")
    }
}