package com.driverapp.repartidor.domain.model

/** Notificación/aviso del repartidor (S09: recurso CRUD sobre la API). */
data class Notificacion(
    val id: Int,
    val title: String,
    val message: String,
    val read: Boolean,
    val createdAt: String? = null
)
