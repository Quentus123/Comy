package logic

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.*
import logic.jwt.JWTServices
import logic.jwt.UsersDataSource
import models.commands.AsyncCommand
import models.commands.Command
import models.commands.CommandInfos
import models.commands.SyncCommand
import models.commands.params.IntCommandParameter
import models.commands.params.Parameter
import models.jwt.UserTokenVerificationResult
import models.messages.*
import models.responses.*
import models.security.SecurityGroup
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
                 val securityConfiguration: SecurityConfiguration = SecurityConfiguration(isSecured = false), port: Int) : WebSocketServer(InetSocketAddress(port)), UsersDataSource {

    private val jwtServices: JWTServices
    private val allowedUsers: MutableList<User> = mutableListOf()
    private val securityGroups: MutableList<SecurityGroup> = mutableListOf()

    init {
        require(assertNoDuplicate())
        jwtServices = JWTServices(secret = securityConfiguration.secretKey, dataSource = this)
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
                    val parsedMessage = Klaxon().parse<NeedStateMessage>(message)
                    if (parsedMessage != null){
                        sendState(conn = conn, token = parsedMessage.token)
                    }
                }
                ExecuteCommandMessage.type -> {
                    val parsedMessage = Klaxon().parse<ExecuteCommandMessage>(message)
                    if(parsedMessage != null){
                        executeCommand(named = parsedMessage.commandName, token = parsedMessage.token, params = parsedMessage.params, conn = conn)
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
        sendState(conn = null, token = null)
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
                userId = authentificationTokenResult.user.username,
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

    private fun authorize(token: String?, securityGroups: Array<SecurityGroup>): Pair<Boolean, UserTokenVerificationResult?> {
        var authResult: UserTokenVerificationResult? = null
        if(token != null) {
            authResult = jwtServices.verifyUserToken(token)
        }

        var checkPermission = securityGroups.count() == 0
        if (!checkPermission && authResult?.user?.securityGroup != null) {
            for (group in securityGroups) {
                checkPermission = checkRecursivelyPermissions(userSecurityGroup = authResult.user!!.securityGroup!!, targetSecurityGroup = group)
                if(checkPermission) { break }
            }
        }

        return if(securityConfiguration.isSecured && (authResult?.user == null || !checkPermission)) { //unauthorized
            false to authResult
        } else {
            true to authResult
        }
    }

    /**
     * Check is user has at least the same permissions of target security group.
     *
     * In fact this function simply check if user's security group is the target security group or one of the super group.
     *
     * @param userSecurityGroup user's security group.
     * @param targetSecurityGroup the security group we want to check if user has permissions.
     */
    private fun checkRecursivelyPermissions(userSecurityGroup: SecurityGroup, targetSecurityGroup: SecurityGroup): Boolean {
        return if (userSecurityGroup.name == targetSecurityGroup.name) {
            true
        } else {
            if (targetSecurityGroup.superGroup != null) {
                checkRecursivelyPermissions(userSecurityGroup = userSecurityGroup, targetSecurityGroup = targetSecurityGroup.superGroup)
            } else {
                false
            }
        }
    }

    private fun refreshToken(conn: WebSocket?, refreshToken: String) {
        val newToken = jwtServices.refreshToken(refreshToken)
        if(newToken != null) {
            conn?.send(Klaxon().toJsonString(AuthentificationResponse(
                    token = newToken,
                    refreshToken = null,
                    userId = null,
                    message = "refreshed token",
                    code = 200,
                    tokenExpiredError = false,
                    wrongCredentialsError = false
            )))
        } else {
            conn?.send(Klaxon().toJsonString(AuthentificationResponse(
                token = null,
                refreshToken = null,
                userId = null,
                message = "error with refresh token",
                code = 401,
                tokenExpiredError = false,
                wrongCredentialsError = false
            )))
        }
    }

    private fun executeCommand(named: String, token: String?, params: Map<String, Any>, conn: WebSocket?){
        if(!commands.map { it.name }.contains(element = named)){
            val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Command named \"$named\" not found"))
            val response = CommandResponse(commandName = named, result = result, authError = null)
            conn?.send(Klaxon().toJsonString(response))
            return
        }
        val command = commands.first { it.name == named }
        GlobalScope.launch {

            //Check if client is allowed
            val authorizeResult = authorize(token, command.securityGroups)
            if (!authorizeResult.first) {
                val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Permissions missing"))
                val response = CommandResponse(commandName = named, result = result, authError = AuthentificationResponse(
                        token = token,
                        refreshToken = null,
                        userId = null,
                        message = "Unauthorized",
                        code = 401,
                        tokenExpiredError = authorizeResult.second?.tokenExpired ?: false,
                        wrongCredentialsError = false
                ))
                conn?.send(Klaxon().toJsonString(response))
            }

            //Execute the command
            //We need to check and convert params types
            val commandParams = mutableListOf<Parameter>()
            if (command.mainParameter != null) commandParams.add(command.mainParameter!!)
            commandParams.addAll(command.secondariesParameters ?: arrayOf())
            val convertedParams: MutableMap<String, Any> = mutableMapOf()
            for (param in params) {
                val correspondingType = commandParams.firstOrNull { it.name == param.key }?.typeCode
                if (correspondingType == null) {
                    val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Parameter error"))
                    val response = CommandResponse(commandName = named, result = result, authError = null)
                    conn?.send(Klaxon().toJsonString(response))
                    return@launch
                } else {
                    val rightTypeParam: Any? = when (correspondingType) {
                        IntCommandParameter.typeCode -> (param.value as? String)?.toIntOrNull()
                        else -> null
                    }
                    if (rightTypeParam == null) {
                        val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Parameter type error"))
                        val response = CommandResponse(commandName = named, result = result, authError = null)
                        conn?.send(Klaxon().toJsonString(response))
                        return@launch
                    } else {
                        convertedParams[param.key] = rightTypeParam
                    }
                }
            }
            val infos = CommandInfos(
                    user = authorizeResult.second?.user,
                    params = convertedParams
            )
            if (command is SyncCommand){
                val result = command.function(infos)
                val response = CommandResponse(commandName = named, result = result, authError = null)
                conn?.send(Klaxon().toJsonString(response))
            } else if (command is AsyncCommand){
                var executionSuccessful = false
                var isTimeout = false
                val timer = Timer("Timeout async command", false).schedule(timeout) {
                    isTimeout = true
                    if (!executionSuccessful){
                        val result = CommandResult(message = "", status = CommandResultStatus(success = false, message = "Command timed out"))
                        val response = CommandResponse(commandName = named, result = result, authError = null)
                        conn?.send(Klaxon().toJsonString(response))
                    }
                }

                command.function(infos,
                    {
                    executionSuccessful = true
                    if(!isTimeout) {
                        timer.cancel()
                        val result = it
                        val response = CommandResponse(commandName = named, result = result, authError = null)
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
        val response = CommandResponse(commandName = "", result = result, authError = null)
        conn?.send(Klaxon().toJsonString(response))
    }

    private fun sendState(conn: WebSocket?, token: String?){

        val authorizedCommands: MutableList<Command> = mutableListOf()
        var isTokenExpirated = false
        for (command in commands) {
            val authorizeResult = authorize(token, securityGroups = command.securityGroups)
            if (authorizeResult.second?.tokenExpired == true) {
                isTokenExpirated = true
            }
            if (authorizeResult.first) {
                authorizedCommands.add(command)
            }
        }

        if (authorizedCommands.count() == 0) {
            val stateResponse = ServerStateResponse(name = name, commands = arrayOf(), authError = AuthentificationResponse(
                    token = token,
                    refreshToken = null,
                    userId = null,
                    message = "Unauthorized",
                    code = 401,
                    tokenExpiredError = isTokenExpirated,
                    wrongCredentialsError = false
            ))
            conn?.send(Klaxon().toJsonString(stateResponse))
        } else {
            val stateResponse = ServerStateResponse(name = name, commands = authorizedCommands.toTypedArray(), authError = null)
            val json = Klaxon().toJsonString(stateResponse)
            conn?.send(json)
        }

    }

    override fun allowedUsers(): MutableList<User> = allowedUsers

    /**
     * Will register a security group and recursively every super group to the server.
     *
     * @param securityGroup The security group.
     */
    fun registerSecurityGroup(securityGroup: SecurityGroup) {
        if (!securityGroups.map { it.name }.contains(securityGroup.name)) {
            securityGroups.add(securityGroup)
            if (securityGroup.superGroup != null) {
                registerSecurityGroup(securityGroup = securityGroup.superGroup)
            }
        }
    }

    /**
     * Will ad an user to the server, will check if the user's security group is already registered to the server, if not throw an error.
     *
     * @param user The user.
     */
    fun addUser(user: User) {
        if (user.securityGroup == null ||(securityGroups.map { it.name }.contains(user.securityGroup.name)) ) {
            if (!allowedUsers.map { it.username }.contains(user.username)) {
                allowedUsers.add(user)
            } else {
                throw  Exception("\"User named ${user.username} already registered.\"")
            }
        } else {
            throw Exception("User's security group named ${user.securityGroup?.name} not registered.")
        }
    }
}