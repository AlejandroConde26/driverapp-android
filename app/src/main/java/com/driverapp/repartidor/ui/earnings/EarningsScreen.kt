package com.driverapp.repartidor.ui.earnings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.domain.model.ResumenGanancias
import com.driverapp.repartidor.ui.common.ErrorState
import com.driverapp.repartidor.ui.common.LoadingState
import com.driverapp.repartidor.ui.components.ChartLegendRow
import com.driverapp.repartidor.ui.components.EarningsChart
import com.driverapp.repartidor.ui.components.ErrorStateBox
import com.driverapp.repartidor.ui.components.LoadingStateBox
import com.driverapp.repartidor.ui.components.PeriodPills
import java.util.Locale

/** Pestaña Ganancias (HU-05): periodos, total con máscara, dona y leyenda. */
@Composable
fun EarningsScreen(
    vm: EarningsViewModel,
    modifier: Modifier = Modifier
) {
    val summary by vm.earnings.collectAsStateWithLifecycle()
    val history by vm.earningsHistory.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val showBalance by vm.showBalance.collectAsStateWithLifecycle()

    var period by remember { mutableStateOf(0) }
    val periods = listOf("day", "week", "month", "year")

    LaunchedEffect(Unit) {
        vm.load(periods[period])
        vm.refreshHistory()
    }

    Column(modifier.fillMaxSize()) {
        PeriodPills(
            labels = listOf(
                stringResource(R.string.period_day),
                stringResource(R.string.period_week),
                stringResource(R.string.period_month),
                stringResource(R.string.period_year)
            ),
            selected = period,
            onSelect = { index ->
                period = index
                vm.load(periods[index])
                vm.refreshHistory()
            }
        )

        when {
            loading is LoadingState.Loading && summary == null -> LoadingStateBox()
            error !is ErrorState.None && summary == null -> ErrorStateBox(error, onRetry = { vm.retry() })
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                val data = summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.earnings_total),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showBalance) {
                                "$" + String.format(Locale.ROOT, "%.2f", data?.total ?: 0.0)
                            } else {
                                stringResource(R.string.earnings_masked)
                            },
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                    IconButton(onClick = { vm.toggleBalance() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fa_eye),
                            contentDescription = stringResource(R.string.earnings_toggle),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        EarningsChart(
                            deliveries = (data?.deliveriesTotal ?: 0.0).toFloat(),
                            tips = (data?.tipsTotal ?: 0.0).toFloat(),
                            bonuses = (data?.bonusesTotal ?: 0.0).toFloat(),
                            bars = data?.chartValues?.map { it.toFloat() } ?: emptyList()
                        )
                        Spacer(Modifier.height(12.dp))
                        ChartLegendRow(
                            stringResource(R.string.earnings_deliveries),
                            "$" + String.format(Locale.ROOT, "%.2f", data?.deliveriesTotal ?: 0.0)
                        )
                        ChartLegendRow(
                            stringResource(R.string.earnings_tips),
                            "$" + String.format(Locale.ROOT, "%.2f", data?.tipsTotal ?: 0.0)
                        )
                        ChartLegendRow(
                            stringResource(R.string.earnings_bonuses),
                            "$" + String.format(Locale.ROOT, "%.2f", data?.bonusesTotal ?: 0.0)
                        )
                        ChartLegendRow(
                            stringResource(R.string.earnings_other),
                            "$" + String.format(Locale.ROOT, "%.2f", data?.otherTotal ?: 0.0)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.earnings_orders_count),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${data?.ordersCount ?: 0}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.earnings_tips),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$" + String.format(Locale.ROOT, "%.2f", data?.tipsTotal ?: 0.0),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (history.isNotEmpty()) {
                    Text(
                        stringResource(R.string.tab_history),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    history.take(20).forEach { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.clientName ?: entry.type,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    entry.createdAt?.take(10) ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "$" + String.format(Locale.ROOT, "%.2f", entry.amount),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun ResumenGanancias.formattedTotal(): String =
    "$" + String.format(Locale.ROOT, "%.2f", total)
