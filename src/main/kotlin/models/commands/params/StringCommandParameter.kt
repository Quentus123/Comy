package models.commands.params

import com.beust.klaxon.Json

data class StringCommandParameter(override val name: String, override val defaultValue: String): Parameter {
    override val typeCode = StringCommandParameter.typeCode
    @Json(ignored = true) override val canBeMain: Boolean = false

    companion object {
        const val typeCode: Int = 2
    }
}