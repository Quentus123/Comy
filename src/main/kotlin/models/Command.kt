package models

class Command(val name: String, val function: () -> CommandResult)