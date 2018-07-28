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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;

/**
 * Simplified range, [lowBound(), highBound()).
 */
@AutoValue
public abstract class SampleSelection implements Comparable<SampleSelection> {

    public static SampleSelection make(int low, int high) {
        if (low > high) {
            throw new IllegalArgumentException("Low end cannot be bigger than high end");
        }
        return new AutoValue_SampleSelection(low, high);
    }

    SampleSelection() {
    }

    @Override
    public int compareTo(SampleSelection o) {
        return ComparisonChain.start()
                // see if o is higher than this one
                .compare(highBound(), o.highBound())
                // maybe it is on low bounds?
                .compare(lowBound(), o.lowBound())
                .result();
    }

    public abstract int lowBound();

    public abstract int highBound();

    public final int length() {
        return highBound() - lowBound();
    }

}