package com.driverapp.repartidor.ui.earnings

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.repartidor.App
import com.driverapp.repartidor.R
import com.driverapp.repartidor.databinding.FragmentEarningsBinding
import com.driverapp.repartidor.databinding.ItemHistoryBinding
import com.driverapp.repartidor.domain.model.ResumenGanancias
import kotlinx.coroutines.launch
import java.util.Locale

class EarningsFragment : Fragment() {

    private var _binding: FragmentEarningsBinding? = null
    private val binding get() = _binding!!
    private val vm: EarningsViewModel by viewModels {
        (requireActivity().application as App).container.earningsViewModelFactory()
    }

    private data class DayBar(val label: String, val value: Float, val color: Int)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEarningsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pills = listOf(
            binding.filterDay, binding.filterWeek, binding.filterMonth, binding.filterYear
        )
        val periods = listOf("day", "week", "month", "year")
        pills.forEachIndexed { i, pill ->
            pill.setOnClickListener {
                selectPill(i, pills)
                vm.loadEarnings(periods[i])
                vm.refreshHistory()
                renderHistory()
            }
        }

        binding.balanceToggle.setOnClickListener {
            vm.toggleBalance()
            render(vm.earnings.value)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.earnings.collect { render(it) } }
                launch { vm.earningsHistory.collect { renderHistory() } }
            }
        }

        pills[0].callOnClick()
    }

    private var activePill = 0

    private fun selectPill(index: Int, pills: List<android.widget.Button>) {
        activePill = index
        pills.forEachIndexed { j, p ->
            val active = j == index
            p.setBackgroundResource(if (active) R.drawable.bg_pill_active else android.R.color.transparent)
            p.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (active) R.color.on_primary else R.color.text_muted
                )
            )
        }
    }

    private fun render(summary: ResumenGanancias?) {
        if (summary == null) {
            binding.earningsTotal.text = masked("$0.00")
            binding.ordersCount.text = "0"
            binding.connectedTime.text = "0:00 hrs"
            binding.tipsTotal.text = "$0.00"
            renderBars(emptyList())
            renderPie(0f, 0f, 0f)
            binding.legendDeliveries.text = "Entregas · $0.00"
            binding.legendTips.text = "Propinas · $0.00"
            binding.legendBonuses.text = "Bonos · $0.00"
            binding.legendOther.text = "Otros · $0.00"
            return
        }
        binding.earningsTotal.text = masked("$" + String.format(Locale.ROOT, "%.2f", summary.total))
        binding.ordersCount.text = "${summary.ordersCount}"
        binding.connectedTime.text = "-- hrs"
        binding.tipsTotal.text = "$" + String.format(Locale.ROOT, "%.2f", summary.tipsTotal)

        val values = summary.chartValues
        if (values.isNullOrEmpty()) {
            renderBars(emptyList())
        } else {
            val bars = values.mapIndexed { i, v ->
                DayBar(
                    label = shortDay(i),
                    value = (v / 100.0).toFloat(),
                    color = barColor(i)
                )
            }
            renderBars(bars)
        }
        renderPie(
            summary.deliveriesTotal.toFloat(),
            summary.tipsTotal.toFloat(),
            summary.bonusesTotal.toFloat()
        )
        val other = summary.otherTotal.coerceAtLeast(0.0)
        binding.legendDeliveries.text = "Entregas · $${"%.2f".format(summary.deliveriesTotal)}"
        binding.legendTips.text = "Propinas · $${"%.2f".format(summary.tipsTotal)}"
        binding.legendBonuses.text = "Bonos · $${"%.2f".format(summary.bonusesTotal)}"
        binding.legendOther.text = "Otros · $${"%.2f".format(other)}"
    }

    private fun masked(amount: String): String =
        if (vm.prefs.showBalance) amount else "$••••••"

    private val density: Float by lazy { resources.displayMetrics.density }

    private fun renderBars(bars: List<DayBar>) {
        binding.chartBars.removeAllViews()
        if (bars.isEmpty()) return
        val max = bars.maxOf { it.value }.coerceAtLeast(1f)
        bars.forEach { day ->
            val column = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            }
            val fraction = (day.value / max).coerceIn(0.02f, 1f)
            val bar = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    cornerRadius = 6 * density
                    setColor(day.color)
                }
                layoutParams = LinearLayout.LayoutParams(
                    (18 * density).toInt(),
                    (fraction * 120 * density).toInt()
                )
            }
            val label = TextView(requireContext()).apply {
                text = day.label
                textSize = 9f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            column.addView(bar)
            column.addView(label)
            binding.chartBars.addView(column)
        }
    }

    private fun renderPie(deliveries: Float, tips: Float, bonuses: Float) {
        val total = deliveries + tips + bonuses
        val ctx = requireContext()
        if (total <= 0f) {
            binding.pieView.segments = listOf(
                1f to ContextCompat.getColor(ctx, R.color.primary_light)
            )
            return
        }
        binding.pieView.segments = listOf(
            deliveries to ContextCompat.getColor(ctx, R.color.text_main),
            tips to ContextCompat.getColor(ctx, R.color.text_muted),
            bonuses to ContextCompat.getColor(ctx, R.color.info),
            (total * 0.08f).coerceAtMost(1f) to ContextCompat.getColor(ctx, R.color.border)
        )
    }

    private fun renderHistory() {
        binding.historyList.removeAllViews()
        binding.historyTitle.text = if (vm.earningsHistory.value.isEmpty()) {
            "Historial"
        } else {
            "Historial de pagos"
        }
        vm.earningsHistory.value.forEach { e ->
            val row = ItemHistoryBinding.inflate(layoutInflater, binding.historyList, false)
            row.historyTitle.text = "Orden #${e.orderId ?: e.id}"
            row.historySub.text = "PEDIDO · ${e.createdAt?.take(10) ?: "-"}"
            row.historyAmount.text = "+$${"%.2f".format(e.amount)}"
            binding.historyList.addView(row.root)
        }
    }

    private fun shortDay(idx: Int): String {
        val days = listOf("L", "M", "X", "J", "V", "S", "D")
        return days[idx.coerceIn(0, 6)]
    }

    private fun barColor(idx: Int): Int {
        val ctx = requireContext()
        val colors = listOf(
            ContextCompat.getColor(ctx, R.color.text_main),
            ContextCompat.getColor(ctx, R.color.info),
            ContextCompat.getColor(ctx, R.color.text_muted),
            ContextCompat.getColor(ctx, R.color.text_main),
            ContextCompat.getColor(ctx, R.color.info),
            ContextCompat.getColor(ctx, R.color.text_muted),
            ContextCompat.getColor(ctx, R.color.text_main)
        )
        return colors[idx.coerceIn(0, 6)]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
