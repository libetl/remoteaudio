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

import org.toilelibre.libe.remoteaudio.arrays.TransformCharShort;

public class PCMDecoder implements Decoder {

    public short [] decodeSamples (byte [] buffer, int size) {

        short [] dataS = new short [(int) Math.ceil (size / 2.0)];
        TransformCharShort.byteArrayToShortArray (buffer, dataS, size);
        return dataS;
    }

}
