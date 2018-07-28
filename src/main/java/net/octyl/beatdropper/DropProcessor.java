/*
 * This file is part of beat-dropper, licensed under the MIT License (MIT).
 *
 * Copyright (c) Kenzie Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.octyl.beatdropper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedSet;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.octyl.beatdropper.droppers.BeatDropper;

public class DropProcessor {

    private final ByteArrayDataOutput output = ByteStreams.newDataOutput();
    private final Path source;
    private final BeatDropper dropper;

    public DropProcessor(Path source, BeatDropper dropper) {
        this.source = checkNotNull(source, "source");
        this.dropper = checkNotNull(dropper, "dropper");
    }

    public void process() throws IOException, UnsupportedAudioFileException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(source.toFile());
        AudioFormat format = stream.getFormat();
        AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(),
                16,
                format.getChannels(),
                format.getChannels() * 2,
                format.getSampleRate(),
                true);
        AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, stream);

        int channels = decodedFormat.getChannels();
        int frameSize = decodedFormat.getFrameSize();
        System.err.println(decodedFormat);

        if (frameSize == -1) {
            frameSize = 2;
        }

        processAudioStream(din, channels, frameSize);
        writeToFile(format.getSampleRate(), renameFile(source));
    }

    private Path renameFile(Path file) {
        String newFileName = renameFile(file.getFileName().toString());
        return file.resolveSibling(newFileName);
    }

    private String renameFile(String fileName) {
        String modStr = " [" + dropper.describeModification() + "].wav";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return fileName + modStr;
        }
        return fileName.substring(0, lastDot) + modStr;
    }

    private void writeToFile(float sampleRate, Path target) throws IOException {
        AudioFormat fmt = new AudioFormat(sampleRate, 16, 2, true, true);
        AudioInputStream audio = new AudioInputStream(
                new ByteArrayInputStream(output.toByteArray()),
                fmt,
                AudioSystem.NOT_SPECIFIED);
        AudioSystem.write(audio, Type.WAVE, target.toFile());
    }

    private void processAudioStream(AudioInputStream stream, int channels, int frameSize) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(stream));
        int sampleAmount = (int) ((dropper.requestedTimeLength() * stream.getFormat().getFrameRate()) / 1000);
        short[] left = new short[sampleAmount];
        short[] right = new short[sampleAmount];
        boolean reading = true;
        while (reading) {
            int read = 0;
            if (channels == 1) {
                while (read < left.length) {
                    try {
                        short nextShort = dis.readShort();
                        left[read] = nextShort;
                        right[read] = nextShort;
                    } catch (EOFException e) {
                        reading = false;
                        break;
                    }
                    read++;
                }
            } else {
                while (read < left.length * 2) {
                    short[] buf = read % 2 == 0 ? left : right;
                    try {
                        buf[read / 2] = dis.readShort();
                    } catch (EOFException e) {
                        reading = false;
                        break;
                    }
                    read++;
                }
            }

            SortedSet<SampleSelection> ranges = dropper.selectSamples(left.length);
            short[] cutLeft = extractSelection(left, ranges);
            short[] cutRight = extractSelection(right, ranges);
            for (int i = 0; i < cutLeft.length; i++) {
                output.writeShort(cutLeft[i]);
                output.writeShort(cutRight[i]);
            }
        }
    }

    private short[] extractSelection(short[] buffer, SortedSet<SampleSelection> ranges) {
        int sizeOfAllSelections = ranges.stream().mapToInt(sel -> sel.length()).sum();
        if (sizeOfAllSelections == buffer.length) {
            return buffer;
        }
        short[] sel = new short[sizeOfAllSelections];
        int index = 0;
        for (SampleSelection range : ranges) {
            System.arraycopy(buffer, range.lowBound(), sel, index, range.length());
            index += range.length();
        }
        return sel;
    }
}