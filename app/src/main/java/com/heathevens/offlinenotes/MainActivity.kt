package com.heathevens.offlinenotes

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * MainActivity hosts the entire UI. This app has no other activities or
 * fragments: everything is one screen that switches between a "list of
 * notes" view and a "note detail" view using simple in-memory state
 * (selectedNote). That's enough for an app this small - no navigation
 * library needed.
 */
class MainActivity : ComponentActivity() {

    private lateinit var repository: NotesRepository
    private lateinit var transcriber: SpeechTranscriber

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingStartAfterPermission = granted
    }

    // Simple flag read by the Compose UI right after a permission prompt.
    private var pendingStartAfterPermission by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = NotesRepository(applicationContext)
        transcriber = SpeechTranscriber(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OfflineNotesApp(
                        repository = repository,
                        transcriber = transcriber,
                        hasMicPermission = { hasMicPermission() },
                        requestMicPermission = { requestMicPermission.launch(Manifest.permission.RECORD_AUDIO) },
                        onShare = { file -> shareNote(file) }
                    )
                }
            }
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun shareNote(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share note"))
    }

    override fun onDestroy() {
        super.onDestroy()
        transcriber.stop()
    }
}

@Composable
fun OfflineNotesApp(
    repository: NotesRepository,
    transcriber: SpeechTranscriber,
    hasMicPermission: () -> Boolean,
    requestMicPermission: () -> Unit,
    onShare: (File) -> Unit
) {
    var notes by remember { mutableStateOf(repository.listNotes()) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        notes = repository.listNotes()
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!hasMicPermission()) {
            requestMicPermission()
            return
        }
        statusMessage = "Listening..."
        transcriber.start(
            onResult = onResult,
            onError = { message -> statusMessage = message },
            onListeningChange = { listening -> isListening = listening }
        )
    }

    val current = selectedNote
    if (current != null) {
        NoteDetailScreen(
            note = current,
            isListening = isListening,
            onBack = { selectedNote = null },
            onDelete = {
                repository.deleteNote(current.file)
                selectedNote = null
                refresh()
            },
            onShare = { onShare(current.file) },
            onAddMoreTapped = {
                startListening { text ->
                    val updated = repository.appendToNote(current.file, text)
                    selectedNote = updated
                    refresh()
                    statusMessage = "Saved."
                }
            },
            onStopTapped = { transcriber.finishListening() },
            onSaveEdit = { newContent ->
                val updated = repository.updateNote(current.file, newContent)
                selectedNote = updated
                refresh()
            }
        )
    } else {
        NoteListScreen(
            notes = notes,
            isListening = isListening,
            statusMessage = statusMessage,
            onRecordTapped = {
                startListening { text ->
                    val file = repository.saveNote(text)
                    refresh()
                    statusMessage = "Saved."
                    selectedNote = notes.find { it.file == file }
                }
            },
            onStopTapped = { transcriber.finishListening() },
            onNoteTapped = { note -> selectedNote = note }
        )
    }
}

@Composable
fun NoteListScreen(
    notes: List<Note>,
    isListening: Boolean,
    statusMessage: String?,
    onRecordTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onNoteTapped: (Note) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Vault Voice", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Everything stays on this phone. No internet permission, no accounts.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = if (isListening) onStopTapped else onRecordTapped,
            modifier = Modifier.fillMaxWidth(),
            colors = if (isListening) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
        ) {
            Text(if (isListening) "Stop" else "Record a note")
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Saved notes", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text("No notes yet. Tap the button above and start talking.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onNoteTapped(note) }
                    ) {
                        Text(
                            text = note.title,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteDetailScreen(
    note: Note,
    isListening: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onAddMoreTapped: () -> Unit,
    onStopTapped: () -> Unit,
    onSaveEdit: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isEditing by remember(note.file) { mutableStateOf(false) }
    var editedText by remember(note.file) { mutableStateOf(note.content) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(note.title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        } else {
            Text(
                note.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (isEditing) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        editedText = note.content
                        isEditing = false
                    }
                ) { Text("Cancel") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSaveEdit(editedText)
                        isEditing = false
                    }
                ) { Text("Save") }
            }
        } else {
            Button(
                onClick = if (isListening) onStopTapped else onAddMoreTapped,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isListening) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
            ) {
                Text(if (isListening) "Stop" else "Add more")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { copyToClipboard(context, note.content) }
                ) { Text("Copy") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                ) { Text("Share") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { isEditing = true }
                ) { Text("Edit") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("OfflineNotes", text))
}
