package com.hexacore.aidaapplications

import com.google.gson.*
import java.lang.reflect.Type

class AlarmItemDeserializer : JsonDeserializer<AlarmItem> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AlarmItem {
        val obj = json!!.asJsonObject

        val time = obj.get("time").asString
        val isEnabled = obj.get("isEnabled").asBoolean

        val repeatDaysElement = obj.get("repeatDays")
        val repeatDays: MutableList<String> = when {
            repeatDaysElement.isJsonArray -> {
                repeatDaysElement.asJsonArray.map { it.asString }.toMutableList()
            }
            repeatDaysElement.isJsonPrimitive -> {
                // Old format: "Mon,Tue,Wed"
                repeatDaysElement.asString.split(",").map { it.trim() }.toMutableList()
            }
            else -> mutableListOf()
        }

        return AlarmItem(time, repeatDays, isEnabled)
    }
}
