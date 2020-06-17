package logic

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.*
import models.commands.AsyncCommand
import models.commands.Command
import models.commands.SyncCommand
import models.messages.ExecuteCommandMessage
import models.messages.Message
import models.messages.NeedStateMessage
import models.responses.CommandResponse
import models.responses.CommandResult
import models.responses.CommandResultStatus
import models.responses.ServerStateResponse
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.schedule

class ComyServer(val name: String, var commands: Array<Command>, val timeout: Long = 15000L, port: Int) : WebSocketServer(InetSocketAddress(port)) {

    init {
        require(assertNoDuplicate())
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
        println(ex)
    }

    fun changeCommands(commands: Array<Command>) {
        this.commands = commands
        require(assertNoDuplicate())
        sendState(conn = null)
    }

    private fun assertNoDuplicate(): Boolean {
        val commandsNames = commands.map { it.name }
        val commandsNamesDistinct = commandsNames.distinct()

        return commandsNames.count() == commandsNamesDistinct.count()
    }

    private fun executeCommand(named: String, conn: WebSocket?){
        if(!commands.map { it.name }.contains(element = named)){
            val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Command named \"$named\" not found"))
            val response = CommandResponse(commandName = named, result = result)
            conn?.send(Klaxon().toJsonString(response))
            return
        }
        val command = commands.first { it.name == named }
        GlobalScope.launch {

            if (command is SyncCommand){
                val result = command.function()
                val response = CommandResponse(commandName = named, result = result)
                conn?.send(Klaxon().toJsonString(response))
            } else if (command is AsyncCommand){
                var executionSuccessful = false
                var isTimeout = false
                val timer = Timer("Timeout async command", false).schedule(timeout) {
                    isTimeout = true
                    if (!executionSuccessful){
                        val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Command timed out"))
                        val response = CommandResponse(commandName = named, result = result)
                        conn?.send(Klaxon().toJsonString(response))
                    }
                }

                command.function({
                    executionSuccessful = true
                    if(!isTimeout) {
                        timer.cancel()
                        val result = it
                        val response = CommandResponse(commandName = named, result = result)
                        conn?.send(Klaxon().toJsonString(response))
                    }
                }, {
                    isTimeout
                })
            }
        }
    }

    private fun sendUnexpectedError(conn: WebSocket?){
        val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Unexpected error"))
        val response = CommandResponse(commandName = "", result = result)
        conn?.send(Klaxon().toJsonString(response))
    }

    private fun sendState(conn: WebSocket?){
        val stateResponse = ServerStateResponse(name = name, commands = commands)
        val json = Klaxon().toJsonString(stateResponse)
        conn?.send(json)
        if (conn == null){
            broadcast(json)
        }
    }
}