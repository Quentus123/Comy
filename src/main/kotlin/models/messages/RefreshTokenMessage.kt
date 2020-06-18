package models.messages

data class RefreshTokenMessage(val refreshToken: String): Message(type = type){
    companion object{
        val type = "RefreshTokenMessage"
    }
}