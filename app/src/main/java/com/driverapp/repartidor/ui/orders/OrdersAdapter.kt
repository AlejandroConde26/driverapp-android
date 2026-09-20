package com.driverapp.repartidor.ui.orders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.driverapp.repartidor.R
import com.driverapp.repartidor.databinding.ItemOrderBinding
import com.driverapp.repartidor.domain.model.EstadoPedido
import com.driverapp.repartidor.domain.model.Pedido
import java.util.Locale

class OrdersAdapter(
    private val onDetail: (Pedido) -> Unit,
    private val onAccept: (Pedido) -> Unit,
    private val onReject: (Pedido) -> Unit,
    private val onTrack: (Pedido) -> Unit,
    private val currentUserId: () -> Int? = { null },
) : ListAdapter<Pedido, OrdersAdapter.VH>(DIFF) {

    var isHistory: Boolean = false

    class VH(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = getItem(position)
        val b = holder.binding
        val mine = order.driverId == currentUserId()

        b.orderId.text = "Orden #${order.id}"
        b.orderPrice.text = "$" + String.format(Locale.ROOT, "%.2f", order.total)
        b.orderPrice.setTextColor(
            when (order.estado) {
                EstadoPedido.ENTREGADO -> Color.rgb(16, 185, 129)
                EstadoPedido.RECHAZADO, EstadoPedido.CANCELADO -> Color.rgb(107, 114, 128)
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

        if (mine && order.estado == EstadoPedido.ACEPTADO) {
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
            if (order.estado == EstadoPedido.ENTREGADO) {
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
        val DIFF = object : DiffUtil.ItemCallback<Pedido>() {
            override fun areItemsTheSame(a: Pedido, b: Pedido) = a.id == b.id
            override fun areContentsTheSame(a: Pedido, b: Pedido) = a == b
        }
    }
}
