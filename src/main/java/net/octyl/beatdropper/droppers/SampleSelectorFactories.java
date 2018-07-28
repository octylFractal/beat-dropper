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
package net.octyl.beatdropper.droppers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;

public class SampleSelectorFactories {

    private static final ImmutableMap<String, SampleSelectorFactory> byId;

    static {
        byId = Streams.stream(ServiceLoader.load(SampleSelectorFactory.class))
                .sorted(Comparator.comparing(SampleSelectorFactory::getId))
                .collect(toImmutableMap(SampleSelectorFactory::getId, Function.identity()));
    }

    public static SampleSelectorFactory getById(String id) {
        SampleSelectorFactory factory = byId.get(id);
        checkArgument(factory != null, "No factory by the ID '%s'", id);
        return factory;
    }

    public static String formatAvailableForCli() {
        return byId.keySet().stream()
                .collect(Collectors.joining("\n\t", "\t", ""));
    }

}