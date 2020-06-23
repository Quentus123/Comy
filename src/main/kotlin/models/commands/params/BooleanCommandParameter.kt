package models.commands.params

import com.beust.klaxon.Json

class BooleanCommandParameter(override val name: String, defaultValue: Boolean): Parameter {
    override val typeCode = 0
    override val defaultValue: String = defaultValue.toString()
    @Json(ignored = true) override val canBeMain: Boolean = true
}