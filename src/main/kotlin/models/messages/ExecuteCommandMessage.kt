package models.messages

data class ExecuteCommandMessage(val commandName: String, val token: String? = null): Message(type = type){
    companion object{
        val type = "ExecuteCommand"
    }
}