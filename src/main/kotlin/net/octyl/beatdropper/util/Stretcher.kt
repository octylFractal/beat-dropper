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

package net.octyl.beatdropper.util

import org.bytedeco.ffmpeg.global.avfilter.av_buffersink_set_frame_size
import org.bytedeco.ffmpeg.global.avutil.AV_OPT_SEARCH_CHILDREN
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_double
import org.bytedeco.ffmpeg.global.avutil.av_opt_set_int

class Stretcher(
    inputFormat: Format,
    outputFormat: Format,
    factor: Double,
    sizeOfInput: Int
) : FilterGraph(inputFormat, outputFormat,
    filters = listOf(
        // TODO We might consider switching to rubberband
        Filter("atempo", "tempo") { ctx ->
            av_opt_set_double(ctx, "tempo", factor, AV_OPT_SEARCH_CHILDREN)
        },
        Filter("asetnsamples", "samples") { ctx ->
            av_opt_set_int(ctx, "nb_out_samples", (sizeOfInput / factor).toLong(), AV_OPT_SEARCH_CHILDREN)
        }
    )
) {
    init {
        av_buffersink_set_frame_size(bufferSinkCtx, (sizeOfInput / factor).toInt())
    }
}
