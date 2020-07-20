package logic

import com.beust.klaxon.Klaxon
import io.mockk.*
import models.commands.Command
import models.commands.CommandResult
import models.commands.SyncCommand
import models.messages.*
import org.java_websocket.WebSocket
import org.junit.Test
import java.lang.Exception
import java.lang.IllegalArgumentException

internal class ComyServerTest {

    class MethodCalledException: Exception("Method was called")

    private fun neutralizeServerMethods(sendState: Boolean = false, executeCommand: Boolean = false, authenticateUsername: Boolean = false, authenticateToken: Boolean = false, refreshToken: Boolean = false, sendUnexpectedError: Boolean = false): ComyServer {
        val server = spyk(ComyServer(name = "", commands = arrayOf(), port = 0), recordPrivateCalls = true)
        if (sendState) every { server["sendState"](any<WebSocket>(), any<String>()) } throws MethodCalledException()
        if (executeCommand) every { server["executeCommand"](any<String>(), any<String>(), any<Map<String, String>>(), any<WebSocket>()) } throws MethodCalledException()
        if (authenticateUsername)  every { server["authenticate"](any<WebSocket>(), any<String>(), any<String>()) } throws MethodCalledException()
        if (authenticateToken) every { server["authenticate"](any<WebSocket>(), any<String>()) } throws MethodCalledException()
        if (refreshToken) every { server["refreshToken"](any<WebSocket>(), any<String>()) } throws MethodCalledException()
        if (sendUnexpectedError) every { server["sendUnexpectedError"](any<WebSocket>()) } throws MethodCalledException()
        return server
    }

    @Test
    fun `test changeCommands without duplicate`() {
        //arrange
        val server = spyk(ComyServer(name = "", commands = arrayOf(), port = 0))
        val command1 = SyncCommand(
                name = "dummy command 1",
                imageURL = null,
                function = {
                    CommandResult.DEFAULT_RESULT
                }
        )
        val command2 = SyncCommand(
                name = "dummy command 2",
                imageURL = null,
                function = {
                    CommandResult.DEFAULT_RESULT
                }
        )

        val commands = arrayOf<Command>(command1, command2)

        //act & assert
        try {
            server.changeCommands(commands)
            assert(true)
        } catch (e: IllegalArgumentException) {
            assert(false) { "an error occured while changing commands without duplicates" }
        }

    }

    @Test
    fun `test changeCommands with duplicate`() {
        //arrange
        val server = spyk(ComyServer(name = "", commands = arrayOf(), port = 0))
        val command1 = SyncCommand(
                name = "dummy command",
                imageURL = null,
                function = {
                    CommandResult.DEFAULT_RESULT
                }
        )
        val command2 = SyncCommand(
                name = "dummy command",
                imageURL = null,
                function = {
                    CommandResult.DEFAULT_RESULT
                }
        )

        val commands = arrayOf<Command>(command1, command2)

        //act & assert
        try {
            server.changeCommands(commands)
            assert(false) { "no error when change commands with duplicates" }
        } catch (e: IllegalArgumentException) {
            assert(true)
        }

    }

    @Test
    fun `test onMessage with empty message`() {
        //arrange
        val server = neutralizeServerMethods(
                sendUnexpectedError = true
        )

        //assert
        try {
            server.onMessage(conn = null, message = "")
            assert(false) { "server didn't send unexpected error on empty message" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with null message`() {
        //arrange
        val server = neutralizeServerMethods(
                sendUnexpectedError = true
        )

        //assert
        try {
            server.onMessage(conn = null, message = "")
            assert(false) { "server didn't send unexpected error on null message" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with random message`() {
        //arrange
        val server = neutralizeServerMethods(
                sendUnexpectedError = true
        )

        //assert
        try {
            server.onMessage(conn = null, message = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor.")
            assert(false) { "server didn't send unexpected error on null message" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with invalid json message`() {
        //arrange
        val server = neutralizeServerMethods(
                sendUnexpectedError = true
        )

        //assert
        try {
            server.onMessage(conn = null, message = "{\"id\":1}")
            assert(false) { "server didn't send unexpected error with invalid json message" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with NeedStateMessage without token`() {
        //arrange
        val server = neutralizeServerMethods(
                sendState = true
        )

        val messageWithoutToken = Klaxon().toJsonString(NeedStateMessage())

        //assert
        try {
            server.onMessage(conn = null, message = messageWithoutToken)
            assert(false) { "Need state message without token was not handled" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with NeedStateMessage with token`() {
        //arrange
        val server = neutralizeServerMethods(
                sendState = true
        )

        val messageWithToken = Klaxon().toJsonString(NeedStateMessage(token = "JJFHDHCHZG567485423"))

        //assert
        try {
            server.onMessage(conn = null, message = messageWithToken)
            assert(false) { "Need state message with token was not handled" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with ExecuteCommandMessage without token without params`() {
        //arrange
        val server = neutralizeServerMethods(
                executeCommand = true
        )

        val message = Klaxon().toJsonString(ExecuteCommandMessage(commandName = "", params = mapOf()))

        //assert
        try {
            server.onMessage(conn = null, message = message)
            assert(false) { "Execute command message without token without params was not handled" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with ExecuteCommandMessage with token with params`() {
        //arrange
        val server = neutralizeServerMethods(
                executeCommand = true
        )

        val message = Klaxon().toJsonString(ExecuteCommandMessage(commandName = "", token = "HS5584QQ22", params = mapOf("Hello" to "World", "bar" to "foo")))

        //assert
        try {
            server.onMessage(conn = null, message = message)
            assert(false) { "Execute command message witho token witho params was not handled" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with AuthenticateMessage with username and password`() {
        //arrange
        val server = neutralizeServerMethods(
                authenticateUsername = true
        )

        val message = Klaxon().toJsonString(AuthenticateUserMessage(username = "username", password = "password"))

        //assert
        try {
            server.onMessage(conn = null, message = message)
            assert(false) { "Username/Password authentication message was not handled" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with AuthenticateMessage with token`() {
        //arrange
        val server = neutralizeServerMethods(
                authenticateToken = true
        )

        val message = Klaxon().toJsonString(AuthenticateTokenMessage(token = "jhghhdWDH4554xx"))

        //assert
        try {
            server.onMessage(conn = null, message = message)
            assert(false) { "Token authentication message was not handled" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }

    @Test
    fun `test onMessage with RefreshTokenMessage`() {
        //arrange
        val server = neutralizeServerMethods(
                refreshToken = true
        )

        val message = Klaxon().toJsonString(RefreshTokenMessage(refreshToken = "KJWxsd578"))

        //assert
        try {
            server.onMessage(conn = null, message = message)
            assert(false) { "Refresh token message was not handled" }
        } catch (e: MethodCalledException) {
            assert(true)
        }
    }
}