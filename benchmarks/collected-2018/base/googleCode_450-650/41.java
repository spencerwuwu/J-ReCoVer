// https://searchcode.com/api/result/3863920/

/*
 * SimpleSpeedTests.java
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
package sandbox;

import static net.jcores.jre.CoreKeeper.$;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcores.jre.interfaces.functions.F0;
import net.jcores.jre.interfaces.functions.F1;
import net.jcores.jre.interfaces.functions.F2DeltaObjects;
import net.jcores.jre.interfaces.functions.F2ReduceObjects;
import sandbox.dummys.F0Impl;

/**
 * @author rb
 *
 */
public class SimpleSpeedTest {

    static Random rnd = new Random();

    /**
     * @param args
     */
    @SuppressWarnings("boxing")
    public static void main(String[] args) {
        final int size = 100000;
        final String strings[] = constructStrings(size);
        final List<String> c1 = Arrays.asList(strings);
        final Collection<String> c2 = new ConcurrentLinkedQueue<String>(c1);
        final Collection<String> r = new ConcurrentLinkedQueue<String>();
        final Collection<String> r2 = new ArrayList<String>(size);
        final List<Object> o1 = new ArrayList<Object>();
        final List<Integer> intobjs = new ArrayList<Integer>();

        final int[] ints = constructInts(size);
        final long[] longs = constructLongs(size);

        final int[] res = new int[size];
        final long[] resl = new long[size];

        for (int i = 0; i < size; i++) {
            intobjs.add(i);
        }

        System.out.println();
        System.out.println("--- Array and Collections ---");
        System.out.println(benchmark(new F0() { // 9195
            public void f() {
                c1.toArray(new String[0]);
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 9460
            public void f() {
                c1.toArray();
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 62650
            public void f() {
                c2.toArray();
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 181898
            public void f() {
                c2.toArray(new String[0]);
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 84118
            public void f() {
                for (int i = 0; i < size; i++) {
                    strings[i] = c1.get(i).toLowerCase();
                }
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 80942
            public void f() {
                for (int i = 0; i < size; i++) {
                    strings[i] = strings[i].toLowerCase();
                }
            }

        }, 10) + "ls");
        r.clear();
        System.out.println(benchmark(new F0() { // 89496
            public void f() {
                for (String s : c1) {
                    r2.add(s.toLowerCase());
                }
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 87272
            public void f() {
                int i = 0;
                for (String s : c2) {
                    strings[i++] = s.toLowerCase();
                }
            }
        }, 10) + "ls");
        r.clear();
        System.out.println(benchmark(new F0() { // 413666
            public void f() {
                for (String s : c2) {
                    r.add(s.toLowerCase());
                }
            }
        }, 10) + "ls");
        r.clear();
        System.out.println(benchmark(new F0() { // 413666
            public void f() {
                for (String s : c2) {
                    r.add(s.toLowerCase());
                }
            }
        }, 10) + "ls");

        System.out.println();
        System.out.println("--- Object Creation ---");
        o1.clear();
        System.out.println(benchmark(new F0() { // 159663 (wo caching),
            // 356117 (w caching)
            // 331769
            // 14166 (optimized)
            public void f() {
                /*CoreClass<F0Impl> $2 = $(F0Impl.class);
                for (int i = 0; i < size; i++)
                    o1.add($2.spawn());*/
            }
        }, 10) + "ls");
        o1.clear();
        System.out.println(benchmark(new F0() { // 2277 
            public void f() {
                for (int i = 0; i < size; i++)
                    o1.add(new F0Impl());
            }
        }, 10) + "ls");

        final AtomicInteger ress = new AtomicInteger();
        System.out.println();
        System.out.println("--- $() usage ---");
        System.out.println(benchmark(new F0() { // 15117
            public void f() {
                int cnt = 0;
                for (int i = 0; i < size; i++)
                    cnt += $(getOrNotObject()).compact().size();
                ress.addAndGet(cnt);
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 3034
            public void f() {
                int cnt = 0;
                for (int i = 0; i < size; i++) {
                    if (getOrNotObject() != null) cnt++;
                }
                ress.addAndGet(cnt);
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 227518
            public void f() {
                int cnt = 0;
                for (int i = 0; i < size; i++)
                    cnt += $(getOrNotList()).size();
                ress.addAndGet(cnt);
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 133315
            public void f() {
                int cnt = 0;
                for (int i = 0; i < size; i++) {
                    List<Object> orNotList = getOrNotList();
                    cnt += orNotList.size();
                }
                ress.addAndGet(cnt);
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 3857
            public void f() {
                Object o = new Object();
                int cnt = 0;
                for (int i = 0; i < size; i++)
                    cnt += $(o).size();
                ress.addAndGet(cnt);
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 30
            public void f() {
                Object o = new Object();
                int cnt = 0;
                for (int i = 0; i < size; i++) {
                    if (o != null) cnt += 1;
                }
                ress.addAndGet(cnt);
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 10409 // 839
            public void f() {
                int cnt = 0;
                Object o = new Object();
                for (int i = 0; i < size; i++) {
                    cnt += new Object().equals(o) ? 1 : 0;
                }
                ress.addAndGet(cnt);
            }

        }, 10) + "ls");
        System.out.println(benchmark(new F0() { // 12071 // 2782
            public void f() {
                int cnt = 0;
                Object o = new Object();
                for (int i = 0; i < size; i++) {
                    cnt += $(o).equals(o) ? 1 : 0;
                }
                ress.addAndGet(cnt);
            }
        }, 10) + "ls");

        System.out.println();
        System.out.println("--- Iteration over Created ---");
        System.out.println(benchmark(new F0() { // 278347 
            public void f() {
                //$(F0Impl.class).spawned().each(F0.class).f();
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() {
            public void f() { // 7450
                for (Object f0 : o1) {
                    ((F0Impl) f0).f();
                }
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() {
            public void f() { // 118806
                $(o1).each(F0.class).f();
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() {
            public void f() { // 26346
                $(o1).map(new F1() {
                    public Object f(Object x) {
                        ((F0) x).f();
                        return null;
                    }
                });
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() {
            public void f() { // 11012
                $(o1);
            }
        }, 10) + "ls");

        /*
         * Removed, the ___map was the first version of map without the mapper, was ~10-20% faster but only
         * when F1 was very lightweight. For heavyweight cases of F1 no significant difference was noticable.
        long b11 = benchmark(new F0() {
            public void f() {
                $(strings).____map(new F1<String, String>() {
                    public String f(String x) {
                        return x.toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase();
                        //return x.toLowerCase();
                    }
                });
            }
        }, 10) + "ls";
         */
        long b1 = benchmark(new F0() {
            public void f() {
                $(strings).map(new F1<String, String>() {
                    public String f(String x) {
                        return x.toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase();
                        //return x.toLowerCase();
                    }
                });
            }
        }, 10);

        long b2 = benchmark(new F0() {
            public void f() {
                for (int i = 0; i < strings.length; i++) {
                    strings[i] = strings[i].toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase().toLowerCase();
                    //strings[i] = strings[i].toLowerCase();
                }
            }
        }, 10);

        /*
        long b3 = benchmark(new F0() {
            public void f() {
                $(ints).map(new F1Int2Int() {
                    public int f(int x) {
                        return (int) Math.sqrt(Math.tan(x * x));
                    }
                });
            }
        }, 10);*/

        long b4 = benchmark(new F0() {
            public void f() {
                for (int i = 0; i < ints.length; i++) {
                    int x = ints[i];
                    res[i] = (int) Math.sqrt(Math.tan(x * x));
                }
            }
        }, 10);

        long b6 = benchmark(new F0() {
            public void f() {
                for (int i = 0; i < longs.length; i++) {
                    resl[i] = longs[i] * longs[i];
                }
            }
        }, 10);

        System.out.println();
        System.out.println("--- Parallelization Results ---");
        System.out.println(b1);
        System.out.println(b2);
        //System.out.println(b3);
        System.out.println(b4);
        System.out.println(b6);

        System.out.println();
        System.out.println("--- Fancy Stuff ---");
        System.out.println(benchmark(new F0() {
            public void f() {
                $(intobjs).fold(new F2ReduceObjects<Integer>() {
                    @Override
                    public Integer f(Integer stack, Integer next) {
                        return stack + next;
                    }
                }).get(0);
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() {
            public void f() {
                $(intobjs).reduce(new F2ReduceObjects<Integer>() {
                    @Override
                    public Integer f(Integer stack, Integer next) {
                        return stack + next;
                    }
                }).get(0);
            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() {
            public void f() {

                /*$(intobjs).staple(0, new F2ReduceObjects<Integer>() {
                    public Integer f(Integer left, Integer right) {
                        return left + right;
                    }
                });*/

            }
        }, 10) + "ls");
        System.out.println(benchmark(new F0() {
            public void f() {
                $(intobjs).delta(new F2DeltaObjects<Integer, Integer>() {
                    public Integer f(Integer left, Integer right) {
                        return right - left;
                    }
                }).size();
            }
        }, 10) + "ls");
    }

    private static String[] constructStrings(int size) {
        final List<String> l = new ArrayList<String>();
        final Random r = new Random();

        for (int i = 0; i < size; i++) {
            l.add("" + r.nextInt());
        }

        return l.toArray(new String[0]);
    }

    private static int[] constructInts(int size) {
        int[] l = new int[size];
        final Random r = new Random();

        for (int i = 0; i < size; i++) {
            l[i] = r.nextInt();
        }

        return l;
    }

    private static long[] constructLongs(int size) {
        long[] l = new long[size];
        final Random r = new Random();

        for (int i = 0; i < size; i++) {
            l[i] = r.nextLong();
        }

        return l;
    }

    /**
     * Executes f and returns the time taken.
     * 
     * @param f
     * @return .
     */
    public static long benchmark(F0 f) {
        long start = System.nanoTime();
        f.f();
        long stop = System.nanoTime();
        return (stop - start) / 1000;
    }

    /**
     * @param f
     * @param passes
     * @return .
     */
    public static long benchmark(F0 f, int passes) {
        long results[] = new long[passes];

        for (int i = 0; i < results.length; i++) {
            results[i] = benchmark(f);
            //System.out.println(results[i]);
        }

        int delta = passes - 1;
        long avg = 0;

        for (int i = delta; i < results.length; i++) {
            avg += results[i];
        }

        avg /= passes - delta;

        return avg;
    }

    public static Object getOrNotObject() {
        if (rnd.nextBoolean()) return new Object();
        return null;
    }

    public static List<Object> getOrNotList() {
        int nextInt = rnd.nextInt(100);
        List<Object> rval = new ArrayList<Object>();
        for (int i = 0; i < nextInt; i++) {
            rval.add(new Object());
        }
        return rval;
    }

}

