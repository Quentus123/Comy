package logic

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.*
import logic.jwt.JWTServices
import logic.jwt.UsersDataSource
import models.commands.*
import models.commands.params.BooleanCommandParameter
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

/**
 * Represent the server.
 *
 * @param name The server's name.
 * @param commands The list of commands available. IMPORTANT: Commands must not have duplicates names. If two commands have the same name, an error will occur.
 * @param timeout Optional: Timeout in milliseconds before AsyncCommands return an timeout result. Default value: 15000 = 15 seconds.
 * @param securityConfiguration Optional: Represent how the server must manage security. Default value: No security.
 *
 * @see SyncCommand
 * @see AsyncCommand
 * @see SecurityConfiguration
 */

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
                AuthenticateUserMessage.type -> {
                    val parsedMessage = Klaxon().parse<AuthenticateUserMessage>(message)
                    if(parsedMessage != null){
                        authenticate(conn = conn, username = parsedMessage.username, password = parsedMessage.password)
                    }
                }
                AuthenticateTokenMessage.type -> {
                    val parsedMessage = Klaxon().parse<AuthenticateTokenMessage>(message)
                    if(parsedMessage != null){
                        authenticate(conn = conn, token = parsedMessage.token)
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

    /**
     * Change commands of the server.
     *
     * @param commands The list of commands available. IMPORTANT: Commands must not have duplicates names. If two commands have the same name, an error will occur.
     */
    fun changeCommands(commands: Array<Command>) {
        this.commands = commands
        require(assertNoDuplicate())
        sendState(conn = null, token = null)
    }


    /**
     * Assert that there is no commands with the same name.
     *
     * @return true if there is no duplicate, false else.
     */
    private fun assertNoDuplicate(): Boolean {
        val commandsNames = commands.map { it.name }
        val commandsNamesDistinct = commandsNames.distinct()

        return commandsNames.count() == commandsNamesDistinct.count()
    }


    /**
     * Authenticate user with username and password.
     * Send a success AuthenticationResponse when authentication succeeded with accessToken, refreshToken and user's username or an error AuthenticationResponse on fail.
     *
     * @param conn The client to send response.
     * @param username The user's username.
     * @param password The user's password.
     */
    private fun authenticate(conn: WebSocket?, username: String, password: String) {
        val authenticationResult = jwtServices.authenticateUser(username = username, password = password)
        if (authenticationResult.token != null && authenticationResult.refreshToken != null && authenticationResult.username != null) {
            conn?.send(Klaxon().toJsonString(AuthenticationResponse(
                token = authenticationResult.token,
                refreshToken = authenticationResult.refreshToken,
                username = authenticationResult.username,
                message = "Successfully authentificated",
                code = 200,
                tokenExpiredError = false,
                wrongCredentialsError = false
            )))
        } else {
            conn?.send(Klaxon().toJsonString(AuthenticationResponse(
                token = null,
                refreshToken = null,
                username = null,
                message = "Wrong credentials",
                code = 401,
                tokenExpiredError = false,
                wrongCredentialsError = true
            )))
        }
    }

    /**
     * Authenticate user with accessToken.
     * Send a success AuthenticationResponse when authentication succeeded or an error AuthenticationResponse on fail.
     *
     * @param conn The client to send response.
     * @param token The user's accessToken.
     */
    private fun authenticate(conn: WebSocket?, token: String) {
        val authenticationTokenResult = jwtServices.verifyUserToken(token)
        if (authenticationTokenResult.user != null) {
            conn?.send(Klaxon().toJsonString(AuthenticationResponse(
                token = null,
                refreshToken = null,
                username = authenticationTokenResult.user.username,
                message = "Successfully authentificated",
                code = 200,
                tokenExpiredError = false,
                wrongCredentialsError = false
            )))
        } else {
            conn?.send(Klaxon().toJsonString(AuthenticationResponse(
                token = null,
                refreshToken = null,
                username = null,
                message = "Error with token",
                code = 401,
                tokenExpiredError = authenticationTokenResult.tokenExpired,
                wrongCredentialsError = false
            )))
        }
    }

    /**
     * Verify if user has permissions to continue.
     *
     * @param token user's accessToken, null means that user is not logged.
     * @param securityGroups SecurityGroups allowed to execute the action. Just need to put groups with less permissions. User must be in one of the security group or a super group of them.
     *
     * @return A pair where first value is a boolean that indicates if user has permissions. Second value is the verification result, useful to tell, e.g, if accessToken is expired.
     */
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

    /**
     * Refresh client's accessToken if refreshToken is valid.
     * Send to client an success AuthenticationResponse which contains new accessToken if success. If refreshing fails, send to client an error AuthenticationResponse.
     *
     * @param conn The client to send response.
     * @param refreshToken The client's refresh token.
     */
    private fun refreshToken(conn: WebSocket?, refreshToken: String) {
        val newToken = jwtServices.refreshToken(refreshToken)
        if(newToken != null) {
            conn?.send(Klaxon().toJsonString(AuthenticationResponse(
                    token = newToken,
                    refreshToken = null,
                    username = null,
                    message = "refreshed token",
                    code = 200,
                    tokenExpiredError = false,
                    wrongCredentialsError = false
            )))
        } else {
            conn?.send(Klaxon().toJsonString(AuthenticationResponse(
                token = null,
                refreshToken = null,
                username = null,
                message = "error with refresh token",
                code = 401,
                tokenExpiredError = false,
                wrongCredentialsError = false
            )))
        }
    }

    /**
     * Execute a command for a client and send him the result.
     *
     * @param named The command's name.
     * @param token The client's accessToken.
     * @param params Command's params.
     * @param conn The client to send result as CommandReponse.
     */
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
                val response = CommandResponse(commandName = named, result = result, authError = AuthenticationResponse(
                        token = token,
                        refreshToken = null,
                        username = null,
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
            commandParams.addAll(command.secondariesParameters)
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
                        BooleanCommandParameter.typeCode -> (param.value as? String)?.toBoolean()
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

    /**
     * Send server's state to client or an error if client is not allowed.
     *
     * @param conn The client to send response.
     * @param token The client's accessToken.
     */
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
            val stateResponse = ServerStateResponse(name = name, commands = arrayOf(), authError = AuthenticationResponse(
                    token = token,
                    refreshToken = null,
                    username = null,
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

    /**
     * @return A list of allowed users.
     */
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
            throw Exception("User's security group named ${user.securityGroup.name} not registered.")
        }
    }
}