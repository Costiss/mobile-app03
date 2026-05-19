package br.ufpr.carteiravirtual

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvBrl: TextView
    private lateinit var tvUsd: TextView
    private lateinit var tvBtc: TextView

    private val brLocale = Locale("pt", "BR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Preserva o tema do App ainda que o Androide mude
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        tvBrl = findViewById(R.id.tv_brl_balance)
        tvUsd = findViewById(R.id.tv_usd_balance)
        tvBtc = findViewById(R.id.tv_btc_balance)

        findViewById<Button>(R.id.btn_convert).setOnClickListener {
            startActivity(Intent(this, ConverterActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateBalances()
    }

    private fun updateBalances() {
        val nf2 = NumberFormat.getNumberInstance(brLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val nf6 = NumberFormat.getNumberInstance(brLocale).apply {
            minimumFractionDigits = 6
            maximumFractionDigits = 6
        }
        tvBrl.text = "R$ ${nf2.format(Wallet.brl)}"
        tvUsd.text = "$ ${nf2.format(Wallet.usd)}"
        tvBtc.text = "${nf6.format(Wallet.btc)} BTC"
    }
}
