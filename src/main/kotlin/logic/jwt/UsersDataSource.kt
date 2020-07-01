package logic.jwt

import models.users.User

/**
 * Describe a class' ability to provide users.
 */
interface UsersDataSource {
    fun allowedUsers(): MutableList<User>
}