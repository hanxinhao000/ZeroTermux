package com.termux.zerocore.workstation

import android.content.Context
import android.provider.ContactsContract
import com.google.gson.Gson

object ZtWorkstationContactsHelper {

    private val gson = Gson()

    data class ContactEntry(val id: String, val name: String, val phone: String)

    fun listContacts(context: Context): String {
        val result = ArrayList<ContactEntry>()
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val id = if (idIdx >= 0) it.getString(idIdx) else ""
                val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "" else ""
                val phone = if (phoneIdx >= 0) it.getString(phoneIdx) ?: "" else ""
                if (phone.isNotBlank()) {
                    result.add(ContactEntry(id, name, phone))
                }
            }
        }
        return gson.toJson(mapOf("contacts" to result))
    }
}
