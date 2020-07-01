package models.jwt

/**
 * The result of an user authentication by username and password.
 *
 * @param token The user's access token. Null if authentication failed.
 * @param refreshToken The user's refresh token. Null if Authentication failed.
 * @param username The user's username. Null if authentication failed.
 * @param wrongLoginPassword true if username and password given are not valid, false else.
 */
class UserAuthenticationResult (val token: String?, val refreshToken: String?, val username: String?, val wrongLoginPassword: Boolean)