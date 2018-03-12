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
package com.mastfrog.util.service;

import java.util.Iterator;
import java.util.ServiceLoader;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ServiceProviderTest {

    @Test
    public void testSomeMethod() {
        Iterator<AbstractWoogle> woogs = ServiceLoader.load(AbstractWoogle.class).iterator();
        assertTrue(woogs.hasNext());
        AbstractWoogle first = woogs.next();
        assertNotNull(first);
        assertTrue(woogs.hasNext());
        AbstractWoogle second = woogs.next();
        assertNotNull(second);
        assertFalse(woogs.hasNext());

        assertTrue(first.getClass().getName(), first instanceof ConcreteWoogle || first instanceof AnotherWoogle);
        assertTrue(second.getClass().getName() + " and " + first.getClass().getName(),
                (second instanceof AnotherWoogle || second instanceof ConcreteWoogle));
        assertFalse(first.getClass() == second.getClass());
    }

}
