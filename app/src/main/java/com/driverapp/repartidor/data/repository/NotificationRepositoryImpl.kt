package com.driverapp.repartidor.data.repository

import com.driverapp.repartidor.data.remote.PedidoApiService
import com.driverapp.repartidor.data.remote.toApiFailure
import com.driverapp.repartidor.domain.model.Notificacion
import com.driverapp.repartidor.domain.repository.NotificationRepository

class NotificationRepositoryImpl(
    private val api: PedidoApiService
) : NotificationRepository {

    override suspend fun list(): List<Notificacion> = try {
        api.myNotifications().map {
            Notificacion(
                id = it.id,
                title = it.title,
                message = it.message,
                read = it.read,
                createdAt = it.createdAt
            )
        }
    } catch (e: Throwable) {
        throw e.toApiFailure()
    }

    override suspend fun markAllRead() {
        try {
            api.markNotificationsRead()
        } catch (e: Throwable) {
            throw e.toApiFailure()
        }
    }
}
