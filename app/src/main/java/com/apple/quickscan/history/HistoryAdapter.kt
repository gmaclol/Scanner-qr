package com.apple.quickscan.history

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apple.quickscan.R
import com.apple.quickscan.databinding.ItemHistoryBinding
import java.util.Date

class HistoryAdapter(
    private var items: List<ScanHistoryItem>,
    private val onCopyClick: (ScanHistoryItem) -> Unit,
    private val onShareClick: (ScanHistoryItem) -> Unit,
    private val onItemClick: (ScanHistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    fun updateData(newItems: List<ScanHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScanHistoryItem) {
            binding.tvItemFormat.text = item.formatName
            val dateStr = DateFormat.format("dd MMM, HH:mm", Date(item.timestamp)).toString()
            binding.tvItemDate.text = " • $dateStr"
            binding.tvItemContent.text = item.content

            // Choose icon based on format or content
            val iconRes = when {
                item.isGenerated -> R.drawable.ic_qr_code
                item.content.startsWith("http://") || item.content.startsWith("https://") -> R.drawable.ic_link
                item.content.startsWith("WIFI:") -> R.drawable.ic_wifi
                item.content.startsWith("tel:") -> R.drawable.ic_phone
                item.content.startsWith("mailto:") -> R.drawable.ic_email
                item.content.startsWith("smsto:") -> R.drawable.ic_sms
                item.content.startsWith("BEGIN:VCARD") -> R.drawable.ic_contact
                item.formatName.contains("Barcode", ignoreCase = true) || item.formatName.contains("EAN", ignoreCase = true) || item.formatName.contains("CODE", ignoreCase = true) -> R.drawable.ic_barcode
                else -> R.drawable.ic_qr_code
            }
            binding.ivItemIcon.setImageResource(iconRes)

            binding.btnItemCopy.setOnClickListener { onCopyClick(item) }
            binding.btnItemShare.setOnClickListener { onShareClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}
