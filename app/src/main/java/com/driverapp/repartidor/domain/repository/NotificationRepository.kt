package com.driverapp.repartidor.domain.repository

import com.driverapp.repartidor.domain.model.Notificacion

/**
 * Recurso de notificaciones (S09).
 * El repartidor puede listarlas (R), marcarlas como leídas (U) y descartarlas
 * localmente (D). La creación (C) la hace el backend al cambiar el estado de un
 * pedido, no el repartidor.
 */
interface NotificationRepository {

    suspend fun list(): List<Notificacion>
    suspend fun markAllRead()
}
