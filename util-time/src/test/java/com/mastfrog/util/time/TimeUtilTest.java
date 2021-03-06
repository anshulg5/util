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
package com.mastfrog.util.time;

import static com.mastfrog.util.time.TimeUtil.GMT;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class TimeUtilTest {

    private final String s = "Mon, 29 May 2017 03:33:57 -04:00";
    private final long millis = 1496043237000L;

    private static final String GIT_LOG_ISO_FORMAT = "2019-04-28 16:59:53 -0400";

    @Test
    public void testGitFormat() {
        ZonedDateTime dt = ZonedDateTime.now().with(ChronoField.MILLI_OF_SECOND, 0).withZoneSameInstant(GMT);
        assertEquals(dt.toInstant(), TimeUtil.fromGitLogFormat(TimeUtil.toGitLogFormat(dt)).toInstant());

        ZonedDateTime zdt = TimeUtil.fromGitLogFormat(GIT_LOG_ISO_FORMAT).withZoneSameInstant(GMT);
        assertEquals(4, zdt.getMonthValue());
        assertEquals(28, zdt.getDayOfMonth());
        assertEquals(2019, zdt.getYear());
        assertEquals(20, zdt.getHour());
        assertEquals(59, zdt.getMinute());
        assertEquals(53, zdt.getSecond());

        String gitFmt = TimeUtil.toGitLogFormat(dt);
        ZonedDateTime got = TimeUtil.fromGitLogFormat(gitFmt);
        assertEquals(dt.toInstant(), got.toInstant());
    }

    @Test
    public void testOtherConversions() {
        ZonedDateTime zdt = TimeUtil.fromUnixTimestamp(millis);
        LocalDateTime ldt = TimeUtil.localFromUnixTimestamp(millis);
        OffsetDateTime odt = TimeUtil.offsetFromUnixTimestamp(millis);

        assertEquals(millis, TimeUtil.toUnixTimestamp(odt));
        assertEquals(millis, TimeUtil.toUnixTimestamp(zdt));
        assertEquals(millis, TimeUtil.toUnixTimestampSystemDefault(ldt));
    }

    @Test
    public void testIsoStrings() {
        ZonedDateTime zdt = TimeUtil.fromUnixTimestamp(millis);
        LocalDateTime ldt = zdt.toLocalDateTime();
        OffsetDateTime odt = TimeUtil.offsetFromUnixTimestamp(millis);
        Instant ins = Instant.ofEpochMilli(millis);
        Date date = new Date(millis);

        String z = TimeUtil.toIsoFormat(zdt);
        String l = TimeUtil.toIsoFormat(ldt);
        String o = TimeUtil.toIsoFormat(odt);
        String i = TimeUtil.toIsoFormat(ins);
        String d = TimeUtil.toIsoFormat(date);

        assertEquals("local", z, l);
        assertEquals("offset", z, o);
        assertEquals("instant", z, i);
        assertEquals("date", z, d);

        ZonedDateTime z2 = TimeUtil.fromIsoFormat(z);
        LocalDateTime loc = TimeUtil.localFromIsoFormat(l);
        OffsetDateTime off = TimeUtil.offsetFromIsoFormat(o);
        Instant inst = TimeUtil.instantFromIsoFormat(i);

        assertTrue(TimeUtil.equals(zdt, z2));
        assertTrue(odt + " vs " + off, TimeUtil.equals(odt, off));
        assertEquals(ins + " vs " + inst, ins, inst);
        assertEquals(ldt.toInstant(ZoneOffset.UTC), loc.toInstant(ZoneOffset.UTC));
        assertTrue(ldt + " vs " + loc, TimeUtil.equals(ldt, loc));

    }

    @Test
    public void testConversions() {

        ZonedDateTime zdt = TimeUtil.fromUnixTimestamp(millis);
        long test = TimeUtil.toUnixTimestamp(zdt);
        assertEquals(millis, test);

        ZonedDateTime zoned = TimeUtil.fromHttpHeaderFormat(s);
        assertEquals(millis, TimeUtil.toUnixTimestamp(zoned));

        long test2 = TimeUtil.timestampFromHttpHeaderFormat(s);
        assertEquals(millis, test2);
    }

    @Test
    public void testDurationParse() {
        Duration a = Duration.ofDays(362)
                .plus(Duration.ofHours(3))
                .plus(Duration.ofMinutes(17))
                .plus(Duration.ofSeconds(23))
                .plus(Duration.ofMillis(39));
        String stringRep = "P362DT3H17M23.039S";
        Duration b = TimeUtil.parseDuration(a.toString());
        Duration c = TimeUtil.parseDuration(TimeUtil.format(a));
        Duration d = TimeUtil.parseDuration(TimeUtil.format(a, true));
        Duration e = TimeUtil.parseDuration(stringRep);
        assertEquals(a, b);
        assertEquals(a, c);
        assertEquals(a, d);
        assertEquals(a, e);
    }

    private void testDur(String sht, String full, Duration d) {
        String shortFormatted = TimeUtil.format(d);
        String longFormatted = TimeUtil.format(d, true);
        assertEquals(sht, shortFormatted);
        assertEquals(full, longFormatted);
        assertEquals(d, TimeUtil.parse(shortFormatted));
        assertEquals(d, TimeUtil.parse(longFormatted));
        assertEquals(d, TimeUtil.parseDuration(d.toString()));
    }

    @Test
    public void testFormatDuration() {
        Duration d = Duration.ofDays(3).plus(Duration.ofHours(13).plus(Duration.ofMinutes(57)).plus(Duration.ofSeconds(5).plus(Duration.ofMillis(536))));
        testDur("03:13:57:05.536", "03:13:57:05.536", d);

        d = Duration.ofHours(13).plus(Duration.ofMinutes(57)).plus(Duration.ofSeconds(5).plus(Duration.ofMillis(536)));
        testDur("13:57:05.536", "00:13:57:05.536", d);

        d = Duration.ofMinutes(57).plus(Duration.ofSeconds(5)).plus(Duration.ofMillis(536));
        testDur("57:05.536", "00:00:57:05.536", d);

        d = Duration.ofSeconds(5).plus(Duration.ofMillis(536));
        testDur("05.536", "00:00:00:05.536", d);

        d = Duration.ofMillis(536);
        testDur("00.536", "00:00:00:00.536", d);

        d = Duration.ofDays(3).plus(Duration.ofHours(0).plus(Duration.ofMinutes(0)).plus(Duration.ofSeconds(0).plus(Duration.ofMillis(536))));
        testDur("03:00:00:00.536", "03:00:00:00.536", d);

        d = Duration.ofDays(0).plus(Duration.ofHours(0).plus(Duration.ofMinutes(0)).plus(Duration.ofSeconds(0).plus(Duration.ofMillis(536))));
        testDur("00.536", "00:00:00:00.536", d);

        d = Duration.ZERO;
        testDur("00.000", "00:00:00:00.000", d);

        d = Duration.ofDays(7233);
        testDur("7233:00:00:00.000", "7233:00:00:00.000", d);

        d = Duration.ofDays(2937233);
        testDur("2937233:00:00:00.000", "2937233:00:00:00.000", d);

        d = Duration.ofDays(2937233).plus(Duration.ofMillis(12));
        testDur("2937233:00:00:00.012", "2937233:00:00:00.012", d);

        d = Duration.ofDays(Integer.MAX_VALUE).plus(Duration.ofMillis(12));
        testDur(Integer.MAX_VALUE + ":00:00:00.012", Integer.MAX_VALUE + ":00:00:00.012", d);

//        long max = 106751991167300L; // highest value Duration.ofDays() can take before overflow on JDK 8
        long max = findMax(); // jdk 8 this is 106,751,991,167,299
        d = Duration.ofDays(max);
        testDur(max + ":00:00:00.000", max + ":00:00:00.000", d);
    }

    public long findMax() {
        // Does a fast binary search for the mxaximum value you can pass to
        // Duration.ofDays() with high values for hours, minutes, seconds and milliseconds
        long start = 0L;
        long end = Long.MAX_VALUE;
        long test = 0;
        for (;;) {
            if (end - start == 1) {
                break;
            }
            long offset = ((end - start) / 2L);
            try {
                long curr = start + offset;
                Duration.ofDays(curr).plus(Duration.ofHours(23)).plus(Duration.ofMinutes(59)).plus(Duration.ofSeconds(59)).plus(Duration.ofMillis(999));
//                Duration.ofDays(curr);
                test = curr;
                start = curr;
            } catch (Exception e) {
                end = start + offset;
                continue;
            }
        }
        return test;
    }

}
