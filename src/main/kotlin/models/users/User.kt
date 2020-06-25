package models.users

import models.security.SecurityGroup

/**
 * Represent an user of the server.
 *
 * @param username The user's username.
 * @param password The user's password.
 * @param securityGroup The security group of the user, nullable. If securityGroup is null, user will have the same permissions than an not authenticated user.
 * @param refreshKey Will be used by Comy to provide new access token after expiration. You should not manipulate this property.
 */
data class User(val username: String, val password: String, val securityGroup: SecurityGroup?, var refreshKey: String? = null)