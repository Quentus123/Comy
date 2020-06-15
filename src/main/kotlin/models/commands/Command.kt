package models.commands

import com.beust.klaxon.Json
import models.responses.CommandResult

class Command(val name: String, val imageURL: String?, @Json(ignored = true) val function: () -> CommandResult)