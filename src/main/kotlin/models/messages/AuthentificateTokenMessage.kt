package models.messages

data class AuthentificateTokenMessage(val token: String): Message(type = type){
    companion object{
        val type = "AuthentificateTokenMessage"
    }
}