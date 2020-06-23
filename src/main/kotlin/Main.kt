import logic.ComyServer
import models.commands.SyncCommand
import models.commands.params.IntCommandParameter
import models.responses.CommandResult
import models.responses.CommandResultStatus
import models.users.User
import kotlin.random.Random

fun main(args: Array<String>){

    val user = User(id = "xXShadowXx", password = "password", refreshKey = "FWympsbtEjhTlav2WqA")

    val fakeDiceCommand = SyncCommand(
            name = "Fake dice",
            imageURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/6sided_dice.jpg/800px-6sided_dice.jpg",
            mainParameter = IntCommandParameter(
                    name = "Number of dices",
                    defaultValue = 1
            ),
            function = { infos ->
                if (infos.params.containsKey("Number of dices") && (infos.params["Number of dices"] is String) && (infos.params["Number of dices"] as String).toIntOrNull() != null) {
                    val numberOfDices = (infos.params["Number of dices"] as String).toInt()
                    if (numberOfDices <= 0) {
                        return@SyncCommand CommandResult(message = "", status = CommandResultStatus(success = false, message = "Number of dices must be positive"))
                    } else {
                        return@SyncCommand CommandResult(message = "${if (infos.user != null) "Hey ${infos.user.id}, d" else "D"}ice is ${Random.nextInt(from = 1, until = 7)}", status = CommandResultStatus.DEFAULT_SUCCESS)
                    }
                } else {
                    return@SyncCommand CommandResult(message = "", status = CommandResultStatus(success = false, message = "Parameter error"))
                }

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