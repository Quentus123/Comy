![logo](https://github.com/Quentus123/Comy/blob/master/assets/logo_readme.png?raw=true)

Comy is a library that make server creation easier, a mobile application is provided with the server.

# Features 

- [x] No need to create an user interface thanks to Comy iOS, an application designated for your users !
- [x] Create commands that can be trigerred from users.
- [x] Let users to pass parameters to commands
- [x] User authentification

# In the future

- [x] Create a chat server with channels, etc...
- [x] Send notifications to users
- [x] Create permissions groups of users (Admins, Members, ...)

# Install
```
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation "com.github.quentus123:comy:0.0.2"
}
```

# Usage

## Minimal stuff

The next sample shows how to create a server with a sample command (that print "Command triggered" in the console) :

```
val sampleCommand = SyncCommand(
    name = "https://store-images.microsoft.com/image/apps.11723.9007199267201339.ce269f09-e058-4417-b4f9-f520497476f5.39a5fe32-13c6-4157-ac15-46df8946de29?mode=scale&q=90&h=300&w=300",
    imageURL = null,
    function = {
        println("Command triggered")
        CommandResult.DEFAULT_RESULT //Send to client default success response
    }
)

val server = ComyServer(
    name = "Sample server name",
    commands = arrayOf(sampleCommand),
    port = 14279
)

server.start()
```

And connected users will see :

![minimalStuffSample](https://github.com/Quentus123/Comy/blob/master/assets/readme/minimal_stuff_sample.png?raw=true)

## Customize responses

You can send custom response to users, to do this you need to return a custom CommandResult object in the SyncCommand's "function" lambda parameter :

```
val sampleCommand = SyncCommand(
        name = "A sample command",
        imageURL = "https://store-images.microsoft.com/image/apps.11723.9007199267201339.ce269f09-e058-4417-b4f9-f520497476f5.39a5fe32-13c6-4157-ac15-46df8946de29?mode=scale&q=90&h=300&w=300",
        function = {
            println("Command triggered")
            CommandResult(
                message = "Hey! What an amazing day!",
                status = CommandResultStatus(
                    success = true,
                    message = ""
                )
            )
        }
    )
```

![custom_success_response](https://github.com/Quentus123/Comy/blob/master/assets/readme/custom_success_response.png?raw=true)

You can also send error to user by setting `success` to `false` in `CommandResultStatus` :

```
val sampleCommand = SyncCommand(
        name = "A sample command",
        imageURL = "https://store-images.microsoft.com/image/apps.11723.9007199267201339.ce269f09-e058-4417-b4f9-f520497476f5.39a5fe32-13c6-4157-ac15-46df8946de29?mode=scale&q=90&h=300&w=300",
        function = {
            println("Command triggered")
            CommandResult(
                message = "",
                status = CommandResultStatus(
                    success = false,
                    message = "Oh no! An error occurred :("
                )
            )
        }
    )
```

In this case, user will see the status' message :

![custom_error_response](https://github.com/Quentus123/Comy/blob/master/assets/readme/custom_error_response.png?raw=true)

## Add parameters to commands

There is two types of parameters : the main parameter and secondaries parameters. Let's create a command that will simule dices, user will have the choice of how many dices will be simulated :

```
//...

val diceSimulator = SyncCommand(
        name = "Dice simulator",
        imageURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/6sided_dice.jpg/800px-6sided_dice.jpg",
        mainParameter = IntCommandParameter(
            name = "Number of dices",
            defaultValue = 1
        ),
        function = { commandInfos ->
            val numberOfDices = commandInfos.params["Number of dices"] as Int

            //Check if numberOfDices is > 0 else send to user an error
            if (numberOfDices > 0){
                val dices = mutableListOf<Int>()
                (0 until numberOfDices).forEach { _ ->
                    dices.add(Random.nextInt(from = 1, until = 7))
                }
                CommandResult(
                    message = dices.joinToString { it.toString() },
                    status = CommandResultStatus.DEFAULT_SUCCESS
                )
            } else {
                CommandResult(
                    message = "",
                    status = CommandResultStatus(
                        success = false,
                        message = "Number of dices must be > 0"
                    )
                )
            }
        }
    )
    
val server = ComyServer(
        name = "Sample server name",
        commands = arrayOf(sampleCommand, diceSimulator),
        port = 14278
    )

server.start()
```

![main_parameter](https://github.com/Quentus123/Comy/blob/master/assets/readme/main_parameter.png?raw=true)

Note : You can only add integers parameters as main parameters.

Now if we want to add more params, we can do as following :
    

```
val diceSimulator = SyncCommand(
        name = "Dice simulator",
        imageURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/6sided_dice.jpg/800px-6sided_dice.jpg",
        mainParameter = IntCommandParameter(
            name = "Number of dices",
            defaultValue = 1
        ),
        secondariesParameters = arrayOf(
            IntCommandParameter(
                name ="Min value",
                defaultValue = 1
            ),
            IntCommandParameter(
                name = "Max value",
                defaultValue = 6
            ),
            BooleanCommandParameter(
                name = "An useless boolean parameter",
                defaultValue = true
            )
        ),
        function = { commandInfos ->
            val numberOfDices = commandInfos.params["Number of dices"] as Int
            val minValue = commandInfos.params["Min value"] as Int
            val maxValue = commandInfos.params["Max value"] as Int
            val uselessParameter = commandInfos.params["An useless boolean parameter"] as Boolean

            //IMPORTANT : don't forget to check if 0 < minValue < maxValue
            //...

            //Check if numberOfDices is > 0 else send to user an error
            if (numberOfDices > 0){
                val dices = mutableListOf<Int>()
                (0 until numberOfDices).forEach { _ ->
                    dices.add(Random.nextInt(from = minValue, until = maxValue))
                }
                CommandResult(
                    message = dices.joinToString { it.toString() },
                    status = CommandResultStatus.DEFAULT_SUCCESS
                )
            } else {
                CommandResult(
                    message = "",
                    status = CommandResultStatus(
                        success = false,
                        message = "Number of dices must be > 0"
                    )
                )
            }
        }
    )
```

![secondaries_parameters](https://github.com/Quentus123/Comy/blob/master/assets/readme/secondaries_parameters.png?raw=true)

# Mobile App
Open Source iOS app is currently under devloppement, you can see code here : https://github.com/Quentus123/Comy-iOS

# Documention
You can see documentation here : https://quentus123.github.io/Comy/
