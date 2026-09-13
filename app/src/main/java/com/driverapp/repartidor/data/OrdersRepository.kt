package com.driverapp.repartidor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OrdersRepository(private val api: ApiService = ApiClient.service) {
    val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder: StateFlow<Order?> = _activeOrder

    suspend fun login(idToken: String, fcmToken: String?): User {
        val resp = api.login(LoginRequest(idToken, fcmToken))
        Session.saveToken(resp.token)
        Session.saveUser(resp.user)
        return resp.user
    }

    suspend fun initFirebaseProfile(user: User) {
        if (Auth.isFirebaseConfigured()) {
            FirebaseTokenProvider.fcmToken?.let { runCatching { api.updateFcmToken(FcmTokenUpdate(it)) } }
        }
    }

    suspend fun fetchMe(): User = api.me()

    suspend fun available(): List<Order> = api.availableOrders()

    suspend fun myActive(): List<Order> = api.myOrders()

    suspend fun history(): List<Order> = api.orderHistory()

    suspend fun accept(orderId: Long): Order = api.acceptOrder(orderId).also { _activeOrder.value = it }

    fun markActive(order: Order) {
        _activeOrder.value = order
    }

    suspend fun reject(orderId: Long): Order = api.rejectOrder(orderId)

    suspend fun inTransit(orderId: Long): Order = api.inTransit(orderId).also { _activeOrder.value = it }

    suspend fun complete(orderId: Long): Order = api.completeOrder(orderId).also { _activeOrder.value = null }

    suspend fun earningsSummary(period: String): EarningsSummary = api.earningsSummary(period)

    suspend fun earningsHistory(): List<EarningOut> = api.earningsHistory()

    suspend fun updateVehicle(vehicle: String): User = api.updateVehicle(VehicleUpdate(vehicle)).also { Session.saveUser(it) }

    suspend fun updateStatus(online: Boolean): User = api.updateStatus(StatusUpdate(online)).also { Session.saveUser(it) }

    suspend fun updateLocation(lat: Double, lng: Double) = api.updateLocation(LocationUpdate(lat, lng))

    suspend fun updateProfile(name: String?, phone: String?): User =
        api.updateMe(UserUpdate(name, phone)).also { Session.saveUser(it) }

    suspend fun updateFcmToken(token: String): User = api.updateFcmToken(FcmTokenUpdate(token))
}

object FirebaseTokenProvider {
    @Volatile
    var fcmToken: String? = null
}