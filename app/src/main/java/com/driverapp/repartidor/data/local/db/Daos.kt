package com.driverapp.repartidor.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Transaction
    @Query("SELECT * FROM orders_cache WHERE bucket = :bucket ORDER BY updatedAt DESC")
    fun observeByBucket(bucket: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders_cache WHERE bucket = :bucket")
    suspend fun getByBucket(bucket: String): List<OrderWithItems>

    @Transaction
    @Query("SELECT * FROM orders_cache WHERE orderId = :orderId LIMIT 1")
    suspend fun getById(orderId: Int): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders_cache WHERE bucket = :bucket ORDER BY updatedAt DESC LIMIT 1")
    fun observeFirstOfBucket(bucket: String): Flow<OrderWithItems?>

    @Upsert
    suspend fun upsert(order: OrderEntity)

    @Upsert
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items_cache WHERE orderId = :orderId")
    suspend fun deleteItems(orderId: Int)

    @Query("DELETE FROM orders_cache WHERE bucket = :bucket")
    suspend fun deleteByBucket(bucket: String)

    @Query("UPDATE orders_cache SET status = :status, bucket = :bucket, updatedAt = :now WHERE orderId = :orderId")
    suspend fun updateStatus(orderId: Int, status: String, bucket: String, now: Long)

    /** TTL: borra pedidos históricos más viejos que [threshold] (epoch millis). */
    @Query("DELETE FROM orders_cache WHERE bucket = 'HISTORY' AND updatedAt < :threshold")
    suspend fun deleteHistoryOlderThan(threshold: Long)

    @Query("DELETE FROM orders_cache")
    suspend fun clear()
}

@Dao
interface EarningsDao {

    @Upsert
    suspend fun upsertSummary(summary: EarningsEntity)

    @Query("SELECT * FROM earnings_cache WHERE period = :period LIMIT 1")
    suspend fun getSummary(period: String): EarningsEntity?

    @Query("SELECT * FROM earnings_cache WHERE period = :period LIMIT 1")
    fun observeSummary(period: String): Flow<EarningsEntity?>

    @Upsert
    suspend fun upsertHistory(entries: List<EarningEntity>)

    @Query("SELECT * FROM earnings_history_cache ORDER BY id DESC")
    suspend fun getHistory(): List<EarningEntity>

    @Query("DELETE FROM earnings_history_cache")
    suspend fun clearHistory()

    @Query("DELETE FROM earnings_cache")
    suspend fun clearSummaries()
}

@Dao
interface PendingActionDao {

    @Insert
    suspend fun insert(action: PendingActionEntity): Long

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun all(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun observeCount(): Flow<Int>

    @Query("UPDATE pending_actions SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: Long, error: String?)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_actions")
    suspend fun clear()
}
