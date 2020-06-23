package models.commands

import com.beust.klaxon.Json
import models.commands.params.Parameter
import models.responses.CommandResult
import models.users.User

class AsyncCommand(override val name: String,
                   override val imageURL: String?,
                   override val mainParameter: Parameter? = null,
                   override val secondariesParameters: Array<Parameter>? = null,
                   @Json(ignored = true) val function: suspend (CommandInfos, (CommandResult) -> Unit, () -> Boolean) -> Unit): Command {

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