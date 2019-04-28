/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

import com.mastfrog.util.collections.CollectionUtils.ComparableComparator;
import static com.mastfrog.util.collections.CollectionUtils.checkDuplicates;
import com.mastfrog.util.tree.Indexed;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
final class ComparatorListIndexedImpl<T> implements Indexed<T> {

    private final Object[] sorted;
    private final int[] indices;
    private final List<T> origOrder;

    @SuppressWarnings("unchecked")
    ComparatorListIndexedImpl(Comparator<T> compar, List<T> items) {
        origOrder = items;
        sorted = items.toArray();
        Arrays.sort(sorted, (Comparator) compar);
        indices = new int[items.size()];
        List<T> order = new ArrayList<>(items);
        checkDuplicates(order);
        for (int i = 0; i < items.size(); i++) {
            Object s = sorted[i];
            indices[i] = order.indexOf(s);
        }
    }

    static <T extends Comparable<T>> Indexed<T> create(List<T> items) {
        return new ComparatorListIndexedImpl<>(new ComparableComparator<T>(), items);
    }

    @Override
    public int size() {
        return sorted.length;
    }

    @Override
    public int indexOf(Object o) {
        int ix = Arrays.binarySearch(sorted, o);
        return ix < 0 ? -1 : indices[ix];
    }

    @Override
    public T get(int index) {
        return origOrder.get(index);
    }

}
