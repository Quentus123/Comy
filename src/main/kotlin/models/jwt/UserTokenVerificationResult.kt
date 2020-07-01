package models.jwt

import models.users.User

/**
 * The result of an user authentication by username and password.
 *
 * @param user The user. Null if authentication failed.
 * @param tokenExpired true if verification failed because of an expried access token.
 * @param userNotFound true if user is not found in the UsersDataSource
 * @param verificationError true if there is an unexpected error during verification.
 *
 * @see User
 * @see logic.jwt.UsersDataSource
 */
data class UserTokenVerificationResult(val user: User?, val tokenExpired: Boolean, val userNotFound: Boolean, val verificationError: Boolean)