package pl.mojeroboty.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/** Local-only SQLite persistence. There is no network permission or account dependency. */
private class WorkDb(context: Context) : SQLiteOpenHelper(context, "moje_roboty.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE clients(
            id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL,
            address TEXT NOT NULL, notes TEXT NOT NULL, archived INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("""CREATE TABLE jobs(
            id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL,
            start_date TEXT NOT NULL, end_date TEXT NOT NULL,
            cents INTEGER NOT NULL, status TEXT NOT NULL, client_id TEXT,
            address TEXT NOT NULL, notes TEXT NOT NULL, deleted INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE RESTRICT)""")
        db.execSQL("""CREATE TABLE payments(
            id TEXT PRIMARY KEY NOT NULL, job_id TEXT NOT NULL, cents INTEGER NOT NULL,
            paid_at TEXT NOT NULL, method TEXT NOT NULL, notes TEXT NOT NULL,
            FOREIGN KEY(job_id) REFERENCES jobs(id) ON DELETE CASCADE)""")
        db.execSQL("CREATE INDEX jobs_dates ON jobs(start_date,end_date)")
        db.execSQL("CREATE INDEX jobs_client ON jobs(client_id)")
        db.execSQL("CREATE INDEX payments_job ON payments(job_id)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Brak migracji bazy danych z wersji $oldVersion do $newVersion; nie wolno usuwać danych.")
    }
}

class AppStore(context: Context) {
    private val helper = WorkDb(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = mutable

    init { scope.launch { refresh() } }

    private fun Cursor.str(name: String) = getString(getColumnIndexOrThrow(name))
    private fun Cursor.num(name: String) = getLong(getColumnIndexOrThrow(name))
    private fun Cursor.int(name: String) = getInt(getColumnIndexOrThrow(name))

    private fun getSnapshot(): Snapshot {
        val db = helper.readableDatabase
        val clients = mutableListOf<Client>()
        db.rawQuery("SELECT * FROM clients ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) clients += Client(
                c.str("id"), c.str("name"), c.str("phone"), c.str("address"),
                c.str("notes"), c.int("archived") != 0)
        }
        val jobs = mutableListOf<Job>()
        db.rawQuery("SELECT * FROM jobs WHERE deleted = 0 ORDER BY start_date,title", null).use { c ->
            while (c.moveToNext()) jobs += Job(c.str("id"), c.str("title"),
                LocalDate.parse(c.str("start_date")), LocalDate.parse(c.str("end_date")),
                c.num("cents"), c.str("status"), c.getString(c.getColumnIndexOrThrow("client_id")),
                c.str("address"), c.str("notes"))
        }
        val payments = mutableListOf<Payment>()
        db.rawQuery("SELECT * FROM payments ORDER BY paid_at DESC", null).use { c ->
            while (c.moveToNext()) payments += Payment(c.str("id"), c.str("job_id"),
                c.num("cents"), LocalDate.parse(c.str("paid_at")), c.str("method"), c.str("notes"))
        }
        return Snapshot(jobs, clients, payments, true)
    }

    suspend fun refresh() = withContext(Dispatchers.IO) { mutable.value = getSnapshot() }

    private fun clientValues(c: Client) = ContentValues().apply {
        put("id", c.id); put("name", c.name); put("phone", c.phone)
        put("address", c.address); put("notes", c.notes); put("archived", if (c.archived) 1 else 0)
    }

    private fun jobValues(j: Job) = ContentValues().apply {
        put("id", j.id); put("title", j.title); put("start_date", j.start.toString())
        put("end_date", j.end.toString()); put("cents", j.cents); put("status", j.status)
        put("client_id", j.clientId); put("address", j.address); put("notes", j.notes); put("deleted", 0)
    }

    private fun paymentValues(p: Payment) = ContentValues().apply {
        put("id", p.id); put("job_id", p.jobId); put("cents", p.cents)
        put("paid_at", p.paidAt.toString()); put("method", p.method); put("notes", p.notes)
    }

    suspend fun saveJob(job: Job) = withContext(Dispatchers.IO) {
        require(job.title.isNotBlank() && !job.end.isBefore(job.start) && job.cents >= 0)
        helper.writableDatabase.insertWithOnConflict("jobs", null, jobValues(job), SQLiteDatabase.CONFLICT_REPLACE)
            .also { check(it != -1L) }
        refresh()
    }

    suspend fun saveClient(client: Client) = withContext(Dispatchers.IO) {
        require(client.name.isNotBlank())
        helper.writableDatabase.insertWithOnConflict("clients", null, clientValues(client), SQLiteDatabase.CONFLICT_REPLACE)
            .also { check(it != -1L) }
        refresh()
    }

    suspend fun addPayment(payment: Payment) = withContext(Dispatchers.IO) {
        require(payment.cents > 0 && getSnapshot().jobs.any { it.id == payment.jobId })
        helper.writableDatabase.insertOrThrow("payments", null, paymentValues(payment))
        refresh()
    }

    suspend fun removeJob(id: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.execSQL("UPDATE jobs SET deleted = 1 WHERE id = ?", arrayOf(id))
        refresh()
    }

    suspend fun removePayment(id: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("payments", "id = ?", arrayOf(id))
        refresh()
    }

    /** All records, including soft-deleted jobs, are included for a lossless export. */
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val state = getSnapshot()
        val jobsAll = mutableListOf<Pair<Job, Boolean>>()
        helper.readableDatabase.rawQuery("SELECT * FROM jobs", null).use { c ->
            while (c.moveToNext()) jobsAll += Job(
                c.str("id"), c.str("title"), LocalDate.parse(c.str("start_date")),
                LocalDate.parse(c.str("end_date")), c.num("cents"), c.str("status"),
                c.getString(c.getColumnIndexOrThrow("client_id")), c.str("address"), c.str("notes")
            ) to (c.int("deleted") == 1)
        }
        JSONObject().apply {
            put("schemaVersion", 1); put("currency", "EUR")
            put("clients", JSONArray().apply { state.clients.forEach { c ->
                put(JSONObject().apply { put("id",c.id); put("name",c.name); put("phone",c.phone)
                    put("address",c.address); put("notes",c.notes); put("archived",c.archived) }) } })
            put("jobs", JSONArray().apply { jobsAll.forEach { (j,deleted) ->
                put(JSONObject().apply { put("id",j.id); put("title",j.title); put("start",j.start.toString())
                    put("end",j.end.toString()); put("cents",j.cents); put("status",j.status)
                    put("clientId",j.clientId ?: JSONObject.NULL); put("address",j.address)
                    put("notes",j.notes); put("deleted",deleted) }) } })
            put("payments", JSONArray().apply { state.payments.forEach { p ->
                put(JSONObject().apply { put("id",p.id); put("jobId",p.jobId); put("cents",p.cents)
                    put("paidAt",p.paidAt.toString()); put("method",p.method); put("notes",p.notes) }) } })
        }.toString(2)
    }

    /** Parse and verify entire document before touching the database; commit atomically. */
    suspend fun importJson(text: String) = withContext(Dispatchers.IO) {
        require(text.length <= 20_000_000) { "Plik kopii jest za duży." }
        val json = JSONObject(text)
        require(json.getInt("schemaVersion") == 1 && json.getString("currency") == "EUR") {
            "Nieobsługiwana wersja kopii lub waluta. Import wymaga EUR."
        }
        val clients = buildList {
            val arr = json.getJSONArray("clients")
            repeat(arr.length()) { val x = arr.getJSONObject(it)
                add(Client(x.getString("id"), x.getString("name"), x.getString("phone"),
                    x.getString("address"), x.getString("notes"), x.getBoolean("archived"))) }
        }
        val jobs = buildList {
            val arr = json.getJSONArray("jobs")
            repeat(arr.length()) { val x = arr.getJSONObject(it)
                val j = Job(x.getString("id"), x.getString("title"), LocalDate.parse(x.getString("start")),
                    LocalDate.parse(x.getString("end")), x.getLong("cents"), x.getString("status"),
                    x.optString("clientId").takeIf { it.isNotBlank() && it != "null" },
                    x.getString("address"), x.getString("notes"))
                require(j.title.isNotBlank() && j.end >= j.start && j.cents >= 0 && j.status in STATUS)
                add(j to x.getBoolean("deleted"))
            }
        }
        val payments = buildList {
            val arr = json.getJSONArray("payments")
            repeat(arr.length()) { val x = arr.getJSONObject(it)
                val p = Payment(x.getString("id"), x.getString("jobId"), x.getLong("cents"),
                    LocalDate.parse(x.getString("paidAt")), x.getString("method"), x.getString("notes"))
                require(p.cents > 0); add(p)
            }
        }
        require(clients.map { it.id }.distinct().size == clients.size)
        require(jobs.map { it.first.id }.distinct().size == jobs.size)
        require(payments.map { it.id }.distinct().size == payments.size)
        require(clients.all { it.name.isNotBlank() })
        require(jobs.all { it.first.clientId == null || clients.any { c -> c.id == it.first.clientId } })
        require(payments.all { p -> jobs.any { it.first.id == p.jobId } })

        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("payments", null, null)
            db.delete("jobs", null, null)
            db.delete("clients", null, null)
            clients.forEach { db.insertOrThrow("clients", null, clientValues(it)) }
            jobs.forEach { (j, deleted) ->
                db.insertOrThrow("jobs", null, jobValues(j).apply { put("deleted", if (deleted) 1 else 0) })
            }
            payments.forEach { db.insertOrThrow("payments", null, paymentValues(it)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
