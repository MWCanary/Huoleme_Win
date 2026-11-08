package com.huoleme.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckInRecord(
    @SerialName("date")
    val date: String,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("note")
    val note: String = "",
    // Kept for Android backup/restore compatibility. Windows never writes a real location.
    @SerialName("location")
    val location: String = ""
)
