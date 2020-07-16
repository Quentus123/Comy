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
    implementation "com.github.quentus123:comy:0.0.3"
}
```

# How to use

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

Now if we want to add more params, we can do the following :
    

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

Note : String parameters are available in the library but the UI isn't implemented in iOS app yet.

You can specify how you want parameters to be grouped thanks to optional `groupIndex` property (default value is 0) :

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
                groupIndex = 1,
                defaultValue = 1
            ),
            IntCommandParameter(
                name = "Max value",
                groupIndex = 1,
                defaultValue = 6
            ),
            BooleanCommandParameter(
                name = "An useless boolean parameter",
                groupIndex = 2,
                defaultValue = true
            )
        ),
        function = { commandInfos ->
            //...
    )
```

![group_index_parameters](https://github.com/Quentus123/Comy/blob/master/assets/readme/group_index_parameters.png?raw=true)

## Asynchronous commands

Previous samples showed synchronous commands (which are thread blocking) but you can create asynchronous commands like this :

```
//...

val fakeAPICall = AsyncCommand(
    name = "Fake API call",
    imageURL = "https://lh3.googleusercontent.com/proxy/2fiai5xMJMimLi7OPCsamwNj1ppN-inRsBply5KhZmjPM53pefecruJwH56kLr9aBF7BhFMMVN34uVlEEhMFtu_iElrct-K9uKfBAk8Qu6zJ1dTRBV5ki2Y342RMB1PpSyV3oetbNxN9fN-0Y9ukxWEIgQekut0edVTe",
    function = { commandInfos, completion, isActive ->  
        delay(5000L)
        completion(CommandResult.DEFAULT_RESULT)
    }
)

val server = ComyServer(
        name = "Sample server name",
        commands = arrayOf(sampleCommand, diceSimulator, fakeAPICall),
        port = 14278
    )

    server.start()
```

All asynchronous commands have a timeout delay (which is a `ComyServer` property, default value is 15000 miliseconds). On timeout, an error will be send to user.

Lambda parameter `function` has three parameters :

- `commandInfos`, same has synchronous commands.
- `completion`, a function that you must call when command is completed, pass the result as `CommandResult` object, this result will be send to user if command is not timed out.
- `isActive`, a function that give a boolean. The returned boolean determine if command is timed out (useful to stop long computation, etc...)

# Manage users

You can create users and set permissions. In next samples, we will create two users : an admin and a normal member.

## Create security groups

We need to create permissions groups :

```
val adminsSecurityGroup = SecurityGroup(
    name = "admins",
    superGroup = null
)

val membersSecurityGroup = SecurityGroup(
    name = "members",
    superGroup = adminsSecurityGroup
)
```

This create two groups : admins and members. Members group has admins group has `superGroup`, it means that every command that a member will be allowed to trigger, an admin will be allowed too.

Now we need to register groups :

`server.registerSecurityGroup(membersSecurityGroup)`

This will register `adminsSecurityGroup` too because this is a `membersSecurityGroup`'s `superGroup`. In fact, you only need to register groups which have the least permissions.

## Create user

```
val adminUser = User(
    username = "root",
    password = "password",
    securityGroup = adminsSecurityGroup
)

val memberUser = User(
    username = "member",
    password = "abcd1234",
    securityGroup = membersSecurityGroup
)
```

Here password are hardcoded but feel free to get them from a file or a database.

Now we add users to servers :

```
//...
server.addUser(memberUser)
server.addUser(adminUser)
```

Important : You can't add user if his security group is not already registered!

## Secure server and commands

The complete code :

```
val adminsSecurityGroup = SecurityGroup(
    name = "admins",
    superGroup = null
)

val membersSecurityGroup = SecurityGroup(
    name = "members",
    superGroup = adminsSecurityGroup
)

val adminUser = User(
    username = "root",
    password = "password",
    securityGroup = adminsSecurityGroup
)

val memberUser = User(
    username = "member",
    password = "abcd1234",
    securityGroup = membersSecurityGroup
)

val commandForAdminOnly = SyncCommand(
    name = "Admin commands",
    imageURL = null,
    securityGroups = arrayOf(adminsSecurityGroup),
    function = {
        CommandResult.DEFAULT_RESULT
    }
)

val commandForMembers = SyncCommand(
    name = "Members command",
    imageURL = null,
    securityGroups = arrayOf(membersSecurityGroup),
    function = {
        CommandResult.DEFAULT_RESULT
    }
)

val commandForEveryone = SyncCommand(
    name = "A command that everyone can trigger",
    imageURL = null,
    securityGroups = arrayOf(),
    function = {
        CommandResult.DEFAULT_RESULT
    }
)

val server = ComyServer(
    name = "Sample server name",
    commands = arrayOf(commandForAdminOnly, commandForMembers, commandForEveryone),
    securityConfiguration = SecurityConfiguration(
        isSecured = true
    ),
    port = 14278
)

server.registerSecurityGroup(membersSecurityGroup)
server.addUser(memberUser)
server.addUser(adminUser)

server.start()
```

Don't forget `securityConfiguration` of `ComyServer` constructor and set `isSecured` to `true` else everyone will have max permissions!

# Mobile App
Open Source iOS app is currently under devloppement, you can see code here : https://github.com/Quentus123/Comy-iOS

# Documention
You can see documentation here : https://quentus123.github.io/Comy/
