package models.responses

data class AuthentificationResponse(val token: String?, val refreshToken: String?, val userId: String?, val message: String, val code: Int, val tokenExpiredError: Boolean, val wrongCredentialsError: Boolean): Response(type = type){
    companion object{
        const val type = "AuthentificationResponse"
    }
}