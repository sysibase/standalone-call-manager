package com.capacitor.callmanager

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

/**
 * =============================================================================
 * ContactsHelper.kt — Android Contacts Query Helper
 * =============================================================================
 * Android ke ContactsContract se phone contacts efficiently query karta hai.
 *
 * FEATURES:
 *  - Contact list retrieval with multiple numbers per contact
 *  - Search filter (name ya number)
 *  - Contact photo URI support
 * =============================================================================
 */
object ContactsHelper {

    private const val TAG = "ContactsHelper"

    data class ContactEntry(
        val id: String,
        val name: String,
        val numbers: List<String>,
        val photoUri: String?
    )

    fun getContacts(context: Context, search: String? = null, limit: Int = 500, offset: Int = 0): List<ContactEntry> {
        val contactsMap = mutableMapOf<String, ContactEntry>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (!search.isNullOrBlank()) {
            selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            selectionArgs = arrayOf("%$search%", "%$search%")
        }

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use { c ->
            val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            var currentContactId = ""
            var uniqueContactCount = 0

            while (c.moveToNext()) {
                val contactId = c.getString(idIdx)
                
                if (contactId != currentContactId) {
                    currentContactId = contactId
                    if (!contactsMap.containsKey(contactId)) {
                        uniqueContactCount++
                    }
                }

                if (uniqueContactCount <= offset) continue
                if (contactsMap.size >= limit && !contactsMap.containsKey(contactId)) continue

                val name = c.getString(nameIdx) ?: "Unknown"
                val number = c.getString(numberIdx) ?: ""
                val photoUri = c.getString(photoIdx)

                if (contactsMap.containsKey(contactId)) {
                    val existing = contactsMap[contactId]!!
                    if (!existing.numbers.contains(number)) {
                        contactsMap[contactId] = existing.copy(numbers = existing.numbers + number)
                    }
                } else {
                    contactsMap[contactId] = ContactEntry(
                        id = contactId,
                        name = name,
                        numbers = listOf(number),
                        photoUri = photoUri
                    )
                }
            }
        }

        Log.d(TAG, "getContacts returned ${contactsMap.size} unique contacts (search: $search)")
        return contactsMap.values.toList()
    }
}
