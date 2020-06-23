package models.commands

import models.commands.params.Parameter

interface Command {
    val name: String
    val imageURL: String?
    val mainParameter: Parameter?
    val secondariesParameters: Array<Parameter>?
}