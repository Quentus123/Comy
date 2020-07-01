package models.users

import kotlin.random.Random

/**
 * Describe how the server manage security.
 *
 * @param isSecured Determine if logging is required.
 * @param secretKey The secret key that will be used to create and verify json web tokens.
 */
data class SecurityConfiguration(
    val isSecured: Boolean,
    val secretKey: String = {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        (1 until 20)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }()
)