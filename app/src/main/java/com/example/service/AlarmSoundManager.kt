package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

object AlarmSoundManager {
    private var ringtone: Ringtone? = null

    fun play(context: Context, uri: Uri) {
        synchronized(this) {
            stop()
            try {
                ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        synchronized(this) {
            try {
                if (ringtone?.isPlaying == true) {
                    ringtone?.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ringtone = null
            }
        }
    }
}
