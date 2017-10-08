package com.xsonus.ffttest

import android.app.Activity
import android.media.AudioFormat
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.experimental.and


class MainActivity : Activity() {

    private val RECORDER_SAMPLERATE = 8000
    private val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
    private val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

    //private var bufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
    //private var bytesPerElement = 2 // 2 bytes in 16bit format
    private val bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)

    private val recorder: AudioRecord
    //private var recordingThread: Thread? = null
    private var isRecording = false

    lateinit var tv : TextView

    init {
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv = tv_test.apply {
            textSize = 20f;
            text = "Test TV"
        }
    }

    private fun startRecording() {
        if (recorder.state == AudioRecord.STATE_INITIALIZED) {
            tv.text = "Recording has started"
            recorder.startRecording()
            isRecording = true
            // TODO make use of coroutines
            launch(UI) { process() }
            //recordingThread = Thread(Runnable { writeAudioDataToFile() }, "AudioRecorder Thread")
            //recordingThread.start()
        } else
            tv.text = "The recorder isn't initialized."
    }

    override fun onStart() {
        super.onStart()
        startRecording()
    }

    private fun stopRecording() {
        // stops the recording activity
        if (isRecording) {
            isRecording = false
            recorder.stop()
            recorder.release()
            //recorder = null
            //recordingThread = null
        }
    }

    /**
     * The main DSP function
     */
    private suspend fun process() {
        val fScanner = FrequencyScanner()
        val sData = ShortArray(bufferSize)
        while (isRecording) {
            // process the data from buffer
            launch(CommonPool) {
                recorder.read(sData, 0, bufferSize)
            }.join()
            tv.text = "${async(CommonPool) {
                fScanner.extractFrequency(sData, RECORDER_SAMPLERATE).toInt().toString()
            }.await()} Hz"
        }
    }

    /*private fun writeAudioDataToFile() {
        // Write the output audio in byte
        val filePath = "/sdcard/8k16bitMono.pcm"

        val sData = ShortArray(BufferElements2Rec)

        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(filePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec)
            println("Short wirting to file" + sData.toString())
            try {
                // writes the data to file from buffer stores the voice buffer
                val bData = short2byte(sData)

                os!!.write(bData, 0, BufferElements2Rec * BytesPerElement)

            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        try {
            os!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }*/

    //Conversion of short to byte
    /*private fun short2byte(sData: ShortArray): ByteArray {
        val shortArrsize = sData.size
        val bytes = ByteArray(shortArrsize * 2)

        for (i in 0..shortArrsize - 1) {
            bytes[i * 2] = (sData[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
            sData[i] = 0
        }
        return bytes
    }*/

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

}
