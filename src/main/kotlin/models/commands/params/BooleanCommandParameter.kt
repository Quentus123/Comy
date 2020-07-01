package models.commands.params

import com.beust.klaxon.Json

/**
 * A boolean paramater.
 *
 * @param name The parameter's name.
 * @param defaultValue The first value that will appear to the client. Note: This value will be converted to string in runtime.
 * @param groupIndex The parameter's group index. User will see parameters grouped by this value, lower value means higher priority.
 */
class BooleanCommandParameter(override val name: String, defaultValue: Boolean, override val groupIndex: Int = 0): Parameter {
    override val typeCode = BooleanCommandParameter.typeCode
    override val defaultValue: String = defaultValue.toString()
    @Json(ignored = true)
    override val canBeMain: Boolean = false

    companion object {
        const val typeCode: Int = 0
    }
}