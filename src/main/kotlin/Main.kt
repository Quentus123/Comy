import logic.ComyServer
import logic.jwt.JWTServices
import models.commands.AsyncCommand
import models.commands.SyncCommand
import models.responses.CommandResult
import models.responses.CommandResultStatus
import models.users.SecurityConfiguration
import models.users.User
import kotlin.random.Random

fun main(args: Array<String>){

    val user = User(id = "xXShadowXx", password = "password", refreshKey = "FWympsbtEjhTlav2WqA")

    val fakeDiceCommand = AsyncCommand(
            name = "Fake dice",
            imageURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/6sided_dice.jpg/800px-6sided_dice.jpg",
            function = { _ , completion, _ ->
                completion(CommandResult(message = "Dice is ${Random.nextInt(from = 1, until = 7)}", status = CommandResultStatus.DEFAULT_SUCCESS))
            })
    val test1Command = SyncCommand(
            name = "Test1",
            imageURL = null,
            function = {
                return@SyncCommand CommandResult.DEFAULT_RESULT
            })
    val test2Command = SyncCommand(
            name = "Test2",
            imageURL = null,
            function = {
                return@SyncCommand CommandResult.DEFAULT_RESULT
            })

    val server = ComyServer(name = "Test server", timeout = 3000, commands = arrayOf(fakeDiceCommand, test1Command, test2Command), port = 12478)
    server.start()
}