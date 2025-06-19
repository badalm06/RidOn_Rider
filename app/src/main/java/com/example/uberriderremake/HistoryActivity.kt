package com.example.uberriderremake

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uberriderremake.Adapter.HistoryAdapter
import com.example.uberriderremake.Model.History
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<History>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        recyclerView = findViewById(R.id.recyclerHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter

        loadHistory()
    }

    private fun loadHistory() {
        val historyRef = FirebaseDatabase.getInstance().getReference("History")
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        historyRef.orderByChild("rider").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    historyList.clear()
                    historyList.reverse()
                    for (child in snapshot.children) {
                        val history = child.getValue(History::class.java)
                        if (history != null) {
                            historyList.add(history)
                        }
                    }
                    historyList.reverse()
                    if (historyList.isEmpty()) {
                        Toast.makeText(this@HistoryActivity, "No trip history found.", Toast.LENGTH_SHORT).show()
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HistoryActivity, "Failed to load history: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
