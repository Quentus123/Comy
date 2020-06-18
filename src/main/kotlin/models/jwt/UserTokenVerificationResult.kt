package models.jwt

import models.users.User

data class UserTokenVerificationResult(val user: User?, val tokenExpired: Boolean, val userNotFound: Boolean, val verificationError: Boolean)