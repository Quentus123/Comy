package models.commands.params

import com.beust.klaxon.Json

class IntCommandParameter(override val name: String, defaultValue: Int): Parameter {
    override val typeCode = 1
    override val defaultValue: String = defaultValue.toString()
    @Json(ignored = true) override val canBeMain: Boolean = true
}