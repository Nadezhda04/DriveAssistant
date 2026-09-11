package com.example.driveassistant.car

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.car.app.CarContext
import androidx.car.app.media.CarAudioRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class CarVoiceInputManager(
    private val carContext: CarContext
) {

    private var carAudioRecord: CarAudioRecord? = null
    private var recordingJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    fun startRecording(
        onStarted: () -> Unit,
        onAudioData: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                carContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            onError("Няма разрешение за микрофона")
            return
        }

        if (recordingJob != null) {
            return
        }

        try {
            val record = CarAudioRecord.create(carContext)
            carAudioRecord = record

            val audioManager =
                carContext.getSystemService(AudioManager::class.java)

            if (audioManager == null) {
                onError("AudioManager не е наличен")
                return
            }

            val audioAttributes =
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(
                        AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                    )
                    .build()

            val focusRequest =
                AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener { state ->
                        if (state == AudioManager.AUDIOFOCUS_LOSS) {
                            stopRecording()
                        }
                    }
                    .build()

            audioFocusRequest = focusRequest

            val focusResult =
                audioManager.requestAudioFocus(focusRequest)

            if (
                focusResult !=
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            ) {
                onError("Не беше получен audio focus")
                return
            }

            record.startRecording()
            onStarted()

            recordingJob =
                CoroutineScope(Dispatchers.IO).launch {

                    val buffer =
                        ByteArray(
                            CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE
                        )

                    while (true) {

                        val bytesRead =
                            record.read(
                                buffer,
                                0,
                                buffer.size
                            )

                        if (bytesRead < 0) {
                            break
                        }

                        if (bytesRead > 0) {
                            onAudioData(
                                buffer.copyOf(bytesRead)
                            )
                        }
                    }
                }

        } catch (e: Exception) {
            onError(
                e.message ?: "Грешка при запис от микрофона"
            )
        }
    }

    fun stopRecording() {

        recordingJob?.cancel()
        recordingJob = null

        try {
            carAudioRecord?.stopRecording()
        } catch (_: Exception) {
        }

        carAudioRecord = null

        val audioManager =
            carContext.getSystemService(AudioManager::class.java)

        audioFocusRequest?.let { request ->
            audioManager?.abandonAudioFocusRequest(request)
        }

        audioFocusRequest = null
    }
}