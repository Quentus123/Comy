import logic.ComyServer
import models.commands.Command
import models.responses.CommandResult

fun main(args: Array<String>){
    val commands = arrayOf(
        Command(name = "Test1", function = {
            return@Command CommandResult.DEFAULT_RESULT
        }),
        Command(name = "Test2", function = {
            return@Command CommandResult.DEFAULT_RESULT
        })
    )
    val server = ComyServer(commands = commands, port = 12478)
    server.start()
}