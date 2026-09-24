package com.apexforge.godlauncher.data

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One contact match for universal search. No contact data is persisted. */
data class ContactResult(
    val name: String,
    val phoneNumber: String?
)

/**
 * Contacts search for universal search. Caller must hold READ_CONTACTS;
 * this function does not request it — the permission is requested
 * in-context by the home screen, and search degrades to apps-only without it.
 */
object ContactSearch {

    private const val MAX_RESULTS = 6

    suspend fun search(context: Context, query: String): List<ContactResult> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty()) return@withContext emptyList()
            val results = LinkedHashMap<String, ContactResult>()
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$q%")
            val sort = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            try {
                context.contentResolver.query(
                    uri, projection, selection, selectionArgs, sort
                )?.use { cursor ->
                    val nameIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                        val name = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }
                            ?: continue
                        // One row per name: keep the first number we see.
                        results.putIfAbsent(
                            name,
                            ContactResult(name, cursor.getString(numIdx))
                        )
                    }
                }
            } catch (_: SecurityException) {
                // Permission revoked mid-flight: behave as if no permission.
                return@withContext emptyList()
            } catch (_: Exception) {
                return@withContext emptyList()
            }
            results.values.toList()
        }
}
