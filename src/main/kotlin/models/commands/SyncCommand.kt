package models.commands

import com.beust.klaxon.Json
import models.commands.params.Parameter
import models.responses.CommandResult

/**
 * A synchronous command.
 *
 *
 * The first sample describe a command that does nothing, this is the minimal stuff to make a command valid
 * The second sample describe a command that will simulate n dices, where n is an integer paramater given by the user :
 *
 * @sample dokka.samples.SyncCommandSamples.createEasiestCommand
 * @sample dokka.samples.SyncCommandSamples.createDicesSimulatorCommand
 *
 * @see AsyncCommand
 * @see CommandInfos
 * @see CommandResult
 *
 * @param name The name of the command. This will be show to the user.
 * @param imageURL The command image url, user device will download it.
 * @param mainParameter Optional: Describe the most important parameter of the command, this MUST be an IntCommandParameter. The user will be able to set this parameter without going in the settings screen.
 * @param secondariesParameters Optional: Less important params. To modify them, user must go in settings screen.
 * @param function What will be done when user execute the command. It MUST be a synchronous function. If you want an asynchronous function, look for AsyncCommand. When executing the command, Comy will pass a CommandInfos instance to the function, this where you can see which user execute the command (null if there is no user) and params given. You also need to cast params type. The function must return a CommandResult object that show if command execution is a success or not (and give result or error message)
 *
 *
 */

class SyncCommand(override val name: String,
                  override val imageURL: String?,
                  override val mainParameter: Parameter? = null,
                  override val secondariesParameters: Array<Parameter>? = null,
                  @Json(ignored = true) val function: (CommandInfos) -> CommandResult): Command {

    init {
        val parametersNames = (arrayOf(mainParameter?.name).filterNotNull() union (secondariesParameters?.map { it.name }
                ?: arrayListOf()))
        require(parametersNames.count() == parametersNames.distinct().count()) {
            "Error in command named $name: duplicate parameter name"
        }
        require((mainParameter?.canBeMain ?: true)) {
            "Error in command named $name: wrong main parameter type"
        }
    }
}