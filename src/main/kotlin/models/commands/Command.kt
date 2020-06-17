package models.commands

interface Command {
    val name: String
    val imageURL: String?
}