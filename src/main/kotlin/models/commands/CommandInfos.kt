package models.commands

import models.users.User


/**
 * All the infos from the command execution.
 *
 * @param user The user that trigerred the command. Null means that user is not logged.
 * @param params The params given by the user.
 */
class CommandInfos(val user: User?, val params: Map<String, Any>)