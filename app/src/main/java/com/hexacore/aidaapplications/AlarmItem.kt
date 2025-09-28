
package com.hexacore.aidaapplications

data class AlarmItem(
    var time: String,                   // "07:00 AM"
    var repeatDays: MutableList<String>,// e.g. ["Mon","Wed","Fri"]
    var isEnabled: Boolean
)
