import logic.ComyServer
import models.commands.Command
import models.responses.CommandResult
import models.responses.CommandResultStatus
import kotlin.random.Random

fun main(args: Array<String>){
    val fakeDiceCommand = Command(name = "Fake dice", function = {
        return@Command CommandResult(result = "Dice is ${Random.nextInt(from = 1, until = 7)}", status = CommandResultStatus.DEFAULT_SUCCESS)
    })
    val test1Command = Command(name = "Test1", function = {
        return@Command CommandResult.DEFAULT_RESULT
    })
    val test2Command = Command(name = "Test2", function = {
        return@Command CommandResult.DEFAULT_RESULT
    })

    val server = ComyServer(commands = arrayOf(fakeDiceCommand, test1Command, test2Command), port = 12478)
    server.start()
}