package com.ms.myworld

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityOneOnOneChatBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class OneOnOneChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOneOnOneChatBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private lateinit var messageAdapter: MessageAdapter

    private var friendUid: String? = null
    private var friendName: String? = null
    private var friendPhotoUrl: String? = null
    private var friendEmail: String? = null
    private var chatChannelId: String? = null

    private var messagesList = ArrayList<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOneOnOneChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        friendUid = intent.getStringExtra("FRIEND_UID")
        friendName = intent.getStringExtra("FRIEND_NAME")
        val currentUserUid = auth.currentUser?.uid

        if (friendUid == null || currentUserUid == null) {
            Toast.makeText(this, "Error: User information not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()

        chatChannelId = getChatChannelId(currentUserUid, friendUid!!)
        loadFriendDetails()
        loadMessages()

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText, currentUserUid)
            }
        }

        binding.toolbarLayout.profileContainer.setOnClickListener {
            showUserInfoDialog()
        }
    }

    private fun loadFriendDetails() {
        friendUid?.let {
            db.collection("users").document(it).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("CHAT", "Failed to listen for friend details.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    friendName = snapshot.getString("displayName")
                    friendPhotoUrl = snapshot.getString("photoUrl")
                    friendEmail = snapshot.getString("email")
                    val isOnline = snapshot.getBoolean("isOnline") ?: false
                    val lastSeenTimestamp = snapshot.getTimestamp("lastSeen")?.toDate()

                    updateToolbarUI(isOnline, lastSeenTimestamp)
                }
            }
        }
    }

    private fun updateToolbarUI(isOnline: Boolean, lastSeen: Date?) {
        binding.toolbarLayout.displayNameTextView.text = friendName
        binding.toolbarLayout.profileImageView.load(friendPhotoUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_person)
            error(R.drawable.ic_person)
        }
        if (isOnline) {
            binding.toolbarLayout.statusTextView.text = "Online"
        } else {
            if (lastSeen != null) {
                binding.toolbarLayout.statusTextView.text = "Last seen: ${formatLastSeen(lastSeen)}"
            } else {
                binding.toolbarLayout.statusTextView.text = "Offline"
            }
        }
    }

    private fun formatLastSeen(date: Date): String {
        val now = Calendar.getInstance()
        val lastSeenCal = Calendar.getInstance().apply { time = date }

        return when {
            now.get(Calendar.YEAR) != lastSeenCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
            }
            now.get(Calendar.DAY_OF_YEAR) == lastSeenCal.get(Calendar.DAY_OF_YEAR) -> {
                "today at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
            }
            now.get(Calendar.DAY_OF_YEAR) - lastSeenCal.get(Calendar.DAY_OF_YEAR) == 1 -> {
                "yesterday at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
            }
            else -> {
                SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault()).format(date)
            }
        }
    }

    private fun showUserInfoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_info, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val profileImage = dialogView.findViewById<ShapeableImageView>(R.id.dialogProfileImageView)
        val displayName = dialogView.findViewById<TextView>(R.id.dialogDisplayNameTextView)
        val status = dialogView.findViewById<TextView>(R.id.dialogStatusTextView)
        val email = dialogView.findViewById<TextView>(R.id.dialogEmailTextView)

        profileImage.load(friendPhotoUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_person)
            error(R.drawable.ic_person)
        }
        displayName.text = friendName
        status.text = binding.toolbarLayout.statusTextView.text
        email.text = friendEmail

        dialog.show()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.messagesRecyclerView.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(this@OneOnOneChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun getChatChannelId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }

    private fun sendMessage(text: String, senderId: String) {
        if (chatChannelId == null) return

        val tempMessage = Message(senderId, text, Date())

        messagesList.add(tempMessage)
        messageAdapter.submitList(messagesList.toList())
        binding.messagesRecyclerView.scrollToPosition(messagesList.size - 1)
        binding.messageEditText.text.clear()

        val messageData = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val chatRef = db.collection("chats").document(chatChannelId!!)

        chatRef.collection("messages").add(messageData).addOnSuccessListener {
            Log.d("CHAT", "Message successfully written to Firestore.")
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show()
            Log.e("CHAT_ERROR", "Error writing message", e)
            messagesList.remove(tempMessage)
            messageAdapter.submitList(messagesList.toList())
        }

        val myFriendshipRef = db.collection("users").document(senderId).collection("friendships").document(friendUid!!)
        val theirFriendshipRef = db.collection("users").document(friendUid!!).collection("friendships").document(senderId)
        myFriendshipRef.update("lastMessage", text)
        theirFriendshipRef.update("lastMessage", text)
    }

    private fun loadMessages() {
        if (chatChannelId == null) return

        db.collection("chats").document(chatChannelId!!)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("CHAT", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val serverMessages = snapshots?.mapNotNull { doc -> doc.toObject<Message>() }

                if (serverMessages != null) {
                    messagesList.clear()
                    messagesList.addAll(serverMessages)
                    messageAdapter.submitList(messagesList.toList())
                    binding.messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }
            }
    }

    // --- THIS IS THE FIX ---
    // We now inflate our single, correct menu file.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.one_on_one_chat_menu, menu)
        return true
    }

    // This function handles all menu item clicks correctly now.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_video_call, R.id.menu_voice_call -> {
                Toast.makeText(this, "This feature is coming soon!", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_view_profile -> {
                showUserInfoDialog()
                true
            }
            R.id.menu_clear_chat -> {
                showClearChatConfirmationDialog()
                true
            }
            R.id.menu_search, R.id.menu_block_user -> {
                Toast.makeText(this, "This feature is coming soon!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearChatConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear Chat")
            .setMessage("Are you sure you want to permanently delete all messages in this chat?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear Chat") { _, _ ->
                clearChatHistory()
            }
            .show()
    }

    private fun clearChatHistory() {
        if (chatChannelId == null) return
        val messagesRef = db.collection("chats").document(chatChannelId!!).collection("messages")

        messagesRef.get().addOnSuccessListener { snapshot ->
            db.runBatch { batch ->
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
            }.addOnSuccessListener {
                Toast.makeText(this, "Chat cleared.", Toast.LENGTH_SHORT).show()
                val currentUserUid = auth.currentUser?.uid
                if (currentUserUid != null && friendUid != null) {
                    val myFriendshipRef = db.collection("users").document(currentUserUid).collection("friendships").document(friendUid!!)
                    val theirFriendshipRef = db.collection("users").document(friendUid!!).collection("friendships").document(currentUserUid)
                    myFriendshipRef.update("lastMessage", FieldValue.delete())
                    theirFriendshipRef.update("lastMessage", FieldValue.delete())
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to clear chat.", Toast.LENGTH_SHORT).show()
                Log.e("CHAT_CLEAR_ERROR", "Error clearing chat history", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}