package com.apple.quickscan.history

data class ScanHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val formatName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGenerated: Boolean = false,
    val title: String = ""
)
