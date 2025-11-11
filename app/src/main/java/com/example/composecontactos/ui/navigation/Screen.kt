package com.example.composecontactos.ui.navigation

import kotlinx.serialization.Serializable


@Serializable
object Home

@Serializable
object Contacts

@Serializable
data class Detail(
    val contactId: String,
    val name: String,
    val phoneNumber: String?
)
