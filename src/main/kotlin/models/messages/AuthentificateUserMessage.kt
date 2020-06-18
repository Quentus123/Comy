package models.messages

data class AuthentificateUserMessage(val id: String, val password: String): Message(type = type){
    companion object{
        val type = "AuthentificateUserMessage"
    }
}