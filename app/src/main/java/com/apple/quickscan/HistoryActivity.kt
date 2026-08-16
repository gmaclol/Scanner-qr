package com.apple.quickscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.apple.quickscan.databinding.ActivityHistoryBinding
import com.apple.quickscan.history.HistoryAdapter
import com.apple.quickscan.history.HistoryRepository
import com.apple.quickscan.history.ScanHistoryItem
import com.apple.quickscan.utils.ClipboardHelper

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyRepository: HistoryRepository
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyRepository = HistoryRepository(this)

        setupRecyclerView()
        setupListeners()
        loadHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            items = emptyList(),
            onCopyClick = { item ->
                ClipboardHelper.copyToClipboard(this, item.content)
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            },
            onShareClick = { item ->
                shareContent(item.content)
            },
            onItemClick = { item ->
                ClipboardHelper.copyToClipboard(this, item.content)
                Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnClearAll.setOnClickListener {
            historyRepository.clearAll()
            loadHistory()
            Toast.makeText(this, "Cronologia cancellata", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHistory() {
        val items = historyRepository.getAllItems()
        if (items.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            adapter.updateData(items)
        }
    }

    private fun shareContent(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }
}
