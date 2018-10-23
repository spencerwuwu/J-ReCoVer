// https://searchcode.com/api/result/59403497/

package net.ninjacat.collections;

import net.ninjacat.functions.F;
import net.ninjacat.functions.F2;
import net.ninjacat.functions.Predicate;
import net.ninjacat.functions.Promise;
import org.junit.Test;

import java.util.*;

import static net.ninjacat.collections.IterFixtures.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LazyIterTest {

    @Test
    public void toListShouldReturnOriginalList() throws Exception {
        List<String> original = Arrays.asList("1", "1", "Last");
        List<String> result = LazyIter.of(original).toList();

        assertThat(result, is(original));
    }

    @Test
    public void toSetShouldReturnOnlyUniqueElements() throws Exception {
        List<String> original = Arrays.asList("1", "1", "Last");
        Set<String> result = LazyIter.of(original).toSet();

        assertThat(result, is(Collect.setOf("1", "Last")));
    }

    @Test
    public void mapShouldTransformValues() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2, 3));
        Iterable<Integer> transformed = iter.map(new F<Integer, Integer>() {
            @Override
            public Integer apply(Integer integer) {
                return integer * 2;
            }
        });
        Iterator<Integer> iterator = transformed.iterator();
        verifyNext(iterator, 2);
        verifyNext(iterator, 4);
        verifyNext(iterator, 6);
        verifyNoNext(iterator);
    }

    @Test
    public void filterShouldReturnOddValues() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2, 3));
        Iterable<Integer> transformed = iter.filter(new Predicate<Integer>() {
            @Override
            public boolean matches(Integer integer) {
                return integer % 2 != 0;
            }
        });
        Iterator<Integer> iterator = transformed.iterator();
        verifyNext(iterator, 1);
        verifyNext(iterator, 3);
        verifyNoNext(iterator);
    }

    @Test
    public void reduceShouldFoldLeft() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2, 3, 4));

        Promise<Integer> result = iter.reduce(0, new F2<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer integer, Integer integer2) {
                return integer + integer2;
            }
        });

        assertThat(result.get(), is(10));
    }

    @Test
    public void reduceShouldBeLazy() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2));

        final SideEffect sideEffect = new SideEffect();

        iter.reduce(0, new F2<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer integer, Integer integer2) {
                sideEffect.sideEffect();
                return 0;
            }
        });

        assertThat(sideEffect.hasSideEffects(), is(false));
    }

    @Test
    public void findShouldBeAbleToLocateElement() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2, 3, 4));

        Promise<Integer> result = iter.find(new Predicate<Integer>() {
            @Override
            public boolean matches(Integer integer) {
                return integer.equals(3);
            }
        }, -1);

        assertThat(result.get(), is(3));
    }

    @Test
    public void findShouldReturnDefaultValueIfElementNotFound() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2, 3, 4));

        Promise<Integer> result = iter.find(new Predicate<Integer>() {
            @Override
            public boolean matches(Integer integer) {
                return integer.equals(5);
            }
        }, -1);

        assertThat(result.get(), is(-1));
    }

    @Test
    public void findShouldBeLazy() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2));

        final SideEffect sideEffect = new SideEffect();

        iter.find(new Predicate<Integer>() {
            @Override
            public boolean matches(Integer integer) {
                sideEffect.sideEffect();
                return integer.equals(3);
            }
        }, -1);

        assertThat(sideEffect.hasSideEffects(), is(false));
    }

    @Test
    public void headShouldReturnFirstElement() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2));

        assertThat(iter.head(), is(1));
    }

    @Test(expected = NoSuchElementException.class)
    public void headShouldFailOnEmptyCollection() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(new ArrayList<Integer>());

        iter.head();
    }

    @Test
    public void tailShouldReturnAllButFirstElement() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1, 2, 3));
        Iterator<Integer> tail = iter.tail().iterator();

        verifyNext(tail, 2);
        verifyNext(tail, 3);
        verifyNoNext(tail);
    }

    @Test
    public void tailShouldReturnEmptyIterableForOneItemCollection() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(Arrays.asList(1));
        Iterator<Integer> tail = iter.tail().iterator();

        verifyNoNext(tail);
    }

    @Test(expected = NoSuchElementException.class)
    public void tailShouldFailOnEmptyCollection() throws Exception {
        LazyIter<Integer> iter = LazyIter.of(new ArrayList<Integer>());

        iter.tail();
    }

    @Test
    public void anyShouldReturnTrueIfAnyMatchingElementFound() throws Exception {
        LazyIter<String> iter = LazyIter.of("First", "Middle", "Last");

        Promise<Boolean> promise = iter.any(new Predicate<String>() {
            @Override
            public boolean matches(String o) {
                return o.startsWith("M");
            }
        });

        assertThat(promise.get(), is(true));
    }

    @Test
    public void anyShouldReturnFalseIfNoMatchingElementFound() throws Exception {
        LazyIter<String> iter = LazyIter.of("First", "Middle", "Last");

        Promise<Boolean> promise = iter.any(new Predicate<String>() {
            @Override
            public boolean matches(String o) {
                return o.startsWith("S");
            }
        });

        assertThat(promise.get(), is(false));
    }

    @Test
    public void anyShouldBeLazy() throws Exception {
        LazyIter<String> iter = LazyIter.of("First", "Middle", "Last");

        final SideEffect sideEffect = new SideEffect();

        iter.any(new Predicate<String>() {
            @Override
            public boolean matches(String o) {
                sideEffect.sideEffect();
                return o.startsWith("M");
            }
        });

        assertThat(sideEffect.hasSideEffects(), is(false));
    }

    @Test
    public void allShouldReturnTrueIfAllElementsMatchPredicate() throws Exception {
        LazyIter<String> iter = LazyIter.of("Mary", "Molly", "Maria");

        Promise<Boolean> promise = iter.all(new Predicate<String>() {
            @Override
            public boolean matches(String o) {
                return o.startsWith("M");
            }
        });

        assertThat(promise.get(), is(true));
    }

    @Test
    public void allShouldReturnFalseIfAtLeastOneElementDoesNotMatchPredicate() throws Exception {
        LazyIter<String> iter = LazyIter.of("Mary", "Molly", "Jenny");

        Promise<Boolean> promise = iter.all(new Predicate<String>() {
            @Override
            public boolean matches(String o) {
                return o.startsWith("M");
            }
        });

        assertThat(promise.get(), is(false));
    }

    @Test
    public void allShouldBeLazy() throws Exception {
        LazyIter<String> iter = LazyIter.of("First", "Middle", "Last");

        final SideEffect sideEffect = new SideEffect();

        iter.any(new Predicate<String>() {
            @Override
            public boolean matches(String o) {
                sideEffect.sideEffect();
                return o.startsWith("M");
            }
        });

        assertThat(sideEffect.hasSideEffects(), is(false));
    }
}

