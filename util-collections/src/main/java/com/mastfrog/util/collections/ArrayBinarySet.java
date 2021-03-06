/*
 * The MIT License
 *
 * Copyright 2018 tim.
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

import com.mastfrog.util.strings.Strings;
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A lighter-weight set which uses a comparator to establish membership.
 *
 * @author Tim Boudreau
 */
final class ArrayBinarySet<T> extends AbstractSet<T> implements SortedSet<T> {

    final boolean comparatorEquality;

    final Comparator<? super T> comp;
    final T[] objs;

    @SafeVarargs
    ArrayBinarySet(boolean check, boolean comparatorEquality, Comparator<? super T> comp, T... objs) {
        this.comparatorEquality = comparatorEquality;
        this.comp = comp;
        this.objs = check ? ArrayUtils.dedup(objs) : objs;
        Arrays.sort(this.objs, comp);
    }

    ArrayBinarySet(Comparator<? super T> comp, T[] objs, boolean comparatorEquality) {
        this.comparatorEquality = comparatorEquality;
        this.objs = objs;
        this.comp = comp;
    }

    @SafeVarargs
    static <T extends Comparable<? super T>> ArrayBinarySet<T> of(T... objs) {
        return new ArrayBinarySet<>(true, true, Comparator.naturalOrder(), objs);
    }

    Class<?> type() {
        return objs.getClass().getComponentType();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comp;
    }

    private void checkType(Object obj) {
        if (obj == null) {
            throw new NullPointerException("Passed object is null");
        }
        if (!type().isInstance(obj)) {
            throw new ClassCastException(obj + " is not a " + type().getName());
        }
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        checkType(fromElement);
        checkType(toElement);
        int a = indexOf(fromElement);
        if (a < 0) {
            if (comp.compare(fromElement, first()) < 0) {
                a = 0;
            } else {
                return Collections.emptySortedSet();
            }
        }
        int b = indexOf(toElement);
        if (b < 0) {
            if (comp.compare(toElement, last()) > 0) {
                b = objs.length;
            } else {
                return Collections.emptySortedSet();
            }
        }
        int start = a;
        int stop = b;
        if (a > b) {
            throw new IllegalArgumentException("to < from");
        }
        T[] sub = ArrayUtils.extract(objs, start, stop - start);
        return new ArrayBinarySet<>(comp, sub, comparatorEquality);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        checkType(toElement);
        int ix = indexOf(toElement);
        if (ix < 0) {
            if (comp.compare(toElement, first()) < 0) {
                ix = 0;
            } else {
                return Collections.emptySortedSet();
            }
        }
        T[] sub = Arrays.copyOf(objs, ix);
        return new ArrayBinarySet<>(comp, sub, comparatorEquality);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        checkType(fromElement);
        int ix = indexOf(fromElement);
        if (ix < 0) {
            return Collections.emptySortedSet();
        }
        T[] sub = ArrayUtils.extract(objs, ix, objs.length - ix);
        return new ArrayBinarySet<>(comp, sub, comparatorEquality);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return objs[0];
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return objs[objs.length - 1];
    }

    @Override
    public Object[] toArray() {
        return (Object[]) objs.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length == objs.length) {
            System.arraycopy(objs, 0, a, 0, objs.length);
            return a;
        }
        a = (T[]) Array.newInstance(a.getClass().getComponentType(), objs.length);
        System.arraycopy(objs, 0, a, 0, objs.length);
        return a;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (int i = 0; i < objs.length; i++) {
            action.accept(objs[i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        if (o == null || objs.length == 0) {
            return false;
        }
        if (objs.getClass().getComponentType().isInstance(o)) {
            return binaryComparatorSearch((T) o);
        }
        return false;
    }

    private boolean binaryComparatorSearch(T o) {
        int start = 0;
        int end = objs.length - 1;
        return binaryComparatorSearch(comparatorEquality, o, objs, start, end, comp) >= 0;
    }

    private int indexOf(T obj) {
        if (isEmpty()) {
            return -1;
        }
        int start = 0;
        int end = objs.length - 1;
        return binaryComparatorSearch(comparatorEquality, obj, objs, start, end, comp);
    }

    static <T> int binaryComparatorSearch(boolean comparatorEquality, T o, T[] objs, int start, int end, Comparator<? super T> comp) {
        if (start < 0) {
            throw new IllegalArgumentException("Negative start " + start);
        }
        if (end < 0) {
            throw new IllegalArgumentException("Negative end " + end);
        }
        if (!comparatorEquality) {
//            System.out.println("USE BINARY SEARCH");
//            return Arrays.binarySearch(objs, start, end + 1, o, comp);
        }
        if (start == end) {
            return -1;
        }
        int startCompare = compareAt(o, objs, start, comp);
        if (startCompare == 0) {
            return start;
        }
        if (startCompare < 0) {
            return -1;
        }
        int endCompare = compareAt(o, objs, end, comp);
        if (endCompare == 0) {
            return end;
        }
        if (endCompare > 0) {
            return -1;
        }
        int amt = ((end - start) + 1) / 2;
        int pos = binaryComparatorSearch(false, o, objs, start + amt, end, comp);
        if (pos >= 0) {
            return pos;
        }
        return binaryComparatorSearch(false, o, objs, start, end - amt, comp);
    }

    private static <T> int compareAt(T o, T[] objs, int position, Comparator<T> comp) {
        return comp.compare(o, objs[position]);
    }

    @Override
    public Iterator<T> iterator() {
        return CollectionUtils.toIterator(objs);
    }

    @Override
    public int size() {
        return objs.length;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < objs.length; i++) {
            h += objs[i].hashCode();
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof ArrayBinarySet<?>) {
            ArrayBinarySet<?> abs = (ArrayBinarySet<?>) o;
            return Arrays.equals(objs, abs.objs);
        }
        return super.equals(o);
    }

    @Override
    public boolean isEmpty() {
        return objs.length == 0;
    }

    @Override
    public String toString() {
        return '[' + Strings.join(',', (Object[]) objs).toString() + ']';
    }

    @Override
    public Spliterator<T> spliterator() {
        return new ArraySpliterator<>(objs);
    }
}
