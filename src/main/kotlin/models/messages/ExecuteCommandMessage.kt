package models.messages

data class ExecuteCommandMessage(val commandName: String, val token: String? = null, val params: Map<String, Any>): Message(type = type){
    companion object{
        const val type = "ExecuteCommand"
    }
}