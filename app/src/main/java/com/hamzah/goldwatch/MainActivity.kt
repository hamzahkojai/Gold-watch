package com.hamzah.goldwatch

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val spotApi = "https://api.gold-api.com/price/XAU"
    private fun candleApi(tf: Int) = "https://api.kraken.com/0/public/OHLC?pair=PAXGUSD&interval=$tf"

    private val refreshMs = 30_000L
    private val io = Executors.newFixedThreadPool(2)
    private val ui = Handler(Looper.getMainLooper())
    private val gold = Color.parseColor("#E8B931")

    private lateinit var tvPrice: TextView
    private lateinit var tvChange: TextView
    private lateinit var tvUpdated: TextView
    private lateinit var chart: CandleView
    private lateinit var btnM5: TextView
    private lateinit var btnM15: TextView

    private var tf = 15
    private var lastPrice: Double? = null

    private val loop = object : Runnable {
        override fun run() { refresh(); ui.postDelayed(this, refreshMs) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvPrice = findViewById(R.id.price)
        tvChange = findViewById(R.id.change)
        tvUpdated = findViewById(R.id.updated)
        chart = findViewById(R.id.chart)
        btnM5 = findViewById(R.id.btnM5)
        btnM15 = findViewById(R.id.btnM15)

        tf = getPreferences(MODE_PRIVATE).getInt("tf", 15)
        btnM5.setOnClickListener { setTf(5) }
        btnM15.setOnClickListener { setTf(15) }
        chart.setOnClickListener { refresh() }
        styleButtons()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() { super.onResume(); ui.post(loop) }
    override fun onPause() { super.onPause(); ui.removeCallbacks(loop) }
    override fun onDestroy() { super.onDestroy(); io.shutdownNow() }

    private fun setTf(v: Int) {
        if (tf == v) return
        tf = v
        getPreferences(MODE_PRIVATE).edit().putInt("tf", v).apply()
        styleButtons()
        chart.setCandles(emptyList())
        fetchCandles()
    }

    private fun styleButtons() {
        fun st(b: TextView, on: Boolean) {
            b.background = GradientDrawable().apply {
                cornerRadius = 50f
                if (on) setColor(gold) else { setColor(Color.TRANSPARENT); setStroke(2, Color.parseColor("#555555")) }
            }
            b.setTextColor(if (on) Color.BLACK else Color.parseColor("#AAAAAA"))
        }
        st(btnM5, tf == 5); st(btnM15, tf == 15)
    }

    private fun refresh() { fetchSpot(); fetchCandles() }

    private fun get(url: String): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "GoldWatch/2.0")
        }
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    private fun fetchSpot() {
        io.execute {
            try {
                val j = JSONObject(get(spotApi))
                val p = j.getDouble("price")
                val at = j.optString("updatedAt", "")
                ui.post { showSpot(p, at) }
            } catch (e: Exception) {
                ui.post { tvUpdated.text = "No connection - retry 30s"; tvUpdated.setTextColor(Color.parseColor("#FF6B6B")) }
            }
        }
    }

    private fun fetchCandles() {
        val want = tf
        io.execute {
            try {
                val j = JSONObject(get(candleApi(want)))
                val res = j.getJSONObject("result")
                val key = res.keys().asSequence().first { it != "last" }
                val arr = res.getJSONArray(key)
                val list = ArrayList<Candle>(arr.length())
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONArray(i)
                    list.add(Candle(r.getLong(0), r.getString(1).toDouble(), r.getString(2).toDouble(),
                        r.getString(3).toDouble(), r.getString(4).toDouble()))
                }
                ui.post { if (want == tf) chart.setCandles(list) }
            } catch (_: Exception) { /* biar chart lama */ }
        }
    }

    private fun showSpot(price: Double, at: String) {
        val prev = lastPrice
        lastPrice = price
        tvPrice.text = String.format(Locale.US, "%.2f", price)
        tvPrice.setTextColor(when {
            prev == null || price == prev -> Color.WHITE
            price > prev -> Color.parseColor("#4CD964")
            else -> Color.parseColor("#FF6B6B")
        })
        if (prev != null && price != prev) {
            val d = price - prev
            tvChange.text = String.format(Locale.US, "%s%.2f", if (d > 0) "▲" else "▼", kotlin.math.abs(d))
            tvChange.setTextColor(if (d > 0) Color.parseColor("#4CD964") else Color.parseColor("#FF6B6B"))
        }
        tvUpdated.setTextColor(Color.parseColor("#666666"))
        tvUpdated.text = "XAU " + toMyt(at) + " MYT · chart PAXG"
    }

    private fun toMyt(iso: String): String = try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val d: Date = inFmt.parse(iso.take(19))!!
        SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur") }.format(d)
    } catch (e: Exception) {
        SimpleDateFormat("HH:mm", Locale.US).format(Date())
    }
}
