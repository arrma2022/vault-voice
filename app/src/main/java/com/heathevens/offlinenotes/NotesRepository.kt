package com.heathevens.offlinenotes

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles saving, listing, and deleting transcript files.
 *
 * Everything lives in the app's private "files" directory
 * (context.filesDir), which on Android is internal storage that only
 * this app can read or write. Nothing here ever touches the network or
 * shared/external storage, so no extra permissions are needed to use it.
 */
data class Note(val file: File, val title: String, val content: String)

class NotesRepository(private val context: Context) {

    private val notesDir: File
        get() {
            val dir = File(context.filesDir, "notes")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val displayFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)

    fun saveNote(text: String): File {
        val fileName = "note_${fileNameFormat.format(Date())}.txt"
        val file = File(notesDir, fileName)
        file.writeText(text)
        return file
    }

    fun listNotes(): List<Note> {
        val files = notesDir.listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: emptyArray()
        return files.sortedByDescending { it.lastModified() }.map { f ->
            val content = f.readText()
            val title = titleFor(f, content)
            Note(f, title, content)
        }
    }

    fun deleteNote(file: File): Boolean = file.delete()

    fun appendToNote(file: File, text: String): Note {
        val existing = file.readText()
        val updated = if (existing.isBlank()) text else "$existing\n\n$text"
        file.writeText(updated)
        return Note(file, titleFor(file, updated), updated)
    }

    fun updateNote(file: File, newContent: String): Note {
        file.writeText(newContent)
        return Note(file, titleFor(file, newContent), newContent)
    }

    private fun titleFor(file: File, content: String): String {
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() }?.take(40)
        return if (!firstLine.isNullOrBlank()) firstLine else displayFormat.format(Date(file.lastModified()))
    }
}
