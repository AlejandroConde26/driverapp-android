package com.driverapp.repartidor.data.local.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Caché local de pedidos (Sprint 6). Una fila por pedido; el campo [bucket]
 * indica en qué lista de la UI vive: AVAILABLE / ACTIVE / HISTORY.
 */
@Entity(tableName = "orders_cache", indices = [Index("bucket"), Index("status")])
data class OrderEntity(
    @PrimaryKey val orderId: Int,
    val restaurantId: Int,
    val driverId: Int?,
    val clientName: String,
    val clientAddress: String,
    val destLat: Double,
    val destLng: Double,
    val status: String,
    val total: Double,
    val deliveryFee: Double,
    val paymentMethod: String,
    val distanceKm: Double,
    val estTimeMin: Int,
    val restaurantName: String?,
    val restaurantAddress: String?,
    val restaurantPhone: String?,
    val restaurantLat: Double?,
    val restaurantLng: Double?,
    val bucket: String,
    val updatedAt: Long
)

@Entity(
    tableName = "order_items_cache",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Int,
    val menuItemId: Int?,
    val productName: String,
    val description: String?,
    val quantity: Int,
    val unitPrice: Double,
    val notes: String?
)

/** Resumen de ganancias por periodo (day|week|month|year). */
@Entity(tableName = "earnings_cache")
data class EarningsEntity(
    @PrimaryKey val period: String,
    val total: Double,
    val ordersCount: Int,
    val deliveriesTotal: Double,
    val tipsTotal: Double,
    val bonusesTotal: Double,
    val otherTotal: Double,
    val chartLabelsJson: String,
    val chartValuesJson: String,
    val updatedAt: Long
)

/** Historial de ganancias (una fila por movimiento). */
@Entity(tableName = "earnings_history_cache")
data class EarningEntity(
    @PrimaryKey val id: Int,
    val driverId: Int,
    val orderId: Int?,
    val amount: Double,
    val type: String,
    val clientName: String?,
    val createdAt: String?,
    val updatedAt: Long
)

/** Cola offline de transiciones de pedido (S09). */
@Entity(tableName = "pending_actions", indices = [Index("orderId")])
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Int,
    val type: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null
)

/** Pedido + sus items, tal como se lee de Room. */
data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "orderId", entityColumn = "orderId")
    val items: List<OrderItemEntity>
)

object Buckets {
    const val AVAILABLE = "AVAILABLE"
    const val ACTIVE = "ACTIVE"
    const val HISTORY = "HISTORY"
}
