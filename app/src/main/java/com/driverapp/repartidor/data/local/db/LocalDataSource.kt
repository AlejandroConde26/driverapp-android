package com.driverapp.repartidor.data.local.db

import androidx.room.withTransaction
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Ganancia
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.PedidoItem
import com.driverapp.repartidor.domain.model.Restaurante
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Fachada única sobre Room: convierte entidades a dominio y viceversa.
 * Los repositorios cache-first sólo hablan con esta clase.
 */
class LocalDataSource(private val db: AppDatabase) {

    private val orders = db.orderDao()
    private val earnings = db.earningsDao()
    private val actions = db.pendingActionDao()

    // ---------------- Pedidos ----------------

    fun observeBucket(bucket: String): Flow<List<Pedido>> =
        orders.observeByBucket(bucket).map { rows -> rows.map { it.toDomain() } }

    fun observeActive(): Flow<Pedido?> =
        orders.observeFirstOfBucket(Buckets.ACTIVE).map { it?.toDomain() }

    suspend fun get(orderId: Int): Pedido? = orders.getById(orderId)?.toDomain()

    suspend fun replaceBucket(bucket: String, pedidos: List<Pedido>) = db.withTransaction {
        orders.deleteByBucket(bucket)
        pedidos.forEach { save(it, bucket) }
    }

    suspend fun save(pedido: Pedido, bucket: String) {
        orders.upsert(pedido.toEntity(bucket))
        orders.deleteItems(pedido.id)
        if (pedido.items.isNotEmpty()) {
            orders.insertItems(pedido.items.map { it.toEntity(pedido.id) })
        }
    }

    /** Mueve un pedido de bucket y actualiza su estado (usado en transiciones). */
    suspend fun moveTo(orderId: Int, estado: EstadoPedido, bucket: String) {
        orders.updateStatus(orderId, estado.raw, bucket, System.currentTimeMillis())
    }

    suspend fun clearOrders() = orders.clear()

    suspend fun purgeHistoryOlderThan(days: Int) {
        val threshold = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
        orders.deleteHistoryOlderThan(threshold)
    }

    // ---------------- Ganancias ----------------

    suspend fun saveSummary(summary: ResumenGanancias) =
        earnings.upsertSummary(summary.toEntity())

    suspend fun summary(period: String): ResumenGanancias? =
        earnings.getSummary(period)?.toDomain()

    fun observeSummary(period: String): Flow<ResumenGanancias?> =
        earnings.observeSummary(period).map { it?.toDomain() }

    suspend fun saveHistory(entries: List<Ganancia>) {
        earnings.clearHistory()
        earnings.upsertHistory(entries.map { it.toEntity() })
    }

    suspend fun history(): List<Ganancia> = earnings.getHistory().map { it.toDomain() }

    suspend fun clearEarnings() {
        earnings.clearSummaries()
        earnings.clearHistory()
    }

    // ---------------- Cola offline ----------------

    suspend fun enqueue(orderId: Int, type: String): Long =
        actions.insert(PendingActionEntity(orderId = orderId, type = type, createdAt = System.currentTimeMillis()))

    suspend fun pendingActions(): List<PendingActionEntity> = actions.all()

    fun observePendingCount(): Flow<Int> = actions.observeCount()

    suspend fun markActionFailed(id: Long, error: String?) = actions.markFailed(id, error)

    suspend fun deleteAction(id: Long) = actions.delete(id)

    suspend fun clearActions() = actions.clear()
}

// ---------------- Mappers ----------------

private val gson = Gson()

private fun Pedido.toEntity(bucket: String) = OrderEntity(
    orderId = id,
    restaurantId = restaurantId,
    driverId = driverId,
    clientName = clientName,
    clientAddress = clientAddress,
    destLat = destLat,
    destLng = destLng,
    status = estado.raw,
    total = total,
    deliveryFee = deliveryFee,
    paymentMethod = paymentMethod,
    distanceKm = distanceKm,
    estTimeMin = estTimeMin,
    restaurantName = restaurant?.name,
    restaurantAddress = restaurant?.address,
    restaurantPhone = restaurant?.phone,
    restaurantLat = restaurant?.lat,
    restaurantLng = restaurant?.lng,
    bucket = bucket,
    updatedAt = System.currentTimeMillis()
)

private fun PedidoItem.toEntity(orderId: Int) = OrderItemEntity(
    orderId = orderId,
    menuItemId = menuItemId,
    productName = productName,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    notes = notes
)

private fun OrderWithItems.toDomain(): Pedido = Pedido(
    id = order.orderId,
    restaurantId = order.restaurantId,
    driverId = order.driverId,
    clientName = order.clientName,
    clientAddress = order.clientAddress,
    destLat = order.destLat,
    destLng = order.destLng,
    estado = EstadoPedido.from(order.status),
    total = order.total,
    deliveryFee = order.deliveryFee,
    paymentMethod = order.paymentMethod,
    distanceKm = order.distanceKm,
    estTimeMin = order.estTimeMin,
    restaurant = order.restaurantName?.let { name ->
        Restaurante(
            id = order.restaurantId,
            name = name,
            address = order.restaurantAddress ?: "",
            lat = order.restaurantLat ?: 0.0,
            lng = order.restaurantLng ?: 0.0,
            phone = order.restaurantPhone
        )
    },
    items = items.map {
        PedidoItem(
            id = it.id.toInt(),
            menuItemId = it.menuItemId,
            productName = it.productName,
            description = it.description,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            notes = it.notes
        )
    }
)

private val resumenGananciasType = object : TypeToken<ResumenGanancias>() {}.type

private fun ResumenGanancias.toEntity() = EarningsEntity(
    period = period,
    total = total,
    ordersCount = ordersCount,
    deliveriesTotal = deliveriesTotal,
    tipsTotal = tipsTotal,
    bonusesTotal = bonusesTotal,
    otherTotal = otherTotal,
    chartLabelsJson = gson.toJson(chartLabels),
    chartValuesJson = gson.toJson(chartValues),
    updatedAt = System.currentTimeMillis()
)

private fun EarningsEntity.toDomain(): ResumenGanancias = ResumenGanancias(
    period = period,
    total = total,
    ordersCount = ordersCount,
    deliveriesTotal = deliveriesTotal,
    tipsTotal = tipsTotal,
    bonusesTotal = bonusesTotal,
    otherTotal = otherTotal,
    chartLabels = gson.fromJson(chartLabelsJson, Array<String>::class.java)?.toList() ?: emptyList(),
    chartValues = gson.fromJson(chartValuesJson, Array<Double>::class.java)?.toList() ?: emptyList(),
    history = emptyList()
)

private fun Ganancia.toEntity() = EarningEntity(
    id = id,
    driverId = driverId,
    orderId = orderId,
    amount = amount,
    type = type,
    clientName = clientName,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)

private fun EarningEntity.toDomain(): Ganancia = Ganancia(
    id = id,
    driverId = driverId,
    orderId = orderId,
    amount = amount,
    type = type,
    clientName = clientName,
    createdAt = createdAt
)
