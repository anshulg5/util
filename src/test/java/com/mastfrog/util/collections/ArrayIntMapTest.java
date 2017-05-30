/*
 * The MIT License
 *
 * Copyright 2004 Tim Boudreau.
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ArrayIntMapTest {

    NumberFormat nf = NumberFormat.getIntegerInstance(Locale.UK);

    {
        nf.setMinimumIntegerDigits(4);
    }

    @Test
    public void testEmpty() {
        ArrayIntMap<String> map = new ArrayIntMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.values().size());
        assertFalse(map.iterator().hasNext());
        assertFalse(map.keySet().iterator().hasNext());
        assertFalse(map.values().iterator().hasNext());
        assertNull(map.get(0));
        assertNull(map.get(4024));
        assertNull(map.get(Integer.MAX_VALUE));
        assertNull(map.get(Integer.MIN_VALUE));
        assertFalse(map.containsKey(0));
        assertFalse(map.containsKey(-1));
        assertFalse(map.containsKey(Integer.MAX_VALUE));
        assertFalse(map.containsKey(Integer.MIN_VALUE));
        assertEquals(0, map.keySet().toArray().length);
        map.put(23, "foo");
        assertEquals(1, map.size());
        assertEquals("foo", map.remove(23));
        assertNull(map.get(23));
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testCopyConstructor() {
        Map<Integer, String> mm = new HashMap<>();
        int i = 3;
        while (mm.size() < 100) {
            int k = i * 2;
            String v = "v" + nf.format(k);
            mm.put(k, v);
            i += 3;
        }
        ArrayIntMap<String> map = new ArrayIntMap<>(mm);

        assertEquals(mm.size(), map.size());
        assertEquals("v0,162", map.get(162));
        assertEquals(mm, map);
    }

    @Test
    public void testReplacePut() {
        ArrayIntMap<String> map = new ArrayIntMap<>();
        Set<Integer> l = new HashSet<>();
        Map<Integer, String> mm = new HashMap<>();
        int i = 3;
        while (map.size() < 100) {
            int k = i * 2;
            String v = "v" + Integer.toString(k);
            map.put(k, v);
            mm.put(k, v);
            l.add(k);
            i += 3;
        }
        assertEquals(mm, map);
        assertEquals(mm.keySet(), map.keySet());
        List<String> expect = new ArrayList<>(mm.values());
        List<String> got = new ArrayList<>(map.values());
        Collections.sort(expect);
        Collections.sort(got);
        assertEquals(expect, got);

        assertTrue(map.containsKey(6));
        String old = map.put(6, "first");
        assertEquals("v6", old);
        assertEquals(100, map.size());
        assertEquals("first", map.get(6));
        assertTrue(map.values().contains("first"));
        assertTrue(map.containsKey(6));

        int last = map.highestKey();
        assertTrue(map.containsKey(last));
        old = map.put(last, "last");
        assertNotNull(old);
        assertEquals("last", map.get(last));

        int mid = 36;
        assertTrue(map.containsKey(mid));
        old = map.put(mid, "mid");
        assertNotNull(old);
        assertEquals("mid", map.get(mid));

        assertFalse(map.containsKey(206));
        assertFalse(map.containsKey(-1));
    }

    @Test
    public void testRemove() {
        ArrayIntMap<String> map = new ArrayIntMap<>();
        Set<Integer> keys = new HashSet<>();
        Set<String> values = new HashSet<>();
        int i = 3;
        while (map.size() < 100) {
            int k = i * 2;
            String v = "v" + nf.format(k);
            values.add(v);
            map.put(k, v);
            keys.add(k);
            assertEquals(v, map.get(k));
            assertTrue(map.containsKey(k));
            assertTrue(map.containsValue(v));
            i += 3;
        }
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());
        assertEquals(keys.size(), map.keySet().size());
        assertTrue(keys.containsAll(map.keySet()));
        assertTrue(map.keySet().containsAll(keys));
        assertEquals(keys, map.keySet());
        assertEquals(100, map.size());

        // Remove at head, tail and middle use different code
        int first = map.lowestKey();
        assertEquals(6, first);
        assertTrue(map.containsKey(first));
        String old = map.get(first);
        assertNotNull(old);
        assertTrue(map.values().contains(old));
        String oold = map.remove(first);
        keys.remove(first);
        assertSame(old, oold);
        values.remove(oold);
        assertEquals(99, map.size());
        assertFalse(map.containsKey(first));
        assertFalse(map.values().contains(old));
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());

        int last = map.highestKey();
        assertTrue(map.containsKey(last));
        old = map.get(last);
        assertNotNull(old);
        assertTrue(map.values().contains(old));
        oold = map.remove(last);
        keys.remove(last);
        assertSame(old, oold);
        values.remove(oold);
        assertEquals(98, map.size());
        assertFalse(map.containsKey(last));
        assertFalse(map.values().contains(old));
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());

        int mid = 162;
        old = map.get(mid);
        assertEquals("v0,162", old);
        assertTrue(map.containsKey(mid));
        assertTrue(map.keySet().contains(mid));
        oold = map.remove(mid);
        keys.remove(mid);
        values.remove(oold);
        assertSame(oold, old);
        assertEquals(97, map.size());
        assertFalse(map.containsKey(mid));
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());

        assertFalse("Should not contain " + oold + " but does at "
                + map.keyForValue(oold) + ": " + map.values(), map.values().contains(old));
    }

    private <T extends Comparable<T>> void assertCollectionsEquals(Collection<T> a, Collection<T> b) {
        List<T> aa = new ArrayList<>(a);
        List<T> bb = new ArrayList<>(b);
        Collections.sort(aa);
        Collections.sort(bb);
        assertEquals(aa.size(), bb.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aa.size(); i++) {
            T aaa = aa.get(i);
            T bbb = bb.get(i);
            if (!Objects.equals(aaa, bbb)) {
                sb.append("Difference at " + i + " - " + aaa + " vs " + bbb + "\n");
            }
        }
        assertEquals(sb.toString(), aa, bb);
    }

    @Test
    public void testFirst() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        assertTrue("First entry should be 5", map.lowestKey() == 5);
    }

    @Test
    public void testNextEntry() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = 0; i < indices.length - 1; i++) {
            int val = indices[i + 1];
            int next = map.nextEntry(indices[i]);
            assertTrue("Entry after " + indices[i] + " should be " + val + " not " + next, next == val);
        }
    }

    @Test
    public void testPrevEntry() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = indices.length - 1; i > 0; i--) {
            int val = indices[i - 1];
            int next = map.prevEntry(indices[i]);
            assertTrue("Entry before " + indices[i] + " should be " + val + " not " + next, next == val);
        }
    }

    @Test
    public void testNearest() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = 0; i < indices.length - 1; i++) {
            int toTest = indices[i] + ((indices[i + 1] - indices[i]) / 2);
            int next = map.nearest(toTest, false);
            assertTrue("Nearest value to " + toTest + " should be " + indices[i + 1] + ", not " + next, next == indices[i + 1]);
        }

        assertTrue("Value after last entry should be 0th", map.nearest(indices[indices.length - 1] + 1000, false) == indices[0]);

        assertTrue("Value before first entry should be last", map.nearest(-1, true) == indices[indices.length - 1]);

        assertTrue("Value after < first entry should be 0th", map.nearest(-1, false) == indices[0]);

        for (int i = indices.length - 1; i > 0; i--) {
//            int toTest = indices[i] - (indices[i-1] + ((indices[i] - indices[i-1]) / 2));
            int toTest = indices[i - 1] + ((indices[i] - indices[i - 1]) / 2);
            int prev = map.nearest(toTest, true);
            assertTrue("Nearest value to " + toTest + " should be " + indices[i - 1] + ", not " + prev, prev == indices[i - 1]);
        }

        assertTrue("Entry previous to value lower than first entry should be last entry",
                map.nearest(indices[0] - 1, true) == indices[indices.length - 1]);

        assertTrue("Value after > last entry should be last 0th", map.nearest(indices[indices.length - 1] + 100, false) == indices[0]);

        assertTrue("Value before > last entry should be last entry", map.nearest(indices[indices.length - 1] + 100, true) == indices[indices.length - 1]);

        assertTrue("Value after < first entry should be 0th", map.nearest(-10, false) == indices[0]);

    }

    /**
     * Test of get method, of class org.netbeans.core.output2.ArrayIntMap.
     */
    @Test
    public void testGet() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = 0; i < indices.length; i++) {
            assertTrue(map.get(indices[i]) == values[i]);
        }
    }

    @Test
    public void testGetKeys() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        int[] keys = map.getKeys();
        assertTrue("Keys returned should match those written.  Expected: " + i2s(indices) + " Got: " + i2s(keys), Arrays.equals(keys, indices));
    }

    private static String i2s(int[] a) {
        StringBuffer result = new StringBuffer(a.length * 2);
        for (int i = 0; i < a.length; i++) {
            result.append(a[i]);
            if (i != a.length - 1) {
                result.append(',');
            }
        }
        return result.toString();
    }
}