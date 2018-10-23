// https://searchcode.com/api/result/67705164/

/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., InfraDNA, Inc., CloudBees, Inc.
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
package hudson;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.common.collect.ImmutableList;
import hudson.init.InitMilestone;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import jenkins.ExtensionComponentSet;
import jenkins.ExtensionRefreshException;
import jenkins.ProxyInjector;
import jenkins.model.Jenkins;
import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Discovers the implementations of an extension point.
 *
 * <p>
 * This extension point allows you to write your implementations of {@link ExtensionPoint}s
 * in arbitrary DI containers, and have Hudson discover them.
 *
 * <p>
 * {@link ExtensionFinder} itself is an extension point, but to avoid infinite recursion,
 * Hudson discovers {@link ExtensionFinder}s through {@link Sezpoz} and that alone.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.286
 */
public abstract class ExtensionFinder implements ExtensionPoint {
    /**
     * @deprecated as of 1.356
     *      Use and implement {@link #find(Class,Hudson)} that allows us to put some metadata.
     */
    @Restricted(NoExternalUse.class)
    public <T> Collection<T> findExtensions(Class<T> type, Hudson hudson) {
        return Collections.emptyList();
    }

    /**
     * Returns true if this extension finder supports the {@link #refresh()} operation.
     */
    public boolean isRefreshable() {
        try {
            return getClass().getMethod("refresh").getDeclaringClass()!=ExtensionFinder.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Rebuilds the internal index, if any, so that future {@link #find(Class, Hudson)} calls
     * will discover components newly added to {@link PluginManager#uberClassLoader}.
     *
     * <p>
     * The point of the refresh operation is not to disrupt instances of already loaded {@link ExtensionComponent}s,
     * and only instantiate those that are new. Otherwise this will break the singleton semantics of various
     * objects, such as {@link Descriptor}s.
     *
     * <p>
     * The behaviour is undefined if {@link #isRefreshable()} is returning false.
     *
     * @since 1.442
     * @see #isRefreshable()
     * @return never null
     */
    public abstract ExtensionComponentSet refresh() throws ExtensionRefreshException;

    /**
     * Discover extensions of the given type.
     *
     * <p>
     * This method is called only once per the given type after all the plugins are loaded,
     * so implementations need not worry about caching.
     *
     * <p>
     * This method should return all the known components at the time of the call, including
     * those that are discovered later via {@link #refresh()}, even though those components
     * are separately retruend in {@link ExtensionComponentSet}.
     *
     * @param <T>
     *      The type of the extension points. This is not bound to {@link ExtensionPoint} because
     *      of {@link Descriptor}, which by itself doesn't implement {@link ExtensionPoint} for
     *      a historical reason.
     * @param jenkins
     *      Jenkins whose behalf this extension finder is performing lookup.
     * @return
     *      Can be empty but never null.
     * @since 1.356
     *      Older implementations provide {@link #findExtensions(Class,Hudson)}
     */
    public abstract <T> Collection<ExtensionComponent<T>> find(Class<T> type, Hudson jenkins);

    /**
     * A pointless function to work around what appears to be a HotSpot problem. See JENKINS-5756 and bug 6933067
     * on BugParade for more details.
     */
    public <T> Collection<ExtensionComponent<T>> _find(Class<T> type, Hudson hudson) {
        return find(type,hudson);
    }

    /**
     * Performs class initializations without creating instances. 
     *
     * If two threads try to initialize classes in the opposite order, a dead lock will ensue,
     * and we can get into a similar situation with {@link ExtensionFinder}s.
     *
     * <p>
     * That is, one thread can try to list extensions, which results in {@link ExtensionFinder}
     * loading and initializing classes. This happens inside a context of a lock, so that
     * another thread that tries to list the same extensions don't end up creating different
     * extension instances. So this activity locks extension list first, then class initialization next.
     *
     * <p>
     * In the mean time, another thread can load and initialize a class, and that initialization
     * can eventually results in listing up extensions, for example through static initializer.
     * Such activity locks class initialization first, then locks extension list.
     *
     * <p>
     * This inconsistent locking order results in a dead lock, you see.
     *
     * <p>
     * So to reduce the likelihood, this method is called in prior to {@link #find(Class,Hudson)} invocation,
     * but from outside the lock. The implementation is expected to perform all the class initialization activities
     * from here.
     *
     * <p>
     * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459208 for how to force a class initialization.
     * Also see http://kohsuke.org/2010/09/01/deadlock-that-you-cant-avoid/ for how class initialization
     * can results in a dead lock.
     */
    public void scout(Class extensionType, Hudson hudson) {
    }

    @Extension
    public static final class GuiceFinder extends AbstractGuiceFinder<Extension> {
        public GuiceFinder() {
            super(Extension.class);

            // expose Injector via lookup mechanism for interop with non-Guice clients
            Jenkins.getInstance().lookup.set(Injector.class,new ProxyInjector() {
                protected Injector resolve() {
                    return getContainer();
                }
            });
        }

        @Override
        protected boolean isOptional(Extension annotation) {
            return annotation.optional();
        }

        @Override
        protected double getOrdinal(Extension annotation) {
            return annotation.ordinal();
        }
    }

    /**
     * Discovers components via sezpoz but instantiates them by using Guice.
     */
    public static abstract class AbstractGuiceFinder<T extends Annotation> extends ExtensionFinder {
        /**
         * Injector that we find components from.
         * <p>
         * To support refresh when Guice doesn't let us alter the bindings, we'll create
         * a child container to house newly discovered components. This field points to the
         * youngest such container.
         */
        private volatile Injector container;

        /**
         * Sezpoz index we are currently using in {@link #container} (and its ancestors.)
         * Needed to compute delta.
         */
        private List<IndexItem<T,Object>> sezpozIndex;

        private final Map<Key,T> annotations = new HashMap<Key,T>();
        private final Sezpoz moduleFinder = new Sezpoz();
        private final Class<T> annotationType;

        public AbstractGuiceFinder(final Class<T> annotationType) {
            this.annotationType = annotationType;

            sezpozIndex = ImmutableList.copyOf(Index.load(annotationType, Object.class, Jenkins.getInstance().getPluginManager().uberClassLoader));

            List<Module> modules = new ArrayList<Module>();
            modules.add(new SezpozModule(sezpozIndex));

            for (ExtensionComponent<Module> ec : moduleFinder.find(Module.class, Hudson.getInstance())) {
                modules.add(ec.getInstance());
            }

            try {
                container = Guice.createInjector(modules);
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "Failed to create Guice container from all the plugins",e);
                // failing to load all bindings are disastrous, so recover by creating minimum that works
                // by just including the core
                container = Guice.createInjector(new SezpozModule(
                        ImmutableList.copyOf(Index.load(annotationType, Object.class, Jenkins.class.getClassLoader()))));
            }
        }

        public Injector getContainer() {
            return container;
        }

        /**
         * The basic idea is:
         *
         * <ul>
         *     <li>List up delta as a series of modules
         *     <li>
         * </ul>
         */
        @Override
        public synchronized ExtensionComponentSet refresh() throws ExtensionRefreshException {
            // figure out newly discovered sezpoz components
            List<IndexItem<T, Object>> delta = Sezpoz.listDelta(annotationType,sezpozIndex);
            List<IndexItem<T, Object>> l = Lists.newArrayList(sezpozIndex);
            l.addAll(delta);
            sezpozIndex = l;

            List<Module> modules = new ArrayList<Module>();
            modules.add(new SezpozModule(delta));
            for (ExtensionComponent<Module> ec : moduleFinder.refresh().find(Module.class)) {
                modules.add(ec.getInstance());
            }

            try {
                final Injector child = container.createChildInjector(modules);
                container = child;

                return new ExtensionComponentSet() {
                    @Override
                    public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                        List<ExtensionComponent<T>> result = new ArrayList<ExtensionComponent<T>>();
                        _find(type, result, child);
                        return result;
                    }
                };
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "Failed to create Guice container from newly added plugins",e);
                throw new ExtensionRefreshException(e);
            }
        }

        protected abstract double getOrdinal(T annotation);

        /**
         * Hook to enable subtypes to control which ones to pick up and which ones to ignore.
         */
        protected boolean isActive(AnnotatedElement e) {
            return true;
        }

        protected abstract boolean isOptional(T annotation);

        private Object instantiate(IndexItem<T,Object> item) {
            try {
                return item.instance();
            } catch (LinkageError e) {
                // sometimes the instantiation fails in an indirect classloading failure,
                // which results in a LinkageError
                LOGGER.log(isOptional(item.annotation()) ? Level.FINE : Level.WARNING,
                           "Failed to load "+item.className(), e);
            } catch (InstantiationException e) {
                LOGGER.log(isOptional(item.annotation()) ? Level.FINE : Level.WARNING,
                           "Failed to load "+item.className(), e);
            }
            return null;
        }

        public <U> Collection<ExtensionComponent<U>> find(Class<U> type, Hudson jenkins) {
            // the find method contract requires us to traverse all known components
            List<ExtensionComponent<U>> result = new ArrayList<ExtensionComponent<U>>();
            for (Injector i=container; i!=null; i=i.getParent()) {
                _find(type, result, i);
            }
            return result;
        }

        private <U> void _find(Class<U> type, List<ExtensionComponent<U>> result, Injector container) {
            for (Entry<Key<?>, Binding<?>> e : container.getBindings().entrySet()) {
                if (type.isAssignableFrom(e.getKey().getTypeLiteral().getRawType())) {
                    T a = annotations.get(e.getKey());
                    Object o = e.getValue().getProvider().get();
                    if (o!=null)
                        result.add(new ExtensionComponent<U>(type.cast(o),a!=null?getOrdinal(a):0));
                }
            }
        }

        /**
         * TODO: need to learn more about concurrent access to {@link Injector} and how it interacts
         * with classloading.
         */
        @Override
        public void scout(Class extensionType, Hudson hudson) {
        }

        /**
         * {@link Scope} that allows a failure to create a component,
         * and change the value to null.
         *
         * <p>
         * This is necessary as a failure to load one plugin shouldn't fail the startup of the entire Jenkins.
         * Instead, we should just drop the failing plugins.
         */
        public static final Scope FAULT_TOLERANT_SCOPE = new Scope() {
            public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
                final Provider<T> base = Scopes.SINGLETON.scope(key,unscoped);
                return new Provider<T>() {
                    public T get() {
                        try {
                            return base.get();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING,"Failed to instantiate. Skipping this component",e);
                            return null;
                        }
                    }
                };
            }
        };

        private static final Logger LOGGER = Logger.getLogger(GuiceFinder.class.getName());

        /**
         * {@link Module} that finds components via sezpoz index.
         * Instead of using SezPoz to instantiate, we'll instantiate them by using Guice,
         * so that we can take advantage of dependency injection.
         */
        private class SezpozModule extends AbstractModule {
            private final List<IndexItem<T,Object>> index;;

            public SezpozModule(List<IndexItem<T,Object>> index) {
                this.index = index;
            }

            /**
             * Guice performs various reflection operations on the class to figure out the dependency graph,
             * and that process can cause additional classloading problems, which will fail the injector creation,
             * which in turn has disastrous effect on the startup.
             *
             * <p>
             * Ultimately I'd like to isolate problems to plugins and selectively disable them, allowing
             * Jenkins to start with plugins that work, but I haven't figured out how.
             *
             * So this is an attempt to detect subset of problems eagerly, by invoking various reflection
             * operations and try to find non-existent classes early.
             */
            private void resolve(Class c) {
                try {
                    c.getGenericSuperclass();
                    c.getGenericInterfaces();
                    ClassLoader ecl = c.getClassLoader();
                    Method m = ClassLoader.class.getDeclaredMethod("resolveClass", Class.class);
                    m.setAccessible(true);
                    m.invoke(ecl, c);
                    c.getMethods();
                    c.getFields();
                } catch (Exception x) {
                    throw (LinkageError)new LinkageError("Failed to resolve "+c).initCause(x);
                }
            }

            @SuppressWarnings({"unchecked", "ChainOfInstanceofChecks"})
            @Override
            protected void configure() {
                int id=0;

                for (final IndexItem<T,Object> item : index) {
                    id++;
                    try {
                        AnnotatedElement e = item.element();
                        if (!isActive(e))   continue;
                        T a = item.annotation();

                        if (e instanceof Class) {
                            Key key = Key.get((Class)e);
                            resolve((Class)e);
                            annotations.put(key,a);
                            bind(key).in(FAULT_TOLERANT_SCOPE);
                        } else {
                            Class extType;
                            if (e instanceof Field) {
                                extType = ((Field)e).getType();
                            } else
                            if (e instanceof Method) {
                                extType = ((Method)e).getReturnType();
                            } else
                                throw new AssertionError();

                            resolve(extType);

                            // use arbitrary id to make unique key, because Guice wants that.
                            Key key = Key.get(extType, Names.named(String.valueOf(id)));
                            annotations.put(key,a);
                            bind(key).toProvider(new Provider() {
                                    public Object get() {
                                        return instantiate(item);
                                    }
                                }).in(FAULT_TOLERANT_SCOPE);
                        }
                    } catch (LinkageError e) {
                        // sometimes the instantiation fails in an indirect classloading failure,
                        // which results in a LinkageError
                        LOGGER.log(isOptional(item.annotation()) ? Level.FINE : Level.WARNING,
                                   "Failed to load "+item.className(), e);
                    } catch (InstantiationException e) {
                        LOGGER.log(isOptional(item.annotation()) ? Level.FINE : Level.WARNING,
                                   "Failed to load "+item.className(), e);
                    }
                }
            }
        }
    }

    /**
     * The bootstrap implementation that looks for the {@link Extension} marker.
     *
     * <p>
     * Uses Sezpoz as the underlying mechanism.
     */
    public static final class Sezpoz extends ExtensionFinder {

        private volatile List<IndexItem<Extension,Object>> indices;

        /**
         * Loads indices (ideally once but as few times as possible), then reuse them later.
         * {@link ExtensionList#ensureLoaded()} guarantees that this method won't be called until
         * {@link InitMilestone#PLUGINS_PREPARED} is attained, so this method is guaranteed to
         * see all the classes and indices.
         */
        private List<IndexItem<Extension,Object>> getIndices() {
            // this method cannot be synchronized because of a dead lock possibility in the following order of events:
            // 1. thread X can start listing indices, locking this object 'SZ'
            // 2. thread Y starts loading a class, locking a classloader 'CL'
            // 3. thread X needs to load a class, now blocked on CL
            // 4. thread Y decides to load extensions, now blocked on SZ.
            // 5. dead lock
            if (indices==null) {
                ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;
                indices = ImmutableList.copyOf(Index.load(Extension.class, Object.class, cl));
            }
            return indices;
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * SezPoz implements value-equality of {@link IndexItem}, so
         */
        @Override
        public synchronized ExtensionComponentSet refresh() {
            final List<IndexItem<Extension,Object>> old = indices;
            if (old==null)      return ExtensionComponentSet.EMPTY; // we haven't loaded anything

            final List<IndexItem<Extension, Object>> delta = listDelta(Extension.class,old);

            List<IndexItem<Extension,Object>> r = Lists.newArrayList(old);
            r.addAll(delta);
            indices = ImmutableList.copyOf(r);

            return new ExtensionComponentSet() {
                @Override
                public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                    return _find(type,delta);
                }
            };
        }

        static <T extends Annotation> List<IndexItem<T, Object>> listDelta(Class<T> annotationType, List<IndexItem<T, Object>> old) {
            // list up newly discovered components
            final List<IndexItem<T,Object>> delta = Lists.newArrayList();
            ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;
            for (IndexItem<T,Object> ii : Index.load(annotationType, Object.class, cl)) {
                if (!old.contains(ii)) {
                    delta.add(ii);
                }
            }
            return delta;
        }

        public <T> Collection<ExtensionComponent<T>> find(Class<T> type, Hudson jenkins) {
            return _find(type,getIndices());
        }

        /**
         * Finds all the matching {@link IndexItem}s that match the given type and instantiate them.
         */
        private <T> Collection<ExtensionComponent<T>> _find(Class<T> type, List<IndexItem<Extension,Object>> indices) {
            List<ExtensionComponent<T>> result = new ArrayList<ExtensionComponent<T>>();

            for (IndexItem<Extension,Object> item : indices) {
                try {
                    AnnotatedElement e = item.element();
                    Class<?> extType;
                    if (e instanceof Class) {
                        extType = (Class) e;
                    } else
                    if (e instanceof Field) {
                        extType = ((Field)e).getType();
                    } else
                    if (e instanceof Method) {
                        extType = ((Method)e).getReturnType();
                    } else
                        throw new AssertionError();

                    if(type.isAssignableFrom(extType)) {
                        Object instance = item.instance();
                        if(instance!=null)
                            result.add(new ExtensionComponent<T>(type.cast(instance),item.annotation()));
                    }
                } catch (LinkageError e) {
                    // sometimes the instantiation fails in an indirect classloading failure,
                    // which results in a LinkageError
                    LOGGER.log(logLevel(item), "Failed to load "+item.className(), e);
                } catch (InstantiationException e) {
                    LOGGER.log(logLevel(item), "Failed to load "+item.className(), e);
                }
            }

            return result;
        }

        @Override
        public void scout(Class extensionType, Hudson hudson) {
            for (IndexItem<Extension,Object> item : getIndices()) {
                try {
                    // we might end up having multiple threads concurrently calling into element(),
                    // but we can't synchronize this --- if we do, the one thread that's supposed to load a class
                    // can block while other threads wait for the entry into the element call().
                    // looking at the sezpoz code, it should be safe to do so
                    AnnotatedElement e = item.element();
                    Class<?> extType;
                    if (e instanceof Class) {
                        extType = (Class) e;
                    } else
                    if (e instanceof Field) {
                        extType = ((Field)e).getType();
                    } else
                    if (e instanceof Method) {
                        extType = ((Method)e).getReturnType();
                    } else
                        throw new AssertionError();
                    // according to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6459208
                    // this appears to be the only way to force a class initialization
                    Class.forName(extType.getName(),true,extType.getClassLoader());
                } catch (InstantiationException e) {
                    LOGGER.log(logLevel(item), "Failed to scout "+item.className(), e);
                } catch (ClassNotFoundException e) {
                    LOGGER.log(logLevel(item), "Failed to scout "+item.className(), e);
                } catch (LinkageError e) {
                    LOGGER.log(logLevel(item), "Failed to scout "+item.className(), e);
                }
            }
        }

        private Level logLevel(IndexItem<Extension, Object> item) {
            return item.annotation().optional() ? Level.FINE : Level.WARNING;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ExtensionFinder.class.getName());
}

