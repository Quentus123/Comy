package models.messages

data class ExecuteCommandMessage(val commandName: String): Message(type = type){
    companion object{
        val type = "ExecuteCommand"
    }
}