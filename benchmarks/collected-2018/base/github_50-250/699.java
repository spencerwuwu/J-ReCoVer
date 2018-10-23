// https://searchcode.com/api/result/14971842/

package com.github.gardentree.jambalaya;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Ignore;
import org.junit.Test;

import com.github.gardentree.colors.crimson.CrimsonRuntime;
import com.github.gardentree.utilities.Calling;
import com.github.gardentree.utilities.Entirety;
import com.github.gardentree.utilities.Resource;

/**
 * @author garden_tree
 * @since 2011/04/29
 */
public class JambalayaForUnderscoreTest {
	///////////////////////////////////
	@Test
	public void each_array() {
		final IRubyObject result = execute();

		assertArrayEquals(new Object[]{1L,2L,3L},(Object[])CrimsonRuntime.deriveJavaFrom(result));
	}
	@Test
	public void each_hash() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[]{1L,2L,3L},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void map_array() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[]{4L,6L,8L},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void reduce() {
		final IRubyObject actual = execute();

		assertEquals(6L,CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void reduceRight() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[]{4L,5L,2L,3L,0L,1L},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void all() {
		final IRubyObject actual = execute();

		assertEquals(false,CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void invoke() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[][]{{1L,5L,7L},{1L,2L,3L}},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void toArray() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[]{1D,2L,3L},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void times() {
		final IRubyObject actual = execute();

		assertEquals(3L,CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void value() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[]{1L,2L,3L},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void max() {
		final IRubyObject actual = execute();

		final Map<String,Object> expected = new LinkedHashMap<String,Object>();
		expected.put("age",60L);
		expected.put("name","curly");

		assertEquals(expected,CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void isRegExp() {
		final IRubyObject actual = execute();

		assertEquals(true,CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void testClone() {
		final IRubyObject actual = execute(Resource.getUrl("underscore/clone.rb"));

		assertEquals("{name=moe}",CrimsonRuntime.deriveJavaFrom(actual).toString());
	}
	@Test @Ignore
	public void isEqual() {
		final IRubyObject actual = execute();

		assertEquals(true,CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void zip() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[][]{{"moe",30L,true},{"larry",40L,false},{"curly",50L,false}},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void tap() {
		final IRubyObject actual = execute();

		assertArrayEquals(new Object[][]{{2L,200L},{4L,40000L}},(Object[])CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void bind() {
		final IRubyObject actual = execute();

		assertEquals("hi: moe",CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void functions() {
		final IRubyObject actual = execute();

		final Set functions = new HashSet(Arrays.asList((Object[])CrimsonRuntime.deriveJavaFrom(actual)));
		assertTrue(functions.containsAll(Arrays.asList("each","first","bind","keys","noConflict","[]","VERSION")));
	}
	@Test
	public void bindAll() {
		final IRubyObject actual = execute();

		assertEquals("clicked: underscore&hovering: underscore",CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test @Ignore
	public void isUndefined() {
		final IRubyObject actual = execute();

		assertEquals(true,CrimsonRuntime.deriveJavaFrom(actual));
	}
	@Test
	public void isDate() {
		final IRubyObject actual = execute();

		assertEquals(true,CrimsonRuntime.deriveJavaFrom(actual));
	}

	///////////////////////////////////
	private IRubyObject execute() {
		return execute(Resource.getUrl("underscore/" + Calling.fromMethodName(1) + ".rb"));
	}
	private IRubyObject execute(final URL actingscript) {
		final CrimsonRuntime runtime = CrimsonRuntime.newInstance();

		return runtime.evaluate(Entirety.getFromFile(actingscript));
	}
}

