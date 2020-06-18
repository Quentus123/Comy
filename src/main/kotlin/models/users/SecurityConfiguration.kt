package models.users

data class SecurityConfiguration(
    val isSecured: Boolean,
    val usersAllowed: MutableList<User>
)