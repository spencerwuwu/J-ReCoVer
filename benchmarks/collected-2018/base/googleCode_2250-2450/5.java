// https://searchcode.com/api/result/3863799/

/*
 * CoreObject.java
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
package net.jcores.jre.cores;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jcores.jre.CommonCore;
import net.jcores.jre.annotations.SupportsOption;
import net.jcores.jre.cores.adapter.AbstractAdapter;
import net.jcores.jre.cores.adapter.ArrayAdapter;
import net.jcores.jre.cores.adapter.CollectionAdapter;
import net.jcores.jre.cores.adapter.EmptyAdapter;
import net.jcores.jre.cores.adapter.ListAdapter;
import net.jcores.jre.interfaces.functions.F0;
import net.jcores.jre.interfaces.functions.F1;
import net.jcores.jre.interfaces.functions.F1Object2Bool;
import net.jcores.jre.interfaces.functions.F2DeltaObjects;
import net.jcores.jre.interfaces.functions.F2ReduceObjects;
import net.jcores.jre.interfaces.functions.Fn;
import net.jcores.jre.managers.ManagerDebugGUI;
import net.jcores.jre.managers.ManagerDeveloperFeedback;
import net.jcores.jre.options.Args;
import net.jcores.jre.options.InvertSelection;
import net.jcores.jre.options.KillSwitch;
import net.jcores.jre.options.MapType;
import net.jcores.jre.options.MessageType;
import net.jcores.jre.options.Option;
import net.jcores.jre.utils.Async;
import net.jcores.jre.utils.Async.Queue;
import net.jcores.jre.utils.internal.Objects;
import net.jcores.jre.utils.internal.Options;
import net.jcores.jre.utils.internal.Streams;
import net.jcores.jre.utils.internal.processing.Folder;
import net.jcores.jre.utils.internal.processing.Mapper;
import net.jcores.jre.utils.internal.wrapper.Wrapper;
import net.jcores.jre.utils.map.Compound;

/**
 * <i>The</i> base class for all other cores that provides basic functions 
 * like <code>map()</code>, <code>slice()</code>, and many more; give it a look. For example,
 * to get the last three elements of an array of Strings, write:<br/>
 * <br/>
 * 
 * <code>$(strings).slice(-3, 3).array(String.class)</code><br/>
 * <br/>
 * 
 * If you implement your own core you should extend this class.<br/>
 * <br/>
 * 
 * A core is immutable. No method will ever change its content array (it is, however,
 * possible, that the individual elements enclosed might change).
 * 
 * @author Ralf Biedert
 * @since 1.0
 * 
 * @param <T> Type of the objects to wrap.
 */
public class CoreObject<T> extends Core implements Iterable<T> {

    /** */
    private static final long serialVersionUID = -6436821141631907999L;

    /** The adapter we work on */
    protected final AbstractAdapter<T> adapter;

    /**
     * Creates the core object for the given single object.
     * 
     * @param supercore CommonCore to use.
     * @param type Type of the object to wrap (in case it is null).
     * @param object Object to wrap.
     */
    @SuppressWarnings("unchecked")
    public CoreObject(CommonCore supercore, Class<?> type, T object) {
        super(supercore);

        // Check if we have an object. If not, and if there is no type, use an
        // empty Object array
        if (object != null) {
            this.adapter = new ArrayAdapter<T>(object);
        } else {
            this.adapter = new EmptyAdapter<T>();
        }
    }

    /**
     * Creates the core object for the given array.
     * 
     * @param supercore CommonCore to use.
     * @param objects Object to wrap.
     */
    public CoreObject(CommonCore supercore, T... objects) {
        super(supercore);
        this.adapter = new ArrayAdapter<T>(objects);
    }

    /**
     * Creates the core object for the given array.
     * 
     * @param supercore CommonCore to use.
     * @param objects Object to wrap.
     */
    public CoreObject(CommonCore supercore, List<T> objects) {
        super(supercore);

        if (objects instanceof ArrayList) {
            this.adapter = new ListAdapter<T>(objects);
        } else {
            this.adapter = new CollectionAdapter<T, T>(objects);
        }
    }

    /**
     * Creates the core object for the given array.
     * 
     * @param supercore CommonCore to use.
     * @param objects Object to wrap.
     */
    public CoreObject(CommonCore supercore, Collection<T> objects) {
        super(supercore);

        if (objects instanceof ArrayList) {
            this.adapter = new ListAdapter<T>((List<T>) objects);
        } else {
            this.adapter = new CollectionAdapter<T, T>(objects);
        }
    }

    /**
     * Creates the core object for the given array.
     * 
     * @param supercore CommonCore to use.
     * @param core Core to wrap.
     */
    public CoreObject(CommonCore supercore, CoreObject<T> core) {
        super(supercore);
        this.adapter = core.adapter;
    }

    /**
     * Creates the core object for the given adapter. This is the main constructor each
     * subclass <b>must</b> implement, otherwise <code>object.as()</code> will not work.
     * 
     * @param supercore CommonCore to use.
     * @param adapter The adapter to wrap.
     */
    public CoreObject(CommonCore supercore, AbstractAdapter<T> adapter) {
        super(supercore);
        this.adapter = adapter;
    }

    /**
     * Returns a core containing all elements of this core and the other core.
     * Elements that are in both cores will appear twice.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b").add($("c"))</code> - The resulting core contains <code>a</code>, <code>b</code> and
     * <code>c</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param toAdd The core to add to this core.
     * 
     * @return A CoreObject containing all objects of this core and the other
     * core.
     */
    @SuppressWarnings("unchecked")
    public CoreObject<T> add(CoreObject<T> toAdd) {
        if (size() == 0) return toAdd;
        if (toAdd.size() == 0) return this;

        Class<?> clazz = this.adapter.clazz();

        final T[] copy = (T[]) Array.newInstance(clazz, size() + toAdd.size());
        final Object[] a = this.adapter.array(clazz);
        final Object[] b = toAdd.adapter.array(clazz);

        System.arraycopy(a, 0, copy, 0, a.length);
        System.arraycopy(b, 0, copy, a.length, b.length);

        return new CoreObject<T>(this.commonCore, copy);
    }

    /**
     * Returns a core containing all elements of this core and the other array.
     * Elements that are in both will appear twice.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b").add("c")</code> - The resulting core contains <code>a</code>, <code>b</code> and
     * <code>c</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param toAdd The array to add to this core.
     * 
     * @return A CoreObject containing all objects of this core and the other
     * array.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CoreObject<T> add(T... toAdd) {
        return this.add(new CoreObject(this.commonCore, toAdd));
    }

    /**
     * Returns the core's content as an array of the given type. Elements that don't fit
     * into the given target type will be skipped.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(list).array(String.class)</code> - Returns a String array for the given list. Elements that are no
     * String will be returned as null.</li>
     * </ul>
     * 
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param in Type of the target array to use.
     * @param <N> Type of the array.
     * 
     * @return An array containing the all assignable elements.
     */
    public <N> N[] array(Class<N> in) {
        return this.adapter.array(in);
    }

    /**
     * Returns a core that tries to treat all elements as being of the given type.
     * Elements which don't match are ignored. Can also be used to load extensions
     * (e.g., <code>somecore.as(CoreString.class)</code>). This function should not
     * be called within hot-spots (functions called millions of times a second)
     * as it relies heavily on reflection.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(objects).as(MyExtensionCore.class).method()</code> - If you wrote a jCores extension this is how you
     * could activate the extension for a given set of objects and execute one of its methods.</li>
     * <li><code>$("a", null, "c").compact().as(CoreString.class)</code> - Sometimes you call methods from a parent Core
     * (like <code>compact()</code>, which is part of CoreObject, not of {@link CoreString}) that does returns a more
     * general return type then what you want. Using <code>as()</code> you can cast the Core back.</li>
     * </ul>
     * 
     * 
     * Single-threaded. Heavyweight.<br/>
     * <br/>
     * 
     * @param <C> Type of the clazz.
     * @param clazz Core to be spawned and returned.
     * @return If successful, spawns a core of type <code>clazz</code> and returns it,
     * wrapping all contained elements.
     */
    @SuppressWarnings({ "unchecked", "null" })
    public <C extends Core> C as(Class<C> clazz) {
        try {
            final Constructor<?>[] constructors = clazz.getConstructors();
            Constructor<C> constructor = null;

            // Find a proper constructor!
            for (Constructor<?> c : constructors) {
                if (c.getParameterTypes().length != 2) continue;
                if (!c.getParameterTypes()[0].equals(CommonCore.class)) continue;
                if (!c.getParameterTypes()[1].equals(AbstractAdapter.class)) continue;

                // Sanity check.
                if (constructor != null)
                    System.err.println("There should only be one constructor with (CommonCore.class, AbstractAdapter.class) per core! And here comes your exception ... ;-)");

                constructor = (Constructor<C>) c;
            }

            return constructor.newInstance(this.commonCore, this.adapter);

            // NOTE: We do not swallow all execptions, because as() is a bit special and
            // we cannot return anything that would still be usable.
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            this.commonCore.report(MessageType.EXCEPTION, "No constructor found for " + clazz);
            System.err.println("No suitable constructor found!");
        }

        return null;
    }

    /**
     * Performs an asynchronous map operation on this core. The order in which
     * the objects are being mapped is not defined.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$(names).async(lookup)</code> - Performs an asynchronous lookup for the set of names.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param f The function to execute asynchronously on the enclosed objects.
     * @param options The supported options, esp. {@link KillSwitch}.
     * @param <R> Return type for the {@link Async} object.
     * @return An {@link Async} object that will hold the results (in an arbitrary order).
     */
    @SupportsOption(options = { KillSwitch.class })
    public <R> Async<R> async(final F1<T, R> f, Option... options) {
        final Queue<R> queue = Async.Queue();
        final Async<R> async = new Async<R>(this.commonCore, queue);
        final Options options$ = Options.$(this.commonCore, options);
        final KillSwitch killswitch = options$.killswitch();

        this.commonCore.sys.oneTime(new F0() {
            @Override
            public void f() {
                try {
                    // Now process all elements
                    for (T t : CoreObject.this) {
                        // Check if we should terminate.
                        if(killswitch != null && killswitch.terminated())  return;
                        
                        // Otherwise process next element.
                        try {
                            queue.add(Async.QEntry(f.f(t)));
                        } catch (Exception e) {
                            CoreObject.this.commonCore.report(MessageType.EXCEPTION, "Exception when processing " + t + " ... " + e.getMessage());
                            options$.failure(t, e, "for/f", "Unknown exception when processing element");
                        }
                    }
                } finally {
                    // When we're done, close the queue.
                    queue.close();
                }
            }
        }, 0, options);

        return async;

    }

    /**
     * Casts all elements to the given type or sets them null if they are not castable.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$(object).cast(String.class).get(0)</code> - Same as <code>(object instanceof 
     * String) ? (String) object : null</code>.</li>
     * </ul>
     * 
     * Multi-threaded. <br/>
     * <br/>
     * 
     * @param <N> Target type.
     * @param target Class to cast all elements
     * @return A CoreObject wrapping all cast elements.
     */
    public <N> CoreObject<N> cast(final Class<N> target) {
        return map(new F1<T, N>() {
            @SuppressWarnings("unchecked")
            @Override
            public N f(T x) {
                if (target.isAssignableFrom(x.getClass())) return (N) x;
                return null;
            }
        }, MapType.TYPE(target));
    }

    /**
     * Performs a generic call on each element of this core (for
     * example <code>core.call("toString()")</code>), or returns a field (for
     * example <code>core.call("field")</code>). The return values will
     * be stored in a {@link CoreObject}. This is a dirty but shorthand way
     * to call the same function on objects that don't share a common superclass. Should not be
     * called within hot-spots (functions called millions of times a second) as it relies heavily on reflection.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$(tA, tB, tC, tD).call("method()").unique().size()</code> - Calls a method <code>method()</code> on
     * some objects that have no common supertype (except {@link Object}), and returns the number of distinct objects
     * returned</li>
     * <li><code>$(tA, tB, tC, tD).call("field")</code> - Gets the value of the field <code>field</code> on each of the
     * elements, and returns a core with the results.</li>
     * </ul>
     * 
     * Multi-threaded. Heavyweight.<br/>
     * <br/>
     * 
     * @param string The call to perform, e.g. <code>toString</code>
     * @param params Parameters the call takes
     * 
     * @return A CoreObject wrapping the results of each invocation.
     */
    @SuppressWarnings("null")
    public CoreObject<Object> call(final String string, final Object... params) {
        final int len = params == null ? 0 : params.length;
        final Class<?>[] types = new Class[len];
        final CommonCore cc = this.commonCore;

        // Convert classes.
        for (int i = 0; i < len; i++) {
            types[i] = params[i].getClass();
        }

        final boolean methodcall = string.endsWith("()");
        final String call = methodcall ? string.substring(0, string.length() - 2) : string;

        // If this is a method call ...
        if (methodcall) { return map(new F1<T, Object>() {
            public Object f(T x) {
                try {
                    final Method method = x.getClass().getDeclaredMethod(call, types);
                    method.setAccessible(true);
                    return method.invoke(x, params);
                } catch (SecurityException e) {
                    cc.report(MessageType.EXCEPTION, "SecurityException for " + x + " (method was " + string + ")");
                } catch (NoSuchMethodException e) {
                    cc.report(MessageType.EXCEPTION, "NoSuchMethodException for " + x + " (method was " + string + ")");
                } catch (IllegalArgumentException e) {
                    cc.report(MessageType.EXCEPTION, "IllegalArgumentException for " + x + " (method was " + string + ")");
                } catch (IllegalAccessException e) {
                    cc.report(MessageType.EXCEPTION, "IllegalAccessException for " + x + " (method was " + string + ")");
                } catch (InvocationTargetException e) {
                    cc.report(MessageType.EXCEPTION, "InvocationTargetException for " + x + " (method was " + string + ")");
                }

                return null;
            }
        }); }

        // Or a field access
        return map(new F1<T, Object>() {
            public Object f(T x) {
                try {
                    final Field field = x.getClass().getDeclaredField(call);
                    field.setAccessible(true);
                    return field.get(x);
                } catch (SecurityException e) {
                    cc.report(MessageType.EXCEPTION, "SecurityException for " + x + " (method was " + string + ")");
                } catch (IllegalArgumentException e) {
                    cc.report(MessageType.EXCEPTION, "IllegalArgumentException for " + x + " (method was " + string + ")");
                } catch (IllegalAccessException e) {
                    cc.report(MessageType.EXCEPTION, "IllegalAccessException for " + x + " (method was " + string + ")");
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    /**
     * Serializes this core into the given file. Objects that are not serializable
     * are ignored. The file can later be restored with the function <code>deserialize()</code> in {@link CoreFile}.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("Hello", "World").serialize("data.ser")</code> - Writes the core to a file.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param path The location to which this core should be serialized.
     * @param options Currently not used.
     * @return This core.
     */
    @SupportsOption(options = {})
    public CoreObject<T> serialize(final String path, Option... options) {
        try {
            Streams.serializeCore(this, new FileOutputStream(new File(path)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * For each of the contained elements an object of the type <code>wrapper</code> is
     * being created with the element passed as the first argument to the constructor. If the element is
     * already of the type <code>wrapper</code>, nothing is being done for that element.<br/>
     * <br/>
     * 
     * Single-threaded. Heavyweight.<br/>
     * <br/>
     * 
     * @param wrapper The class to spawn for each element.
     * @param <W> The type of the wrapper.
     * 
     * @return A {@link CoreObject} with the wrapped objects. Elements which could not be wrapped are set to
     * <code>null</code>.
     */
    public <W> CoreObject<W> wrap(final Class<W> wrapper) {
        @SuppressWarnings("unchecked")
        final CoreClass<W> w = new CoreClass<W>(this.commonCore, wrapper);

        return forEach(new F1<T, W>() {
            @SuppressWarnings("unchecked")
            @Override
            public W f(T x) {
                // Check if the object is already assignable
                if (wrapper.isAssignableFrom(x.getClass())) return (W) x;

                return w.spawn(Args.WRAP(x)).get(0);
            }
        });
    }

    /**
     * Return a CoreClass for all enclosed objects' classes.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(o1, o2).clazz()</code> - Wraps the classes for <code>o1</code> and <code>o2</code> in a
     * {@link CoreClass}.</li>
     * </ul>
     * 
     * Multi-threaded. <br/>
     * <br/>
     * 
     * @return A new {@link CoreClass} containing the classes for all objects.
     */
    @SuppressWarnings("unchecked")
    public CoreClass<T> clazz() {
        return new CoreClass<T>(this.commonCore, map(new F1<T, Class<T>>() {
            @Override
            public Class<T> f(T x) {
                return (Class<T>) x.getClass();
            }
        }).array(Class.class));
    }

    /**
     * Returns a compacted core whose underlying array does not
     * contain null anymore, therefore the positions of elements will be moved to the left to
     * fill null values.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", null, "b").compact()</code> - Returns a core that only contains the elements <code>a</code> and
     * <code>b</code> and has a size of <code>2</code> (the original core also contains <code>null</code> and has a size
     * of <code>3</code>).</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @return A new CoreObject of the same type, with a (probably) reduced size without
     * any null element.
     */
    public CoreObject<T> compact() {
        // No size == no fun.
        if (size() == 0) return this;

        final T[] tmp = this.adapter.array();
        int dst = 0;

        for (T element : this) {
            if (element == null) continue;
            tmp[dst++] = element;
        }

        return new CoreObject<T>(this.commonCore, new ArrayAdapter<T>(dst, tmp));
    }

    /**
     * Creates a {@link Compound} out of this core's content. A Compound is a String -> Object
     * map, which is useful for quickly creating complex objects which should be handled by the
     * framework.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("name", name, "age", age).compound()</code> - Quickly creates an untyped {@link Compound} with the
     * keys <code>name</code> and <code>age</code> and the corresponding values.</li>
     * </ul>
     * 
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @return A new {@link Compound} with this core's content.
     */
    public Compound compound() {
        return Compound.create(this.adapter.array());
    }

    /**
     * Returns true if this core contains the given object. An object is contained if there is
     * another object in this core that is equal to it. <br/>
     * <br/>
     * 
     * Note that on a {@link CoreString} this method
     * does <b>not behave as</b> <code>String.contains()</code> (which checks for substrings).
     * If you want to do a substring search, use <code>CoreString.containssubstr()</code>.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(a, b, c, d).contains(c)</code> - Returns <code>true</code>.</li>
     * <li><code>$("aa", "bb", "cc").contains("bb")</code> - Returns <code>true</code>.</li>
     * <li><code>$("aa", "bb", "cc").contains("b")</code> - Returns <b><code>false</code></b>!</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param object The object to search for.
     * @return True if the object is there, false if not.
     */
    public boolean contains(final T object) {
        for (T next : this) {
            if (next != null && next.equals(object)) return true;
            if (next == null && object == null) return true;
        }

        return false;
    }

    /**
     * Counts how many times each unique item is contained in this core (i.e., computes a
     * histogram). <br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "a", "b").contains().value("a")</code> - Returns 2.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @return A CoreMap with the counts for each unique object.
     */
    @SuppressWarnings("boxing")
    public CoreMap<T, Integer> count() {
        final Map<T, Integer> results = new HashMap<T, Integer>();

        // Now generate the histogram.
        for (T e : this) {
            if (e == null) continue;

            if (results.containsKey(e)) {
                results.put(e, results.get(e) + 1);
            } else {
                results.put(e, 1);
            }
        }

        // Eventually return the results
        return new CoreMap<T, Integer>(this.commonCore, Wrapper.convert(results));
    }

    /**
     * Prints debug output to the console. Useful for figuring out what's going wrong in a
     * chain of map() operations.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>...somecore.debug().map(f).debug()... </code> - Typical pattern to figure out why a function (
     * <code>map()</code> in this case) might go wrong.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @return This object again.
     */
    @SuppressWarnings("unused")
    public CoreObject<T> debug() {

        // Print the result
        System.out.println(fingerprint(false));

        // And add it to the debug GUI
        ManagerDebugGUI debugGUI = this.commonCore.manager(ManagerDebugGUI.class);

        return this;
    }

    /**
     * Returns a core of length size() - 1 consisting of the results of the delta
     * function. Delta always takes two adjacent elements and execute stores the
     * delta function's output. In contrast to the common map operation this function
     * does not ignore <code>null</code> elements. If of two adjacent slots any
     * is <code>null</code>, the value <code>null</code> will be stored. <br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c").delta(joiner)</code> - If the given delta function joins two elements then the
     * resulting core contains <code>"ab"</code> and <code>"bc"</code>.</li>
     * </ul>
     * 
     * Multi-threaded. <br/>
     * <br/>
     * 
     * @param delta The delta function, taking two elements and return a result.
     * @param <R> Type of the result.
     * @param options Relevant options, especiall {@link MapType}.
     * 
     * @return A core of size n - 1 containing all deltas.
     */
    @SuppressWarnings("unchecked")
    @SupportsOption(options = { MapType.class })
    public <R> CoreObject<R> delta(final F2DeltaObjects<T, R> delta, Option... options) {
        // Create mapper
        final int size = size();
        final Mapper<T, R> mapper = new Mapper<T, R>(this, options) {
            @Override
            public void handle(int i) {
                // We don't handle the last iteration
                if (i == size - 1) return;

                // Get our target-array (if it is already there)
                R[] a = this.returnArray.get();

                // Get the in-value from the source-array
                final T ii = CoreObject.this.adapter.get(i);
                final T jj = CoreObject.this.adapter.get(i + 1);

                // Convert
                if (ii == null || jj == null) return;

                final R out = delta.f(ii, jj);

                if (out == null) return;

                // If we haven't had an in-array, create it now, according to the return type
                if (a == null) {
                    a = updateReturnArray((R[]) Array.newInstance(out.getClass(), size));
                }

                // Eventually set the out value
                a[i] = out;
            }
        };

        // Map ...
        map(mapper, options);

        // ... and return result.
        return new CoreObject<R>(this.commonCore, new ArrayAdapter<R>(size - 1, mapper.getFinalReturnArray()));

    }

    /**
     * Returns a single object that, if any of its functions is executed, the
     * corresponding function is executed on all enclosed elements. Only works
     * if <code>c</code> is an interface and only on enclosed elements implementing <code>c</code>. From a performance
     * perspective this method only makes sense
     * if the requested operation is complex, as on simple methods the reflection
     * costs will outweigh all benefits. Also note that all return values are skipped. <br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(x1, x2, x3, x4, x5).each(XInterface.class).x()</code> - Given all objects implement
     * <code>XInterface</code> the function <code>each()</code> returns a new <code>X</code> object that, when
     * <code>x()</code> is executed on it, the function is executed on all enclosed objects in parallel.</li>
     * </ul>
     * 
     * Multi-threaded. Heavyweight.<br/>
     * <br/>
     * 
     * @param c The interface to use.
     * @param <X> The interface's type.
     * 
     * @return Something implementing c that acts on each element implementing c.
     */
    @SuppressWarnings("unchecked")
    public <X> X each(final Class<X> c) {
        if (c == null || !c.isInterface()) {
            System.err.println("You must pass an interface.");
            return null;
        }

        // Get only assignable classes of our collection
        final CoreObject<X> filtered = cast(c);

        // Provide an invocation handler
        return (X) Objects.getProxy(new InvocationHandler() {
            public Object invoke(Object proxy, final Method method, final Object[] args)
                                                                                        throws Throwable {
                filtered.map(new F1<X, Object>() {
                    public Object f(X x) {

                        try {
                            method.invoke(x, args);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                });

                return null;
            }
        }, c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    @SuppressWarnings("null")
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CoreObject)) return false;

        final CoreObject<?> other = (CoreObject<?>) obj;

        if (size() != other.size()) return false;

        ListIterator<T> i1 = this.adapter.iterator();
        ListIterator<?> i2 = other.adapter.iterator();

        while (i1.hasNext()) {
            T a = i1.next();
            Object b = i2.next();

            if (a == null && b != null) return false;
            if (a == null && b == null) continue;

            boolean equals = a.equals(b);
            if (!equals) return false;
        }

        return true;
    }

    /**
     * Expands contained arrays into a single array of the given type. This means,
     * if this core wraps a number of cores, collections, lists or arrays, each of
     * which are containing elements on their own, <code>expand()</code> will break
     * up all of these lists and return a single CoreObject wrapping the union of
     * everything that was previously held in them.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", $("b", "c"), new String[]{"d", "e"}).expand(String.class)</code> - Returns a core of size
     * <code>5</code>, directly containing the elements <code>a</code>, <code>b</code>, <code>c</code>, <code>d</code>
     * and <code>e</code>.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param <N> Type of the return core.
     * @param class1 Defines the class element of the returned core's array.
     * @return A CoreObject wrapping all broken up collections, arrays, ...
     */
    @SuppressWarnings("unchecked")
    public <N> CoreObject<N> expand(Class<N> class1) {
        int length = 0;

        if (size() == 0) return new CoreObject<N>(this.commonCore, class1, null);

        // Compute overall size
        for (T x : this) {
            if (x == null) continue;

            // Is it a collection?
            if (x instanceof Collection<?>) {
                length += ((Collection<?>) x).size();
                continue;
            }

            // Is it a core?
            if (x instanceof CoreObject<?>) {
                length += ((CoreObject<?>) x).size();
                continue;
            }

            // An array?
            try {
                length += Array.getLength(x);
                continue;
            } catch (IllegalArgumentException e) {
                //
            }

            // A single object?!
            length++;
        }

        // Generate array
        N[] n = (N[]) Array.newInstance(class1, length);
        int offset = 0;

        // Copy to array
        for (T x : this) {
            if (x == null) continue;

            // Is it a collection?
            if (x instanceof Collection<?>) {
                Object[] array = ((Collection<?>) x).toArray();
                System.arraycopy(array, 0, n, offset, array.length);
                offset += array.length;
                continue;
            }

            // Is it a core?
            if (x instanceof CoreObject<?>) {
                Object[] array = ((CoreObject<?>) x).array(Object.class);
                System.arraycopy(array, 0, n, offset, array.length);
                offset += array.length;
                continue;
            }

            // An array?
            try {
                int size = Array.getLength(x);
                System.arraycopy(x, 0, n, offset, size);
                offset += size;
                continue;
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            } catch (ArrayStoreException e) {
                //
            } catch (IllegalArgumentException e) {
                //
            }

            // A single element?
            Array.set(n, offset++, x);
        }

        return new CoreObject<N>(this.commonCore, n);
    }

    /**
     * Sends a request to the developers requesting a feature with the given name. The
     * request will be sent to a server and collected. Information of the enclosed objects and the
     * feature request string will be transmitted as well.
     * 
     * Examples:
     * <ul>
     * <li><code>$("abba").featurerequest(".palindrome() -- Should be supported!")</code></li>
     * </ul>
     * 
     * @param functionName Call this function for example like this
     * $(myobjects).featurerequest(".compress() -- Should compress the given objects.");
     */
    public void featurerequest(String functionName) {
        this.commonCore.manager(ManagerDeveloperFeedback.class).featurerequest(functionName, getClass(), fingerprint(true));
    }

    /**
     * Returns a new core with all null elements set to <code>fillValue</code>, the other
     * elements are transferred unchanged.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", null, "b").fill("x")</code> - Returns a core where the <code>null</code> element is set to
     * <code>"x"</code></li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param fillValue Value used to fill up all <code>null</code> slots.
     * 
     * @return A filled up CoreObject.
     */
    public CoreObject<T> fill(T fillValue) {
        if (size() == 0) return this;

        final T[] copy = this.adapter.array();

        for (int i = 0; i < copy.length; i++) {
            copy[i] = copy[i] == null ? fillValue : copy[i];
        }

        return new CoreObject<T>(this.commonCore, copy);
    }

    /**
     * Filters the object using the given function. A compacted array will be returned
     * that contains only values for which f returned true.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "bb", "ccc").filter(f)</code> - Given the filter function returns true for all elements with a
     * length <code>&gt;=2</code> the resulting core contains <code>"bb"</code> and <code>"ccc"</code>.</li>
     * </ul>
     * 
     * Multi-threaded.<br/>
     * <br/>
     * 
     * @param f If f returns true the object is kept.
     * @param options Supports {@link InvertSelection} if the filter logic should be inverted.
     * 
     * @return A new CoreObject of our type, containing only kept elements.
     */
    @SupportsOption(options = { InvertSelection.class })
    public CoreObject<T> filter(final F1Object2Bool<T> f, Option... options) {
        final boolean invert = Options.$(this.commonCore, options).invert();
        final CoreObject<T> rval = map(new F1<T, T>() {
            public T f(T x) {
                final boolean result = f.f(x);

                if ((!invert && result) || (invert == !result)) return x;

                return null;
            }
        });

        return rval.compact();
    }

    /**
     * Filters all object by their toString() value using the given regular
     * expression. Only elements that match the regular expression are being kept.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("ax", "bx", "cy").filter(".y")</code> - Returns a core containing <code>"cy"</code>.</li>
     * </ul>
     * 
     * Multi-threaded.<br/>
     * <br/>
     * 
     * @param regex The regular expression to use.
     * @param options Supports INVERT_SELECTION if the filter logic should be inverted
     * (options that match the regular expression will not be considered).
     * 
     * @return A CoreObject containing a filtered subset of our elements.
     */
    public CoreObject<T> filter(final String regex, Option... options) {
        final Pattern p = Pattern.compile(regex);

        return filter(new F1Object2Bool<T>() {
            public boolean f(T x) {
                final Matcher matcher = p.matcher(x.toString());
                return matcher.matches();
            }
        }, options);
    }

    /**
     * Finds the first element that is not null.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$(null, "a", "b").first()</code> - Returns <code>"a"</code>.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @return The first element that was not null, or null, if all elements
     * were null.
     */
    public T first() {
        for (T t : this) {
            if (t != null) return t;
        }
        return null;
    }

    /**
     * Generates a textual fingerprint for this element for debugging purporses.
     * 
     * @param detailed If the fingerprint should contain detailed information or not.
     * @return A user-readable string which can be printed.
     */
    protected String fingerprint(boolean detailed) {
        final StringBuilder sb = new StringBuilder();
        sb.append("@(");
        sb.append(getClass().getSimpleName());
        sb.append("; outerSize:");
        sb.append(size());
        sb.append("; innerSize:");

        // Append inner size
        Object first = null;
        int ctr = 0;

        // Count elements and extract first nonnull element.
        for (T next : this) {
            if (next != null) {
                ctr++;
                if (first == null) first = next;
            }
        }

        sb.append(ctr);

        // Append type of first element (disabled)
        if (first != null && detailed) {
            sb.append("; firstElement:");
            sb.append(first.getClass().getSimpleName());
        }

        // Append fingerprint
        if (size() <= 16) {
            sb.append("; fingerprint:");

            for (T next : this) {
                if (next != null) {
                    sb.append(next.getClass().getSimpleName().charAt(0));
                } else
                    sb.append(".");
            }
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * Folds the given object, multi-threaded version. Fold removes two arbitrary elements,
     * executes <code>f()</code> on them and stores the result again. This is done in parallel until
     * only one element remains.<br/>
     * <br/>
     * 
     * It is guaranteed that each element will have been compared at least once, but the chronological-
     * or parameter-order when and where this occurs is, in contrast to <code>reduce()</code>, not defined.
     * <code>Null</code> elements are gracefully ignored.<br/>
     * <br/>
     * 
     * At present, <code>reduce()</code> is much faster for simple operations and small cores, as it involves much less
     * synchronization overhead, while <code>fold()</code> has advantages especially
     * with very complex <code>f</code> operators.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$(1, 2, 3, 4).fold(fmax)</code> - When <code>fmax</code> returns the larger of both objects the
     * resulting core will contain <code>4</code>.</li>
     * </ul>
     * 
     * Multi-threaded. Heavyweight.<br/>
     * <br/>
     * 
     * @param f The reduce function. Takes two elements, returns one.
     * @param options Supports {@link MapType}.
     * @return A CoreObject, containing at most a single element.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SupportsOption(options = { MapType.class })
    public CoreObject<T> fold(final F2ReduceObjects<T> f, Option... options) {

        // In case we only have zero or one elements, don't do anything
        if (size() <= 1) return this;

        final AtomicReferenceArray array = new AtomicReferenceArray(this.adapter.array());
        final Folder<T> folder = new Folder<T>(this) {
            @Override
            public void handle(int i, int j, int destination) {
                // Get the in-value from the source-array

                final T ii = (T) array.get(i);
                final T jj = (T) array.get(j);

                if (ii == null && jj == null) return;
                if (ii == null && jj != null) {
                    array.set(destination, jj);
                    return;
                }

                if (ii != null && jj == null) {
                    array.set(destination, ii);
                    return;
                }

                array.set(destination, f.f(ii, jj));
            }
        };

        // Now do fold ...
        fold(folder, options);

        T[] target = (T[]) Array.newInstance(this.adapter.clazz(), 1); // Arrays.copyOf(this.t, 1);
        target[0] = (T) array.get(0);

        // ... and return result.
        return new CoreObject<T>(this.commonCore, target);
    }

    /**
     * Performs the given operation on each element and returns a new core. This is the
     * single-threaded version of map().<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", null, "c").forEach(f)</code> - Performs the operation <code>f</code> on each element of the
     * core, except <code>null</code>.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param <R> Return type.
     * @param f Mapper function.
     * @param _options Relevant options: <code>OptionMapType</code>.
     * 
     * @return A CoreObject containing the mapped elements in a stable order.
     */
    @SuppressWarnings("unchecked")
    public <R> CoreObject<R> forEach(final F1<T, R> f, Option... _options) {

        // Create a mapper and iterate over it
        final Mapper<T, R> mapper = mapper(f, _options);
        for (int i = 0; i < size(); i++) {
            mapper.handle(i);
        }

        // ... and return result.
        return new CoreObject<R>(this.commonCore, mapper.getFinalReturnArray());
    }

    /**
     * Performs the given operation for each <code>n</code> elements of this core.
     * Elements will only be used once, so for example <code>forEach(f, 2)</code> means
     * that <code>f</code> will be called with elements <code>f(0, 1)</code>, <code>f(2, 3)</code> ... Remaining
     * elements are ignored. This function also acts on <code>null</code> elements. If you don't want that,
     * <code>compact()</code> the core.
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", null, "c").forEach(f)</code> - Performs the operation <code>f</code> on each element of the
     * core, except <code>null</code>.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param <R> The return type.
     * @param f The function to execute for each <code>n</code> elements.
     * @param n The number of elements to put into <code>f</code>
     * @return A core with the new elements.
     */
    @SuppressWarnings("unchecked")
    public <R> CoreObject<R> forEach(final Fn<T, R> f, int n) {
        if (size() == 0) return new CoreObject<R>(this.commonCore, null, null);

        R[] rval = null;
        T[] slice = (T[]) Array.newInstance(this.adapter.clazz(), n); // Arrays.copyOf(this.t, n);

        int ptr = 0;
        int tptr = 0;

        // Now go over the array
        for (int i = 0; i < size(); i++) {
            T e = this.adapter.get(i);

            // When our current element is null, do nothing.
            if (e == null) continue;

            // Store element to slice
            slice[ptr++] = e;

            // If the slice is not full, continue
            if (ptr < n) continue;

            // Execute the call
            R result = f.f(slice);

            // If we have a result, create result array
            if (rval == null && result != null) {
                rval = (R[]) Array.newInstance(result.getClass(), size() / n);
            }

            // If we have the arry, store the result
            if (rval != null) {
                rval[tptr] = result;
            }

            // Increase the target ptr in any case and reset the source ptr
            tptr++;
            ptr = 0;
        }

        // ... and return result.
        return new CoreObject<R>(this.commonCore, rval);
    }

    /**
     * Return the element at the the given relative position (0 <= x <= 1) or return <code>dflt</code> if that element
     * is null.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", null, "c").get(0.5, "b")</code> - Returns <code>"b"</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param percent 0.0 returns the first element, 1.0 the last element, 0.5 returns the
     * element in the
     * middle, and so on.
     * @param dflt The value to return if null had been returned otherwise.
     * 
     * @return The value at the requested position, or dflt if there is none.
     */
    public T get(double percent, T dflt) {
        if (Double.isNaN(percent)) return dflt;
        if (percent < 0) return dflt;
        if (percent > 1) return dflt;

        if (size() == 0) return dflt;

        int offset = (int) (percent * size());
        if (offset >= size()) return dflt;

        return this.adapter.get(offset);
    }

    /**
     * Return an element at the the relative position (0 <= x <= 1).<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c", "d", "e").get(0.75)</code> - Returns <code>"d"</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param percent 0.0 returns the first element, 1.0 the last element, 0.5 returns the
     * element in the
     * middle, and so on.
     * 
     * @return The value at the requested position, or null if there is none.
     */
    public T get(double percent) {
        return get(percent, null);
    }

    /**
     * Returns the first element that is an instance of the requested type.
     * 
     * Examples:
     * <ul>
     * <li><code>$(1, new Object(), "Hi").get(String.class, "Oops")</code> - Returns <code>"Hi"</code>.</li>
     * </ul>
     * 
     * 
     * Single-threaded. Heavyweight.<br/>
     * <br/>
     * 
     * @param <X> Subtype to request.
     * @param request A subclass of our type to request.
     * @param dflt The value to return when no class was found.
     * 
     * @return The first object that is assignable to request, or dflt if there was no
     * such element.
     */
    @SuppressWarnings("unchecked")
    public <X extends T> X get(Class<X> request, X dflt) {
        final ListIterator<T> iterator = this.adapter.iterator();
        while (iterator.hasNext()) {
            final T next = iterator.next();
            if (next != null && request.isAssignableFrom(next.getClass()))
                return (X) next;

        }

        return dflt;
    }

    /**
     * Return the ith element.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c").get(-1)</code> - Returns <code>"c"</code>.</li>
     * </ul>
     * 
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param i Position to retrieve. Negative indices are treated as values starting at
     * the end (i.e., -1 is the last element, -2 the second-last, ...)
     * 
     * @return The element at the given position.
     */
    public T get(int i) {
        final int offset = indexToOffset(i);

        if (offset < 0 || offset > size()) return null;

        return this.adapter.get(offset);
    }

    /**
     * Return the ith element or dflt if the element if otherwise <code>null</code> had
     * been returned.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", null).get(2, "c")</code> - Returns <code>"c"</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param i Position to retrieve. Negative indices are treated as values starting at
     * the end (i.e., -1 is the last element, -2 the second-last, ...)
     * @param dflt The value to return if null had been returned.
     * 
     * @return Unless dflt is null, this function is guaranteed to return a non-null
     * value.
     */
    public T get(int i, T dflt) {
        final T rval = get(i);
        return rval == null ? dflt : rval;
    }

    /**
     * Returns the first element, or, if there is none, return dflt.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(name).get("Unknown")</code> - Returns <code>"Unknown"</code> if <code>name</code> is
     * <code>null</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param dflt The value to return if get(0) is null.
     * @return Unless dflt is null, this function is guaranteed to return a non-null
     * value.
     */
    public T get(T dflt) {
        return get(0, dflt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.adapter.hashCode();
    }

    /**
     * Checks if all elements are not null. If the core is empty, <code>false</code> is returned as well.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", null, "b").hasAll()</code> - Returns <code>false</code>.</li>
     * <li><code>$("x").hasAll()</code> - Returns <code>true</code>.</li>
     * <li><code>$().hasAll()</code> - Returns <code>false</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @return True if all elements are not null, false if a single element was null.
     */
    public boolean hasAll() {
        if (size() == 0) return false;

        for (T t : this) {
            if (t == null) return false;
        }

        return true;
    }

    /**
     * Checks if the element has any element. If the core is empty, <code>false</code> is returned.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(null, "b").hasAny()</code> - Returns <code>true</code>.</li>
     * <li><code>$().hasAny()</code> - Returns <code>false</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @return True if any element is set. False if all elements are null.
     */
    public boolean hasAny() {
        for (T t : this) {
            if (t != null) return true;
        }

        return false;
    }

    /**
     * If all elements are present, execute f0.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(null, "b").ifAll(f)</code> - Does not execute f.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param f0 The function to execute if all elements are given.
     * @return This core.
     */
    public CoreObject<T> ifAll(F0 f0) {
        if (hasAll()) f0.f();
        return this;
    }

    /**
     * Returns the first index positions for all objects equal to the given object, or null if no object
     * equalled the given one.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "c", "b").index("c", "b", "a")</code> - Returns a core <code>$(1, 2, 0)</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param objects The objects to return the first index for.
     * @return A {@link CoreNumber} object with the corresponding index position.
     */
    @SuppressWarnings("boxing")
    public CoreNumber index(T... objects) {
        if (objects == null) return new CoreNumber(this.commonCore, new Number[0]);
        Integer indices[] = new Integer[objects.length];

        // Check all objects ...
        for (int i = 0; i < objects.length; i++) {
            final T obj = objects[i];

            final ListIterator<T> iterator = iterator();
            while (iterator.hasNext()) {
                final int j = iterator.nextIndex();
                final T next = iterator.next();

                if (obj != null && obj.equals(next)) {
                    indices[i] = j;
                    break;
                }

                if (obj == null && next == null) {
                    indices[i] = j;
                    break;
                }
            }
        }

        return new CoreNumber(this.commonCore, indices);
    }

    /**
     * Returns a core intersected with another core.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("x", "y", "z").intersect($("y", "z"))</code> - Returns a core <code>$("y", "z")</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param other The other core to intersect.
     * @return Returns a core enclosing only objects present in this and the other core.
     */
    public CoreObject<T> intersect(CoreObject<T> other) {
        if (size() == 0) return this;
        if (other.size() == 0) return other;

        final T[] copy = this.adapter.array();

        // Remove every element we in the other core
        for (int i = 0; i < copy.length; i++) {
            final T element = copy[i];
            if (element == null) continue;

            boolean found = false;

            // Check if the copy contains the element
            for (T x : other) {
                if (x == null || !x.equals(element)) continue;
                found = true;
                break;
            }

            // If not in both, remove.
            if (!found) {
                copy[i] = null;
            }
        }

        // Return a compacted core.
        return new CoreObject<T>(this.commonCore, copy).compact();
    }

    /**
     * Returns a core intersected with another array.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$("x", "y", "z").intersect("a", "x", "b")</code> - Returns a core <code>$("x")</code>.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @param other The array to intersect.
     * @return Returns a core enclosing only objects present in this core and the other array.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CoreObject<T> intersect(T... other) {
        return intersect(new CoreObject(this.commonCore, other));
    }

    /**
     * Returns the wrapped collection as a list.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$(array).list()</code> - Returns a typed {@link List} for the given array.</li>
     * </ul>
     * 
     * Single-threaded. <br/>
     * <br/>
     * 
     * @return A list containing all elements. Null values should be preserved.
     */
    public List<T> list() {
        return new ArrayList<T>(this.adapter.unsafelist());
    }

    /**
     * Maps the core's content with the given function and returns the result. This is the
     * most fundamental function of jCores. If the core is of size 0 nothing is done, if it is of size 1 <code>f</code>
     * is executed directly. In all other cases <code>map</code> (at least in the
     * current implementation) will go parallel <i>on demand</i>. It takes a test run for the
     * first element and measures the time to process it. If the estimated time it takes to
     * complete the rest of the core is less than the measured time it takes to go parallel (which
     * has some overhead), no parallelization is being performed.<br/>
     * <br/>
     * 
     * As a general rule of thumb, <code>map</code> works relatively best on large cores (number of elements) and
     * time-consuming <code>f</code>-
     * operations and it works worst on small cores and very simple <code>f</code>. However,
     * the good message is that these disadvantageous cores/functions only impact your
     * application's performance in case you call <code>map</code> on them hundreds of
     * thousands times a second, as the absolute overhead is still very small.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$.range(1000).map(convert)</code> - Given <code>convert</code> performs some conversion, this would
     * convert the numbers from 0 to 999 in parallel (using as many CPUs as there are available).</li>
     * </ul>
     * 
     * Multi-threaded.<br/>
     * <br/>
     * 
     * <b>Warning:</b> The larger the core (number of objects) the more you should make sure
     * that <code>f</code> is as <i>isolated</i> as possible, since sharing even a single object
     * (e.g., a shared {@link Random} object like <code>$.random()</code>) among different mapper
     * threads can have a dramatic performance impact. In some cases (e.g., many CPUs (>2), large
     * core (<code>size >> 1000</code>), simple <code>f</code> with shared, synchronized variable
     * access) the performance of <code>map</code> can even drop below the performance of <code>forEach</code>.<br/>
     * <br/>
     * 
     * 
     * @param <R> Return type.
     * @param f Mapper function, must be thread-safe.
     * @param _options Relevant options: {@link MapType}.
     * 
     * @return A CoreObject containing the mapped elements in a stable order.
     */
    @SuppressWarnings("unchecked")
    @SupportsOption(options = { MapType.class })
    public <R> CoreObject<R> map(final F1<T, R> f, Option... _options) {

        // Map what we got
        final Mapper<T, R> mapper = mapper(f, _options);
        map(mapper, _options);

        // ... and return result.
        return new CoreObject<R>(this.commonCore, mapper.getFinalReturnArray());
    }

    /**
     * Prints all strings to the console. Almost the same as <code>string().print()</code>,
     * except that this method returns a CoreObject again.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c").print().intersect("a").print()</code> - The first output will be <code>a b c</code>,
     * then again <code>a</code>.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @return Returns this CoreObject object again.
     */
    public CoreObject<T> print() {
        if (size() == 0) return this;

        for (Object s : this) {
            if (s == null) continue;
            System.out.println(s);
        }

        return this;
    }

    /**
     * Prints all strings to the console in a single line with the given joiner. <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c").print(",")</code> - Prints <code>a,b,c</code></li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @since 1.0
     * @param joiner The string to put in between the elements.
     * @return Returns this CoreObject object again.
     */
    public CoreObject<T> print(String joiner) {
        System.out.println(string().join(joiner));
        return this;
    }

    /**
     * Returns a randomly selected object, including null values.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c").random()</code> - Returns ... well, we don't know yet.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @return A randomly selected object from this core.
     */
    public T random() {
        final int size = size();
        if (size == 0) return null;
        return this.adapter.get(this.commonCore.random().nextInt(size));
    }

    /**
     * Returns a randomly selected subset, including null values. The elements will be
     * returned in a random order. Elements will never be drawn twice.<br/>
     * <br/>
     * 
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c", "d").random(0.5)</code> - Could return <code>$("c", "a")</code>, but never
     * <code>$("b", "b")</code>.</li>
     * </ul>
     * 
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param percent Specifies how many percent of elements of this core should be in the
     * resulting core. For example <code>0.5</code> means that half of the elements of
     * this core will be randomly selected and returned, <code>0.0</code> means an empty
     * core will be returned and <code>1.0</code> a shuffled core will be returned.
     * 
     * @return A core enclosing randomly selected objects.
     */
    public CoreObject<T> random(double percent) {
        final double p = Math.max(Math.min(1.0, percent), 0.0);
        return random((int) (p * size()));
    }

    /**
     * Returns a randomly selected subset, including null values. The elements will be
     * returned in a random order.<br/>
     * <br/>
     * 
     * Examples:
     * <ul>
     * <li><code>$("a", "b", "c", "d").random(2)</code> - Same as <code>.random(0.5)</code> in the example above.</li>
     * </ul>
     * 
     * Single-threaded.<br/>
     * <br/>
     * 
     * @param newSize Specifies how many elements of this core should be in the
     * resulting core.
     * 
     * @return A core enclosing randomly selected objects.
     */
    public CoreObject<T> random(int newSize) {
        final int size = size();

        if (size == 0) return this;

        // Create a shuffletable
        final T[] copyOf = this.adapter.array();

        // Shuffle the copy
        for (int i = copyOf.length - 1; i >= 1; i--) {
            int j = this.commonCore.random().nextInt(i + 1);

            T x = copyOf[j];
            copyOf[j] = copyOf[i];
            copyOf[i] = x;
        }

        // And return th
