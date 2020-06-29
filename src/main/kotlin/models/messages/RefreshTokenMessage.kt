package models.messages

data class RefreshTokenMessage(val refreshToken: String): Message(type = type){
    companion object{
        const val type = "RefreshTokenMessage"
    }
}