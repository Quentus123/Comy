package dokka.samples

import models.commands.SyncCommand
import models.commands.params.IntCommandParameter
import models.responses.CommandResult
import models.responses.CommandResultStatus
import kotlin.random.Random

class SyncCommandSamples {

    fun createEasiestCommand() {
        val commandThatDoesNothing = SyncCommand(
            name = "An useless command",
            imageURL = null,
            function = {
                CommandResult.DEFAULT_RESULT
            }
        )
    }

    fun createDicesSimulatorCommand() {
        val fakeDiceCommand = SyncCommand(
            name = "Dices simulator",
            imageURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/6sided_dice.jpg/800px-6sided_dice.jpg",
            mainParameter = IntCommandParameter(
                name = "Number of dices",
                defaultValue = 1
            ),
            function = { infos ->
                if (infos.params["Number of dices"] != null) { //Check if number of dices parameter is given, if yes it is an Int
                    val numberOfDices = infos.params["Number of dices"] as Int // Cast Any to Int, we already check that parameter is not null so this cast will not fail
                    if (numberOfDices <= 0) { // We can't simulate zero or less dice!
                        CommandResult(message = "", status = CommandResultStatus(success = false, message = "Number of dices must be positive"))
                    } else { //Everything is ok, just simule dices and send result to user
                        val dices = mutableListOf<Int>()
                        for (i in 0 until numberOfDices) {
                            dices.add(Random.nextInt(from = 1, until = 7))
                        }
                        var dicesString = "Dice${if (dices.count() > 1) "s" else ""} ${if (dices.count() > 1) "are " else "is "}"
                        for (dice in dices) {
                            dicesString += "$dice "
                        }
                        CommandResult(message = dicesString, status = CommandResultStatus.DEFAULT_SUCCESS)
                    }
                } else {
                    CommandResult(message = "", status = CommandResultStatus(success = false, message = "Parameter missing"))
                }
            })
    }

}