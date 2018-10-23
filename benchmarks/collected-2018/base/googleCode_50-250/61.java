// https://searchcode.com/api/result/1618252/

/*
 * Copyright 2011 Administrator.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bramosystems.oss.player.core.client;

import com.google.gwt.junit.client.GWTTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Administrator
 */
public class TxtPlayTime extends GWTTestCase {

    public TxtPlayTime() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testAdd() {
        System.out.println("add");
        PlayTime instance = new PlayTime(2000);
        PlayTime expResult = new PlayTime(5000);
        PlayTime result = instance.add(3000);
        assertEquals(expResult, result);
    }

    @Test
    public void testReduce() {
        System.out.println("reduce");
        PlayTime instance = new PlayTime(5000);
        PlayTime expResult = new PlayTime(2000);
        PlayTime result = instance.reduce(3000);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetHour() {
        System.out.println("getHour");
        assertEquals(3, new PlayTime(3, 20, 40, 990).getHour());
    }

    @Test
    public void testSetHour() {
        System.out.println("setHour");
        PlayTime instance = new PlayTime();
        instance.setHour(9);
        assertEquals(instance, new PlayTime(9, 0, 0, 0));
    }

    @Test
    public void testGetMinute() {
        System.out.println("getMinute");
        assertEquals(20, new PlayTime(3, 20, 40, 990).getMinute());
    }

    @Test
    public void testSetMinute() {
        System.out.println("setMinute");
        PlayTime instance = new PlayTime();
        instance.setMinute(20);
        assertEquals(instance, new PlayTime(0, 20, 0, 0));
    }

    @Test
    public void testGetSecond() {
        System.out.println("getSecond");
        assertEquals(40, new PlayTime(3, 20, 40, 990).getSecond());
    }

    @Test
    public void testSetSecond() {
        System.out.println("setSecond");
        PlayTime instance = new PlayTime();
        instance.setSecond(40);
        assertEquals(instance, new PlayTime(0, 0, 40, 0));
    }

    @Test
    public void testGetFract() {
        System.out.println("getFract");
        assertEquals(990, new PlayTime(3, 20, 40, 990).getFract());
    }

    @Test
    public void testSetFract() {
        System.out.println("setFract");
        PlayTime instance = new PlayTime();
        instance.setFract(990);
        assertEquals(instance, new PlayTime(0, 0, 0, 990));
    }

    @Test
    public void testToString_boolean() {
        System.out.println("toString");
        PlayTime inst = new PlayTime(3, 20, 40, 990);
        assertEquals("03:20:40.990", inst.toString(true));
        assertEquals("3:20:40", inst.toString(false));
    }

    @Test
    public void testParseTimeString() {
        System.out.println("parseTimeString");
        assertEquals(new PlayTime("03:20:40.990"), new PlayTime(3, 20, 40, 990));
        assertEquals(new PlayTime("3:20:40"), new PlayTime(3, 20, 40, 0));
        assertEquals(new PlayTime("20:40"), new PlayTime(0, 20, 40, 0));
        assertEquals(new PlayTime("20:40.990"), new PlayTime(0, 20, 40, 990));
    }

    @Override
    public String getModuleName() {
        return "com.bramosystems.oss.player.core.Core";
    }
}
