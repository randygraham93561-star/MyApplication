package com.google.refereeschedule.domain.model

data class Organization(
    val id: String = "",
    val name: String = "",
    val contactEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
