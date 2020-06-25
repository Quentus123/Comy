package models.commands

import models.users.User

class CommandInfos(val user: User?, val params: Map<String, Any>)