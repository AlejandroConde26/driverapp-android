package com.driverapp.repartidor.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local v1 (Sprint 6 — persistencia local).
 *
 * Tablas:
 *  - orders_cache            caché offline de pedidos
 *  - order_items_cache       items de cada pedido (1-N, FK CASCADE)
 *  - earnings_cache          resumen de ganancias por periodo
 *  - earnings_history_cache  movimientos de ganancias
 *  - pending_actions         cola offline de transiciones (S09)
 *
 * El esquema exportado vive en `app/schemas/` (room.schemaLocation en build.gradle.kts).
 * No se usa fallbackToDestructiveMigration: cualquier cambio de esquema debe llevar
 * su Migration explícita.
 */
@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        EarningsEntity::class,
        EarningEntity::class,
        PendingActionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun earningsDao(): EarningsDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        private const val NAME = "driverapp.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME
                ).build().also { instance = it }
            }
    }
}
