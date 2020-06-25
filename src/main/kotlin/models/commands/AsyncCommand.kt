package models.commands

import com.beust.klaxon.Json
import models.commands.params.Parameter
import models.responses.CommandResult
import models.security.SecurityGroup

/**
 * A asynchronous command.
 *
 *
 * Here is a sample that simule an asynchronous command :
 * @sample dokka.samples.AsyncCommandSamples.createAnnoyingComputationCommand
 *
 * @see SyncCommand
 * @see CommandInfos
 * @see CommandResult
 *
 * @param name The name of the command. This will be show to the user.
 * @param imageURL The command image url, user device will download it.
 * @param mainParameter Optional: Describe the most important parameter of the command, this MUST be an IntCommandParameter. The user will be able to set this parameter without going in the settings screen.
 * @param secondariesParameters Optional: Less important params. To modify them, user must go in settings screen.
 * @param securityGroups Optional: All the security groups allowed to execute this command. Important note: you only need to add groups add the end of the tree, no need to add super groups.
 * @param function What will be done when user execute the command. It could be an asynchronous function. For synchronous commands, you should use SyncCommand. When executing the command, Comy will pass three objects to the function : First, a CommandInfos instance to the function, this where you can see which user execute the command (null if there is no user) and params given. You also need to cast params type. Second, an completion handler that you must call when your async task is finished. Third, an function that give a boolean to indicate if the command is not timed out, use it to stop every computation.
 *
 *
 */

class AsyncCommand(override val name: String,
                   override val imageURL: String?,
                   override val mainParameter: Parameter? = null,
                   override val secondariesParameters: Array<Parameter> = arrayOf(),
                   @Json(ignored = true) override val securityGroups: Array<SecurityGroup> = arrayOf(),
                   @Json(ignored = true) val function: suspend (CommandInfos, (CommandResult) -> Unit, () -> Boolean) -> Unit): Command {

    init {
        val parametersNames = (arrayOf(mainParameter?.name).filterNotNull() union (secondariesParameters.map { it.name }))
        require(parametersNames.count() == parametersNames.distinct().count()) {
            "Error in command named $name: duplicate parameter name"
        }
        require((mainParameter?.canBeMain ?: true)) {
            "Error in command named $name: wrong main parameter type"
        }
    }

}