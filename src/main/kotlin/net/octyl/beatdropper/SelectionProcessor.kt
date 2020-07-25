/*
 * This file is part of beat-dropper, licensed under the MIT License (MIT).
 *
 * Copyright (c) Octavia Togami <https://octyl.net>
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

package net.octyl.beatdropper

import com.google.common.base.Preconditions
import com.google.common.io.ByteStreams
import com.google.common.io.LittleEndianDataInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.octyl.beatdropper.droppers.SampleModifier
import net.octyl.beatdropper.util.AvioCallbacks
import net.octyl.beatdropper.util.ChannelProvider
import net.octyl.beatdropper.util.FFmpegInputStream
import net.octyl.beatdropper.util.FFmpegOutputStream
import net.octyl.beatdropper.util.FlowInputStream
import org.bytedeco.ffmpeg.global.avcodec
import java.io.BufferedInputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.coroutines.ContinuationInterceptor

class SelectionProcessor(
    private val source: ChannelProvider<out ReadableByteChannel>,
    private val sink: ChannelProvider<out WritableByteChannel>,
    private val modifier: SampleModifier,
    private val raw: Boolean
) {
    @Throws(IOException::class, UnsupportedAudioFileException::class)
    fun process() {
        openAudioInput().use { stream ->
            val format = stream.format
            Preconditions.checkState(format.channels == 2, "Must be 2 channel format")
            System.err.println("Loaded audio as $format (possibly via FFmpeg resampling)")
            runBlocking(Dispatchers.Default) {
                val flow = processAudioStream(stream)
                val byteFlow = gather(stream.format.isBigEndian, flow)
                withContext(Dispatchers.IO) {
                    FlowInputStream(byteFlow).buffered().use {
                        writeToSink(it, stream.format.sampleRate)
                    }
                }
            }
        }
    }

    @Throws(IOException::class, UnsupportedAudioFileException::class)
    private fun openAudioInput(): AudioInputStream {
        // unwrap via FFmpeg
        val stream = FFmpegInputStream(
            source.identifier,
            AvioCallbacks.forChannel(source.openChannel())
        )
        return AudioInputStream(
            stream,
            stream.audioFormat,
            AudioSystem.NOT_SPECIFIED.toLong()
        )
    }

    @Throws(IOException::class)
    private fun writeToSink(inputStream: InputStream, sampleRate: Float) {
        if (raw) {
            FFmpegOutputStream(
                avcodec.AV_CODEC_ID_FLAC,
                "flac",
                sampleRate.toInt(),
                AvioCallbacks.forChannel(sink.openChannel())
            ).use { ffmpeg ->
                ByteStreams.copy(inputStream, ffmpeg)
            }
        } else {
            FFmpegOutputStream(
                avcodec.AV_CODEC_ID_MP3,
                "mp3",
                sampleRate.toInt(),
                AvioCallbacks.forChannel(sink.openChannel())
            ).use { ffmpeg ->
                ByteStreams.copy(inputStream, ffmpeg)
            }
        }
    }

    private data class ChannelContent<C>(
        val left: C,
        val right: C
    ) {
        inline fun <NC> map(mapping: (from: C) -> NC): ChannelContent<NC> {
            return ChannelContent(mapping(left), mapping(right))
        }
    }

    @Throws(IOException::class)
    private fun processAudioStream(stream: AudioInputStream) = flow {
        coroutineScope {
            assert(coroutineContext[ContinuationInterceptor] == Dispatchers.IO) {
                "Not on IO threads!"
            }
            val bufferedStream = BufferedInputStream(stream)
            val dis: DataInput = when {
                stream.format.isBigEndian -> DataInputStream(bufferedStream)
                else -> LittleEndianDataInputStream(bufferedStream)
            }
            val sampleAmount = (modifier.requestedTimeLength() * stream.format.frameRate / 1000).toInt()
            val left = ShortArray(sampleAmount)
            val right = ShortArray(sampleAmount)
            var reading = true
            var numBatches = 0
            while (reading) {
                var read = 0
                while (read < left.size) {
                    try {
                        left[read] = dis.readShort()
                        right[read] = dis.readShort()
                    } catch (e: EOFException) {
                        reading = false
                        break
                    }
                    read++
                }
                emit(modifyAsync(ChannelContent(left, right), read, numBatches))
                numBatches++
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .buffer()
        .map { it.await() }

    private fun CoroutineScope.modifyAsync(samples: ChannelContent<ShortArray>,
                                           read: Int,
                                           batchNum: Int): Deferred<ChannelContent<ShortArray>> {
        val buffer = samples.map { it.copyOf(read) }

        // ensure no references to samples in task
        return async(Dispatchers.Default) {
            buffer.map { modifier.modifySamples(it, batchNum) }
        }
    }

    private fun gather(bigEndian: Boolean, flow: Flow<ChannelContent<ShortArray>>): Flow<ByteBuffer> {
        val order = when {
            bigEndian -> ByteOrder.BIG_ENDIAN
            else -> ByteOrder.LITTLE_ENDIAN
        }
        return flow.map { (left, right) ->
            encodeChannels(order, left, right)
        }
    }

    private fun encodeChannels(order: ByteOrder, left: ShortArray, right: ShortArray): ByteBuffer {
        check(left.size == right.size) {
            "channel sizes should be equal, ${left.size} != ${right.size}"
        }
        val buffer = ByteBuffer.allocate((left.size + right.size) * Short.SIZE_BYTES).order(order)
        for ((l, r) in left.zip(right)) {
            buffer.putShort(l)
            buffer.putShort(r)
        }
        buffer.flip()
        return buffer
    }
}
