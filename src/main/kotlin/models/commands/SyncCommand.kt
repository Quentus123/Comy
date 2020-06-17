package models.commands

import com.beust.klaxon.Json
import models.responses.CommandResult

class SyncCommand(override val name: String, override val imageURL: String?, @Json(ignored = true) val function: suspend () -> CommandResult): Command