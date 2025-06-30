package com.ms.myworld

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityFriendRequestsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendRequestsActivity : AppCompatActivity(), FriendRequestAdapter.OnRequestActionListener {

    private lateinit var binding: ActivityFriendRequestsBinding
    private lateinit var friendRequestAdapter: FriendRequestAdapter
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadFriendRequests()
    }

    private fun setupRecyclerView() {
        friendRequestAdapter = FriendRequestAdapter(this)
        binding.requestsRecyclerView.adapter = friendRequestAdapter
    }

    private fun loadFriendRequests() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid).collection("friendships")
            .whereEqualTo("status", "received")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FRIEND_REQUESTS", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.requestsRecyclerView.visibility = View.GONE
                    friendRequestAdapter.submitList(emptyList())
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.requestsRecyclerView.visibility = View.VISIBLE

                    val requests = snapshots.documents.mapNotNull { doc ->
                        FriendRequest(uid = doc.id)
                    }

                    fetchSenderProfiles(requests)
                }
            }
    }

    private fun fetchSenderProfiles(requests: List<FriendRequest>) {
        CoroutineScope(Dispatchers.IO).launch {
            val fullRequests = requests.mapNotNull { request ->
                try {
                    val userDoc = db.collection("users").document(request.uid).get().await()
                    if (userDoc.exists()) {
                        request.copy(
                            displayName = userDoc.getString("displayName"),
                            shortId = userDoc.getString("shortId"),
                            photoUrl = userDoc.getString("photoUrl")
                        )
                    } else { null }
                } catch (e: Exception) { null }
            }
            withContext(Dispatchers.Main) {
                friendRequestAdapter.submitList(fullRequests)
            }
        }
    }

    // --- THIS IS THE 100% FIX ---
    // This function now fetches the details of BOTH users before saving the friendship.
    override fun onAccept(request: FriendRequest) {
        val currentUser = auth.currentUser ?: return
        val friendUid = request.uid

        // Create references to the main user documents
        val myDocRef = db.collection("users").document(currentUser.uid)
        val friendDocRef = db.collection("users").document(friendUid)

        // Create references to the friendship sub-documents
        val myFriendshipRef = myDocRef.collection("friendships").document(friendUid)
        val theirFriendshipRef = friendDocRef.collection("friendships").document(currentUser.uid)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get both full user profiles first to ensure we have the correct, up-to-date data
                val myProfileDoc = myDocRef.get().await()
                val friendProfileDoc = friendDocRef.get().await()

                if (myProfileDoc.exists() && friendProfileDoc.exists()) {
                    // Prepare the data for my new friend
                    val friendData = hashMapOf(
                        "status" to "friends",
                        "displayName" to friendProfileDoc.getString("displayName"),
                        "photoUrl" to friendProfileDoc.getString("photoUrl"),
                        "shortId" to friendProfileDoc.getString("shortId"),
                        "friendedAt" to FieldValue.serverTimestamp()
                    )

                    // Prepare my data for my new friend's list
                    val myData = hashMapOf(
                        "status" to "friends",
                        "displayName" to myProfileDoc.getString("displayName"),
                        "photoUrl" to myProfileDoc.getString("photoUrl"),
                        "shortId" to myProfileDoc.getString("shortId"),
                        "friendedAt" to FieldValue.serverTimestamp()
                    )

                    // Write both documents in a single, safe transaction
                    db.batch().apply {
                        set(myFriendshipRef, friendData)
                        set(theirFriendshipRef, myData)
                    }.commit().await()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FriendRequestsActivity, "You are now friends with ${friendProfileDoc.getString("displayName")}!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FriendRequestsActivity, "Could not find user profiles.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FriendRequestsActivity, "Failed to accept request.", Toast.LENGTH_SHORT).show()
                    Log.e("ACCEPT_ERROR", "Error accepting friend request", e)
                }
            }
        }
    }

    override fun onDecline(request: FriendRequest) {
        val currentUser = auth.currentUser ?: return

        val myFriendshipRef = db.collection("users").document(currentUser.uid).collection("friendships").document(request.uid)
        val theirFriendshipRef = db.collection("users").document(request.uid).collection("friendships").document(currentUser.uid)

        db.batch().apply {
            delete(myFriendshipRef)
            delete(theirFriendshipRef)
        }.commit().addOnSuccessListener {
            Toast.makeText(this, "Request declined.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to decline request.", Toast.LENGTH_SHORT).show()
            Log.w("FRIEND_REQUESTS", "Error declining request", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}