package models.users

import kotlin.random.Random

data class SecurityConfiguration(
    val isSecured: Boolean,
    val secretKey: String = {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        (1 until 20)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }(),
    val usersAllowed: MutableList<User>
)