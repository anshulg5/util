/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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

package com.mastfrog.predicates.string;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class StringArrayPredicate implements Predicate<String> {

    private final boolean negated;
    private final String[] vals;

    StringArrayPredicate(boolean negated, String[] vals) {
        this.negated = negated;
        this.vals = vals;
    }

    @Override
    public boolean test(String t) {
        boolean result = Arrays.binarySearch(vals, t) >= 0;
        return negated ? !result : result;
    }

    @Override
    public Predicate<String> negate() {
        return new StringArrayPredicate(!negated, vals);
    }

    @Override
    public String toString() {
        String pfx = negated ? "!match(" : "match(";
        return pfx + (vals.length == 1 ? vals[0] : Arrays.toString(vals)) + ")";
    }

}
