package com.driverapp.repartidor.ui.earnings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerHole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    var segments: List<Pair<Float, Int>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = segments.fold(0f) { acc, s -> acc + s.first }
        if (total <= 0f) return

        val rect = RectF(padding, padding.toFloat(), width - padding, height - padding)
        var start = -90f
        for ((value, color) in segments) {
            val sweep = (value / total) * 360f
            paint.color = color
            canvas.drawArc(rect, start, sweep, true, paint)
            start += sweep
        }
        val hole = (width - padding * 2) * 0.45f
        val holeRect = RectF(
            width / 2f - hole, height / 2f - hole,
            width / 2f + hole, height / 2f + hole
        )
        centerHole.color = currentSurfaceColor()
        canvas.drawCircle(width / 2f, height / 2f, hole, centerHole)
    }

    private fun currentSurfaceColor(): Int {
        val nightModeFlags = context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            Color.rgb(55, 65, 81)
        } else {
            Color.WHITE
        }
    }

    private val padding = 16f
}