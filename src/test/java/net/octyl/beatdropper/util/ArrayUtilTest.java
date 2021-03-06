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

package net.octyl.beatdropper.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArrayUtilTest {

    private void assertReverseResult(short[] input, short[] expected) {
        short[] actual = ArrayUtil.reverse(input);
        assertSame(actual, input);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void emptyArrayReverses() {
        assertReverseResult(new short[] {}, new short[] {});
    }

    @Test
    public void oneElementArrayReverses() {
        assertReverseResult(new short[] { 1 }, new short[] { 1 });
    }

    @Test
    public void twoElementArrayReverses() {
        assertReverseResult(new short[] { 1, 2 }, new short[] { 2, 1 });
    }

    @Test
    public void threeElementArrayReverses() {
        assertReverseResult(new short[] { 1, 2, 3 }, new short[] { 3, 2, 1 });
    }

}
