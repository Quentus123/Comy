package logic

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.*
import logic.jwt.JWTServices
import logic.jwt.UsersDataSource
import models.commands.AsyncCommand
import models.commands.Command
import models.commands.SyncCommand
import models.messages.*
import models.responses.*
import models.users.SecurityConfiguration
import models.users.User
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.schedule

class ComyServer(val name: String,
                 var commands: Array<Command>,
                 val timeout: Long = 15000L,
                 val securityConfiguration: SecurityConfiguration = SecurityConfiguration(isSecured = false, usersAllowed = mutableListOf()), port: Int) : WebSocketServer(InetSocketAddress(port)), UsersDataSource {

    private val jwtServices: JWTServices

    init {
        require(assertNoDuplicate())
        jwtServices = JWTServices(secret = "AWonderfulFrenchWordIsRaclette", dataSource = this)
    }

    override fun onStart() {
        println("Server started on port ${this.port}")
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        conn?.send(Klaxon().toJsonString(ServerInfoResponse(
            serverName = name,
            isSecured = securityConfiguration.isSecured
        )))
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
                        executeCommand(named = parsedMessage.commandName, token = parsedMessage.token, conn = conn)
                    }
                }
                AuthentificateUserMessage.type -> {
                    val parsedMessage = Klaxon().parse<AuthentificateUserMessage>(message)
                    if(parsedMessage != null){
                        authentificate(conn = conn, id = parsedMessage.id, password = parsedMessage.password)
                    }
                }
                AuthentificateTokenMessage.type -> {
                    val parsedMessage = Klaxon().parse<AuthentificateTokenMessage>(message)
                    if(parsedMessage != null){
                        authentificate(conn = conn, token = parsedMessage.token)
                    }
                }
                RefreshTokenMessage.type -> {
                    val parsedMessage = Klaxon().parse<RefreshTokenMessage>(message)
                    if(parsedMessage != null){
                        refreshToken(conn = conn, refreshToken = parsedMessage.refreshToken)
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

    private fun authentificate(conn: WebSocket?, id: String, password: String) {
        val authentificationResult = jwtServices.authentificateUser(id = id, password = password)
        if (authentificationResult.token != null && authentificationResult.refreshToken != null && authentificationResult.userId != null) {
            conn?.send(Klaxon().toJsonString(AuthentificationResponse(
                token = authentificationResult.token,
                refreshToken = authentificationResult.refreshToken,
                userId = authentificationResult.userId,
                message = "Successfully authentificated",
                code = 200,
                tokenExpiredError = false,
                wrongCredentialsError = false
            )))
        } else {
            conn?.send(Klaxon().toJsonString(AuthentificationResponse(
                token = null,
                refreshToken = null,
                userId = null,
                message = "Wrong credentials",
                code = 401,
                tokenExpiredError = false,
                wrongCredentialsError = true
            )))
        }
    }

    private fun authentificate(conn: WebSocket?, token: String) {
        val authentificationTokenResult = jwtServices.verifyUserToken(token)
        if (authentificationTokenResult.user != null) {
            conn?.send(Klaxon().toJsonString(AuthentificationResponse(
                token = null,
                refreshToken = null,
                userId = authentificationTokenResult.user.id,
                message = "Successfully authentificated",
                code = 200,
                tokenExpiredError = false,
                wrongCredentialsError = false
            )))
        } else {
            conn?.send(Klaxon().toJsonString(AuthentificationResponse(
                token = null,
                refreshToken = null,
                userId = null,
                message = "Error with token",
                code = 401,
                tokenExpiredError = authentificationTokenResult.tokenExpired,
                wrongCredentialsError = false
            )))
        }
    }

    private fun refreshToken(conn: WebSocket?, refreshToken: String) {
        val newToken = jwtServices.refreshToken(refreshToken)
        if(newToken != null) {
            authentificate(conn = conn, token = newToken)
        } else {
            conn?.send(Klaxon().toJsonString(AuthentificationResponse(
                token = null,
                refreshToken = null,
                userId = null,
                message = "Error with refresh token",
                code = 401,
                tokenExpiredError = false,
                wrongCredentialsError = false
            )))
        }
    }

    private fun authorize(token: String): User? {
        val user = jwtServices.verifyUserToken(token)
        return user.user
    }

    private fun executeCommand(named: String, token: String?, conn: WebSocket?){
        if(!commands.map { it.name }.contains(element = named)){
            val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Command named \"$named\" not found"))
            val response = CommandResponse(commandName = named, result = result)
            conn?.send(Klaxon().toJsonString(response))
            return
        }
        val command = commands.first { it.name == named }
        GlobalScope.launch {

            //First, check if client is auth (or server is not secured)
            var user: User? = null
            if(token != null) {
                user = authorize(token)
            }

            if(securityConfiguration.isSecured && user == null) { //unauthorized to execute command
                val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Permissions missing"))
                val response = CommandResponse(commandName = named, result = result)
                conn?.send(Klaxon().toJsonString(response))
                return@launch
            }

            //Execute the command
            if (command is SyncCommand){
                val result = command.function(user)
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

                command.function(user,
                    {
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

    override fun allowedUsers(): MutableList<User> = securityConfiguration.usersAllowed
}