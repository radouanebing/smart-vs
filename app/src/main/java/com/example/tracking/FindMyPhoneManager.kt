package com.example.tracking

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log

class FindMyPhoneManager(private val context: Context) {
    private var ringtone: Ringtone? = null

    fun triggerMaxVolumeSirenBuzzer() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("FindMyPhoneManager", "Alarm buzzer play failed", e)
        }
    }

    fun stopAlarmBuzzer() {
        ringtone?.stop()
    }

    fun flashCameraTorchLed(seconds: Int, onComplete: () -> Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                // Background pulsed flashes
                Thread {
                    var flashState = true
                    val loops = seconds * 2
                    for (i in 0 until loops) {
                        try {
                            cameraManager.setTorchMode(cameraId, flashState)
                            flashState = !flashState
                            Thread.sleep(500)
                        } catch (e: Exception) {
                            break
                        }
                    }
                    cameraManager.setTorchMode(cameraId, false)
                    onComplete()
                }.start()
            }
        } catch (e: Exception) {
            Log.e("FindMyPhoneManager", "Flashlight hardware control failed/missing", e)
            onComplete()
        }
    }
}
