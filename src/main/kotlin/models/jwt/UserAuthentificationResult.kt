package models.jwt

class UserAuthentificationResult (val token: String?, val refreshToken: String?, val userId: String?, val wrongLoginPassword: Boolean)