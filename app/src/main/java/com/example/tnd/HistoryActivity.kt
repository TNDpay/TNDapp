package com.example.tnd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerViewPaymentHistory: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerViewPaymentHistory = findViewById(R.id.recyclerViewPaymentHistory)
        recyclerViewPaymentHistory.layoutManager = LinearLayoutManager(this)

        val paymentHistoryData = generateFakePaymentHistory()
        val adapter = PaymentHistoryAdapter(paymentHistoryData)
        recyclerViewPaymentHistory.adapter = adapter
    }

    private fun generateFakePaymentHistory(): List<PaymentHistoryItem> {
        return listOf(
            PaymentHistoryItem("Gusto's Gourmet Pizzeria", 25.0, "2023-06-01", "9xTQPW9sPByQHZPH4rTggwr2DXGVVEGETQmDUAT8fKK5", R.drawable.token1_logo),
            PaymentHistoryItem("Scoops & Smiles Ice Cream Parlor", 10.0, "2023-06-02", "4YkUQFLthRZrYtmYTTG6RfRSzBRhY6vGdVfhm2J9Ywtz", R.drawable.token1_logo),
            PaymentHistoryItem("Perky's Fresh Brew Coffee House", 5.0, "2023-06-03", "6RHhN6CtGZEVJak3xhLZ8X9Qv1zLpFXj2VTQDUPiGKQV", R.drawable.token2_logo),
            PaymentHistoryItem("Bunzilla's Gourmet Burgers", 15.0, "2023-06-04", "FkYd9ZLgh6feCSWXHpzt7jJcFimKFbwiSAF2VYcj8HUQ", R.drawable.bonk),
            PaymentHistoryItem("Sakura Sushi Haven", 30.0, "2023-06-05", "8KSEQ2Yt9ogNBPNXLc1VQbm5jxQJq2YdqphvJh7tyzfA", R.drawable.token2_logo),
            PaymentHistoryItem("Taco Fiesta Mexican Grill", 12.5, "2023-06-06", "3Ai2vRvE2E3XmfSjZNbBz1rYYJcMnU8vnfGmD6RtYQtN", R.drawable.token1_logo),
            PaymentHistoryItem("Masala Spice Indian Cuisine", 18.0, "2023-06-07", "J7p7FkL4CjPncCnWzFJ2PbHhU8JTZmGWFT1SRvMG6Qdx", R.drawable.tremp),
            PaymentHistoryItem("Noodle Ninja Asian Fusion", 22.0, "2023-06-08", "7CZpF3ywFTxPMXJByAn8tWQDpG2RsHJh4KinHdZaGz1e", R.drawable.bonk)
        )
    }


    data class PaymentHistoryItem(val businessName: String, val amount: Double, val date: String, val address: String, @DrawableRes val tokenLogo: Int)

    class PaymentHistoryAdapter(private val paymentHistoryItems: List<PaymentHistoryItem>) :
        RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textViewBusinessName: TextView = itemView.findViewById(R.id.textViewBusinessName)
            val textViewAmount: TextView = itemView.findViewById(R.id.textViewAmount)
            val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
            val textViewAddress: TextView = itemView.findViewById(R.id.textViewAddress)
            val imageViewTokenLogo: ImageView = itemView.findViewById(R.id.imageViewTokenLogo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_payment_history, parent, false)
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val paymentHistoryItem = paymentHistoryItems[position]
            holder.textViewBusinessName.text = paymentHistoryItem.businessName
            holder.textViewAmount.text = "$${paymentHistoryItem.amount}"
            holder.textViewDate.text = paymentHistoryItem.date
            holder.textViewAddress.text = paymentHistoryItem.address
            holder.imageViewTokenLogo.setImageResource(paymentHistoryItem.tokenLogo)

            if (position < paymentHistoryItems.size - 1) {
                holder.itemView.findViewById<View>(R.id.dividerLine).visibility = View.VISIBLE
            } else {
                holder.itemView.findViewById<View>(R.id.dividerLine).visibility = View.GONE
            }
        }

        override fun getItemCount(): Int {
            return paymentHistoryItems.size
        }
    }
}