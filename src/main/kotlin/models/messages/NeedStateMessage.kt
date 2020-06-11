package models.messages

class NeedStateMessage: Message(type = type){
    companion object{
        val type = "Need refresh"
    }
}