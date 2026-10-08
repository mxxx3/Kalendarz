package pl.mojeroboty.app

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val Blue = Color(0xFF1269E9)
fun shownDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

class MainActivity : ComponentActivity() {
    private val store by lazy { AppStore(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Blue,
                background = Color(0xFFF4F7FC), surface = Color.White)) {
                WorkApp(store)
            }
        }
    }
}

@Composable
private fun WorkApp(store: AppStore) {
    val snapshot by store.snapshot.collectAsState()
    val sharedStatus by store.cloud.status.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf("Kalendarz") }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var mode by rememberSaveable { mutableStateOf("Miesiąc") }
    var formJob by remember { mutableStateOf<Job?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<String?>(null) }
    var clientForm by remember { mutableStateOf<Client?>(null) }
    var showClientForm by remember { mutableStateOf(false) }
    var paymentJobId by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    fun notifyError(e: Throwable) {
        Toast.makeText(context, e.message ?: "Nie udało się zapisać danych.", Toast.LENGTH_LONG).show()
    }
    fun save(job: Job) {
        scope.launch { runCatching { store.saveJob(job) }.onFailure { notifyError(it) } }
    }
    fun save(client: Client) {
        scope.launch { runCatching { store.saveClient(client) }.onFailure { notifyError(it) } }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = store.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Nie można zapisać pliku.")
            }.onSuccess { Toast.makeText(context,"Kopia zapisana",Toast.LENGTH_SHORT).show() }
                .onFailure { notifyError(it) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: error("Nie można odczytać pliku.")
            }.onSuccess { pendingImport = it }.onFailure { notifyError(it) }
        }
    }

    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Moje Roboty", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (sharedStatus.connected) "Wspólna ekipa · " +
                        (if (sharedStatus.fromCache) "offline" else "synchronizacja")
                        else "Twój kalendarz i zarobki w euro",
                        fontSize = 12.sp, color = Color.Gray)
                }
                IconButton(onClick = { tab = "Ustawienia" }) {
                    Icon(Icons.Default.Settings, "Ustawienia")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Kalendarz", "Roboty", "Klienci", "Finanse").forEach { page ->
                    val icon = when(page) {
                        "Kalendarz" -> Icons.Default.CalendarMonth
                        "Roboty" -> Icons.Default.Construction
                        "Klienci" -> Icons.Default.People
                        else -> Icons.Default.Euro
                    }
                    NavigationBarItem(selected = tab == page, onClick = { tab = page },
                        icon = { Icon(icon, page) }, label = { Text(page) })
                }
            }
        },
        floatingActionButton = {
            if(tab == "Kalendarz" || tab == "Roboty")
                ExtendedFloatingActionButton(
                    onClick = { formJob = null; showForm = true },
                    text = { Text("Dodaj robotę") },
                    icon = { Icon(Icons.Default.Add, null) },
                    containerColor = Blue, contentColor = Color.White)
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            when(tab) {
                "Kalendarz" -> CalendarScreen(snapshot.jobs, LocalDate.parse(selected), mode,
                    onMode = { mode = it }, onDate = { selected = it.toString() },
                    onJob = { details = it.id })
                "Roboty" -> {
                    OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(12.dp),
                        label = { Text("Szukaj roboty, adresu lub klienta") },
                        leadingIcon = { Icon(Icons.Default.Search, null) })
                    val filtered = snapshot.jobs.filter { job ->
                        listOf(job.title,job.address,job.notes,snapshot.clientName(job))
                            .joinToString(" ").contains(query,true)
                    }.sortedBy { it.start }
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(filtered, key = { it.id }) { job ->
                            JobCard(job,snapshot) { details = job.id }
                        }
                    }
                }
                "Klienci" -> {
                    Button(onClick = { clientForm = null; showClientForm = true },
                        modifier = Modifier.padding(12.dp)) { Text("+ Nowy klient") }
                    LazyColumn {
                        items(snapshot.clients, key = { it.id }) { c ->
                            ElevatedCard(onClick = { clientForm = c; showClientForm = true },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(c.name, fontWeight = FontWeight.Bold)
                                    if(c.phone.isNotBlank()) {
                                        TextButton(onClick = {
                                            context.startActivity(Intent(Intent.ACTION_DIAL,
                                                Uri.parse("tel:" + Uri.encode(c.phone))))
                                        }) { Text("Zadzwoń: " + c.phone) }
                                    }
                                    if(c.address.isNotBlank()) Text(c.address)
                                    Text("Saldo klienta: " + money(snapshot.jobs.filter { it.clientId == c.id }
                                        .sumOf { snapshot.remaining(it).coerceAtLeast(0) }))
                                    Text("Robót: " + snapshot.jobs.count { it.clientId == c.id } +
                                        if(c.archived) " · Archiwum" else "",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                "Finanse" -> FinanceScreen(snapshot)
                "Ustawienia" -> Column(Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ustawienia", style = MaterialTheme.typography.headlineSmall)
                    Text("Waluta: EUR (€) · Wersja 1.1")
                    CollaborationScreen(store)
                    HorizontalDivider()
                    Text("Kopia zapasowa danych lokalnych",
                        fontWeight = FontWeight.Bold)
                    Text("Eksport/import JSON dostępny w trybie osobistym. " +
                        "Dane wspólnej ekipy są w Firestore; nie eksportuj ich jako kopii lokalnej.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(enabled = !sharedStatus.connected, onClick = {
                        exportLauncher.launch("moje-roboty-" + LocalDate.now() + ".json")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Eksportuj lokalną kopię JSON") }
                    OutlinedButton(enabled = !sharedStatus.connected,
                        onClick = { importLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth()) { Text("Importuj lokalną kopię JSON") }
                }
            }
        }
    }
    if(showForm) JobForm(formJob, LocalDate.parse(selected), snapshot.clients, snapshot.jobs,
        onCancel = { showForm = false },
        onSave = { save(it); showForm = false })
    if(showClientForm) ClientForm(clientForm, onCancel = { showClientForm = false },
        onSave = { save(it); showClientForm = false })
    snapshot.jobs.firstOrNull { it.id == details }?.let { job ->
        JobDetails(job,snapshot,
            onClose = { details = null },
            onEdit = { formJob = job; showForm = true; details = null },
            onPay = { paymentJobId = job.id; details = null },
            onStatus = { save(job.copy(status = it)); details = null },
            onDelete = {
                scope.launch { runCatching { store.removeJob(job.id) }
                    .onFailure { notifyError(it) } }
                details = null
            },
            onDuplicate = {
                formJob = job.copy(id = AppStore.newId(), start = LocalDate.parse(selected),
                    end = LocalDate.parse(selected), status = "PLANNED")
                showForm = true
                details = null
            },
            onDeletePayment = { payment ->
                scope.launch { runCatching { store.removePayment(payment.id) }
                    .onFailure { notifyError(it) } }
            })
    }
    snapshot.jobs.firstOrNull { it.id == paymentJobId }?.let { job ->
        PaymentForm(job, onCancel = { paymentJobId = null }, onSave = {
            scope.launch { runCatching { store.addPayment(it) }.onFailure { notifyError(it) } }
            paymentJobId = null
        })
    }
    if(pendingImport != null) AlertDialog(
        onDismissRequest = { pendingImport = null },
        title = { Text("Zastąpić wszystkie dane?") },
        text = { Text("Obecne roboty, klienci i wpłaty zastąpią dane z pliku. " +
            "Błędny import nie zmieni bazy.") },
        confirmButton = { Button(onClick = {
            val input = pendingImport ?: return@Button
            pendingImport = null
            scope.launch {
                runCatching { store.importJson(input) }
                    .onSuccess { Toast.makeText(context,"Przywrócono kopię",Toast.LENGTH_SHORT).show() }
                    .onFailure { notifyError(it) }
            }
        }) { Text("Zastąp") } },
        dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Anuluj") } }
    )
}

@Composable
fun ChooseDate(label: String, date: LocalDate, change: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, year, month, day ->
            change(LocalDate.of(year, month+1, day))
        },date.year,date.monthValue-1,date.dayOfMonth).show()
    },modifier = Modifier.fillMaxWidth()) {
        Text(label + ": " + shownDate(date))
    }
}

@Composable
private fun JobForm(initial: Job?, day: LocalDate, clients: List<Client>, jobs: List<Job>,
    onCancel: () -> Unit, onSave: (Job) -> Unit
) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var start by rememberSaveable(initial?.id) { mutableStateOf((initial?.start ?: day).toString()) }
    var end by rememberSaveable(initial?.id) { mutableStateOf((initial?.end ?: day).toString()) }
    var price by rememberSaveable(initial?.id) {
        mutableStateOf(initial?.let { java.math.BigDecimal.valueOf(it.cents,2).toPlainString() } ?: "")
    }
    var address by rememberSaveable(initial?.id) { mutableStateOf(initial?.address ?: "") }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.notes ?: "") }
    var client by rememberSaveable(initial?.id) { mutableStateOf(initial?.clientId) }
    var status by rememberSaveable(initial?.id) { mutableStateOf(initial?.status ?: "PLANNED") }
    var more by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var acceptConflict by remember { mutableStateOf(false) }
    val conflicts = jobs.filter { j ->
        j.id != initial?.id && j.status != "CANCELLED" &&
            intersects(j, LocalDate.parse(start), LocalDate.parse(end))
    }
    AlertDialog(onDismissRequest = onCancel,
        title = { Text(if(initial == null) "Dodaj robotę" else "Edytuj robotę") },
        text = { Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nazwa *") },
                modifier = Modifier.fillMaxWidth())
            ChooseDate("Od", LocalDate.parse(start)) { start = it.toString()
                if(LocalDate.parse(end) < it) end = it.toString()
            }
            ChooseDate("Do", LocalDate.parse(end)) { end = it.toString() }
            OutlinedTextField(price, { price = it }, label = { Text("Za całą robotę (€) *") },
                modifier = Modifier.fillMaxWidth())
            Text("0 € oznacza wycenę do ustalenia.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { more = !more }) {
                Text(if(more) "Ukryj szczegóły" else "Więcej szczegółów")
            }
            if(more) {
                OutlinedTextField(address, { address = it }, label = { Text("Adres") })
                OutlinedTextField(note, { note = it }, label = { Text("Notatka") })
                Text("Klient")
                FilterChip(selected = client == null,onClick = { client = null },
                    label = { Text("Bez klienta") })
                clients.filter { !it.archived || it.id == client }.forEach { c ->
                    FilterChip(selected = client == c.id, onClick = { client = c.id },
                        label = { Text(c.name) })
                }
                Text("Status")
                STATUS.forEach { (key,label) ->
                    FilterChip(selected = status == key, onClick = { status = key },
                        label = { Text(label) })
                }
            }
            if(conflicts.isNotEmpty()) {
                Text("Uwaga: ten termin nakłada się na: " +
                    conflicts.joinToString { it.title }, color = MaterialTheme.colorScheme.error)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = acceptConflict, onCheckedChange = { acceptConflict = it })
                    Text("Zapisz mimo kolizji")
                }
            }
            if(error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(onClick = {
            try {
                require(name.isNotBlank()) { "Podaj nazwę roboty." }
                require(LocalDate.parse(end) >= LocalDate.parse(start)) {
                    "Data końca jest wcześniejsza niż początek."
                }
                require(conflicts.isEmpty() || acceptConflict) {
                    "Potwierdź zapis mimo kolizji terminów."
                }
                val cents = parseEuro(price)
                onSave(Job(initial?.id ?: AppStore.newId(),name.trim(),
                    LocalDate.parse(start),LocalDate.parse(end),cents,status,client,
                    address.trim(),note.trim()))
            } catch(e: Exception) { error = e.message ?: "Niepoprawne dane" }
        }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Anuluj") } })
}

@Composable
private fun ClientForm(initial: Client?, onCancel: () -> Unit, onSave: (Client) -> Unit) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var phone by rememberSaveable(initial?.id) { mutableStateOf(initial?.phone ?: "") }
    var address by rememberSaveable(initial?.id) { mutableStateOf(initial?.address ?: "") }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.notes ?: "") }
    var archived by rememberSaveable(initial?.id) { mutableStateOf(initial?.archived ?: false) }
    AlertDialog(onDismissRequest = onCancel, title = { Text("Klient") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { name = it }, label = { Text("Nazwa *") })
            OutlinedTextField(phone, { phone = it }, label = { Text("Telefon") })
            OutlinedTextField(address, { address = it }, label = { Text("Adres") })
            OutlinedTextField(note, { note = it }, label = { Text("Notatka") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked=archived,onCheckedChange = { archived = it })
                Text("Archiwum")
            }
        } },
        confirmButton = { Button(enabled=name.isNotBlank(), onClick = {
            onSave(Client(initial?.id ?: AppStore.newId(),name.trim(),phone,
                address,note,archived))
        }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Anuluj") } })
}

@Composable
private fun PaymentForm(job: Job, onCancel: () -> Unit, onSave: (Payment) -> Unit) {
    var price by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var method by rememberSaveable { mutableStateOf("Gotówka") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onCancel,title={Text("Wpłata: " + job.title)},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(price, { price = it }, label = { Text("Kwota (€)") })
            ChooseDate("Otrzymano",LocalDate.parse(date)) { date = it.toString() }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Gotówka","Przelew").forEach { m ->
                    FilterChip(selected=method==m,onClick={method=m},label={Text(m)})
                }
            }
            OutlinedTextField(note,{note=it},label={Text("Notatka")})
            if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
        }},
        confirmButton={Button(onClick={
            try {
                val cents = parseEuro(price)
                require(cents>0) { "Wpłata musi być dodatnia." }
                onSave(Payment(AppStore.newId(),job.id,cents,LocalDate.parse(date),method,note))
            } catch(e: Exception) { error=e.message ?: "Błąd kwoty" }
        }){Text("Zapisz")}},
        dismissButton={TextButton(onClick=onCancel){Text("Anuluj")}})
}

@Composable
private fun JobDetails(job: Job, state: Snapshot,
    onClose: () -> Unit, onEdit: () -> Unit, onPay: () -> Unit,
    onStatus: (String) -> Unit, onDelete: () -> Unit,
    onDuplicate: () -> Unit, onDeletePayment: (Payment) -> Unit
) {
    var deleteConfirm by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onClose,title={Text(job.title)},
        text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(shownDate(job.start)+" – "+shownDate(job.end)+" ("+days(job)+" dni)")
            Text(STATUS[job.status] ?: job.status,color=Blue)
            if(state.clientName(job).isNotEmpty()) Text("Klient: "+state.clientName(job))
            if(job.address.isNotBlank()) Text("Adres: "+job.address)
            if(job.notes.isNotBlank()) Text(job.notes)
            HorizontalDivider()
            Text("Kwota uzgodniona: "+money(job.cents),fontWeight=FontWeight.Bold)
            Text("Wpłacono: "+money(state.paid(job)))
            Text("Do zapłaty: "+money(state.remaining(job)),fontWeight=FontWeight.Bold)
            if(state.remaining(job)<0) Text("Nadpłata",color=MaterialTheme.colorScheme.error)
            Button(onClick=onPay){Text("+ Dodaj wpłatę")}
            state.payments.filter { it.jobId == job.id }.forEach { p ->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(shownDate(p.paidAt)+" · "+money(p.cents)+" · "+p.method,
                        modifier=Modifier.weight(1f),fontSize=12.sp)
                    IconButton(onClick={onDeletePayment(p)}){
                        Icon(Icons.Default.DeleteOutline,"Usuń wpłatę")
                    }
                }
            }
            HorizontalDivider()
            Text("Zmień status")
            STATUS.forEach { (code,label) ->
                FilterChip(selected=job.status==code,onClick={onStatus(code)},label={Text(label)})
            }
            TextButton(onClick=onDuplicate){Text("Duplikuj")}
            TextButton(onClick={deleteConfirm=true}){Text("Usuń robotę")}
        }},
        confirmButton={Button(onClick=onEdit){Text("Edytuj")}},
        dismissButton={TextButton(onClick=onClose){Text("Zamknij")}})
    if(deleteConfirm) AlertDialog(onDismissRequest={deleteConfirm=false},
        title={Text("Usunąć robotę?")},
        text={Text("Robota zniknie z kalendarza; kopia JSON zachowa historię.")},
        confirmButton={Button(onClick={deleteConfirm=false;onDelete()}){Text("Usuń")}},
        dismissButton={TextButton(onClick={deleteConfirm=false}){Text("Anuluj")}})
}

@Composable
fun JobCard(job: Job,state: Snapshot, onClick: () -> Unit) {
    ElevatedCard(onClick=onClick,modifier=Modifier.fillMaxWidth().padding(8.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(job.title,modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
                Text(money(job.cents),fontWeight=FontWeight.Bold,color=Blue)
            }
            Text(shownDate(job.start)+" – "+shownDate(job.end)+" · "+days(job)+" dni")
            Text((STATUS[job.status] ?: job.status)+
                " · Pozostało "+money(state.remaining(job)),
                fontSize=12.sp,color=Color.Gray)
        }
    }
}

@Composable
private fun FinanceScreen(state: Snapshot) {
    var monthText by rememberSaveable { mutableStateOf(LocalDate.now().toString().take(7)) }
    val month=LocalDate.parse(monthText+"-01")
    val start=month.withDayOfMonth(1)
    val end=month.withDayOfMonth(month.lengthOfMonth())
    val active=state.jobs.filter { it.status!="CANCELLED" }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={monthText=month.minusMonths(1).toString().take(7)}){
                Icon(Icons.Default.ChevronLeft,"Poprzedni miesiąc")
            }
            Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",
                Locale.forLanguageTag("pl-PL"))),modifier=Modifier.weight(1f),
                fontWeight=FontWeight.Bold)
            IconButton(onClick={monthText=month.plusMonths(1).toString().take(7)}){
                Icon(Icons.Default.ChevronRight,"Następny miesiąc")
            }
        }
        FinanceTile("Wartość robót rozpoczętych", money(plannedValue(active,start,end)))
        FinanceTile("Wpłaty otrzymane w miesiącu", money(received(state.payments,start,end)))
        FinanceTile("Aktualne należności (wszystkie terminy)",
            money(active.sumOf { state.remaining(it).coerceAtLeast(0) }))
        Text("Kwota umowy liczy się raz w dniu rozpoczęcia. " +
            "Wpłaty według daty otrzymania. Zrobione ≠ zapłacone.", color=Color.Gray)
        Text("Niezapłacone roboty",fontWeight=FontWeight.Bold)
        active.filter { state.remaining(it)>0 }.forEach {
            Text(it.title+" — "+money(state.remaining(it)))
        }
    }
}

@Composable
private fun FinanceTile(title: String,amount: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title,color=Color.Gray)
            Text(amount,fontSize=26.sp,color=Blue,fontWeight=FontWeight.Bold)
        }
    }
}
