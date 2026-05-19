package br.ufpr.carteiravirtual

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import br.ufpr.carteiravirtual.model.CurrencyData
import br.ufpr.carteiravirtual.network.RetrofitClient
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class ConverterActivity : AppCompatActivity() {

    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var etAmount: EditText
    private lateinit var btnConvert: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResult: TextView

    private val brLocale = Locale("pt", "BR")
    private val currencies = arrayOf("BRL", "USD", "BTC")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_converter)

        spinnerFrom = findViewById(R.id.spinner_from)
        spinnerTo = findViewById(R.id.spinner_to)
        etAmount = findViewById(R.id.et_amount)
        btnConvert = findViewById(R.id.btn_confirm_convert)
        progressBar = findViewById(R.id.progress_bar)
        cardResult = findViewById(R.id.card_result)
        tvResult = findViewById(R.id.tv_result)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter
        spinnerTo.setSelection(1)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        btnConvert.setOnClickListener { onConvertClicked() }
    }

    private fun onConvertClicked() {
        val from = spinnerFrom.selectedItem.toString()
        val to = spinnerTo.selectedItem.toString()
        val amountStr = etAmount.text.toString().replace(",", ".")
        val amount = amountStr.toDoubleOrNull()

        if (from == to) {
            Toast.makeText(this, getString(R.string.error_same_currency), Toast.LENGTH_SHORT).show()
            return
        }
        if (amount == null || amount <= 0.0) {
            Toast.makeText(this, getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show()
            return
        }

        val balance = getBalance(from)
        if (amount > balance) {
            Toast.makeText(this, getString(R.string.error_insufficient_balance), Toast.LENGTH_LONG).show()
            return
        }

        val pair = resolveApiPair(from, to)
        if (pair == null) {
            Toast.makeText(this, getString(R.string.error_api), Toast.LENGTH_LONG).show()
            return
        }
        fetchAndConvert(from, to, amount, pair)
    }

    private fun getBalance(currency: String): Double = when (currency) {
        "BRL" -> Wallet.brl
        "USD" -> Wallet.usd
        "BTC" -> Wallet.btc
        else -> 0.0
    }

    private fun resolveApiPair(from: String, to: String): String? = when {
        setOf(from, to) == setOf("USD", "BRL") -> "USD-BRL"
        setOf(from, to) == setOf("BTC", "BRL") -> "BTC-BRL"
        setOf(from, to) == setOf("BTC", "USD") -> "BTC-USD"
        else -> null
    }

    private fun calculateConverted(from: String, to: String, amount: Double, rate: Double): Double {
        val baseIsFrom = when {
            from == "USD" && to == "BRL" -> true   // pair USD-BRL: 1 USD = rate BRL
            from == "BRL" && to == "USD" -> false  // invert
            from == "BTC" && to == "BRL" -> true   // pair BTC-BRL: 1 BTC = rate BRL
            from == "BRL" && to == "BTC" -> false  // invert
            from == "BTC" && to == "USD" -> true   // pair BTC-USD: 1 BTC = rate USD
            from == "USD" && to == "BTC" -> false  // invert
            else -> true
        }
        return if (baseIsFrom) amount * rate else amount / rate
    }

    private fun fetchAndConvert(from: String, to: String, amount: Double, pair: String) {
        setLoadingState(true)
        cardResult.visibility = View.GONE

        RetrofitClient.apiService.getRate(pair).enqueue(object : Callback<Map<String, CurrencyData>> {
            override fun onResponse(
                call: Call<Map<String, CurrencyData>>,
                response: Response<Map<String, CurrencyData>>
            ) {
                setLoadingState(false)
                val rate = response.body()?.values?.firstOrNull()?.bid?.toDoubleOrNull()
                if (!response.isSuccessful || rate == null || rate <= 0.0) {
                    Toast.makeText(this@ConverterActivity, getString(R.string.error_api), Toast.LENGTH_LONG).show()
                    return
                }
                val converted = calculateConverted(from, to, amount, rate)
                applyConversion(from, to, amount, converted)
                showResult(from, to, amount, converted)
            }

            override fun onFailure(call: Call<Map<String, CurrencyData>>, t: Throwable) {
                setLoadingState(false)
                Toast.makeText(this@ConverterActivity, getString(R.string.error_api), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun applyConversion(from: String, to: String, amount: Double, converted: Double) {
        when (from) {
            "BRL" -> Wallet.brl -= amount
            "USD" -> Wallet.usd -= amount
            "BTC" -> Wallet.btc -= amount
        }
        when (to) {
            "BRL" -> Wallet.brl += converted
            "USD" -> Wallet.usd += converted
            "BTC" -> Wallet.btc += converted
        }
    }

    private fun showResult(from: String, to: String, amount: Double, converted: Double) {
        tvResult.text = "${formatValue(from, amount)} → ${formatValue(to, converted)}"
        cardResult.visibility = View.VISIBLE
    }

    private fun formatValue(currency: String, value: Double): String {
        val nf = NumberFormat.getNumberInstance(brLocale)
        return when (currency) {
            "BRL" -> {
                nf.minimumFractionDigits = 2; nf.maximumFractionDigits = 2
                "R$ ${nf.format(value)}"
            }
            "USD" -> {
                nf.minimumFractionDigits = 2; nf.maximumFractionDigits = 2
                "$ ${nf.format(value)}"
            }
            "BTC" -> {
                nf.minimumFractionDigits = 6; nf.maximumFractionDigits = 6
                "${nf.format(value)} BTC"
            }
            else -> value.toString()
        }
    }

    private fun setLoadingState(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnConvert.isEnabled = !loading
        etAmount.isEnabled = !loading
        spinnerFrom.isEnabled = !loading
        spinnerTo.isEnabled = !loading
    }
}
