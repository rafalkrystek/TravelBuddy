package com.example.travelbuddy

import java.util.Date

data class Trip(
    val id: String = "",
    val destination: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val budget: Int = 0,
    val remainingBudget: Int? = null,
    val userId: String = "",
    val createdAt: Date = Date(),
    val packingList: List<String> = emptyList(),
    val activities: List<String> = emptyList()
)
