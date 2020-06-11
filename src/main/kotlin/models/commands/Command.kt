package models.commands

import com.beust.klaxon.Json

class Command(val name: String, @Json(ignored = true) val function: () -> CommandResult)