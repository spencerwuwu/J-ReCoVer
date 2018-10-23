// https://searchcode.com/api/result/129122577/

package hv.parser.action;

import hv.Token;
import hv.ast.ASTFixedData;
import hv.message.HVErrorMessageException;
import hv.parser.Location;
import hv.parser.Node;
import hv.parser.Parser;
import hv.parser.TaggedAction;
import hv.util.AnnotatedType;
import hv.util.HVAssert;
import hv.util.HVAssertionError;
import hv.util.MyLogger;
import hv.util.MyLoggerFactory;
import hv.util.SafeReflection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * A parser action which creates Nodes by calling a constructor.
 * <p>
 * The resulting Node's class should have exactly 1 constructor
 * <ul>
 * <li>With at least 1 parameter
 * <li>and each parameter annotated with
 * {@link hv.parser.action.CreateClassAction.Param}
 * </ul>
 * <p>
 * At reduce time, the parameters are filled in with values, whose tag should
 * correspond with the annotations.
 */
public class CreateClassAction implements TaggedAction {
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Param {
		String value();
	}

	private static MyLogger log = MyLoggerFactory
			.getLogger(CreateClassAction.class);
	private Constructor<? extends Node> constructor;
	private int[] paramIndex;
	private Map<String, Integer> tags = new HashMap<String, Integer>();
	private Class<? extends Node> type;

	public CreateClassAction(Class<? extends Node> type, String... tag) {
		this.type = type;
		for (int i = 0; i < tag.length; i++)
			if (tag[i] != null)
				tags.put(tag[i], i);
	}

	@SuppressWarnings("unchecked")
	private Constructor<? extends Node> cast(Constructor<?> c) {
		return (Constructor<? extends Node>) c;
	}

	private Object convert(Node node, Class<?> expectedType) {
		Object val = node;
		while (val instanceof ASTFixedData)
			val = ((ASTFixedData) val).value;
		if (Node.class.isAssignableFrom(expectedType)) {
			return val;
		}
		if (val instanceof Token)
			val = ((Token) val).value;
		return val;
	}

	private boolean lastParameterIsLocation(Constructor<?> c) {
		Class<?>[] types = c.getParameterTypes();
		if (types.length == 0)
			return false;
		Class<?> lastType = types[types.length - 1];
		return Location.class.equals(lastType);
	}

	public void reduce(Parser parser, Node[] in, int base, Location location)
			throws HVErrorMessageException {
		final Object[] initargs = new Object[paramIndex.length + 1];
		initargs[paramIndex.length] = location;
		final Class<?>[] targetType = constructor.getParameterTypes();
		for (int arg = 0; arg < paramIndex.length; arg++) {
			Node value = in[base + paramIndex[arg]];
			initargs[arg] = convert(value, targetType[arg]);
		}
		in[base] = SafeReflection.newInstance(constructor, initargs);
	}

	public void tag(String name, int index) {
		tags.put(name, index);
	}

	public void tagged() {
		if (type == null)
			return;
		log.debug("Resolving {} with {}", type, tags);
		for (Constructor<?> c : type.getConstructors()) {
			int isAcceptable[] = tryConstructor(c);
			if (isAcceptable == null)
				continue;
			log.debug("acceptable");
			HVAssert.assertNull("Multiple candidate constructors", constructor,
					type, constructor, c);
			constructor = cast(c);
			this.paramIndex = isAcceptable;
		}
		log.debug("Chose constructor {}", constructor);
		HVAssert.assertNotNull("No candidate constructors", constructor, type,
				tags);
		// Make sure we are not tagged twice
		tags = null;
		type = null;
	}

	/**
	 * Check if constructor c corresponds to the parameters in {@link #tags}
	 * 
	 * @param c
	 *            a constructor
	 * @return null if no match, param->reduce index mapping if match.
	 */
	private int[] tryConstructor(Constructor<?> c) {
		// The last parameter should be a location
		if (!lastParameterIsLocation(c))
			throw new HVAssertionError(
					"Constructor's last arg should be Location", constructor);
		AnnotatedType<Param>[] params = AnnotatedType.findAnnotations(c,
				Param.class, 1);
		final int paramCount = params.length;
		log.debug("Constructor {} with param count {}", c, paramCount);
		// No-argument constructors are unacceptable
		if (paramCount != tags.size())
			return null;
		int p = 0;
		int[] result = new int[paramCount];
		for (AnnotatedType<Param> at : params) {
			if (at.annotation == null)
				return null;
			Integer target = tags.get(at.annotation.value());
			log.debug("Matching arg {} to {} via {}", new Object[] {
					at.annotation.value(), target, Integer.valueOf(p) });
			if (target == null)
				return null;
			result[p++] = target;
		}
		return result;
	}
}

