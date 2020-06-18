package logic.jwt

import models.users.User

interface UsersDataSource {
    fun allowedUsers(): MutableList<User>
}