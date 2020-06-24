package models.commands.params

import com.beust.klaxon.Json

class BooleanCommandParameter(override val name: String, defaultValue: Boolean): Parameter {
    override val typeCode = BooleanCommandParameter.typeCode
    override val defaultValue: String = defaultValue.toString()
    @Json(ignored = true)
    override val canBeMain: Boolean = false

    companion object {
        const val typeCode: Int = 0
    }
}