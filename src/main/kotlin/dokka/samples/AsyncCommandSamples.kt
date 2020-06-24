package dokka.samples

import models.commands.AsyncCommand
import models.responses.CommandResult

class AsyncCommandSamples {

    fun createAnnoyingComputationCommand() {
        val annoyingComputation = AsyncCommand(
            name = "An useless computation",
            imageURL = null,
            function = { infos, completion, isActive ->
                for (i in 0 until 10000) {
                    for (j in 0 until 10000) {
                        println(i + j)
                        if (!isActive()) { //Command timed out, error is automatically send to user
                            break
                        }
                    }
                }

                if (isActive()) { //Computation finished before timeout, send a success message to user
                    completion(CommandResult.DEFAULT_RESULT)
                }
            }
        )
    }

}