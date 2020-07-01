package models.commands

/**
 * Represent a command execution's result.
 *
 * @param message The result.
 * @param status The command execution status.
 *
 *
 * @see CommandResultStatus
 */
data class CommandResult(val message: String, val status: CommandResultStatus){
    companion object{
        val DEFAULT_RESULT = CommandResult(message = "", status = CommandResultStatus.DEFAULT_SUCCESS)
    }
}