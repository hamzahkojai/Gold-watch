package com.hamzah.goldwatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

// Tekan icon -> terus buka chart TradingView XAUUSD dalam browser jam
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tf = if (intent.component?.className?.endsWith("M5") == true) "5" else "15"
        val url = "https://www.tradingview.com/chart/?symbol=OANDA%3AXAUUSD&interval=$tf"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "Install Samsung Internet kat jam dulu", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
