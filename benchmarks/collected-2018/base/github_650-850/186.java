// https://searchcode.com/api/result/72626929/

/*
 * Copyright 2011 Goldman Sachs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gs.collections.impl.utility;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import com.gs.collections.api.RichIterable;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.block.function.Function0;
import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.block.predicate.Predicate;
import com.gs.collections.api.block.predicate.Predicate2;
import com.gs.collections.api.block.procedure.Procedure;
import com.gs.collections.api.block.procedure.Procedure2;
import com.gs.collections.api.list.MutableList;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.map.MapIterable;
import com.gs.collections.api.map.MutableMap;
import com.gs.collections.api.map.UnsortedMapIterable;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.block.factory.Predicates;
import com.gs.collections.impl.block.procedure.CollectProcedure;
import com.gs.collections.impl.block.procedure.CollectionAddProcedure;
import com.gs.collections.impl.block.procedure.CountProcedure;
import com.gs.collections.impl.block.procedure.MapEntryToProcedure2;
import com.gs.collections.impl.block.procedure.MapPutProcedure;
import com.gs.collections.impl.block.procedure.RejectProcedure;
import com.gs.collections.impl.block.procedure.SelectProcedure;
import com.gs.collections.impl.list.mutable.FastList;
import com.gs.collections.impl.map.mutable.MapAdapter;
import com.gs.collections.impl.map.mutable.UnifiedMap;
import com.gs.collections.impl.tuple.AbstractImmutableEntry;
import com.gs.collections.impl.tuple.Tuples;
import com.gs.collections.impl.utility.internal.IterableIterate;

/**
 * The MapIterate class provides a few of the methods from the Smalltalk Collection Protocol.  This includes:
 * <ul>
 * <li>select: -- a.k.a. filter</li>
 * <li>reject: -- a.k.a. not-filter</li>
 * <li>collect: -- a.k.a. transform, map, tear-off</li>
 * <li>inject:into: -- closely related to reduce and fold</li>
 * <li>detect: -- a.k.a. find, search</li>
 * <li>detect:ifNone:</li>
 * <li>anySatisfy: -- a.k.a. exists</li>
 * <li>allSatisfy:</li>
 * </ul>
 * Since Maps have two data-points per entry (i.e. key and value), most of the implementations in this class
 * iterates over the values only, unless otherwise specified.
 * To iterate over the keys, use keySet() with standard {@link Iterate} methods.
 *
 * @see Iterate
 * @since 1.0
 */
public final class MapIterate
{
    private MapIterate()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * A null-safe check on a map to see if it isEmpty.  A null collection results in {@code true}.
     */
    public static boolean isEmpty(Map<?, ?> map)
    {
        return map == null || map.isEmpty();
    }

    /**
     * A null-safe check on a map to see if it notEmpty.  A null collection results in {@code false}.
     */
    public static boolean notEmpty(Map<?, ?> map)
    {
        return map != null && !map.isEmpty();
    }

    /**
     * Get and return the value in the Map at the specified key, or if there is no value at the key, return the result
     * of evaluating the specified {@link Function0}, and put that value in the map at the specified key.
     * <p/>
     * This method handles the {@code null}-value-at-key case correctly.
     */
    public static <K, V> V getIfAbsentPut(Map<K, V> map, K key, Function0<? extends V> instanceBlock)
    {
        if (map instanceof MutableMap)
        {
            return ((MutableMap<K, V>) map).getIfAbsentPut(key, instanceBlock);
        }
        V result = map.get(key);
        if (MapIterate.isAbsent(result, map, key))
        {
            result = instanceBlock.value();
            map.put(key, result);
        }
        return result;
    }

    /**
     * Get and return the value in the Map at the specified key, or if there is no value at the key, return the result
     * of evaluating the specified {@link Function} with the {@code parameter}, and put that value in the map at
     * the specified key.
     */
    public static <K, V, P> V getIfAbsentPutWith(
            Map<K, V> map,
            K key,
            Function<? super P, ? extends V> function,
            P parameter)
    {
        V result = map.get(key);
        if (MapIterate.isAbsent(result, map, key))
        {
            result = function.valueOf(parameter);
            map.put(key, result);
        }
        return result;
    }

    /**
     * Get and return the value in the Map that corresponds to the specified key, or if there is no value
     * at the key, return the result of evaluating the specified {@link Function0}.
     */
    public static <K, V> V getIfAbsent(Map<K, V> map, K key, Function0<? extends V> instanceBlock)
    {
        if (map instanceof UnsortedMapIterable)
        {
            return ((MapIterable<K, V>) map).getIfAbsent(key, instanceBlock);
        }
        V result = map.get(key);
        if (MapIterate.isAbsent(result, map, key))
        {
            result = instanceBlock.value();
        }
        return result;
    }

    /**
     * Get and return the value in the Map that corresponds to the specified key, or if there is no value
     * at the key, return the result of evaluating the specified {@link Function} with the specified parameter.
     */
    public static <K, V, P> V getIfAbsentWith(
            Map<K, V> map,
            K key,
            Function<? super P, ? extends V> function,
            P parameter)
    {
        if (map instanceof UnsortedMapIterable)
        {
            return ((MapIterable<K, V>) map).getIfAbsentWith(key, function, parameter);
        }
        V result = map.get(key);
        if (MapIterate.isAbsent(result, map, key))
        {
            result = function.valueOf(parameter);
        }
        return result;
    }

    /**
     * Get and return the value in the Map at the specified key, or if there is no value at the key, return the
     * {@code defaultValue}.
     */
    public static <K, V> V getIfAbsentDefault(Map<K, V> map, K key, V defaultValue)
    {
        V result = map.get(key);
        if (MapIterate.isAbsent(result, map, key))
        {
            result = defaultValue;
        }
        return result;
    }

    private static <K, V> boolean isAbsent(V result, Map<K, V> map, K key)
    {
        return result == null && !map.containsKey(key);
    }

    /**
     * If there is a value in the Map tat the specified key, return the result of applying the specified Function
     * on the value, otherwise return null.
     */
    public static <K, V, A> A ifPresentApply(
            Map<K, V> map,
            K key,
            Function<? super V, ? extends A> function)
    {
        if (map instanceof UnsortedMapIterable)
        {
            return ((MapIterable<K, V>) map).ifPresentApply(key, function);
        }
        V result = map.get(key);
        return MapIterate.isAbsent(result, map, key) ? null : function.valueOf(result);
    }

    /**
     * @see Iterate#select(Iterable, Predicate)
     */
    public static <K, V> MutableList<V> select(Map<K, V> map, Predicate<? super V> predicate)
    {
        return MapIterate.select(map, predicate, FastList.<V>newList());
    }

    /**
     * @see Iterate#select(Iterable, Predicate, Collection)
     */
    public static <K, V, R extends Collection<V>> R select(
            Map<K, V> map,
            Predicate<? super V> predicate,
            R targetCollection)
    {
        Procedure<V> procedure = new SelectProcedure<V>(predicate, targetCollection);
        MapIterate.forEachValue(map, procedure);
        return targetCollection;
    }

    /**
     * @see Iterate#count(Iterable, Predicate)
     */
    public static <K, V> int count(Map<K, V> map, Predicate<? super V> predicate)
    {
        CountProcedure<V> procedure = new CountProcedure<V>(predicate);
        MapIterate.forEachValue(map, procedure);
        return procedure.getCount();
    }

    /**
     * For each <em>entry</em> of the source map, the Predicate2 is evaluated.
     * If the result of the evaluation is true, the map entry is moved to a result map.
     * The result map is returned containing all entries in the source map that evaluated to true.
     */
    public static <K, V> MutableMap<K, V> selectMapOnEntry(
            Map<K, V> map,
            Predicate2<? super K, ? super V> predicate)
    {
        return MapIterate.selectMapOnEntry(map, predicate, UnifiedMap.<K, V>newMap());
    }

    /**
     * For each <em>entry</em> of the source map, the Predicate2 is evaluated.
     * If the result of the evaluation is true, the map entry is moved to a result map.
     * The result map is returned containing all entries in the source map that evaluated to true.
     */
    public static <K, V, R extends Map<K, V>> R selectMapOnEntry(
            Map<K, V> map,
            final Predicate2<? super K, ? super V> predicate,
            R target)
    {
        final Procedure2<K, V> mapTransferProcedure = new MapPutProcedure<K, V>(target);
        Procedure2<K, V> procedure = new Procedure2<K, V>()
        {
            public void value(K key, V value)
            {
                if (predicate.accept(key, value))
                {
                    mapTransferProcedure.value(key, value);
                }
            }
        };
        MapIterate.forEachKeyValue(map, procedure);

        return target;
    }

    /**
     * For each <em>key</em> of the source map, the Predicate is evaluated.
     * If the result of the evaluation is true, the map entry is moved to a result map.
     * The result map is returned containing all entries in the source map that evaluated to true.
     */
    public static <K, V> MutableMap<K, V> selectMapOnKey(Map<K, V> map, final Predicate<? super K> predicate)
    {
        MutableMap<K, V> resultMap = UnifiedMap.newMap();
        final Procedure2<K, V> mapTransferProcedure = new MapPutProcedure<K, V>(resultMap);
        Procedure2<K, V> procedure = new Procedure2<K, V>()
        {
            public void value(K key, V value)
            {
                if (predicate.accept(key))
                {
                    mapTransferProcedure.value(key, value);
                }
            }
        };
        MapIterate.forEachKeyValue(map, procedure);
        return resultMap;
    }

    /**
     * For each <em>value</em> of the source map, the Predicate is evaluated.
     * If the result of the evaluation is true, the map entry is moved to a result map.
     * The result map is returned containing all entries in the source map that evaluated to true.
     */
    public static <K, V> MutableMap<K, V> selectMapOnValue(Map<K, V> map, final Predicate<? super V> predicate)
    {
        MutableMap<K, V> resultMap = UnifiedMap.newMap();
        final Procedure2<K, V> mapTransferProcedure = new MapPutProcedure<K, V>(resultMap);
        Procedure2<K, V> procedure = new Procedure2<K, V>()
        {
            public void value(K key, V value)
            {
                if (predicate.accept(value))
                {
                    mapTransferProcedure.value(key, value);
                }
            }
        };
        MapIterate.forEachKeyValue(map, procedure);
        return resultMap;
    }

    /**
     * @see Iterate#reject(Iterable, Predicate)
     */
    public static <K, V> MutableList<V> reject(Map<K, V> map, Predicate<? super V> predicate)
    {
        return MapIterate.reject(map, predicate, FastList.<V>newList());
    }

    /**
     * @see Iterate#reject(Iterable, Predicate, Collection)
     */
    public static <K, V, R extends Collection<V>> R reject(
            Map<K, V> map,
            Predicate<? super V> predicate,
            R targetCollection)
    {
        Procedure<V> procedure = new RejectProcedure<V>(predicate, targetCollection);
        MapIterate.forEachValue(map, procedure);
        return targetCollection;
    }

    /**
     * For each <em>value</em> of the map, predicate is evaluated with the element as the parameter.
     * Each element which causes predicate to evaluate to false is included in the new collection.
     */
    public static <K, V> MutableMap<K, V> rejectMapOnEntry(
            Map<K, V> map,
            Predicate2<? super K, ? super V> predicate)
    {
        return MapIterate.rejectMapOnEntry(map, predicate, UnifiedMap.<K, V>newMap());
    }

    /**
     * For each <em>value</em> of the map, predicate is evaluated with the element as the parameter.
     * Each element which causes predicate to evaluate to false is added to the targetCollection.
     */
    public static <K, V, R extends Map<K, V>> R rejectMapOnEntry(
            Map<K, V> map,
            final Predicate2<? super K, ? super V> predicate,
            final R target)
    {
        MapIterate.forEachKeyValue(map, new Procedure2<K, V>()
        {
            public void value(K argument1, V argument2)
            {
                if (!predicate.accept(argument1, argument2))
                {
                    target.put(argument1, argument2);
                }
            }
        });

        return target;
    }

    /**
     * Adds all the <em>keys</em> from map to a the specified targetCollection.
     */
    public static <K, V> Collection<K> addAllKeysTo(Map<K, V> map, Collection<K> targetCollection)
    {
        MapIterate.forEachKey(map, CollectionAddProcedure.<K>on(targetCollection));
        return targetCollection;
    }

    /**
     * Adds all the <em>values</em> from map to a the specified targetCollection.
     */
    public static <K, V> Collection<V> addAllValuesTo(Map<K, V> map, Collection<V> targetCollection)
    {
        MapIterate.forEachValue(map, CollectionAddProcedure.<V>on(targetCollection));
        return targetCollection;
    }

    /**
     * @see Iterate#collect(Iterable, Function)
     */
    public static <K, V, A> MutableList<A> collect(
            Map<K, V> map,
            Function<? super V, ? extends A> function)
    {
        return collect(map, function, FastList.<A>newList(map.size()));
    }

    /**
     * For each value of the map, the function is evaluated with the key and value as the parameter.
     * The results of these evaluations are collected into a new UnifiedMap.
     */
    public static <K, V, K2, V2> MutableMap<K2, V2> collect(
            Map<K, V> map,
            Function2<? super K, ? super V, Pair<K2, V2>> function)
    {
        return MapIterate.collect(map, function, UnifiedMap.<K2, V2>newMap(map.size()));
    }

    /**
     * For each value of the map, the function is evaluated with the key and value as the parameter.
     * The results of these evaluations are collected into the target map.
     */
    public static <K1, V1, K2, V2, R extends Map<K2, V2>> R collect(
            Map<K1, V1> map,
            final Function2<? super K1, ? super V1, Pair<K2, V2>> function,
            final R target)
    {
        MapIterate.forEachKeyValue(map, new Procedure2<K1, V1>()
        {
            public void value(K1 key, V1 value)
            {
                Pair<K2, V2> pair = function.value(key, value);
                target.put(pair.getOne(), pair.getTwo());
            }
        });
        return target;
    }

    /**
     * For each key and value of the map, the function is evaluated with the key and value as the parameter.
     * The results of these evaluations are collected into the target map.
     */
    public static <K, V, V2> MutableMap<K, V2> collectValues(
            Map<K, V> map,
            Function2<? super K, ? super V, ? extends V2> function)
    {
        return MapIterate.collectValues(map, function, UnifiedMap.<K, V2>newMap(map.size()));
    }

    /**
     * For each key and value of the map, the function is evaluated with the key and value as the parameter.
     * The results of these evaluations are collected into the target map.
     */
    public static <K, V, V2, R extends Map<K, V2>> R collectValues(
            Map<K, V> map,
            final Function2<? super K, ? super V, ? extends V2> function,
            final R target)
    {
        MapIterate.forEachKeyValue(map, new Procedure2<K, V>()
        {
            public void value(K key, V value)
            {
                target.put(key, function.value(key, value));
            }
        });

        return target;
    }

    /**
     * For each value of the map, the Predicate2 is evaluated with the key and value as the parameter,
     * and if true, then {@code function} is applied.
     * The results of these evaluations are collected into a new map.
     */
    public static <K1, V1, K2, V2> MutableMap<K2, V2> collectIf(
            Map<K1, V1> map,
            Function2<? super K1, ? super V1, Pair<K2, V2>> function,
            Predicate2<? super K1, ? super V1> predicate)
    {
        return MapIterate.collectIf(map, function, predicate, UnifiedMap.<K2, V2>newMap());
    }

    /**
     * For each value of the map, the Predicate2 is evaluated with the key and value as the parameter,
     * and if true, then {@code function} is applied.
     * The results of these evaluations are collected into the target map.
     */
    public static <K1, V1, K2, V2> MutableMap<K2, V2> collectIf(
            Map<K1, V1> map,
            final Function2<? super K1, ? super V1, Pair<K2, V2>> function,
            final Predicate2<? super K1, ? super V1> predicate,
            Map<K2, V2> target)
    {
        final MutableMap<K2, V2> result = MapAdapter.adapt(target);

        MapIterate.forEachKeyValue(map, new Procedure2<K1, V1>()
        {
            public void value(K1 key, V1 value)
            {
                if (predicate.accept(key, value))
                {
                    Pair<K2, V2> pair = function.value(key, value);
                    result.put(pair.getOne(), pair.getTwo());
                }
            }
        });

        return result;
    }

    /**
     * For each key-value entry of a map, applies a function to each, and adds the transformed entry to a new Map.
     */
    public static <K1, V1, K2, V2> MutableMap<K2, V2> collect(
            Map<K1, V1> map,
            Function<? super K1, ? extends K2> keyFunction,
            Function<? super V1, ? extends V2> valueFunction)
    {
        return MapIterate.collect(map, keyFunction, valueFunction, UnifiedMap.<K2, V2>newMap());
    }

    /**
     * For each key-value entry of a map, applies a function to each, and adds the transformed entry to the target Map.
     */
    public static <K1, V1, K2, V2> MutableMap<K2, V2> collect(
            Map<K1, V1> map,
            final Function<? super K1, ? extends K2> keyFunction,
            final Function<? super V1, ? extends V2> valueFunction,
            Map<K2, V2> target)
    {
        return MapIterate.collect(map, new Function2<K1, V1, Pair<K2, V2>>()
        {
            public Pair<K2, V2> value(K1 key, V1 value)
            {
                return Tuples.pair(keyFunction.valueOf(key), valueFunction.valueOf(value));
            }
        }, MapAdapter.adapt(target));
    }

    /**
     * @see Iterate#collect(Iterable, Function, Collection)
     */
    public static <K, V, A, R extends Collection<A>> R collect(
            Map<K, V> map,
            Function<? super V, ? extends A> function,
            R targetCollection)
    {
        Procedure<V> procedure = new CollectProcedure<V, A>(function, targetCollection);
        MapIterate.forEachValue(map, procedure);
        return targetCollection;
    }

    /**
     * For each value of the map, {@code procedure} is evaluated with the value as the parameter.
     */
    public static <K, V> void forEachValue(Map<K, V> map, Procedure<? super V> procedure)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot perform a forEachValue on null");
        }

        if (MapIterate.notEmpty(map))
        {
            if (map instanceof UnsortedMapIterable)
            {
                ((MapIterable<K, V>) map).forEachValue(procedure);
            }
            else
            {
                IterableIterate.forEach(map.values(), procedure);
            }
        }
    }

    /**
     * For each key of the map, {@code procedure} is evaluated with the key as the parameter.
     */
    public static <K, V> void forEachKey(Map<K, V> map, Procedure<? super K> procedure)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot perform a forEachKey on null");
        }

        if (MapIterate.notEmpty(map))
        {
            if (map instanceof UnsortedMapIterable)
            {
                ((MapIterable<K, V>) map).forEachKey(procedure);
            }
            else
            {
                IterableIterate.forEach(map.keySet(), procedure);
            }
        }
    }

    /**
     * For each entry of the map, {@code procedure} is evaluated with the element as the parameter.
     */
    public static <K, V> void forEachKeyValue(Map<K, V> map, Procedure2<? super K, ? super V> procedure)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot perform a forEachKeyValue on null");
        }

        if (MapIterate.notEmpty(map))
        {
            if (map instanceof UnsortedMapIterable)
            {
                ((MapIterable<K, V>) map).forEachKeyValue(procedure);
            }
            else
            {
                IterableIterate.forEach(map.entrySet(), new MapEntryToProcedure2<K, V>(procedure));
            }
        }
    }

    public static <K, V> Pair<K, V> detect(
            Map<K, V> map,
            final Predicate2<? super K, ? super V> predicate)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot perform a detect on null");
        }

        if (map instanceof ImmutableMap || map instanceof MutableMap)
        {
            RichIterable<Pair<K, V>> entries;
            if (map instanceof ImmutableMap)
            {
                entries = ((ImmutableMap<K, V>) map).keyValuesView();
            }
            else
            {
                entries = LazyIterate.adapt(map.entrySet()).collect(AbstractImmutableEntry.<K, V>getPairFunction());
            }
            return entries.detect(new Predicate<Pair<K, V>>()
            {
                public boolean accept(Pair<K, V> each)
                {
                    return predicate.accept(each.getOne(), each.getTwo());
                }
            });
        }

        for (Map.Entry<K, V> entry : map.entrySet())
        {
            if (predicate.accept(entry.getKey(), entry.getValue()))
            {
                return Tuples.pairFrom(entry);
            }
        }
        return null;
    }

    /**
     * @see Iterate#detect(Iterable, Predicate)
     */
    public static <K, V> V detect(Map<K, V> map, Predicate<? super V> predicate)
    {
        return IterableIterate.detect(map.values(), predicate);
    }

    /**
     * @see Iterate#detectIfNone(Iterable, Predicate, Object)
     */
    public static <K, V> V detectIfNone(Map<K, V> map, Predicate<? super V> predicate, V ifNone)
    {
        return Iterate.detectIfNone(map.values(), predicate, ifNone);
    }

    /**
     * @see Iterate#injectInto(Object, Iterable, Function2)
     */
    public static <K, V, IV> IV injectInto(
            IV injectValue,
            Map<K, V> map,
            Function2<? super IV, ? super V, ? extends IV> function)
    {
        return Iterate.injectInto(injectValue, map.values(), function);
    }

    /**
     * Same as {@link #injectInto(Object, Map, Function2)}, but only applies the value to the function
     * if the predicate returns true for the value.
     *
     * @see #injectInto(Object, Map, Function2)
     */
    public static <IV, K, V> IV injectIntoIf(
            IV initialValue,
            Map<K, V> map,
            final Predicate<? super V> predicate,
            final Function2<? super IV, ? super V, ? extends IV> function)
    {
        Function2<IV, ? super V, IV> ifFunction = new Function2<IV, V, IV>()
        {
            public IV value(IV accumulator, V item)
            {
                if (predicate.accept(item))
                {
                    return function.value(accumulator, item);
                }
                return accumulator;
            }
        };
        return Iterate.injectInto(initialValue, map.values(), ifFunction);
    }

    /**
     * @see Iterate#anySatisfy(Iterable, Predicate)
     */
    public static <K, V> boolean anySatisfy(Map<K, V> map, Predicate<? super V> predicate)
    {
        return IterableIterate.anySatisfy(map.values(), predicate);
    }

    /**
     * @see Iterate#allSatisfy(Iterable, Predicate)
     */
    public static <K, V> boolean allSatisfy(Map<K, V> map, Predicate<? super V> predicate)
    {
        return IterableIterate.allSatisfy(map.values(), predicate);
    }

    /**
     * Iterate over the specified map applying the specified Function to each value
     * and return the results as a List.
     */
    public static <K, V> MutableList<Pair<K, V>> toListOfPairs(Map<K, V> map)
    {
        final MutableList<Pair<K, V>> pairs = FastList.newList(map.size());
        MapIterate.forEachKeyValue(map, new Procedure2<K, V>()
        {
            public void value(K key, V value)
            {
                pairs.add(Tuples.pair(key, value));
            }
        });
        return pairs;
    }

    /**
     * Iterate over the specified map applying the specified Function to each value
     * and return the results as a sorted List using the specified Comparator.
     */
    public static <K, V> MutableList<V> toSortedList(
            Map<K, V> map,
            Comparator<? super V> comparator)
    {
        return Iterate.toSortedList(map.values(), comparator);
    }

    /**
     * Return a new map swapping key-value for value-key.
     * If the original map contains entries with the same value, the result mapping is undefined,
     * in that the last entry applied wins (the order of application is undefined).
     */
    public static <K, V> MutableMap<V, K> reverseMapping(Map<K, V> map)
    {
        final MutableMap<V, K> reverseMap = UnifiedMap.newMap(map.size());
        MapIterate.forEachKeyValue(map, new Procedure2<K, V>()
        {
            public void value(K sourceKey, V sourceValue)
            {
                reverseMap.put(sourceValue, sourceKey);
            }
        });
        return reverseMap;
    }

    /**
     * Return the number of occurrences of object in the specified map.
     */
    public static <K, V> int occurrencesOf(Map<K, V> map, V object)
    {
        return Iterate.count(map.values(), Predicates.equal(object));
    }

    /**
     * Return the number of occurrences where object is equal to the specified attribute in the specified map.
     */
    public static <K, V, A> int occurrencesOfAttribute(
            Map<K, V> map,
            Function<? super V, ? extends A> function,
            A object)
    {
        return Iterate.count(map.values(), Predicates.attributeEqual(function, object));
    }
}

