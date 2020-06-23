package models.commands.params

import com.beust.klaxon.Json

data class StringCommandParameter(override val name: String, override val defaultValue: String): Parameter {
    override val typeCode = 2
    @Json(ignored = true) override val canBeMain: Boolean = false
}