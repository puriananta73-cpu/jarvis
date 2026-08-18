package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.JarvisApplication
import com.example.data.model.LogType
import com.example.speech.JarvisSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "JarvisCallReceiver"

        // State tracking across broadcast events
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var savedNumber: String? = null
        private var wasAnswered = false
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = JarvisApplication.instance.preferences
        if (!prefs.isServiceEnabledSync()) {
            return
        }

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            if (!number.isNullOrBlank()) {
                savedNumber = number
            }

            var state = TelephonyManager.CALL_STATE_IDLE
            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> state = TelephonyManager.CALL_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> state = TelephonyManager.CALL_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_IDLE -> state = TelephonyManager.CALL_STATE_IDLE
            }

            onCustomCallStateChanged(context, state, savedNumber)
        }
    }

    private fun onCustomCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) return

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                wasAnswered = false
                Log.d(TAG, "Incoming call ringing from: $number")
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (isIncoming) {
                    wasAnswered = true
                    Log.d(TAG, "Incoming call answered: $number")
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (isIncoming) {
                    if (!wasAnswered) {
                        Log.d(TAG, "MISSED CALL DETECTED from: $number")
                        handleMissedCall(context, number ?: "Unknown Caller")
                    }
                    isIncoming = false
                    wasAnswered = false
                }
            }
        }
        lastState = state
    }

    fun handleMissedCall(context: Context, callerNumber: String) {
        receiverScope.launch {
            val prefs = JarvisApplication.instance.preferences
            val repository = JarvisApplication.instance.repository
            val geminiRepository = JarvisApplication.instance.geminiRepository
            val speechManager = JarvisSpeechManager.getInstance(context)

            // Resolve contact name if permission granted
            val contactName = resolveContactName(context, callerNumber)
            val displayName = if (contactName != null) "$contactName ($callerNumber)" else callerNumber
            val ttsAnnounceName = contactName ?: callerNumber

            // 1. Text-to-Speech Announcement
            if (prefs.isMissedCallTtsEnabledSync()) {
                speechManager.playAlertTone()
                speechManager.speak("Missed call from $ttsAnnounceName")
            }

            // 2. Generate Context-Aware Excuse using Gemini API
            val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val defaultTemplate = prefs.getSmsTemplateSync()

            val aiGeneratedExcuse = geminiRepository.generateMissedCallExcuse(
                callerName = ttsAnnounceName,
                timeFormatted = timeString,
                defaultFallback = defaultTemplate
            )

            // 3. Automated SMS Response to Caller
            var smsSent = false
            var smsStatusMessage = "SMS Auto-reply disabled in settings"

            if (prefs.isMissedCallSmsEnabledSync() && callerNumber != "Unknown Caller" && callerNumber.isNotBlank()) {
                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission) {
                    try {
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }

                        val parts = smsManager.divideMessage(aiGeneratedExcuse)
                        if (parts.size > 1) {
                            smsManager.sendMultipartTextMessage(callerNumber, null, parts, null, null)
                        } else {
                            smsManager.sendTextMessage(callerNumber, null, aiGeneratedExcuse, null, null)
                        }
                        smsSent = true
                        smsStatusMessage = "AI Auto-SMS sent: \"$aiGeneratedExcuse\""
                        Log.d(TAG, "Automated AI SMS successfully sent to $callerNumber: \"$aiGeneratedExcuse\"")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send SMS to $callerNumber: ${e.message}")
                        smsStatusMessage = "SMS failed: ${e.message}"
                    }
                } else {
                    smsStatusMessage = "SEND_SMS permission not granted by user"
                    Log.w(TAG, "Cannot send SMS: SEND_SMS permission missing")
                }
            }

            // 4. Log to Jarvis Database
            repository.insertLog(
                type = LogType.CALL_MISSED,
                title = "Missed Call: $displayName",
                description = "TTS announced. $smsStatusMessage",
                sourcePackage = "com.android.server.telecom",
                extraData = callerNumber
            )

            if (smsSent) {
                repository.insertLog(
                    type = LogType.SMS_AUTO_SENT,
                    title = "AI Auto-SMS Dispatched",
                    description = "To: $displayName\nGenerated: \"$aiGeneratedExcuse\"",
                    extraData = callerNumber
                )
            }
        }
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) it.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving contact name: ${e.message}")
            null
        }
    }
}
