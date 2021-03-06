/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.collections;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Like an iterator, but without boxing/unboxing; this interface is deprecated
 * since PrimitiveIterator.OfInt was introduced in JDK 8.
 *
 * @author Tim Boudreau
 * @deprecated Implementations should use PrimitiveIterator.OfInt
 */
@Deprecated
public interface Interator extends IntSupplier {

    boolean hasNext();

    int next();

    @Override
    default int getAsInt() {
        return hasNext() ? next() : -1;
    }

    default void forEachRemaining(IntConsumer consumer) {
        while (hasNext()) {
            consumer.accept(next());
        }
    }

    default void forEachRemaining(Consumer<? super Integer> action) {
        while (hasNext()) {
            action.accept(next());
        }
    }

}
