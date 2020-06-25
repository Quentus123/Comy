import logic.ComyServer
import models.commands.SyncCommand
import models.commands.params.IntCommandParameter
import models.responses.CommandResult
import models.responses.CommandResultStatus
import models.security.SecurityGroup
import models.users.SecurityConfiguration
import models.users.User
import kotlin.random.Random

fun main(args: Array<String>){

    val adminGroup = SecurityGroup(
            name = "Admin",
            superGroup = null
    )
    val memberGroup = SecurityGroup(
            name = "Member",
            superGroup = adminGroup
    )
    val rootUser = User(username = "root", password = "password", securityGroup = adminGroup)
    val memberUser = User(username = "member", password = "password", securityGroup = memberGroup)


    val fakeDiceCommand = SyncCommand(
            name = "Dices simulator",
            imageURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/6sided_dice.jpg/800px-6sided_dice.jpg",
            mainParameter = IntCommandParameter(
                    name = "Number of dices",
                    defaultValue = 1
            ),
            secondariesParameters = arrayOf(
                    IntCommandParameter(
                            name = "Minimum value (included)",
                            defaultValue = 1
                    ),
                    IntCommandParameter(
                            name = "Maximum value (included)",
                            defaultValue = 6
                    )
            ),
            function = { infos ->
                if (infos.params["Number of dices"] != null && infos.params["Minimum value (included)"] != null && infos.params["Maximum value (included)"] != null) {
                    println(infos.params)
                    val numberOfDices = infos.params["Number of dices"] as Int
                    val min = infos.params["Minimum value (included)"] as Int
                    val max = infos.params["Maximum value (included)"] as Int
                    if (numberOfDices <= 0) {
                        CommandResult(message = "", status = CommandResultStatus(success = false, message = "Number of dices must be positive"))
                    } else if (min > max) {
                        CommandResult(message = "", status = CommandResultStatus(success = false, message = "Maximum value must be greater or equal than minimum value"))
                    } else {
                        val dices = mutableListOf<Int>()
                        for (i in 0 until numberOfDices) {
                            dices.add(Random.nextInt(from = min, until = max + 1))
                        }
                        var dicesString = "ice${if (dices.count() > 1) "s" else ""} ${if (dices.count() > 1) "are " else "is "}"
                        for (dice in dices) {
                            dicesString += "$dice "
                        }
                        CommandResult(message = "${if (infos.user != null) "Hey ${infos.user.username}, d" else "D"}$dicesString", status = CommandResultStatus.DEFAULT_SUCCESS)
                    }
                } else {
                    CommandResult(message = "", status = CommandResultStatus(success = false, message = "Parameter error"))
                }

            })
    val test1Command = SyncCommand(
            name = "Test1",
            imageURL = null,
            function = {
                return@SyncCommand CommandResult.DEFAULT_RESULT
            })
    val test2Command = SyncCommand(
            name = "Test2 Admin only",
            imageURL = null,
            securityGroups = arrayOf(adminGroup),
            function = {
                return@SyncCommand CommandResult.DEFAULT_RESULT
            })
    val test3Command = SyncCommand(
            name = "Test 3 members",
            imageURL = null,
            securityGroups = arrayOf(memberGroup),
            function = {
                return@SyncCommand CommandResult.DEFAULT_RESULT
            })

    val server = ComyServer(name = "Test server", timeout = 3000, securityConfiguration = SecurityConfiguration(
        isSecured = true
    ), commands = arrayOf(fakeDiceCommand, test1Command, test2Command, test3Command), port = 12476)

    server.registerSecurityGroup(securityGroup = memberGroup)
    server.addUser(user = rootUser)
    server.addUser(user = memberUser)
    server.start()
}