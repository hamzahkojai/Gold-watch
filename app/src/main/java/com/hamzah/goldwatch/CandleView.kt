package com.hamzah.goldwatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class Candle(val t: Long, val o: Double, val h: Double, val l: Double, val c: Double)

class CandleView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private var candles: List<Candle> = emptyList()
    private var livePrice: Double? = null
    private val maxBars = 30

    private val up = Color.parseColor("#4CD964")
    private val down = Color.parseColor("#FF5A5A")
    private val gold = Color.parseColor("#E8B931")
    private val dp = resources.displayMetrics.density

    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wick = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.2f * dp }
    private val grid = Paint().apply { color = Color.parseColor("#222222"); strokeWidth = 1f }
    private val lastLine = Paint().apply {
        color = gold; strokeWidth = 1f * dp
        pathEffect = DashPathEffect(floatArrayOf(4 * dp, 3 * dp), 0f)
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888"); textSize = 8.5f * dp; textAlign = Paint.Align.RIGHT
    }
    private val tag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textSize = 9f * dp; textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }
    private val tagBg = Paint().apply { color = gold }
    private val empty = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555"); textSize = 11f * dp; textAlign = Paint.Align.CENTER
    }

    fun setCandles(list: List<Candle>) { candles = list.takeLast(maxBars); invalidate() }
    fun setLive(p: Double) { livePrice = p; invalidate() }

    override fun onDraw(cv: Canvas) {
        super.onDraw(cv)
        val w = width.toFloat(); val h = height.toFloat()
        if (candles.isEmpty()) { cv.drawText("Loading chart...", w / 2, h / 2, empty); return }

        val axisW = 30 * dp                       // ruang label harga kanan
        val plotW = w - axisW
        var hi = candles.maxOf { it.h }; var lo = candles.minOf { it.l }
        val last = candles.last().c
        hi = max(hi, last); lo = min(lo, last)
        val pad = max((hi - lo) * 0.08, 0.5)
        hi += pad; lo -= pad
        fun y(p: Double) = (h - (p - lo) / (hi - lo) * h).toFloat()

        // grid + label 3 aras
        for (i in 0..2) {
            val p = lo + (hi - lo) * (0.15 + 0.35 * i)
            val yy = y(p)
            cv.drawLine(0f, yy, plotW, yy, grid)
            cv.drawText(String.format(Locale.US, "%.0f", p), w, yy + 3 * dp, label)
        }

        val slot = plotW / maxBars
        val bw = max(slot * 0.62f, 1.5f * dp)
        val offset = (maxBars - candles.size) * slot
        candles.forEachIndexed { i, k ->
            val cx = offset + slot * i + slot / 2
            val col = if (k.c >= k.o) up else down
            wick.color = col; body.color = col
            cv.drawLine(cx, y(k.h), cx, y(k.l), wick)
            val top = y(max(k.o, k.c)); var bot = y(min(k.o, k.c))
            if (bot - top < 1.2f * dp) bot = top + 1.2f * dp
            cv.drawRect(cx - bw / 2, top, cx + bw / 2, bot, body)
        }

        // garisan harga terkini + tag
        val ly = y(last)
        cv.drawLine(0f, ly, plotW, ly, lastLine)
        val txt = String.format(Locale.US, "%.1f", last)
        val tw = tag.measureText(txt) + 4 * dp
        cv.drawRect(w - tw, ly - 6.5f * dp, w, ly + 6.5f * dp, tagBg)
        cv.drawText(txt, w - 2 * dp, ly + 3 * dp, tag)
    }
}
