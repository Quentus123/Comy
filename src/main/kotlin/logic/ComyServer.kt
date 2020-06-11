package logic

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import models.commands.Command
import models.commands.CommandResult
import models.commands.ResultStatus
import models.messages.ExecuteCommandMessage
import models.messages.Message
import models.messages.NeedStateMessage
import models.responses.ServerStateResponse
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.schedule

class ComyServer(val commands: Array<Command>, val timeout: Long = 15000L, port: Int) : WebSocketServer(InetSocketAddress(port)) {

    init {
        val commandsNames = commands.map { it.name }
        val commandsNamesDistinct = commandsNames.distinct()

        require(commandsNames.count() == commandsNamesDistinct.count()){
            "Duplicates names in commands"
        }
    }

    override fun onStart() {
        println("Server started on port ${this.port}")
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        sendState(conn = conn)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if(message == null || message.count() == 0){
            sendUnexpectedError(conn = conn)
            return
        }

        val messageType = Klaxon().parse<Message>(message)?.type
        if(messageType != null){
            when(messageType){
                NeedStateMessage.type -> {
                    sendState(conn = conn)
                }
                ExecuteCommandMessage.type -> {
                    val parsedMessage = Klaxon().parse<ExecuteCommandMessage>(message)
                    if(parsedMessage != null){
                        executeCommand(named = parsedMessage.commandName, conn = conn)
                    }
                }
                else -> sendUnexpectedError(conn = conn)
            }
        }

    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        TODO("Not yet implemented")
    }

    private fun executeCommand(named: String, conn: WebSocket?){
        if(!commands.map { it.name }.contains(element = named)){
            val result = CommandResult(result = "", status = ResultStatus(success = false, message = "Command named \"$named\" not found"))
            conn?.send(Klaxon().toJsonString(result))
            return
        }

        val timer = Timer("CommandTimedOut", false)
        timer.schedule(timeout) {
            val result = CommandResult(result = "", status = ResultStatus(success = false, message = "Command timed out"))
            conn?.send(Klaxon().toJsonString(result))
        }
        val command = commands.first { it.name == named }
        GlobalScope.launch {
            val result = command.function()
            timer.cancel()
            conn?.send(Klaxon().toJsonString(result))
        }
    }

    private fun sendUnexpectedError(conn: WebSocket?){
        val result = CommandResult(result = "", status = ResultStatus(success = false, message = "Unexpected error"))
        conn?.send(Klaxon().toJsonString(result))
    }

    private fun sendState(conn: WebSocket?){
        val stateResponse = ServerStateResponse(state = commands)
        conn?.send(Klaxon().toJsonString(stateResponse))
    }
}