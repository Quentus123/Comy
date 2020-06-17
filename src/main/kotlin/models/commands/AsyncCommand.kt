package models.commands

import com.beust.klaxon.Json
import models.responses.CommandResult

class AsyncCommand(override val name: String, override val imageURL: String?, @Json(ignored = true) val function: suspend ((CommandResult) -> Unit, () -> Boolean) -> Unit): Command