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
package com.mastfrog.util;

import com.mastfrog.util.strings.ComparableCharSequence;
import com.mastfrog.util.strings.EightBitStrings;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class StringsTest {

    @Test
    public void testSplit() {
        EightBitStrings strs = new EightBitStrings(false, true);
        ComparableCharSequence seq = strs.create("hello world how are you ");
        CharSequence[] result = Strings.split(' ', seq);
        assertEquals(Arrays.asList(result).toString(), 5, result.length);
        String[] actual = seq.toString().split("\\s");
        for (int i = 0; i < result.length; i++) {
            assertEquals(actual[i], result[i].toString());
            assertTrue(result[i] + " vs " + actual[i], Strings.charSequencesEqual(actual[i], result[i], false));
        }
    }

    private final String test = "Mastfrog is awesome!";
    private final String unlike = test + " ";
    private final EightBitStrings strings = new EightBitStrings(true, true);
    private final CharSequence ascii = strings.create("Mastfrog is awesome!");
    private final CharSequence upper = strings.create("MASTFROG IS AWESOME!");

    @Test
    public void testEquality() {
        assertTrue(Strings.charSequencesEqual(test, ascii, false));
        assertTrue(Strings.charSequencesEqual(test, ascii, true));
        assertTrue(Strings.charSequencesEqual(test, upper, true));
        assertFalse(Strings.charSequencesEqual(test, upper, false));
        assertFalse(Strings.charSequencesEqual(test, unlike, false));
        assertFalse(Strings.charSequencesEqual(ascii, unlike, false));
        assertFalse(Strings.charSequencesEqual(upper, unlike, false));
        assertFalse(Strings.charSequencesEqual(test, unlike, true));
        assertFalse(Strings.charSequencesEqual(ascii, unlike, true));
        assertFalse(Strings.charSequencesEqual(upper, unlike, true));
    }

    @Test
    public void testHashCode() {
        assertEquals(test.hashCode(), Strings.charSequenceHashCode(test, false));
        assertEquals(test.toLowerCase().hashCode(), Strings.charSequenceHashCode(test, true));
        assertEquals(test.hashCode(), Strings.charSequenceHashCode(ascii, false));
        assertNotEquals(test.hashCode(), Strings.charSequenceHashCode(unlike, false));
        assertNotEquals(test.hashCode(), Strings.charSequenceHashCode(unlike, true));
    }

}
