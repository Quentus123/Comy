package models.commands.params

import com.beust.klaxon.Json

interface Parameter {
    val name: String
    val typeCode: Int
    val defaultValue: Any
    val canBeMain: Boolean
}