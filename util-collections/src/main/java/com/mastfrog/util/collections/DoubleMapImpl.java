/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.search.Bias;
import com.mastfrog.util.sort.Sort;
import static java.lang.Double.doubleToLongBits;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
final class DoubleMapImpl<T> implements DoubleMap<T> {

    private final DoubleMapSet keySet;
    private Object[] values;

    DoubleMapImpl(int initialCapacity) {
        Checks.greaterThanZero("initialCapacity", initialCapacity);
        keySet = new DoubleMapSet(initialCapacity);
        values = new Object[initialCapacity];
    }

    @Override
    public DoubleSet keySet() {
        return keySet.unmodifiableView();
    }

    @Override
    public boolean remove(double key) {
        return keySet.remove(key);
    }

    @Override
    public int removeRange(double start, double end) {
        return keySet.removeRange(start, end);
    }

    @Override
    public void removeAll(double... keys) {
        if (keys.length == 0) {
            return;
        }
        keySet.removeAll(keys);
    }

    @Override
    public void removeAll(DoubleSet set) {
        keySet.removeAll(set);
    }

    @Override
    public void put(double key, T value) {
        int pos = keySet.size;
        keySet.add(key);
        if (keySet.size > pos) {
            values[pos] = value;
        } else {
            values[keySet.indexOf(key)] = value;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(double key) {
        int ix = keySet.indexOf(key);
        if (ix < 0) {
            return null;
        }
        return (T) values[ix];
    }

    @Override
    public T getOrDefault(double key, T defaultResult) {
        T result = get(key);
        return result == null ? defaultResult : result;
    }

    @Override
    public boolean containsKey(double key) {
        return keySet.contains(key);
    }

    @Override
    public int size() {
        return keySet.size();
    }

    @SuppressWarnings("unchecked")
    public T[] valueArray(Class<T> type) {
        T[] result = (T[]) Array.newInstance(type, size());
        System.arraycopy(values, 0, result, 0, size());
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> values() {
        if (values.length == size()) {
            return Arrays.<T>asList((T[]) values);
        } else {
            List<T> result = new ArrayList<>(size());
            for (int i = 0; i < size(); i++) {
                result.add((T) values[i]);
            }
            return Collections.unmodifiableList(result);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T valueAt(int index) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException("Index out of range: " + index);
        }
        return (T) values[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    public void forEach(DoubleMapConsumer<? super T> c) {
        for (int i = 0; i < size(); i++) {
            double key = keySet.getAsDouble(i);
            T value = (T) values[i];
            c.accept(i, key, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean least(DoubleMapConsumer<? super T> c) {
        if (size() == 0) {
            return false;
        }
        double val = keySet.least();
        T obj = (T) values[0];
        c.accept(0, val, obj);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean greatest(DoubleMapConsumer<? super T> c) {
        if (size() == 0) {
            return false;
        }
        int ix = size() - 1;
        double val = keySet.greatest();
        T obj = (T) values[ix];
        c.accept(ix, val, obj);
        return true;
    }

    @Override
    public boolean nearestValueTo(double approximate, double tolerance, DoubleMapConsumer<? super T> c) {
        BH bh = new BH();
        nearestValueTo(approximate, (ix, key, value) -> {
            if (Math.abs(key - approximate) < tolerance) {
                bh.set();
                c.accept(ix, key, value);
            }
        });
        return bh.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean nearestValueTo(double approximate, DoubleMapConsumer<? super T> c) {
        if (size() == 0) {
            return false;
        } else if (size() == 1) {
            keySet.ensureClean();
            c.accept(0, keySet.data[0], (T) values[0]);
            return true;
        }
        int ix = keySet.nearestIndexTo(approximate, Bias.NEAREST);
        if (ix < 0) {
            return false; // XXX impossible?
        }
        double value = keySet.data[ix];
        c.accept(ix, value, (T) values[ix]);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean nearestValueExclusive(double approximate, DoubleMapConsumer<? super T> c) {
        int size = keySet.size();
        if (size == 0) {
            return false;
        } else if (size == 1) {
            keySet.ensureClean();
            double result = keySet.data[0];
            if (result == approximate) {
                return false;
            }
            c.accept(0, result, (T) values[0]);
            return true;
        }
        int ix = keySet.nearestIndexTo(approximate, Bias.NEAREST);
        double result = keySet.data[ix];
        if (result == approximate) {
            int prevIx = ix - 1;
            int nextIx = ix + 1;
            double prevVal = prevIx < 0 ? (Double.MAX_VALUE - (Math.abs(approximate) + 1)) : keySet.data[prevIx];
            double nextVal = nextIx >= size ? (Double.MAX_VALUE - (Math.abs(approximate) + 1)) : keySet.data[nextIx];
            if (prevIx < 0) {
                c.accept(nextIx, nextVal, (T) values[nextIx]);
                return true;
            } else if (nextIx >= size) {
                c.accept(prevIx, prevVal, (T) values[prevIx]);
                return true;
            }
            double distPrev = Math.abs(approximate - prevVal);
            double distNext = Math.abs(approximate - nextVal);
            if (distPrev < distNext) {
                c.accept(prevIx, prevVal, (T) values[prevIx]);
                return true;
            } else {
                c.accept(nextIx, nextVal, (T) values[nextIx]);
                return true;
            }
        }
        c.accept(ix, result, (T) values[ix]);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256).append("{");
        for (int i = 0; i < size(); i++) {
            double key = keySet.getAsDouble(i);
            T value = valueAt(i);
            sb.append(DoubleSetImpl.FMT.format(key)).append(':')
                    .append(value);
            if (i != size() - 1) {
                sb.append(", ");
            }
        }
        return sb.append("}").toString();
    }

    @Override
    public boolean nearestValueExclusive(double approximate, double tolerance, DoubleMapConsumer<? super T> c) {
        BH bh = new BH();
        nearestValueExclusive(approximate, (ix, key, value) -> {
            if (Math.abs(approximate - key) <= tolerance) {
                bh.set();
                c.accept(ix, key, value);
            }
        });
        return bh.get();
    }

    @Override
    public int indexOf(double key) {
        return keySet.indexOf(key);
    }

    @Override
    public double key(int index) {
        return keySet.getAsDouble(index);
    }

    @Override
    public int hashCode() {
        long hash = 5;
        for (int i = 0; i < size(); i++) {
            hash += (7 * doubleToLongBits(key(i)))
                    + (51 * Objects.hashCode(values[i]));
        }
        return (int) (hash ^ (hash << 32));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof DoubleMap) {
            DoubleMap dm = (DoubleMap) o;
            if (dm.size() == size()) {
                for (int i = 0; i < size(); i++) {
                    double d = key(i);
                    double od = dm.key(i);
                    if (d != od) {
                        return false;
                    }
                    Object obj = values[i];
                    Object o1 = dm.valueAt(i);
                    if (!Objects.equals(obj, o1)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    final class DoubleMapSet extends DoubleSetImpl {

        DoubleMapSet(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        void moveItem(int srcIndex, int targetIndex, double v) {
            System.out.println("move " + srcIndex + " -> " + targetIndex);
            super.moveItem(srcIndex, targetIndex, v);
            values[targetIndex] = values[srcIndex];
        }

        @Override
        void shiftData(int srcIx, int destIx, int len) {
            super.shiftData(srcIx, destIx, len);
            System.arraycopy(values, srcIx, values, destIx, len);
        }

        @Override
        void grow(int newSize) {
            super.grow(newSize);
            values = Arrays.copyOf(values, newSize);
        }

        @Override
        void sort() {
            Sort.biSort(data, values, size);
        }
    }

    static final class BH {

        private boolean value;

        void set() {
            value = true;
        }

        void clear() {
            value = false;
        }

        boolean get() {
            return value;
        }
    }
}
