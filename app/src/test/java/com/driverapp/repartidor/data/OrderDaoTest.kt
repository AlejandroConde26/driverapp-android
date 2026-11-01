package com.driverapp.repartidor.data

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.driverapp.repartidor.data.local.db.AppDatabase
import com.driverapp.repartidor.data.local.db.Buckets
import com.driverapp.repartidor.data.local.db.EarningEntity
import com.driverapp.repartidor.data.local.db.EarningsEntity
import com.driverapp.repartidor.data.local.db.LocalDataSource
import com.driverapp.repartidor.data.local.db.OrderEntity
import com.driverapp.repartidor.data.local.db.OrderItemEntity
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Pedido
import com.driverapp.repartidor.domain.model.PedidoItem
import com.driverapp.repartidor.domain.model.Restaurante
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sprint 6 · Room real (in-memory) sobre Robolectric, sin emulador. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class OrderDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var local: LocalDataSource

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        local = LocalDataSource(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun pedido(id: Int, estado: EstadoPedido = EstadoPedido.PENDIENTE) = Pedido(
        id = id,
        restaurantId = 5,
        driverId = null,
        clientName = "Ana López",
        clientAddress = "Calle Falsa 123",
        destLat = 19.4326,
        destLng = -99.1332,
        estado = estado,
        total = 150.0,
        deliveryFee = 25.0,
        restaurant = Restaurante(
            id = 5,
            name = "SUMAQ",
            address = "Av. Larco 123",
            lat = -12.1219,
            lng = -77.0297,
            phone = "+51 999 111 222"
        ),
        items = listOf(
            PedidoItem(
                id = 1,
                menuItemId = 2,
                productName = "Ceviche Clásico",
                description = "Pesca del día",
                quantity = 2,
                unitPrice = 32.0,
                notes = "Sin picante"
            )
        )
    )

    @Test
    fun `guarda pedido con items y los lee de vuelta`() = runTest {
        local.save(pedido(10), Buckets.AVAILABLE)

        val list = local.observeBucket(Buckets.AVAILABLE).first()

        assertEquals(1, list.size)
        assertEquals(10, list[0].id)
        assertEquals(EstadoPedido.PENDIENTE, list[0].estado)
        assertEquals(1, list[0].items.size)
        assertEquals("Ceviche Clásico", list[0].items[0].productName)
        assertEquals("Sin picante", list[0].items[0].notes)
        assertEquals("SUMAQ", list[0].restaurant?.name)
        assertEquals(5, list[0].restaurantId)
    }

    @Test
    fun `replaceBucket sustituye el contenido anterior`() = runTest {
        local.replaceBucket(Buckets.AVAILABLE, listOf(pedido(10), pedido(11)))
        local.replaceBucket(Buckets.AVAILABLE, listOf(pedido(12)))

        val list = local.observeBucket(Buckets.AVAILABLE).first()

        assertEquals(1, list.size)
        assertEquals(12, list[0].id)
    }

    @Test
    fun `moveTo cambia estado y bucket`() = runTest {
        local.save(pedido(10), Buckets.AVAILABLE)

        local.moveTo(10, EstadoPedido.EN_CAMINO, Buckets.ACTIVE)

        assertTrue(local.observeBucket(Buckets.AVAILABLE).first().isEmpty())
        val active = local.observeActive().first()
        assertEquals(10, active?.id)
        assertEquals(EstadoPedido.EN_CAMINO, active?.estado)
    }

    @Test
    fun `sin red la cola guarda la accion pendiente`() = runTest {
        local.enqueue(10, "IN_TRANSIT")

        val actions = local.pendingActions()

        assertEquals(1, actions.size)
        assertEquals("IN_TRANSIT", actions[0].type)
        assertEquals(10, actions[0].orderId)
        assertEquals(1, local.observePendingCount().first())
    }

    @Test
    fun `cache de ganancias guarda resumen e historial`() = runTest {
        db.earningsDao().upsertSummary(
            EarningsEntity(
                period = "day", total = 100.0, ordersCount = 4,
                deliveriesTotal = 80.0, tipsTotal = 15.0, bonusesTotal = 5.0,
                otherTotal = 0.0, chartLabelsJson = "[\"L\",\"M\"]",
                chartValuesJson = "[1.0,2.0]", updatedAt = 0L
            )
        )
        db.earningsDao().upsertHistory(
            listOf(
                EarningEntity(
                    id = 1, driverId = 2, orderId = 10, amount = 25.0,
                    type = "DELIVERY", clientName = "Ana", createdAt = "2026-10-01", updatedAt = 0L
                )
            )
        )

        val summary = local.summary("day")
        assertEquals(4, summary?.ordersCount)
        assertEquals(listOf("L", "M"), summary?.chartLabels)
        assertEquals(1, local.history().size)
        assertNull(local.summary("week"))
    }

    @Test
    fun `clearOrders vacia la cache de pedidos`() = runTest {
        local.save(pedido(10), Buckets.AVAILABLE)
        local.save(pedido(11), Buckets.HISTORY)

        local.clearOrders()

        assertTrue(local.observeBucket(Buckets.AVAILABLE).first().isEmpty())
        assertTrue(local.observeBucket(Buckets.HISTORY).first().isEmpty())
    }
}
