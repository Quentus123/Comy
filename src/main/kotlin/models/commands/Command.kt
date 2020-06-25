package models.commands

import models.commands.params.Parameter
import models.security.SecurityGroup

interface Command {
    val name: String
    val imageURL: String?
    val mainParameter: Parameter?
    val secondariesParameters: Array<Parameter>
    val securityGroups: Array<SecurityGroup>
}