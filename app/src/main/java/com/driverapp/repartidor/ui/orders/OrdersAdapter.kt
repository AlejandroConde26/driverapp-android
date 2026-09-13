package com.driverapp.repartidor.ui.orders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.Order
import com.driverapp.repartidor.data.Session
import com.driverapp.repartidor.databinding.ItemOrderBinding
import java.util.Locale

class OrdersAdapter(
    private val onDetail: (Order) -> Unit,
    private val onAccept: (Order) -> Unit,
    private val onReject: (Order) -> Unit,
    private val onTrack: (Order) -> Unit,
) : ListAdapter<Order, OrdersAdapter.VH>(DIFF) {

    var isHistory: Boolean = false

    class VH(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = getItem(position)
        val b = holder.binding
        val user = Session.user()
        val mine = order.driverId == user?.id

        b.orderId.text = "Orden #${order.id}"
        b.orderPrice.text = "$" + String.format(Locale.ROOT, "%.2f", order.total)
        b.orderPrice.setTextColor(
            when (order.status) {
                "ENTREGADO" -> Color.rgb(16, 185, 129)
                "RECHAZADO", "CANCELADO" -> Color.rgb(107, 114, 128)
                else -> Color.rgb(229, 57, 53)
            }
        )
        b.orderAvatar.text = order.clientName.take(2).uppercase(Locale.ROOT)
        b.orderClient.text = order.clientName
        b.orderAddress.text = order.clientAddress
        b.metaPayment.text = when (order.paymentMethod) {
            "Tarjeta" -> "Tarjeta"
            else -> "Efectivo"
        }
        b.metaPayment.background = holder.itemView.context.getDrawable(
            if (order.paymentMethod == "Tarjeta") R.drawable.bg_badge_blue else R.drawable.bg_badge_green
        )
        b.metaPayment.setTextColor(
            if (order.paymentMethod == "Tarjeta") Color.rgb(59, 130, 246) else Color.rgb(16, 185, 129)
        )

        val (h, m) = order.estTimeMin / 60 to order.estTimeMin % 60
        val timeLabel = if (h > 0) "$h:${"%02d".format(m)} hrs" else "0:${"%02d".format(m)} hrs"
        b.metaRoute.text = "$timeLabel · ${order.distanceKm} km"

        b.btnDetail.text = "Ver Detalle"
        b.btnDetail.setOnClickListener { onDetail(order) }

        if (mine && order.status == "ACEPTADO") {
            b.cardRoot.setBackgroundResource(R.drawable.bg_item_active_left)
            b.actionsRow.isVisible = false
            b.statusBadge.isVisible = true
            b.statusBadge.text = "Aceptado"
            b.statusBadge.setBackgroundResource(R.drawable.bg_badge_green)
            b.statusBadge.setTextColor(Color.rgb(16, 185, 129))
            b.btnTrack.isVisible = true
            b.btnTrack.setOnClickListener { onTrack(order) }
        } else if (isHistory) {
            b.cardRoot.setBackgroundResource(R.drawable.bg_card)
            b.actionsRow.isVisible = false
            b.btnTrack.isVisible = false
            b.statusBadge.isVisible = true
            if (order.status == "ENTREGADO") {
                b.statusBadge.text = "Entregado"
                b.statusBadge.setBackgroundResource(R.drawable.bg_badge_green)
                b.statusBadge.setTextColor(Color.rgb(16, 185, 129))
            } else {
                b.statusBadge.text = "Rechazado"
                b.statusBadge.setBackgroundResource(R.drawable.bg_badge_red)
                b.statusBadge.setTextColor(Color.rgb(239, 68, 68))
            }
        } else {
            b.cardRoot.setBackgroundResource(R.drawable.bg_card)
            b.actionsRow.isVisible = true
            b.btnTrack.isVisible = false
            b.statusBadge.isVisible = false
            b.btnAccept.setOnClickListener { onAccept(order) }
            b.btnReject.setOnClickListener { onReject(order) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(a: Order, b: Order) = a.id == b.id
            override fun areContentsTheSame(a: Order, b: Order) = a == b
        }
    }
}