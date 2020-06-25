package models.security

/**
 * Represent a permissions group.
 *
 * Each command will be linked to a group, every user associated to the group or a super group in the tree will be allowed to execute the command.
 *
 * @param name Name of the group, MUST be unique, if not, Comy will throw an error.
 * @param superGroup Super group of the group. The super group will have at least the same permissions than the current group.
 */
data class SecurityGroup(val name: String, val superGroup: SecurityGroup?)