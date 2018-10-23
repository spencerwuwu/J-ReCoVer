// https://searchcode.com/api/result/3863896/

/*
 * CoreStringTest.java
 * 
 * Copyright (c) 2010, Ralf Biedert All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the author nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package junit;

import static net.jcores.jre.CoreKeeper.$;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import junit.data.Data;
import net.jcores.jre.cores.CoreNumber;
import net.jcores.jre.cores.CoreObject;
import net.jcores.jre.cores.CoreString;
import net.jcores.jre.interfaces.functions.F1;
import net.jcores.jre.interfaces.functions.F2DeltaObjects;
import net.jcores.jre.interfaces.functions.F2ReduceObjects;
import net.jcores.jre.interfaces.functions.Fn;
import net.jcores.jre.options.Indexer;
import net.jcores.jre.options.KillSwitch;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ralf Biedert
 */
public class CoreObjectTest {

    

    /** Tests if async() works. */
    @Test
    public void testAsync() {
        final CoreString c = $("a", "b", "c", "d", "e");
        final F1<String, String> f = new F1<String, String>() {
            @Override
            public String f(String x) {
                $.sys.sleep(100);
                return x + x;
            }
        };
        
        CoreObject<String> result = c.async(f).await();
        Assert.assertEquals("aabbccddee", result.string().join());
        
        CoreObject<String> result2 = c.async(f, KillSwitch.TIMED(150)).await();
        Assert.assertTrue(result2.contains("aa"));
        Assert.assertFalse(result2.contains("cc"));
    }

    
    /** Tests if intersect() works. */
    @Test
    public void testIntersect() {
        Assert.assertEquals("world", $("hello", "world").intersect($("world", "goodbye")).get(0));
        Assert.assertEquals(1, $("world", "hello").intersect($("world", "goodbye")).compact().size());
    }

    /** Tests if forEach() works. */
    @Test
    public void testForEach() {
        Assert.assertEquals("h", $("hello").forEach(new F1<String, String>() {
            @Override
            public String f(String x) {
                return x.substring(0, 1);
            }

        }).string().join());

        Assert.assertEquals("hw", $("hello", "world").forEach(new F1<String, String>() {
            @Override
            public String f(String x) {
                return x.substring(0, 1);
            }

        }).string().join());

        Assert.assertEquals("abcdefg", $("aa", "bb", "cc", "dd", "ee", "ff", "gg").forEach(new F1<String, String>() {
            @Override
            public String f(String x) {
                return x.substring(0, 1);
            }

        }).string().join());
        
        
        Assert.assertEquals("ab", $("a", "b", "c", "d").forEach(new Fn<String, String>() {
            @Override
            public String f(String... x) {
                return $(x).join();
            }
        }, 2).get(0));
        Assert.assertEquals("ab", $("a", null, "b", "c", "d").forEach(new Fn<String, String>() {
            @Override
            public String f(String... x) {
                return $(x).join();
            }
        }, 2).get(0));
        Assert.assertEquals("ab", $(null, "a", null, "b", "c", "d").forEach(new Fn<String, String>() {
            @Override
            public String f(String... x) {
                return $(x).join();
            }
        }, 2).get(0));
        Assert.assertEquals("abcd", $(null, "a", null, "b", "c", "d").forEach(new Fn<String, String>() {
            @Override
            public String f(String... x) {
                return $(x).join();
            }
        }, 4).get(0));
        Assert.assertEquals("abc", $(null, "a", null, "b", "c", "d").forEach(new Fn<String, String>() {
            @Override
            public String f(String... x) {
                return $(x).join();
            }
        }, 3).get(0));

    }

    /** Tests if map() works. */
    @Test
    public void testMap() {
        Assert.assertEquals("h", $("hello").forEach(new F1<String, String>() {
            @Override
            public String f(String x) {
                return x.substring(0, 1);
            }

        }).string().join());

        Assert.assertEquals("hw", $("hello", "world").forEach(new F1<String, String>() {
            @Override
            public String f(String x) {
                return x.substring(0, 1);
            }

        }).string().join());

        Assert.assertEquals("abcdefg", $("aa", "bb", "cc", "dd", "ee", "ff", "gg").forEach(new F1<String, String>() {
            @Override
            public String f(String x) {
                return x.substring(0, 1);
            }

        }).string().join());
        

        final AtomicInteger c = new AtomicInteger();
        c.set(0);
        $(Data.s5).map(new F1<String, Void>() {
            @Override
            public Void f(String x) {
                c.incrementAndGet();
                return null;
            }
        });
        Assert.assertEquals(Data.s5.length, c.intValue());
        
        c.set(0);
        $(Data.s1).map(new F1<String, Void>() {
            @Override
            public Void f(String x) {
                c.incrementAndGet();
                return null;
            }
        });
        
        Assert.assertEquals(Data.s1.length, c.intValue());
        
        c.set(0);
        final ConcurrentHashMap<Integer, Object> cc = new ConcurrentHashMap<Integer, Object>();
        final Indexer indexer = Indexer.NEW();
        
        $(Data.sn).map(new F1<String, Void>() {
            @SuppressWarnings("boxing")
            @Override
            public Void f(String x) {
                c.getAndIncrement();
                if(cc.containsKey(indexer.i())) System.out.println("DOUBLE " + indexer.i());
                cc.put(indexer.i(), new Object());
                return null;
            }
        }, indexer);
        Assert.assertEquals(Data.sn.length, c.intValue());

    }

    /** Tests if delta() works. */
    @SuppressWarnings("boxing")
    @Test
    public void testDelta() {
        double sum = $.range(0, 100000).delta(new F2DeltaObjects<Number, Number>() {
            @Override
            public Integer f(Number left, Number right) {
                return right.intValue() - left.intValue();
            }
        }).as(CoreNumber.class).sum();
        
        Assert.assertEquals(99999.0, sum, 0.01);
    }

    /** */
    @SuppressWarnings("boxing")
    @Test
    public void testFold() {
    	
        
        int value = $("a", "bb", "ccc", "dddd", "eeeee").map(new F1<String, Integer>() {
            @Override
            public Integer f(String x) {
                return x.length();
            }
        }).as(CoreNumber.class).fold(new F2ReduceObjects<Number>() {
            @Override
            public Number f(Number left, Number right) {
                return Math.max(right.intValue(), left.intValue());
            }
        }).as(CoreNumber.class).get(0).intValue();
        Assert.assertEquals(5, value);

        
        final AtomicInteger ai = new AtomicInteger();
        $.range(0, 100001).fold(new F2ReduceObjects<Number>() {
            @Override
            public Number f(Number left, Number right) {
            	ai.incrementAndGet();
                return Math.max(right.intValue(), left.intValue());
            }
        }).as(CoreNumber.class).get(0).doubleValue();
        Assert.assertEquals(100000, ai.get());
        
        
        double sum = $.range(0, 100001).fold(new F2ReduceObjects<Number>() {
            @Override
            public Number f(Number left, Number right) {
                return Math.max(right.intValue(), left.intValue());
            }
        }).as(CoreNumber.class).get(0).doubleValue();
        Assert.assertEquals(100000, sum, 0.01);
        
     }

    /** */
    @SuppressWarnings("boxing")
    @Test
    public void testReduce() {
        double sum = $.range(0, 100000).reduce(new F2ReduceObjects<Number>() {
            @Override
            public Number f(Number left, Number right) {
                return right.intValue() - left.intValue();
            }
        }).as(CoreNumber.class).get(0).doubleValue();
        Assert.assertEquals(50000, sum, 0.01);
    }
   
    
    /** */
    @Test
    public void testIndexer() {
        final Indexer indexer = Indexer.NEW();

        $.range(100000).forEach(new F1<Number, Number>() {
            @SuppressWarnings("boxing")
            @Override
            public Number f(Number x) {
                Assert.assertEquals(x, indexer.i());
                return null;
            }
        }, indexer);

        $.range(100000).map(new F1<Number, Number>() {
            @SuppressWarnings("boxing")
            @Override
            public Number f(Number x) {
                Assert.assertEquals(x, indexer.i());
                return null;
            }
        }, indexer);

    }

    /** */
    @Test
    public void testSlice() {
        Assert.assertEquals("goodbyecruelworld", $("goodbye", "cruel", "world").slice(0, 3).string().join());
        Assert.assertEquals("a", $("a", "b", "c", "d").slice(0.0, 0.25).string().join());
        Assert.assertEquals("ab", $("a", "b", "c", "d").slice(0.0, 0.50).string().join());
        Assert.assertEquals("abcd", $("a", "b", "c", "d").slice(0.0, 1.0).string().join());

    }

    /** */
    @Test
    public void testList() {
        List<String> list = $("a", "b", "c").list();
        list.remove(1);
        list.remove("a");
        list.add(0, "d");
        Assert.assertEquals("c", list.get(1));

    }
    

    /** */
    @Test
    public void testIndex() {
        final CoreString cs = $(Data.sn);
        Assert.assertEquals(2, cs.index("2").i(0));
        Assert.assertEquals(20, cs.index("10", "20").i(1));
    }

    /** */
    @Test
    public void testRandom() {
        Assert.assertEquals(4, $("a", "b", "c", "d", "e", "f", "g").random(4).size());
        //Assert.assertTrue($("a", "b", "c", "d", "e", "f", "g").equals($("a", "b", "c", "d", "e", "f", "g").random(1.0).sort()));
        Assert.assertEquals($("a", "b", "c", "d", "e", "f", "g"), $("a", "b", "c", "d", "e", "f", "g").random(1.0).sort());
        Assert.assertEquals(0, $("a", "b", "c", "d", "e", "f", "g").random(0.0).size());
    }
    
    /** */
    @SuppressWarnings("boxing")
    @Test
    public void testCount() {
        Assert.assertEquals(4, 0 + $("a", "b", "a", "c", "a", "d", "a").count().value("a"));
    }
    
    
    /** */
    @Test
    public void testArray() {
        String[] array = $("a", "b", "a", "c", "a", "d", "a").slice(1, 4).array(String.class);
        Assert.assertEquals(4, array.length);
        Assert.assertEquals("b", array[0]);
        Assert.assertEquals("a", array[1]);
        Assert.assertEquals("c", array[2]);
        Assert.assertEquals("a", array[3]);
        
        Object[] array2 = $("a", "b", "a", "c", "a", "d", "a").slice(1, 4).array(Object.class);
        Assert.assertEquals(4, array2.length);
        Assert.assertEquals("b", array2[0]);
        Assert.assertEquals("a", array2[1]);
        Assert.assertEquals("c", array2[2]);
        Assert.assertEquals("a", array2[3]);
    }

    
    /** Test if the iterator works as expected */
    /*
    @Test
    public void testIterator() {
        final CoreStringJRE c = $(null, "a", "b", null, null, "c", null);
        final StringBuilder sb = new StringBuilder();
        
        for (String string : c) {
            Assert.assertNotNull(string);
            sb.append(string);
        }
        
        Assert.assertEquals("abc", sb.toString());
        
        final ListIterator<String> it = c.iterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(1, it.nextIndex());
        Assert.assertEquals("a", it.next());
        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(2, it.nextIndex());
        Assert.assertTrue(it.hasPrevious());
        Assert.assertEquals(1, it.previousIndex());
        
    }
    */
    
    /** Test if the iterator works as expected */
    @Test
    public void testAllAny() {
        final CoreString c = $(null, "a", "b", null, null, "c", null);
        Assert.assertTrue(c.hasAny());
        Assert.assertFalse(c.hasAll());

        CoreObject<Object> d = $(new Object[] { null });
        Assert.assertFalse(d.hasAny());
        Assert.assertFalse(d.hasAll());
        
        
        CoreString e = $("a", "b");
        Assert.assertTrue(e.hasAny());
        Assert.assertTrue(e.hasAll());

    }

    /** */
    @SuppressWarnings("boxing")
    @Test
    public void testCall() {
        class X {
            int a = 1;

            @SuppressWarnings("unused")
            double b() {
                return this.a * 2;
            }
        }

        Assert.assertEquals(1, $(new X()).call("a").get(0));
        Assert.assertEquals(2.0, $(new X()).call("b()").get(0));
    }

    /** */
    @Test
    public void testSerialize() {
        $("hello", "world").as(CoreObject.class).serialize("test.jcores");
        final CoreString converted = $("test.jcores").file().deserialize(String.class).string();
        $.report();
        Assert.assertEquals("helloworld", converted.join());

        $(Data.strings(10000)).as(CoreObject.class).serialize("big.file");
    }
}

