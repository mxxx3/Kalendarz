package pl.mojeroboty.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CollaborationScreen(store: AppStore) {
    val cloud = store.cloud
    val status by cloud.status.collectAsState()
    val remote by cloud.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by remember { mutableStateOf(false) }
    var teamName by remember { mutableStateOf("Moja ekipa") }
    var teamCode by remember { mutableStateOf("") }
    var colleagueUid by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf(false) }
    var revokeUid by remember { mutableStateOf<String?>(null) }

    fun copy(value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Moje Roboty", value))
        Toast.makeText(context, "Skopiowano", Toast.LENGTH_SHORT).show()
    }
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = ""
        scope.launch {
            try {
                action()
            } catch (t: Exception) {
                error = t.localizedMessage ?: "Operacja się nie powiodła."
            } finally {
                busy = false
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Wspólna ekipa — synchronizacja", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        Text("Te same roboty, klienci i wpłaty na obu telefonach. " +
            "Każdy pracownik używa własnego konta, lecz jednej przestrzeni ekipy.",
            style = MaterialTheme.typography.bodySmall)
        if (!status.configured) {
            Icon(Icons.Default.CloudOff, contentDescription = null,
                tint = MaterialTheme.colorScheme.error)
            Text("Synchronizacja nie jest jeszcze skonfigurowana w tym APK.",
                fontWeight = FontWeight.SemiBold)
            Text("Administrator musi utworzyć projekt Firebase (Authentication + Cloud Firestore), " +
                "wdrożyć reguły bezpieczeństwa i skompilować aplikację z jego identyfikatorami. " +
                "Tryb lokalny działa niezależnie. Instrukcje: docs/SYNCHRONIZACJA_FIREBASE.md",
                style = MaterialTheme.typography.bodySmall)
        } else if (status.userId.isEmpty()) {
            Text(if (register) "Utwórz konto" else "Zaloguj się",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = email, onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email") })
            OutlinedTextField(value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Hasło") },
                visualTransformation = PasswordVisualTransformation())
            Button(enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                onClick = { perform {
                    if (register) cloud.register(email, password)
                    else cloud.login(email, password)
                } }, modifier = Modifier.fillMaxWidth()) {
                Text(if (register) "Załóż konto" else "Zaloguj")
            }
            TextButton(onClick = { register = !register }) {
                Text(if (register) "Mam konto — zaloguj" else "Nie mam konta — zarejestruj")
            }
        } else {
            Text("Konto: " + status.email, fontWeight = FontWeight.SemiBold)
            Text("Twój identyfikator (UID):", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(status.userId, Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { copy(status.userId) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopiuj UID")
                }
            }
            HorizontalDivider()
            if (status.connected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (status.fromCache) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(when {
                        status.loading -> "Wczytywanie danych ekipy..."
                        status.fromCache -> "Tryb offline / dane z pamięci"
                        else -> "Połączono — dane wspólnej ekipy"
                    }, fontWeight = FontWeight.SemiBold)
                }
                Text("Identyfikator ekipy (udostępnij współpracownikowi):")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(status.workspaceId, Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { copy(status.workspaceId) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopiuj identyfikator ekipy")
                    }
                }
                Text("Roboty: " + remote.jobs.size + " · Klienci: " + remote.clients.size +
                    " · Wpłaty: " + remote.payments.size)
                if (status.owner) {
                    HorizontalDivider()
                    Text("Dodaj współpracownika", fontWeight = FontWeight.Bold)
                    Text("Na drugim telefonie współpracownik zakłada konto i wysyła Ci swój UID. " +
                        "Dodaj ten UID, a następnie wyślij mu identyfikator ekipy.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = colleagueUid,
                        onValueChange = { colleagueUid = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("UID współpracownika") })
                    Button(enabled = !busy && colleagueUid.isNotBlank(), onClick = {
                        perform { cloud.addMember(colleagueUid)
                            colleagueUid = ""
                            Toast.makeText(context, "Dodano współpracownika", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Group, null); Spacer(Modifier.width(6.dp))
                        Text("Nadaj dostęp")
                    }
                    Text("Osoby w ekipie", fontWeight = FontWeight.Bold)
                    status.memberUids.filter { it != status.userId }.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(member.take(16) + "…", Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { revokeUid = member }) { Text("Odbierz dostęp") }
                        }
                    }
                    HorizontalDivider()
                    Text("Przenieś dotychczasowe roboty z telefonu do ekipy",
                        fontWeight = FontWeight.Bold)
                    Text("Wyłącznie dla nowej, pustej ekipy. " +
                        "Roboty z trybu lokalnego pozostaną na telefonie.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(enabled = !busy && remote.loaded &&
                        remote.jobs.isEmpty() && remote.clients.isEmpty() && remote.payments.isEmpty(),
                        onClick = { confirmation = true }) {
                        Text("Wyślij lokalne dane do ekipy")
                    }
                }
                OutlinedButton(onClick = { cloud.disconnect() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Wróć do mojego kalendarza lokalnego")
                }
            } else {
                Text("Utwórz nową ekipę", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = teamName, onValueChange = { teamName = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Nazwa ekipy") })
                Button(enabled = !busy && teamName.isNotBlank(),
                    onClick = { perform { cloud.create(teamName) } }) {
                    Text("Utwórz wspólną ekipę")
                }
                HorizontalDivider()
                Text("Dołącz do ekipy", fontWeight = FontWeight.Bold)
                Text("Właściciel musi najpierw dodać Twoje UID (widoczne wyżej).",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = teamCode, onValueChange = { teamCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Identyfikator ekipy od właściciela") })
                Button(enabled = !busy && teamCode.isNotBlank(),
                    onClick = { perform { cloud.join(teamCode) } }) { Text("Dołącz") }
            }
            TextButton(onClick = { cloud.logout() }) { Text("Wyloguj konto") }
        }
        if (busy || status.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank() || status.error.isNotBlank()) {
            Text(error.ifBlank { status.error }, color = MaterialTheme.colorScheme.error)
        }
        Text("Zmiany podczas braku internetu są buforowane przez Firestore. " +
            "Przy jednoczesnej edycji tej samej roboty ostatnia zapisana wersja wygrywa.",
            style = MaterialTheme.typography.bodySmall)
    }
    if (revokeUid != null) {
        AlertDialog(onDismissRequest = { revokeUid = null },
            title = { Text("Odebrać dostęp?") },
            text = { Text("Współpracownik straci dostęp do wspólnego kalendarza. " +
                "Jego lokalny cache może jednak pozostać na urządzeniu.") },
            confirmButton = { Button(onClick = {
                val who = revokeUid ?: return@Button
                revokeUid = null
                perform { cloud.removeMember(who) }
            }) { Text("Odbierz dostęp") } },
            dismissButton = { TextButton(onClick = { revokeUid = null }) { Text("Anuluj") } })
    }
    if (confirmation) {
        AlertDialog(onDismissRequest = { confirmation = false },
            title = { Text("Skopiować lokalne roboty do ekipy?") },
            text = { Text("Operacja jest dozwolona tylko, gdy zespół nie ma jeszcze żadnych danych. " +
                "Nie usuwa danych lokalnych. Kopiowane są roboty, klienci i wpłaty.") },
            confirmButton = {
                Button(onClick = {
                    confirmation = false
                    perform {
                        cloud.uploadLocal(store.localCopy())
                        Toast.makeText(context, "Przesłano dane do ekipy", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Przenieś dane") }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = false }) { Text("Anuluj") }
            })
    }
}
