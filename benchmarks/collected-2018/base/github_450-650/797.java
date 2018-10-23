// https://searchcode.com/api/result/93220292/

package nz.net.dnh.mapstream;

import static java.util.function.Function.identity;
import static nz.net.dnh.mapstream.MapStreamHelpers.distinctPredicate;
import static nz.net.dnh.mapstream.MapStreamHelpers.entryConsumer;
import static nz.net.dnh.mapstream.MapStreamHelpers.entryFunction;
import static nz.net.dnh.mapstream.MapStreamHelpers.entryPredicate;
import static nz.net.dnh.mapstream.MapStreamHelpers.keyBiFunction;
import static nz.net.dnh.mapstream.MapStreamHelpers.mappedPredicate;
import static nz.net.dnh.mapstream.MapStreamHelpers.valueBiFunction;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * An equivalent of {@link Stream} that operates on key-value pairs, e.g. from a {@link Map}.
 * <p>
 * To obtain a {@link MapStream} from a {@link Map} or a {@link Stream}, use {@link MapStream#of(Map)},
 * {@link MapStream#of(Stream, Function)}, or {@link MapStream#of(Stream, Function, Function)}.
 * <p>
 * To implement {@link MapStream}, only {@link #entryStream()} must be implemented. All other methods have appropriate default
 * implementations.
 * 
 * @see MultimapStream Obtaining a {@link MapStream} from a multimap
 */
@FunctionalInterface
public interface MapStream<K, V> {
	/** Return a new {@link MapStream} based on the entries from the given map */
	public static <K, V> MapStream<K, V> of(final Map<K, V> map) {
		return map.entrySet()::stream;
	}

	/**
	 * Return a new {@link MapStream} based on the elements from the given stream. Entries in the {@link MapStream} are the result of
	 * applying the given mapping functions to elements from the given stream.
	 * <p>
	 * The given stream should not be used after creating a {@link MapStream} from it.
	 * 
	 * @param stream
	 *            The stream to transform into a {@link MapStream}
	 * @param keyFunction
	 *            A mapping function to produce keys
	 * @param valueFunction
	 *            A mapping function to produce values
	 */
	public static <T, K, V> MapStream<K, V> of(final Stream<T> stream, final Function<T, K> keyFunction, final Function<T, V> valueFunction) {
		return of(stream, v -> new SimpleImmutableEntry<>(keyFunction.apply(v), valueFunction.apply(v)));
	}

	/**
	 * Return a new {@link MapStream} based on the elements from the given stream. Entries in the {@link MapStream} are the result of
	 * applying the given mapping function to elements from the given stream.
	 * <p>
	 * The given stream should not be used after creating a {@link MapStream} from it.
	 * 
	 * @param stream
	 *            The stream to transform into a {@link MapStream}
	 * @param entryFunction
	 *            A mapping function to produce entries
	 */
	public static <T, K, V> MapStream<K, V> of(final Stream<T> stream, final Function<T, Entry<K, V>> entryFunction) {
		return () -> stream.map(entryFunction);
	}

	/** @return the stream of entries (key-value pairs) for this MapStream */
	Stream<Entry<K, V>> entryStream();

	/** @return a stream of the keys from this MapStream */
	default Stream<K> keyStream() {
		return entryStream().map(Entry::getKey);
	}

	/** @return a stream of the values from this MapStream */
	default Stream<V> valueStream() {
		return entryStream().map(Entry::getValue);
	}

	/**
	 * Return a MapStream consisting of the elements from this MapStream which match the given predicate
	 * 
	 * @param predicate
	 *            A predicate to apply to each key-value pair to determine whether the entry should be in the returned MapStream
	 * @see Stream#filter(Predicate)
	 */
	default MapStream<K, V> filter(final BiPredicate<? super K, ? super V> predicate) {
		return () -> entryStream().filter(entryPredicate(predicate));
	}

	/**
	 * Return a MapStream consisting of the elements from this MapStream whose keys match the given predicate
	 * 
	 * @param predicate
	 *            A predicate to apply to each key to determine whether the key's entry should be in the returned MapStream
	 * @see Stream#filter(Predicate)
	 */
	default MapStream<K, V> filterKeys(final Predicate<? super K> predicate) {
		return () -> entryStream().filter(mappedPredicate(Entry::getKey, predicate));
	}

	/**
	 * Return a MapStream consisting of the elements from this MapStream whose values match the given predicate
	 * 
	 * @param predicate
	 *            A predicate to apply to each value to determine whether the value's entry should be in the returned MapStream
	 * @see Stream#filter(Predicate)
	 */
	default MapStream<K, V> filterValues(final Predicate<? super V> predicate) {
		return () -> entryStream().filter(mappedPredicate(Entry::getValue, predicate));
	}

	/**
	 * Return a stream consisting of the results of applying the given function to the key-value pairs of this MapStream.
	 * <p>
	 * NB: This flattens this MapStream into a {@link Stream}.
	 * 
	 * @param mapper
	 *            A function to apply to each key-value pair; the return values of this function will be in the returned stream
	 * @see Stream#map(Function)
	 */
	default <R> Stream<R> map(final BiFunction<? super K, ? super V, ? extends R> mapper) {
		return entryStream().map(entryFunction(mapper));
	}

	/**
	 * Return a MapStream whose keys are the result of applying the given function to the keys of this MapStream.
	 * <p>
	 * The values of the entries will remain unchanged.
	 * 
	 * @param mapper
	 *            A function to apply to each key; the return values of this function will be used as the keys of the returned MapStream
	 * @see Stream#map(Function)
	 */
	default <K2> MapStream<K2, V> mapKeys(final Function<? super K, ? extends K2> mapper) {
		return map(mapper, identity());
	}

	/**
	 * Return a MapStream whose keys are the result of applying the given function to the key-value pairs of this MapStream.
	 * <p>
	 * The values of the entries will remain unchanged.
	 * 
	 * @param mapper
	 *            A function to apply to each key-value pair; the return values of this function will be used as the keys of the returned
	 *            MapStream
	 * @see Stream#map(Function)
	 */
	default <K2> MapStream<K2, V> mapKeys(final BiFunction<? super K, ? super V, ? extends K2> mapper) {
		return map(mapper, valueBiFunction());
	}

	/**
	 * Return a MapStream whose values are the result of applying the given function to the values of this MapStream.
	 * <p>
	 * The keys of the entries will remain unchanged.
	 * 
	 * @param mapper
	 *            A function to apply to each value; the return values of this function will be used as the values of the returned MapStream
	 * @see Stream#map(Function)
	 */
	default <V2> MapStream<K, V2> mapValues(final Function<? super V, ? extends V2> mapper) {
		return map(identity(), mapper);
	}

	/**
	 * Return a MapStream whose values are the result of applying the given function to the key-value pairs of this MapStream.
	 * <p>
	 * The keys of the entries will remain unchanged.
	 * 
	 * @param mapper
	 *            A function to apply to each key-value pair; the return values of this function will be used as the values of the returned
	 *            MapStream
	 * @see Stream#map(Function)
	 */
	default <V2> MapStream<K, V2> mapValues(final BiFunction<? super K, ? super V, ? extends V2> mapper) {
		return map(keyBiFunction(), mapper);
	}

	/**
	 * Return a MapStream whose entries are the result of applying the given functions to the keys and values of this MapStream.
	 * 
	 * @param keyMapper
	 *            A function to apply to each key; the return values of this function will be used as the keys of the returned MapStream
	 * @param valueMapper
	 *            A function to apply to each value; the return values of this function will be used as the values of the returned MapStream
	 * @see Stream#map(Function)
	 */
	default <K2, V2> MapStream<K2, V2> map(final Function<? super K, ? extends K2> keyMapper,
			final Function<? super V, ? extends V2> valueMapper) {
		return map((k, v) -> keyMapper.apply(k), (k, v) -> valueMapper.apply(v));
	}

	/**
	 * Return a MapStream whose entries are the result of applying the given functions to the key-value pairs of this MapStream.
	 * 
	 * @param keyMapper
	 *            A function to apply to each key-value pair; the return values of this function will be used as the keys of the returned
	 *            MapStream
	 * @param valueMapper
	 *            A function to apply to each key-value pair; the return values of this function will be used as the values of the returned
	 *            MapStream
	 * @see Stream#map(Function)
	 */
	default <K2, V2> MapStream<K2, V2> map(final BiFunction<? super K, ? super V, ? extends K2> keyMapper,
			final BiFunction<? super K, ? super V, ? extends V2> valueMapper) {
		return () -> entryStream().map(
				e -> new SimpleImmutableEntry<>(keyMapper.apply(e.getKey(), e.getValue()), valueMapper.apply(e.getKey(), e.getValue())));
	}

	/**
	 * Return a MapStream consisting of the distinct key-value pairs of this MapStream.
	 * <p>
	 * Key-value pairs are considered equal if their keys and values both compare as {@link Object#equals(Object) equal}. Key-value pairs
	 * with the same key but different values, or the same value but different keys, are not considered equal. This means that this method
	 * can return a MapStream containing duplicate entries for a single key.
	 * 
	 * @see #distinctKeys() Retrieving a MapStream with distinct keys
	 * @see #distinctValues() Retrieving a MapStream with distinct values
	 * @see Stream#distinct()
	 */
	default MapStream<K, V> distinct() {
		return () -> entryStream().distinct();
	}

	/**
	 * Return a MapStream consisting of the key-value pairs of this MapStream with distinct keys.
	 * <p>
	 * If multiple key-value pairs share the same key (determined by {@link Object#equals(Object)}), only the first key-value pair
	 * encountered will be present in the returned MapStream.
	 * <p>
	 * Note that this operation internally uses a {@link ConcurrentHashMap} and may not be as efficient as {@link #distinct()}.
	 * 
	 * @see Stream#distinct()
	 */
	default MapStream<K, V> distinctKeys() {
		return filterKeys(distinctPredicate());
	}

	/**
	 * Return a MapStream consisting of the key-value pairs of this MapStream with distinct values.
	 * <p>
	 * If multiple key-value pairs share the same value (determined by {@link Object#equals(Object)}), only the first key-value pair
	 * encountered will be present in the returned MapStream.
	 * <p>
	 * Note that this operation internally uses a {@link ConcurrentHashMap} and may not be as efficient as {@link #distinct()}.
	 * 
	 * @see Stream#distinct()
	 */
	default MapStream<K, V> distinctValues() {
		return filterValues(distinctPredicate());
	}

	/**
	 * Return a MapStream consisting of the entries from this MapStream sorted according to the natural order of the keys.
	 * <p>
	 * If the keys of this MapStream are not {@code Comparable}, a {@code java.lang.ClassCastException} may be thrown when the terminal
	 * operation is executed.
	 * 
	 * @see Stream#sorted()
	 */
	@SuppressWarnings("unchecked")
	default MapStream<K, V> sortedKeys() {
		return sortedKeys((Comparator<? super K>) Comparator.naturalOrder());
	}

	/**
	 * Return a MapStream consisting of the entries from this MapStream sorted by their keys according to the given comparator
	 * 
	 * @see Stream#sorted(Comparator)
	 */
	default MapStream<K, V> sortedKeys(final Comparator<? super K> comparator) {
		return () -> entryStream().sorted(Entry.comparingByKey(comparator));
	}

	/**
	 * Return a MapStream consisting of the entries from this MapStream sorted according to the natural order of the values.
	 * <p>
	 * If the values of this MapStream are not {@code Comparable}, a {@code java.lang.ClassCastException} may be thrown when the terminal
	 * operation is executed.
	 * 
	 * @see Stream#sorted()
	 */
	@SuppressWarnings("unchecked")
	default MapStream<K, V> sortedValues() {
		return sortedValues((Comparator<? super V>) Comparator.naturalOrder());
	}

	/**
	 * Return a MapStream consisting of the entries from this MapStream sorted by their values according to the given comparator
	 * 
	 * @see Stream#sorted(Comparator)
	 */
	default MapStream<K, V> sortedValues(final Comparator<? super V> comparator) {
		return () -> entryStream().sorted(Entry.comparingByValue(comparator));
	}

	/**
	 * Return a MapStream consisting of the entries from this MapStream, additionally performing the given action on each key-value pair as
	 * elements are consumed from the resulting stream
	 * 
	 * @param action
	 *            The action to perform on the elements as elements are consumed from the resulting stream
	 * @see Stream#peek(Consumer)
	 */
	default MapStream<K, V> peek(final BiConsumer<? super K, ? super V> action) {
		return () -> entryStream().peek(entryConsumer(action));
	}

	/**
	 * Return a MapStream consisting of the entries from this MapStream, additionally performing the given action on each key as elements
	 * are consumed from the resulting stream
	 * 
	 * @param action
	 *            The action to perform on the keys as elements are consumed from the resulting stream
	 * @see Stream#peek(Consumer)
	 */
	default MapStream<K, V> peekKeys(final Consumer<? super K> action) {
		return () -> entryStream().peek(e -> action.accept(e.getKey()));
	}

	/**
	 * Return a MapStream consisting of the entries from this MapStream, additionally performing the given action on each value as elements
	 * are consumed from the resulting stream
	 * 
	 * @param action
	 *            The action to perform on the values as elements are consumed from the resulting stream
	 * @see Stream#peek(Consumer)
	 */
	default MapStream<K, V> peekValues(final Consumer<? super V> action) {
		return () -> entryStream().peek(e -> action.accept(e.getValue()));
	}

	/**
	 * Return a MapStream consisting of the entries of this MapStream, truncated to be no longer than maxSize in length.
	 * 
	 * @param maxSize
	 *            The number of entries the stream should be limited to
	 * @see Stream#limit(long)
	 */
	default MapStream<K, V> limit(final long maxSize) {
		return () -> entryStream().limit(maxSize);
	}

	/**
	 * Return a MapStream consisting of the remaining entries of this MapStream after discarding the first n entries
	 * 
	 * @param n
	 *            The number of leading entries to skip
	 * @see Stream#skip(long)
	 */
	default MapStream<K, V> skip(final long n) {
		return () -> entryStream().skip(n);
	}

	/**
	 * Returns whether this MapStream would execute terminal operations in parallel.
	 * 
	 * @see BaseStream#isParallel()
	 */
	default boolean isParallel() {
		return entryStream().isParallel();
	}

	/**
	 * Returns an equivalent MapStream that is sequential.
	 *
	 * @return a sequential stream
	 * @see BaseStream#sequential()
	 * @see #parallel()
	 */
	default MapStream<K, V> sequential() {
		return entryStream()::sequential;
	}

	/**
	 * Returns an equivalent MapStream that is parallel.
	 *
	 * @return a parallel stream
	 * @see BaseStream#parallel()
	 * @see #sequential()
	 */
	default MapStream<K, V> parallel() {
		return entryStream()::parallel;
	}

	/**
	 * Returns an equivalent MapStream that is unordered.
	 * 
	 * @see BaseStream#unordered()
	 */
	default MapStream<K, V> unordered() {
		return entryStream()::unordered;
	}

	/**
	 * Returns an equivalent stream with an additional close handler. Close handlers are run when the {@link #close()} method is called on
	 * the stream, and are executed in the order they were added.
	 *
	 * @param closeHandler
	 *            A task to execute when the stream is closed
	 * @see BaseStream#onClose(Runnable)
	 */
	default MapStream<K, V> onClose(final Runnable closeHandler) {
		return () -> entryStream().onClose(closeHandler);
	}

	/**
	 * Closes this stream, causing all close handlers for this stream pipeline to be called.
	 *
	 * @see BaseStream#close()
	 */
	default void close() {
		entryStream().close();
	}

	// Terminal operations

	/**
	 * Perform an action on each key-value pair of this MapStream.
	 * <p>
	 * This is a terminal operation.
	 * 
	 * @param action
	 *            The action to perform on the key-value pairs
	 * @see Stream#forEach(Consumer)
	 */
	default void forEach(final BiConsumer<? super K, ? super V> action) {
		entryStream().forEach(entryConsumer(action));
	}

	/**
	 * Perform an action on each key-value pair of this MapStream, in the encounter order of the stream if the stream has a defined
	 * encounter order.
	 * <p>
	 * This is a terminal operation.
	 * 
	 * @param action
	 *            The action to perform on the key-value pairs
	 * @see Stream#forEachOrdered(Consumer)
	 */
	default void forEachOrdered(final BiConsumer<? super K, ? super V> action) {
		entryStream().forEachOrdered(entryConsumer(action));
	}

	/**
	 * Return the count of the entries in this MapStream.
	 * <p>
	 * This is a terminal operation.
	 * 
	 * @see Stream#count()
	 */
	default long count() {
		return entryStream().count();
	}

	/**
	 * Return whether any key-value pairs of this MapStream match the provided predicate.
	 * <p>
	 * This is a terminal operation.
	 * 
	 * @param predicate
	 *            A predicate to apply to the key-value pairs of this stream
	 * @see Stream#anyMatch(Predicate)
	 */
	default boolean anyMatch(final BiPredicate<? super K, ? super V> predicate) {
		return entryStream().anyMatch(entryPredicate(predicate));
	}

	/**
	 * Return whether all key-value pairs of this MapStream match the provided predicate.
	 * <p>
	 * This is a terminal operation.
	 * 
	 * @param predicate
	 *            A predicate to apply to the key-value pairs of this stream
	 * @see Stream#allMatch(Predicate)
	 */
	default boolean allMatch(final BiPredicate<? super K, ? super V> predicate) {
		return entryStream().allMatch(entryPredicate(predicate));
	}

	/**
	 * Return whether no key-value pairs of this MapStream match the provided predicate.
	 * <p>
	 * This is a terminal operation.
	 * 
	 * @param predicate
	 *            A predicate to apply to the key-value pairs of this stream
	 * @see Stream#noneMatch(Predicate)
	 */
	default boolean noneMatch(final BiPredicate<? super K, ? super V> predicate) {
		return entryStream().noneMatch(entryPredicate(predicate));
	}

	/**
	 * Perform a mutable reduction operation on the entries of the MapStream using a Collector.
	 * <p>
	 * This is a convenience method for the most common collect operation. For other collect/reduce operations on key-value pairs, use
	 * {@link #entryStream()} and then use the collect/reduce methods on {@link Stream}. For collect/reduce operations on keys or values,
	 * call {@link #keyStream()} or {@link #valueStream()} and then use the collect/reduce methods on {@link Stream}.
	 * <p>
	 * This is a terminal operation.
	 * 
	 * @param collector
	 *            The {@link Collector} describing the reduction
	 * @return The result of the reduction
	 * @see EntryCollectors#toMap()
	 * @see MultimapStream#toMultimap()
	 * @see Stream#collect(Collector)
	 */
	default <R, A> R collect(final Collector<? super Entry<K, V>, A, R> collector) {
		return entryStream().collect(collector);
	}

	/**
	 * Returns an iterator for the elements of this stream.
	 * <p>
	 * This is a terminal operation.
	 */
	default Iterator<Entry<K, V>> iterator() {
		return entryStream().iterator();
	}

	/**
	 * Returns a spliterator for the elements of this stream.
	 * <p>
	 * This is a terminal operation.
	 */
	default Spliterator<Entry<K, V>> spliterator() {
		return entryStream().spliterator();
	}

	/** Subclass of MapStream for use in try-with-resources blocks */
	interface CloseableMapStream<K, V> extends MapStream<K, V>, AutoCloseable {
		@Override
		default void close() {
			MapStream.super.close();
		}
	}

	/**
	 * Return an equivalent stream that implements {@link AutoCloseable}, for use in try-with-resources blocks
	 * <p>
	 * The main {@link MapStream} class doesn't extend {@link AutoCloseable} to avoid superfluous "resource leak" warnings.
	 * <p>
	 * NB: {@link #onClose(Runnable) onClose} handlers added after this method call will not be called when close is called. Ensure all
	 * handlers are added before this method is called
	 */
	default CloseableMapStream<K, V> autoCloseable() {
		return this::entryStream;
	}
}

