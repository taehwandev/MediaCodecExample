package tech.thdev.mediacodecexample.audio

import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer


class AACAudioDecoderThread {

    companion object {
        private const val TIMEOUT_US = 1_000L
    }

    private lateinit var extractor: MediaExtractor
    private lateinit var decoder: MediaCodec

    private var endOfReceived = false
    private var sampleRate = 0

    fun startPlay(file: AssetFileDescriptor) {
        endOfReceived = false
        extractor = MediaExtractor()
        try {
            extractor.setDataSource(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var channel = 0
        (0 until extractor.trackCount).forEach { trackNumber ->
            val format = extractor.getTrackFormat(trackNumber)
            format.getString(MediaFormat.KEY_MIME).takeIf { it?.startsWith("audio/") == true }?.let {
                extractor.selectTrack(trackNumber)
                Log.d("TEMP", "format : $format")
                format.getByteBuffer("csd-0")?.let { csd ->
                    (0 until csd.capacity()).forEach {
                        Log.e("TEMP", "csd : ${csd.array()[it]}")
                    }
                }

                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                return@forEach
            }
        }

        val format = makeAACCodecSpecificData(
            MediaCodecInfo.CodecProfileLevel.AACObjectLC,
            sampleRate,
            channel
        ) ?: return

        if (format != null) {
            decoder = MediaCodec.createDecoderByType("audio/mp4a-latm")
            decoder.configure(format, null, null, 0)

            decoder.start()

            Thread(aacDecoderAndPlayRunnable).start()
        }
    }

    /**
     * The code profile, Sample rate, channel Count is used to
     * produce the AAC Codec SpecificData.
     * Android 4.4.2/frameworks/av/media/libstagefright/avc_utils.cpp refer
     * to the portion of the code written.
     *
     * MPEG-4 Audio refer : http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio#Audio_Specific_Config
     *
     * @param audioProfile is MPEG-4 Audio Object Types
     * @param sampleRate
     * @param channelConfig
     * @return MediaFormat
     */
    private fun makeAACCodecSpecificData(
        audioProfile: Int,
        sampleRate: Int,
        channelConfig: Int
    ): MediaFormat? {
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm")
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig)
        val samplingFreq = intArrayOf(
            96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
            16000, 12000, 11025, 8000
        )

        // Search the Sampling Frequencies
        var sampleIndex = -1
        for (i in samplingFreq.indices) {
            if (samplingFreq[i] == sampleRate) {
                Log.d("TEMP", "kSamplingFreq " + samplingFreq[i] + " i : " + i)
                sampleIndex = i
            }
        }
        if (sampleIndex == -1) {
            return null
        }
        val csd: ByteBuffer = ByteBuffer.allocate(2)
        csd.put((audioProfile shl 3 or (sampleIndex shr 1)).toByte())
        csd.position(1)
        csd.put(((sampleIndex shl 7 and 0x80) or ((channelConfig shl 3))).toByte())
        csd.flip()
        format.setByteBuffer("csd-0", csd) // add csd-0
        for (k in 0 until csd.capacity()) {
            Log.e("TEMP", "csd : " + csd.array().get(k))
        }
        return format
    }

    private var aacDecoderAndPlayRunnable = Runnable { AACDecoderAndPlay() }

    /**
     * After decoding AAC, Play using Audio Track.
     */
    fun AACDecoderAndPlay() {
        val inputBuffers: Array<ByteBuffer> = decoder.inputBuffers
        var outputBuffers: Array<ByteBuffer> = decoder.outputBuffers
        val info = BufferInfo()
        val buffsize: Int = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // create an audiotrack object
        var audioTrack: AudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffsize,
            AudioTrack.MODE_STREAM
        )
        audioTrack!!.play()
        while (!endOfReceived) {
            val inIndex: Int = decoder.dequeueInputBuffer(TIMEOUT_US)
            if (inIndex >= 0) {
                val buffer = inputBuffers[inIndex]
                val sampleSize: Int = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    // We shouldn't stop the playback at this point, just pass the EOS
                    // flag to decoder, we will get it again from the
                    // dequeueOutputBuffer
                    Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                    decoder.queueInputBuffer(
                        inIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                } else {
                    decoder.queueInputBuffer(
                        inIndex,
                        0,
                        sampleSize,
                        extractor.sampleTime ?: 0L,
                        0
                    )
                    extractor.advance()
                }
                val outIndex: Int = decoder.dequeueOutputBuffer(info, TIMEOUT_US) ?: -1
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED")
                        outputBuffers = decoder.outputBuffers
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format: MediaFormat = decoder.outputFormat
                        Log.d("DecodeActivity", "New format $format")
                        audioTrack.playbackRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d(
                        "DecodeActivity",
                        "dequeueOutputBuffer timed out!"
                    )
                    else -> {
                        val outBuffer = outputBuffers[outIndex]
                        Log.v(
                            "DecodeActivity",
                            "We can't use this buffer but render it due to the API limit, $outBuffer"
                        )
                        val chunk = ByteArray(info.size)
                        outBuffer[chunk] // Read the buffer all at once
                        outBuffer.clear() // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN
                        audioTrack.write(
                            chunk,
                            info.offset,
                            info.offset + info.size
                        ) // AudioTrack write data
                        decoder.releaseOutputBuffer(outIndex, false)
                    }
                }

                // All decoded frames have been rendered, we can stop playing now
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    break
                }
            }
        }
        decoder.stop()
        decoder.release()
        extractor.release()
        audioTrack.stop()
        audioTrack.release()
    }

    fun stop() {
        endOfReceived = true
    }
}