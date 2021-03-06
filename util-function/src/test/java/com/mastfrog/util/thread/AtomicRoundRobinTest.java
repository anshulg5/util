/*
 * The MIT License
 *
 * Copyright 2010-2015 Tim Boudreau.
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

package com.mastfrog.util.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class AtomicRoundRobinTest {

    private ExecutorService threadPool = Executors.newFixedThreadPool(100);

    @Test
    public void test() {
        AtomicRoundRobin rob = new AtomicRoundRobin(23);
        for (int i = 0; i < 500; i++) {
            int val = rob.next();
            if ((i % 23) == 0) {
                assertEquals(val, 0);
            } else {
                assertEquals(i % 23, val);
            }
        }
    }

    static class R implements Runnable {

        private final AtomicRoundRobin r;
        private final CountDownLatch startLock;
        private final CountDownLatch entryLock;
        private final CountDownLatch exitLock;
        private int[] values;
        boolean failed;

        R(AtomicRoundRobin r, CountDownLatch startLock, CountDownLatch entryLock, CountDownLatch exitLock, int loopCount) {
            this.r = r;
            this.startLock = startLock;
            this.entryLock = entryLock;
            this.exitLock = exitLock;
            values = new int[loopCount];
        }

        @Override
        public void run() {
            try {
                startLock.countDown();
                entryLock.await();
                for (int i = 0; i < values.length; i++) {
                    values[i] = r.next();
                    if (values[i] >= r.maximum() || values[i] < 0) {
                        failed = true;
                        throw new Error(values[i] + "");
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(AtomicRoundRobinTest.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                exitLock.countDown();
            }
        }
    }

    @Test
    public void testNext() throws InterruptedException {
        //Just in case, loop enough times that all the code will be JIT'd
        for (int i = 0; i < 1500; i++) {
            doTestNext(i);
        }
    }

    private void doTestNext(int iteration) throws InterruptedException {
        int maximum = 7;
        //These need to be multiples of maximum to make the test simpler
        int threadCount = 10 * maximum;
        int loopCount = 100 * maximum;
        CountDownLatch startLock = new CountDownLatch(threadCount);
        CountDownLatch entryLock = new CountDownLatch(1);
        CountDownLatch exitLock = new CountDownLatch(threadCount);
        AtomicRoundRobin r = new AtomicRoundRobin(maximum);
        R[] runners = new R[threadCount];
        for (int i = 0; i < threadCount; i++) {
            runners[i] = new R(r, startLock, entryLock, exitLock, loopCount);
            threadPool.submit(runners[i]);
        }
        //Wait for all threads to enter their run() method
        startLock.await();
        //release them all at ~ the same time
        entryLock.countDown();
        //Wait for all of them to complete their loops
        exitLock.await();

        //Okay, every value from 0 to (max-1) should have been seen
        //exactly once per iteration.  This means that no matter how
        //many threads looped over our value, the total number of times
        //each value was seen should be the same.  If it is not, then
        //we know the value was not actually atomic
        //make an array for each legal value, to put the count into
        int[] seenValueCount = new int[maximum];
        for (R runner : runners) {
            //make sure no out-of-range values were seen
            assertFalse(runner.failed);
            //iterate all the values this runner saw
            for (int i = 0; i < runner.values.length - loopCount; i++) {
                //increment the recorded number of times this value was seen
                seenValueCount[runner.values[i]]++;
            }
        }
        //set a placeholder
        int val = -1;
        //now confirm that every value was seen the same number of times
        for (int i = 0; i < seenValueCount.length; i++) {
            if (val == -1) {
                //first iteration
                val = seenValueCount[i];
            } else //every value should be the same
            if (val != seenValueCount[i]) {
                fail("The value " + i + " was seen " + seenValueCount[i]
                        + " times, but other values were seen " + val + " times");
            }
        }

    }

}
