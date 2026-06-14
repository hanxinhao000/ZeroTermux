package com.termux.zerocore.workstation

import android.content.Context
import android.provider.Telephony
import android.telephony.SmsManager
import com.google.gson.Gson

object ZtWorkstationSmsHelper {

    private val gson = Gson()

    data class SmsThread(val address: String, val body: String, val date: Long, val type: Int)
    data class SmsMessage(val address: String, val body: String, val date: Long, val type: Int)

    fun listThreads(context: Context): String {
        val threads = LinkedHashMap<String, SmsThread>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )
        cursor?.use {
            val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            while (it.moveToNext()) {
                val address = if (addressIdx >= 0) it.getString(addressIdx) ?: "" else ""
                if (address.isBlank() || threads.containsKey(address)) continue
                val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                val date = if (dateIdx >= 0) it.getLong(dateIdx) else 0L
                val type = if (typeIdx >= 0) it.getInt(typeIdx) else 0
                threads[address] = SmsThread(address, body, date, type)
            }
        }
        return gson.toJson(mapOf("threads" to threads.values))
    }

    fun listMessages(context: Context, address: String): String {
        val messages = ArrayList<SmsMessage>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            Telephony.Sms.ADDRESS + " = ?",
            arrayOf(address),
            Telephony.Sms.DATE + " ASC"
        )
        cursor?.use {
            val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            while (it.moveToNext()) {
                val addr = if (addressIdx >= 0) it.getString(addressIdx) ?: address else address
                val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                val date = if (dateIdx >= 0) it.getLong(dateIdx) else 0L
                val type = if (typeIdx >= 0) it.getInt(typeIdx) else 0
                messages.add(SmsMessage(addr, body, date, type))
            }
        }
        return gson.toJson(mapOf("address" to address, "messages" to messages))
    }

    fun sendSms(address: String, body: String): String {
        return try {
            SmsManager.getDefault().sendTextMessage(address, null, body, null, null)
            gson.toJson(mapOf("ok" to true))
        } catch (e: Exception) {
            gson.toJson(mapOf("ok" to false, "error" to (e.message ?: "send failed")))
        }
    }
}
