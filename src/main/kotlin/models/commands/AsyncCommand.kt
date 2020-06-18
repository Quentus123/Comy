package models.commands

import com.beust.klaxon.Json
import models.responses.CommandResult
import models.users.User

class AsyncCommand(override val name: String, override val imageURL: String?, @Json(ignored = true) val function: suspend (User?, (CommandResult) -> Unit, () -> Boolean) -> Unit): Command