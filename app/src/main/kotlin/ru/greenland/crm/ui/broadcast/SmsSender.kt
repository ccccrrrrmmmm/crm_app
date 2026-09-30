package ru.greenland.crm.ui.broadcast

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отправка SMS через системный [SmsManager] — без участия пользователя (в отличие от WhatsApp,
 * где отправку подтверждает человек). Длинные сообщения бьются на части автоматически.
 */
@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Возвращает true, если сообщение удалось передать в систему для отправки. */
    fun send(phone: String, text: String): Boolean = try {
        val manager = smsManager()
        val parts = manager.divideMessage(text)
        if (parts.size > 1) {
            manager.sendMultipartTextMessage(phone, null, parts, null, null)
        } else {
            manager.sendTextMessage(phone, null, text, null, null)
        }
        true
    } catch (_: Throwable) {
        false
    }

    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}
