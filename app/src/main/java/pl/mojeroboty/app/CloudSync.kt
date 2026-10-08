package pl.mojeroboty.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

data class SharedStatus(
    val configured: Boolean = false,
    val userId: String = "",
    val email: String = "",
    val workspaceId: String = "",
    val owner: Boolean = false,
    val memberUids: List<String> = emptyList(),
    val connected: Boolean = false,
    val loading: Boolean = false,
    val fromCache: Boolean = false,
    val error: String = ""
)

/**
 * Optional cloud mode. When active, Cloud Firestore is the only source of truth
 * for shared data; local SQLite is left untouched for the solo mode.
 * Cloud Firestore caches writes and snapshots while offline. Conflicting edits
 * to the same document are last-write-wins.
 */
class CloudSync(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("shared_workspace", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val configured = BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
        BuildConfig.FIREBASE_APP_ID.isNotBlank() && BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()
    private val app: FirebaseApp? = if (configured) {
        FirebaseApp.getApps(context).firstOrNull { it.name == "MojeRobotyCloud" }
            ?: FirebaseApp.initializeApp(context.applicationContext, FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID).build(), "MojeRobotyCloud")
    } else null
    private val auth: FirebaseAuth? = app?.let { FirebaseAuth.getInstance(it) }
    private val db: FirebaseFirestore? = app?.let { FirebaseFirestore.getInstance(it) }
    private val mutableStatus = MutableStateFlow(SharedStatus(
        configured = configured, userId = auth?.currentUser?.uid.orEmpty(),
        email = auth?.currentUser?.email.orEmpty()))
    val status: StateFlow<SharedStatus> = mutableStatus
    private val mutableSnapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = mutableSnapshot
    private val registrations = mutableListOf<ListenerRegistration>()
    private var jobList: List<Job> = emptyList()
    private var clientList: List<Client> = emptyList()
    private var paymentList: List<Payment> = emptyList()
    private var jobsReady = false
    private var clientsReady = false
    private var paymentsReady = false
    private var jobsCached = false
    private var clientsCached = false
    private var paymentsCached = false

    init {
        val savedWorkspace = preferences.getString("workspaceId", "").orEmpty()
        if (savedWorkspace.isNotBlank() && auth?.currentUser != null &&
            preferences.getString("boundUid", "") == auth?.currentUser?.uid) {
            scope.launch {
                runCatching { join(savedWorkspace) }.onFailure { cause ->
                    // After a previously authorized session, allow offline restoration
                    // from Firestore's own cache for the same authenticated UID.
                    runCatching {
                        val cached = workspace(savedWorkspace).get(Source.CACHE).await()
                        val users = cached.get("memberUids") as? List<*> ?: emptyList<Any>()
                        require(auth.currentUser?.uid in users) { "Brak lokalnie potwierdzonego członkostwa." }
                        activate(cached, savedWorkspace, true)
                    }.onFailure { setError(cause) }
                }
            }
        }
    }

    private fun requireAuth(): String = auth?.currentUser?.uid ?: error("Najpierw zaloguj się.")
    private fun requireDb(): FirebaseFirestore = db ?: error("Brak konfiguracji Firebase w kompilacji.")
    private fun workspace(id: String) = requireDb().collection("workspaces").document(id)
    private fun selectedWorkspace() = workspace(mutableStatus.value.workspaceId.takeIf {
        mutableStatus.value.connected && it.isNotEmpty()
    } ?: error("Nie połączono jeszcze przestrzeni zespołu."))

    private fun setError(error: Throwable) {
        mutableStatus.value = mutableStatus.value.copy(error = error.localizedMessage ?: "Błąd synchronizacji")
    }

    suspend fun register(email: String, password: String) {
        require(configured) { "Najpierw skonfiguruj Firebase." }
        require(password.length >= 6) { "Hasło musi mieć minimum 6 znaków." }
        val user = (auth ?: error("Brak Firebase Auth"))
            .createUserWithEmailAndPassword(email.trim(), password).await().user ?: error("Brak użytkownika")
        mutableStatus.value = mutableStatus.value.copy(userId = user.uid,
            email = user.email.orEmpty(), error = "")
    }

    suspend fun login(email: String, password: String) {
        require(configured) { "Najpierw skonfiguruj Firebase." }
        val user = (auth ?: error("Brak Firebase Auth"))
            .signInWithEmailAndPassword(email.trim(), password).await().user ?: error("Brak użytkownika")
        mutableStatus.value = mutableStatus.value.copy(userId = user.uid,
            email = user.email.orEmpty(), error = "")
        preferences.getString("workspaceId", "")?.takeIf { it.isNotEmpty() }?.let { join(it) }
    }

    suspend fun create(name: String) {
        val uid = requireAuth()
        require(name.isNotBlank()) { "Wpisz nazwę ekipy." }
        val doc = requireDb().collection("workspaces").document()
        doc.set(mapOf("name" to name.trim(), "ownerId" to uid,
            "memberUids" to listOf(uid), "createdAt" to FieldValue.serverTimestamp())).await()
        join(doc.id)
    }

    suspend fun join(id: String) {
        val uid = requireAuth()
        val safeId = id.trim()
        require(Regex("[a-zA-Z0-9]{10,100}").matches(safeId)) { "Niepoprawny identyfikator ekipy." }
        mutableStatus.value = mutableStatus.value.copy(loading = true, error = "")
        try {
            val doc = workspace(safeId).get(Source.SERVER).await()
            val users = doc.get("memberUids") as? List<*> ?: error("Nie ma takiej ekipy.")
            require(uid in users) { "Właściciel musi najpierw dodać Twój identyfikator użytkownika." }
            activate(doc, safeId, false)
        } catch (e: Exception) {
            mutableStatus.value = mutableStatus.value.copy(loading = false)
            throw e
        }
    }

    private fun activate(doc: DocumentSnapshot, id: String, cached: Boolean) {
        stopListeners()
        val uid = requireAuth()
        preferences.edit().putString("workspaceId", id).putString("boundUid", uid).apply()
        mutableStatus.value = mutableStatus.value.copy(
            connected = true, loading = true, workspaceId = id,
            owner = doc.getString("ownerId") == uid,
            memberUids = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            error = "", fromCache = cached)
        observe(id)
    }

    private fun observe(id: String) {
        val doc = workspace(id)
        registrations.add(doc.addSnapshotListener { value, exception ->
            if (exception != null) { setError(exception); disconnect(); return@addSnapshotListener }
            if (value != null) {
                val users = value.get("memberUids") as? List<*>
                if (value.exists() && requireAuth() !in (users ?: emptyList<Any>())) {
                    disconnect()
                } else {
                    mutableStatus.value = mutableStatus.value.copy(
                        owner = value.getString("ownerId") == auth?.currentUser?.uid,
                        memberUids = (users ?: emptyList<Any>()).filterIsInstance<String>())
                }
            }
        })
        registrations.add(doc.collection("jobs").addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            if (error != null) { setError(error); return@addSnapshotListener }
            if (value == null) return@addSnapshotListener
            try {
                jobList = value.documents.filter { it.getBoolean("deleted") != true }.map { d ->
                    Job(d.id, d.getString("title") ?: "", LocalDate.parse(d.getString("start")),
                        LocalDate.parse(d.getString("end")), d.getLong("cents") ?: 0L,
                        d.getString("status") ?: "PLANNED", d.getString("clientId"),
                        d.getString("address").orEmpty(), d.getString("notes").orEmpty())
                }.sortedBy { it.start }
                jobsReady = true; jobsCached = value.metadata.isFromCache; publish()
            } catch (e: Exception) { setError(e) }
        })
        registrations.add(doc.collection("clients").addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            if (error != null) { setError(error); return@addSnapshotListener }
            if (value == null) return@addSnapshotListener
            try {
                clientList = value.documents.map { d ->
                    Client(d.id, d.getString("name").orEmpty(), d.getString("phone").orEmpty(),
                        d.getString("address").orEmpty(), d.getString("notes").orEmpty(),
                        d.getBoolean("archived") ?: false)
                }.sortedBy { it.name }
                clientsReady = true; clientsCached = value.metadata.isFromCache; publish()
            } catch (e: Exception) { setError(e) }
        })
        registrations.add(doc.collection("payments").addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            if (error != null) { setError(error); return@addSnapshotListener }
            if (value == null) return@addSnapshotListener
            try {
                paymentList = value.documents.map { d ->
                    Payment(d.id, d.getString("jobId") ?: "",
                        d.getLong("cents") ?: 0L, LocalDate.parse(d.getString("paidAt")),
                        d.getString("method") ?: "Gotówka", d.getString("notes").orEmpty())
                }.sortedByDescending { it.paidAt }
                paymentsReady = true; paymentsCached = value.metadata.isFromCache; publish()
            } catch (e: Exception) { setError(e) }
        })
    }

    private fun publish() {
        val ready = jobsReady && clientsReady && paymentsReady
        mutableStatus.value = mutableStatus.value.copy(loading = !ready,
            fromCache = jobsCached || clientsCached || paymentsCached)
        if (ready) mutableSnapshot.value = Snapshot(jobList, clientList, paymentList, true)
    }

    private fun stopListeners() {
        registrations.forEach { it.remove() }
        registrations.clear()
        jobsReady = false; clientsReady = false; paymentsReady = false
        jobsCached = false; clientsCached = false; paymentsCached = false
        jobList = emptyList(); clientList = emptyList(); paymentList = emptyList()
        mutableSnapshot.value = Snapshot()
    }

    fun disconnect() {
        stopListeners()
        preferences.edit().remove("workspaceId").remove("boundUid").apply()
        mutableStatus.value = mutableStatus.value.copy(
            connected = false, workspaceId = "", owner = false,
            memberUids = emptyList(), loading = false, fromCache = false)
    }

    fun logout() {
        disconnect()
        auth?.signOut()
        mutableStatus.value = SharedStatus(configured = configured)
    }

    suspend fun addMember(otherUid: String) {
        require(mutableStatus.value.owner) { "Tylko właściciel dodaje współpracowników." }
        val uid = otherUid.trim()
        require(Regex("[a-zA-Z0-9]{15,150}").matches(uid)) { "Nieprawidłowe UID." }
        selectedWorkspace().update("memberUids", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun removeMember(otherUid: String) {
        require(mutableStatus.value.owner) { "Tylko właściciel może odebrać dostęp." }
        require(otherUid != requireAuth()) { "Nie można usunąć właściciela." }
        selectedWorkspace().update("memberUids", FieldValue.arrayRemove(otherUid)).await()
    }

    private fun jobMap(j: Job): Map<String, Any?> = mapOf(
        "title" to j.title, "start" to j.start.toString(), "end" to j.end.toString(),
        "cents" to j.cents, "status" to j.status, "clientId" to j.clientId,
        "address" to j.address, "notes" to j.notes, "deleted" to false,
        "updatedBy" to requireAuth(), "updatedAt" to FieldValue.serverTimestamp())

    private fun clientMap(c: Client): Map<String, Any?> = mapOf(
        "name" to c.name, "phone" to c.phone, "address" to c.address,
        "notes" to c.notes, "archived" to c.archived,
        "updatedBy" to requireAuth(), "updatedAt" to FieldValue.serverTimestamp())

    private fun paymentMap(p: Payment): Map<String, Any?> = mapOf(
        "jobId" to p.jobId, "cents" to p.cents, "paidAt" to p.paidAt.toString(),
        "method" to p.method, "notes" to p.notes,
        "updatedBy" to requireAuth(), "updatedAt" to FieldValue.serverTimestamp())

    suspend fun saveJob(job: Job) {
        require(job.title.isNotBlank() && !job.end.isBefore(job.start) && job.cents >= 0L)
        selectedWorkspace().collection("jobs").document(job.id).set(jobMap(job)).await()
    }

    suspend fun saveClient(client: Client) {
        require(client.name.isNotBlank())
        selectedWorkspace().collection("clients").document(client.id).set(clientMap(client)).await()
    }

    suspend fun addPayment(payment: Payment) {
        require(payment.cents > 0 && snapshot.value.jobs.any { it.id == payment.jobId })
        selectedWorkspace().collection("payments").document(payment.id).set(paymentMap(payment)).await()
    }

    suspend fun removeJob(id: String) {
        selectedWorkspace().collection("jobs").document(id).update(
            mapOf("deleted" to true, "updatedBy" to requireAuth(),
                "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun removePayment(id: String) {
        selectedWorkspace().collection("payments").document(id).delete().await()
    }

    /**
     * Explicit one-off migration. Never overwrite or merge into a nonempty workspace.
     * 450-document limit leaves capacity below Firestore's batch limit.
     */
    suspend fun uploadLocal(copy: Snapshot) {
        require(mutableStatus.value.owner) { "Import lokalnych danych wymaga roli właściciela." }
        require(copy.jobs.size + copy.clients.size + copy.payments.size <= 450) {
            "Za dużo wpisów do jednorazowego importu (maksimum 450)."
        }
        val doc = selectedWorkspace()
        for (collection in listOf("jobs", "clients", "payments")) {
            require(doc.collection(collection).limit(1).get(Source.SERVER).await().isEmpty) {
                "Ekipa ma już dane. Nie nadpisuję ani nie łączę automatycznie."
            }
        }
        val batch = requireDb().batch()
        copy.clients.forEach { batch.set(doc.collection("clients").document(it.id), clientMap(it)) }
        copy.jobs.forEach { batch.set(doc.collection("jobs").document(it.id), jobMap(it)) }
        copy.payments.forEach { batch.set(doc.collection("payments").document(it.id), paymentMap(it)) }
        batch.commit().await()
    }
}
