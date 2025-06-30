package com.ms.myworld

import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityChatBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var chatAdapter: ChatAdapter
    private var requestListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadFriends()
    }

    override fun onStart() {
        super.onStart()
        listenForFriendRequests()
    }

    override fun onStop() {
        super.onStop()
        requestListener?.remove()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { friend ->
            val intent = Intent(this, OneOnOneChatActivity::class.java).apply {
                putExtra("FRIEND_UID", friend.uid)
                putExtra("FRIEND_NAME", friend.displayName)
            }
            startActivity(intent)
        }
        binding.chatsRecyclerView.adapter = chatAdapter
    }

    private fun loadFriends() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).collection("friendships")
            .whereEqualTo("status", "friends")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("CHAT_ACTIVITY", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val friendList = snapshots.documents.mapNotNull { doc ->
                        Friend(
                            uid = doc.id,
                            displayName = doc.getString("displayName"),
                            shortId = doc.getString("shortId"),
                            photoUrl = doc.getString("photoUrl"),
                            lastMessage = doc.getString("lastMessage")
                        )
                    }.toMutableList()

                    chatAdapter.submitList(friendList)
                    binding.emptyView.visibility = View.GONE
                    binding.chatsRecyclerView.visibility = View.VISIBLE
                } else {
                    chatAdapter.submitList(emptyList())
                    binding.emptyView.visibility = View.VISIBLE
                    binding.chatsRecyclerView.visibility = View.GONE
                }
            }
    }

    private fun listenForFriendRequests() {
        val currentUser = auth.currentUser ?: return
        requestListener?.remove()
        requestListener = db.collection("users").document(currentUser.uid)
            .collection("friendships").whereEqualTo("status", "received")
            .addSnapshotListener { _, _ ->
                invalidateOptionsMenu()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val requestsItem = menu.findItem(R.id.menu_friend_requests)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val requestsRef = db.collection("users").document(currentUser.uid)
                .collection("friendships").whereEqualTo("status", "received")

            requestsRef.get().addOnSuccessListener { snapshots ->
                val icon = requestsItem.icon as? LayerDrawable
                icon?.mutate()
                val badge = icon?.findDrawableByLayerId(R.id.ic_badge)

                if (snapshots != null && !snapshots.isEmpty) {
                    badge?.alpha = 255
                } else {
                    badge?.alpha = 0
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add_friend -> {
                showSearchUserDialog()
                true
            }
            R.id.menu_friend_requests -> {
                startActivity(Intent(this, FriendRequestsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSearchUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_user, null)
        val searchIdEditText = dialogView.findViewById<TextInputEditText>(R.id.searchIdEditText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Friend")
            .setView(dialogView)
            .setPositiveButton("Search") { d, _ ->
                val query = searchIdEditText.text.toString().trim()
                if (query.isNotEmpty()) { searchForUser(query) }
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()

        searchIdEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchIdEditText.text.toString().trim()
                if (query.isNotEmpty()) { searchForUser(query) }
                dialog.dismiss()
                return@setOnEditorActionListener true
            }
            false
        }
        dialog.show()
    }

    private fun searchForUser(query: String) {
        val searchingToast = Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT)
        searchingToast.show()
        val isEmail = "@" in query
        val fieldToSearch = if (isEmail) "email_lowercase" else "shortId"
        val valueToSearch = query.toLowerCase(Locale.ROOT)
        val usersRef = db.collection("users").whereEqualTo(fieldToSearch, valueToSearch)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val querySnapshot = usersRef.get().await()
                withContext(Dispatchers.Main) {
                    searchingToast.cancel()
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(this@ChatActivity, "User not found.", Toast.LENGTH_LONG).show()
                    } else {
                        val document = querySnapshot.documents[0]
                        val userUid = document.id
                        val intent = Intent(this@ChatActivity, UserProfileActivity::class.java)
                        intent.putExtra("USER_UID", userUid)
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    searchingToast.cancel()
                    Toast.makeText(this@ChatActivity, "An error occurred.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}