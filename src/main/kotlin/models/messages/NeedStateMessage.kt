package models.messages

class NeedStateMessage(val token: String? = null): Message(type = type){
    companion object{
        const val type = "Need refresh"
    }
}