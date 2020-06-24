package models.commands.params

import com.beust.klaxon.Json

class IntCommandParameter(override val name: String, defaultValue: Int): Parameter {
    override val typeCode: Int = IntCommandParameter.typeCode
    override val defaultValue: String = defaultValue.toString()
    @Json(ignored = true) override val canBeMain: Boolean = true

    companion object {
        const val typeCode: Int = 1
    }
}