package models.commands.params

interface Parameter {
    val name: String
    val typeCode: Int
    val defaultValue: Any
    val canBeMain: Boolean
}