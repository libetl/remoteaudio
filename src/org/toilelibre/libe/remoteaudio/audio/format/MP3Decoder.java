/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.toilelibre.libe.remoteaudio.audio.format;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class MP3Decoder implements Decoder {

    /*
     * this is used to load the 'MP3Decoder' library on application startup. The
     * library has already been unpacked into MP3Decoder.so at installation time
     * by the package manager.
     */
    static {
        System.loadLibrary ("MP3Decoder");
    }

    private native void closeHandleNative (int handle);

    public short [] decodeSamples (byte [] buffer, int size) {
        return this.decodeSamplesWithBuffer (buffer, size);
    }

    public short [] decodeSamplesWithBuffer (byte [] buffer, int size) {
        int handle = this.openDataNative (buffer);
        short [] samples = this.readSamplesNative (handle, size);
        this.closeHandleNative (handle);
        return samples;
    }

    public short [] decodeSamplesWithFilename (String filename, int size) {
        short [] samples = new short [size];
        int handle = this.openFileNative (filename);
        this.readSamplesWithShortBuffer (handle, samples);
        this.closeHandleNative (handle);
        return samples;
    }

    public short [] decodeSamplesWithShortBuffer (byte [] buffer, int size) {
        int handle = this.openDataNative (buffer);
        short [] samples = new short [size];
        this.readSamplesWithShortBuffer (handle, samples);
        this.closeHandleNative (handle);
        return samples;
    }

    public short [] decodeSamplesWithTmpFile (byte [] buffer, int size) {
        short [] samples = new short [size];
        try {
            String filename = "/tmp/remoteaudio";
            File f = new File (filename);
            f.createNewFile ();
            FileOutputStream fos = new FileOutputStream (f);
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();
            baos.write (buffer);
            baos.writeTo (fos);
            baos.close ();
            fos.close ();
            int handle = this.openFileNative (filename);
            this.readSamplesWithShortBuffer (handle, samples);
            this.closeHandleNative (handle);
        } catch (IOException e) {
            e.printStackTrace ();
        }
        return samples;
    }

    /*
     * A native method that is implemented by the 'MP3Decoder' native library,
     * which is packaged with this application.
     */
    private native int openDataNative (byte [] data);

    /*
     * A native method that is implemented by the 'MP3Decoder' native library,
     * which is packaged with this application.
     */
    private native int openFileNative (String file);

    private native short [] readSamplesNative (int handle, int size);

    /*
     * This is another native method declaration that is *not* implemented by
     * 'MP3Decoder'. This is simply to show that you can declare as many native
     * methods in your Java code as you want, their implementation is searched
     * in the currently loaded native libraries only the first time you call
     * them.
     * 
     * Trying to call this function will result in a
     * java.lang.UnsatisfiedLinkError exception !
     */
    private native int readSamplesNative (int handle, ShortBuffer buffer,
            int size);

    private int readSamplesWithShortBuffer (int handle, short [] samples) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect (samples.length
                * Short.SIZE / 8);
        byteBuffer.order (ByteOrder.nativeOrder ());

        ShortBuffer shortBuffer = byteBuffer.asShortBuffer ();
        int readSamples = this.readSamplesNative (handle, shortBuffer,
                samples.length);
        if (readSamples == 0) {
            return 0;
        }

        shortBuffer.position (0);
        shortBuffer.get (samples);

        return samples.length;
    }
}
