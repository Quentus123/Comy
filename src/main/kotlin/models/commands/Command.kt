package models.commands

import models.commands.params.Parameter
import models.security.SecurityGroup

/**
 * Describe a command.
 *
 * @see SyncCommand
 * @see AsyncCommand
 */
interface Command {
    val name: String
    val imageURL: String?
    val mainParameter: Parameter?
    val secondariesParameters: Array<Parameter>
    val securityGroups: Array<SecurityGroup>
}