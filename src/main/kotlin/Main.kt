import logic.ComyServer
import models.commands.AsyncCommand
import models.commands.SyncCommand
import models.commands.params.BooleanCommandParameter
import models.commands.params.IntCommandParameter
import models.responses.CommandResult
import models.responses.CommandResultStatus
import models.security.SecurityGroup
import models.users.SecurityConfiguration
import models.users.User
import kotlin.math.max
import kotlin.random.Random

fun main() {

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
                    ),
                    BooleanCommandParameter(
                            name = "Pip dices with max value",
                            groupIndex = 1,
                            defaultValue = false
                    )
            ),
            function = { infos ->
                println(infos.params)
                val numberOfDices = infos.params["Number of dices"] as Int
                val min = infos.params["Minimum value (included)"] as Int
                val max = infos.params["Maximum value (included)"] as Int
                val isPiped = infos.params["Pip dices with max value"] as Boolean
                if (numberOfDices <= 0) {
                    CommandResult(message = "", status = CommandResultStatus(success = false, message = "Number of dices must be positive"))
                } else if (min > max) {
                    CommandResult(message = "", status = CommandResultStatus(success = false, message = "Maximum value must be greater or equal than minimum value"))
                } else {
                    val dices = mutableListOf<Int>()
                    for (i in 0 until numberOfDices) {
                        val randomNumber = if (isPiped) max else Random.nextInt(from = min, until = max + 1)
                        dices.add(randomNumber)
                    }
                    var dicesString = "ice${if (dices.count() > 1) "s" else ""} ${if (dices.count() > 1) "are " else "is "}"
                    for (dice in dices) {
                        dicesString += "$dice "
                    }
                    CommandResult(message = "${if (infos.user != null) "Hey ${infos.user.username}, d" else "D"}$dicesString", status = CommandResultStatus.DEFAULT_SUCCESS)
                }
            })

    val turnLightOn = AsyncCommand(
            name = "Turn living room's light on",
            imageURL = "https://file1.pleinevie.fr/var/pleinevie/storage/images/article/toute-la-lumiere-sur-les-ampoules-13054/77288-1-fre-FR/Toute-la-lumiere-sur-les-ampoules.jpg?alias=exact1024x768_l",
            mainParameter = null,
            secondariesParameters = arrayOf(
                    IntCommandParameter(
                            name = "Red value",
                            defaultValue = 255
                    ),
                    IntCommandParameter(
                            name = "Green value",
                            defaultValue = 255
                    ),
                    IntCommandParameter(
                            name = "Blue value",
                            defaultValue = 255
                    )
            ),

            function = { infos, completion, _ ->
                val red = infos.params["Red value"] as Int
                val green = infos.params["Green value"] as Int
                val blue = infos.params["Blue value"] as Int
                if (max(max(red, green), blue) > 255 || red.coerceAtMost(green).coerceAtMost(blue) < 0) {
                    completion(CommandResult(message = "", status = CommandResultStatus(success = false, message = "RGB values must be between 0 and 255")))
                } else {
                    //Call async api to turn lights on and call completion when success
                    //...
                    completion(CommandResult.DEFAULT_RESULT)
                }
            }
    )
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
    ), commands = arrayOf(fakeDiceCommand, turnLightOn, test1Command, test2Command, test3Command), port = 12478)

    server.registerSecurityGroup(securityGroup = memberGroup)
    server.addUser(user = rootUser)
    server.addUser(user = memberUser)
    server.start()
}