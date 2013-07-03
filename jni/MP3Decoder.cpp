#include <mad/mad.h>
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "MP3Decoder.h"

#define SHRT_MAX (32767)
#define INPUT_BUFFER_SIZE	(4*8192)

#define DEBUG_TAG "RemoteAudio/MP3Decoder.cpp"

/**
 * Struct holding the pointer to a wave file.
 */
struct MP3FileHandle {
	int size;
	int actualSize;
	FILE* file;
	mad_stream stream;
	mad_frame frame;
	mad_synth synth;
	mad_timer_t timer;
	int leftSamples;
	int handle;
	int offset;
	int sourceIndex;
	unsigned char* source;
	unsigned char* inputBuffer;
};

/** static WaveFileHandle array **/
static MP3FileHandle* handles[100];

/**
 * Seeks a free handle in the handles array and returns its index or -1 if no handle could be found
 */
static int findFreeHandle() {
	for (int i = 0; i < 100; i++) {
		if (handles[i] == NULL)
			return i;
	}

	return -1;
}

static inline void closeHandle(MP3FileHandle* handle) {
	if (handle->file != NULL) {
		fclose(handle->file);
	}mad_synth_finish(&handle->synth);
	mad_frame_finish(&handle->frame);
	mad_stream_finish(&handle->stream);
	delete handle ->inputBuffer;
	delete handle;
}

static inline signed short fixedToShort(mad_fixed_t Fixed) {
	if (Fixed >= MAD_F_ONE)
		return (SHRT_MAX);
	if (Fixed <= -MAD_F_ONE)
		return (-SHRT_MAX);

	Fixed = Fixed >> (MAD_F_FRACBITS - 15);
	return ((signed short) Fixed);
}


static inline char* logWithHandle(int handle, const char* logText) {
	static char text[256];
	sprintf(text, "[handle#%d] %s", handle, logText);
	return text;
}

static inline void logcat (int handle, const char* text){
	//__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, logWithHandle(handle, text));
}


static inline void logcat (int handle, const char* text, int i1, int i2, int i3, int i4, int i5){
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, logWithHandle(handle, text), i1, i2, i3, i4, i5);
}

JNIEXPORT jint JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_openDataNative(
		JNIEnv *env, jobject obj, jbyteArray jdata) {
	int index = findFreeHandle();

	if (index == -1)
		return -1;

	int length = env->GetArrayLength(jdata);
	jbyte* body = env->GetByteArrayElements(jdata, 0);
	MP3FileHandle* mp3Handle = new MP3FileHandle();
	mp3Handle->file = NULL;
	mp3Handle->size = length;
	mp3Handle->inputBuffer = new unsigned char[mp3Handle->size];
	mp3Handle->source = (unsigned char*) body;
	mp3Handle->sourceIndex = 0;
	mp3Handle->handle = index;
	env->ReleaseByteArrayElements(jdata, body, 0);

	mad_stream_init(&mp3Handle->stream);
	mad_frame_init(&mp3Handle->frame);
	mad_synth_init(&mp3Handle->synth);
	mad_timer_reset(&mp3Handle->timer);

	handles[index] = mp3Handle;
	return index;
}

JNIEXPORT jint JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_openFileNative(
		JNIEnv *env, jobject obj, jstring file) {
	int index = findFreeHandle();

	if (index == -1)
		return -1;

	const char* fileString = env->GetStringUTFChars(file, NULL);
	FILE* fileHandle = fopen(fileString, "rb");
	env->ReleaseStringUTFChars(file, fileString);
	if (fileHandle == 0)
		return -1;

	MP3FileHandle* mp3Handle = new MP3FileHandle();
	mp3Handle->file = fileHandle;
	fseek(fileHandle, 0, SEEK_END);
	mp3Handle->size = ftell(fileHandle);
	mp3Handle->inputBuffer = new unsigned char[mp3Handle->size];
	mp3Handle->handle = index;
	rewind(fileHandle);

	mad_stream_init(&mp3Handle->stream);
	mad_frame_init(&mp3Handle->frame);
	mad_synth_init(&mp3Handle->synth);
	mad_timer_reset(&mp3Handle->timer);

	handles[index] = mp3Handle;
	return index;
}

static inline int readFromSource(MP3FileHandle* mp3, int leftOver) {
	if (mp3->file != NULL) {
		return fread(mp3->inputBuffer + leftOver, 1, INPUT_BUFFER_SIZE
				- leftOver, mp3->file);
	} else if (mp3->source != NULL) {
		int toBeRead = INPUT_BUFFER_SIZE;
		if (mp3->actualSize - mp3->sourceIndex < toBeRead) {
			toBeRead = mp3->actualSize - mp3->sourceIndex;
		}
		logcat(mp3->handle,
				" %d bytes to be read from offset %d (input buffer size : %d, actual size : %d), copied from source [%d]",
				toBeRead, mp3->sourceIndex, mp3->size, mp3->actualSize,
				leftOver);

		memcpy(mp3->inputBuffer + leftOver, &mp3->source[mp3->sourceIndex],
				(toBeRead) * sizeof(unsigned char));
		mp3->sourceIndex += toBeRead;

		/*__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG,
		 " Ok.");*/
		return toBeRead;
	}
	return 0;
}

static inline bool readNextFrame(MP3FileHandle* mp3) {
	bool finished = false;
	bool synth = false;
	int result = 0;
	int retry = 0;
	while (!finished) {
		if (mp3->stream.buffer == NULL || mp3->stream.error == MAD_ERROR_BUFLEN) {
			//Le stream n'a pas encore été lu ou le stream est trop court pour la lecture
			//la source doit être lue.
			int inputBufferSize = 0;
			int leftOver = 0;
			if (mp3->stream.next_frame != 0) {
				//il reste des données dans la frame à décoder
				//elles sont placées en début de buffer
				leftOver = mp3->stream.bufend - mp3->stream.next_frame;
				if (leftOver < 0) {
					leftOver = 0;
				}
				for (int i = 0; i < leftOver; i++) {
					mp3->inputBuffer[i] = mp3->stream.next_frame[i];
				}
			}
			logcat (mp3->handle, "2°/ Reading from source...");
			int readBytes = readFromSource(mp3, leftOver);
			if (readBytes == 0) {
				//rien à lire : fini + rien à decode + rien à synth
				finished = true;
			}
			inputBufferSize = leftOver + readBytes;

			if (!finished) {
				//stream du buffer dans la frame
				mad_stream_buffer(&mp3->stream, mp3->inputBuffer,
						inputBufferSize);
				logcat (mp3->handle, "3°/ buffer streamed");
				mp3->stream.error = MAD_ERROR_NONE;
			}
		}

		if (!finished && mad_frame_decode(&mp3->frame, &mp3->stream) == 0) {
			//décodage de la frame courante réussi
			logcat (mp3->handle, "4°/ Ok. -> now synth");
			finished = true;
			result = true;
			synth = true;
		} else if (!finished) {
			//le décodage de la frame courante n'a pas fonctionné
			if (mp3->stream.error != MAD_ERROR_BUFLEN
					&&
					!MAD_RECOVERABLE(mp3->stream.error)) {
				//erreur anormale, pas de synth
				logcat (mp3->handle, "E°/ error while decoding");
				finished = true;
			} else {
				//sinon il y avait EOF ou erreur récupérable, l'erreur est donc normale
				//on essaie 5 fois et puis sinon on laisse tomber
				retry++;
				if (retry >= 5) {
					logcat (mp3->handle, "E°/ definitely in error");
					finished = true;
				}

			}
		}
	}

	mad_timer_add(&mp3->timer, mp3->frame.header.duration);
	mad_synth_frame(&mp3->synth, &mp3->frame);
	mp3->leftSamples = mp3->synth.pcm.length;
	mp3->offset = 0;

	logcat (mp3->handle, "5°/ synth done");
	return result;
}

static inline int readSamplesInternal(MP3FileHandle* mp3, short* target,
		int size) {
	int i = 0;
	if (mp3 != NULL) {
		mp3->actualSize = size;
	}
	while (i < size) {
		if (mp3->leftSamples > 0) {
			while (i < size && mp3->offset < mp3->synth.pcm.length) {
				int value =
						fixedToShort(mp3->synth.pcm.samples[0][mp3->offset]);

				if (MAD_NCHANNELS(&mp3->frame.header) == 2) {
					value += fixedToShort(
							mp3->synth.pcm.samples[1][mp3->offset]);
					value /= 2;
				}

				target[i] = value / (float) SHRT_MAX;
				i++;
				mp3->leftSamples--;
				mp3->offset++;
			}
		} else {
			int success = readNextFrame(mp3);
			if (success == 0) {
				return 0;
			}
		}

	}
	if (i > size) {
		return 0;
	}
	return size;
}

JNIEXPORT jshortArray JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_readSamplesNative__II(
		JNIEnv * env, jobject obj, jint handle, jint size) {
	MP3FileHandle* mp3 = handles[handle];
	short* sample = new short[size];
	jshortArray result = NULL;

	logcat (handle, "1°/ start of reading");

	readSamplesInternal(mp3, sample, size);

	result = env->NewShortArray(size);
	env->SetShortArrayRegion(result, 0, size, sample);
	delete sample;
	return result;
}

JNIEXPORT jint JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_readSamplesNative__ILjava_nio_ShortBuffer_2I(
		JNIEnv *env, jobject obj, jint handle, jobject buffer, jint size) {
	MP3FileHandle* mp3 = handles[handle];
	short* sample = (short*) env->GetDirectBufferAddress(buffer);

	logcat (handle, "1°/ start of reading");

	return readSamplesInternal(mp3, sample, size);
}

JNIEXPORT void JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_closeHandleNative(
		JNIEnv *env, jobject obj, jint handle) {
	logcat (handle, "6°/ closing handle");
	if (handles[handle] != 0) {
		closeHandle(handles[handle]);
		handles[handle] = 0;
	}
}
