package models.responses

import models.commands.CommandResult

/**
 * A response from the server that tell to the client the result of a command.
 *
 * @param commandName The name of the executed command.
 * @param result The command's result.
 * @param authError An AuthenticationResponse object. Null if there is no authentication error.
 *
 * @see models.commands.Command
 * @see AuthenticationResponse
 */
data class CommandResponse(val commandName: String, val result: CommandResult, val authError: AuthenticationResponse?): Response(type = type){
    companion object{
        const val type = "CommandResponse"
    }
}