/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.thdev.mediacodecexample.decoder;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

/**
 * 
 * @author taehwan
 *
 */
public class AudioDecoderThread {
	private static final int TIMEOUT_US = 1000;
	private MediaExtractor mExtractor;
	private MediaCodec mDecoder;
	
	private boolean eosReceived;
	private int mSampleRate = 0;
	
	/**
	 * 
	 * @param filePath
	 */
	public void startPlay(String path) {
		eosReceived = false;
		mExtractor = new MediaExtractor();
		try {
			mExtractor.setDataSource(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int channel = 0;
		for (int i = 0; i < mExtractor.getTrackCount(); i++) {
			MediaFormat format = mExtractor.getTrackFormat(i);
			String mime = format.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith("audio/")) {
				mExtractor.selectTrack(i);
				Log.d("TAG", "format : " + format);
				ByteBuffer csd = format.getByteBuffer("csd-0");
				
				for (int k = 0; k < csd.capacity(); ++k) {
					Log.e("TAG", "csd : " + csd.array()[k]);
				}
				mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				break;
			}
		}
		MediaFormat format = makeAACCodecSpecificData(MediaCodecInfo.CodecProfileLevel.AACObjectLC, mSampleRate, channel);
		if (format == null)
			return;
		
		mDecoder = MediaCodec.createDecoderByType("audio/mp4a-latm");
		mDecoder.configure(format, null, null, 0);

		if (mDecoder == null) {
			Log.e("DecodeActivity", "Can't find video info!");
			return;
		}

		mDecoder.start();
	
		new Thread(AACDecoderAndPlayRunnable).start();
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
	private MediaFormat makeAACCodecSpecificData(int audioProfile, int sampleRate, int channelConfig) {
		MediaFormat format = new MediaFormat();
		format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
		format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig);
		
	    int samplingFreq[] = {
	        96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
	        16000, 12000, 11025, 8000
	    };
	    
	    // Search the Sampling Frequencies
	    int sampleIndex = -1;
	    for (int i = 0; i < samplingFreq.length; ++i) {
	    	if (samplingFreq[i] == sampleRate) {
	    		Log.d("TAG", "kSamplingFreq " + samplingFreq[i] + " i : " + i);
	    		sampleIndex = i;
	    	}
	    }
	    
	    if (sampleIndex == -1) {
	    	return null;
	    }
	    
		ByteBuffer csd = ByteBuffer.allocate(2);
		csd.put((byte) ((audioProfile << 3) | (sampleIndex >> 1)));
		
		csd.position(1);
		csd.put((byte) ((byte) ((sampleIndex << 7) & 0x80) | (channelConfig << 3)));
		csd.flip();
		format.setByteBuffer("csd-0", csd); // add csd-0
		
		for (int k = 0; k < csd.capacity(); ++k) {
			Log.e("TAG", "csd : " + csd.array()[k]);
		}
		
		return format;
	}
	
	Runnable AACDecoderAndPlayRunnable = new Runnable() {
		
		@Override
		public void run() {
			AACDecoderAndPlay();
		}
	};

	/**
	 * After decoding AAC, Play using Audio Track.
	 */
	public void AACDecoderAndPlay() {
		ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
		ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
		
		BufferInfo info = new BufferInfo();
		
		int buffsize = AudioTrack.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        // create an audiotrack object
		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffsize,
                AudioTrack.MODE_STREAM);
		audioTrack.play();
		
		while (!eosReceived) {
			int inIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
			if (inIndex >= 0) {
				ByteBuffer buffer = inputBuffers[inIndex];
				int sampleSize = mExtractor.readSampleData(buffer, 0);
				if (sampleSize < 0) {
					// We shouldn't stop the playback at this point, just pass the EOS
					// flag to mDecoder, we will get it again from the
					// dequeueOutputBuffer
					Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
					mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
					
				} else {
					mDecoder.queueInputBuffer(inIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
					mExtractor.advance();
				}
				
				int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
				switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffers = mDecoder.getOutputBuffers();
					break;
					
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					MediaFormat format = mDecoder.getOutputFormat();
					Log.d("DecodeActivity", "New format " + format);
					audioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
					
					break;
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
					break;
					
				default:
					ByteBuffer outBuffer = outputBuffers[outIndex];
					Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + outBuffer);
					
					final byte[] chunk = new byte[info.size];
					outBuffer.get(chunk); // Read the buffer all at once
					outBuffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN
					
					audioTrack.write(chunk, info.offset, info.offset + info.size); // AudioTrack write data
					mDecoder.releaseOutputBuffer(outIndex, false);
					break;
				}
				
				// All decoded frames have been rendered, we can stop playing now
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
					break;
				}
			}
		}
		
		mDecoder.stop();
		mDecoder.release();
		mDecoder = null;
		
		mExtractor.release();
		mExtractor = null;
		
		audioTrack.stop();
		audioTrack.release();
		audioTrack = null;
	}
	
	public void stop() {
		eosReceived = true;
	}

}