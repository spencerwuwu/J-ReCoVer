// https://searchcode.com/api/result/120306438/

package org.python.compiler.typechecker;

import java.util.LinkedList;

import org.python.antlr.PythonTree;
import org.python.compiler.typechecker.Aliases.AliasBind;

public class TypeEnvironment extends LinkedList<TypeEnvironment.AliasBind>{
	class AliasBind {
		final String name;
		final Type type;
		public AliasBind(String name, Type type) {
			this.name = name;
			this.type = type;
		}
	}
	
	public static TypeEnvironment Default = new TypeEnvironment(-1);
	
	public TypeEnvironment() {}
	
	private TypeEnvironment(int ignore) {
		push("True",DynamicType.Q);
		push("False",DynamicType.Q);
		
		push("abs",new ArrowType(GroundType.LONG, GroundType.LONG));
		push("all",new ArrowType(DynamicType.Q/*should actually be iterable*/,
					GroundType.LONG));
		push("any",new ArrowType(DynamicType.Q/*should actually be iterable*/,
					GroundType.LONG));
		push("basetype",DynamicType.Q);
		push("bin",new ArrowType(GroundType.LONG, GroundType.STR));
		push("bytearray",DynamicType.Q);
		push("callable",new ArrowType(DynamicType.Q, GroundType.LONG));
		push("chr",new ArrowType(GroundType.LONG, GroundType.STR));
		push("classmethod",DynamicType.Q);
		push("cmp",new ArrowType(GroundType.LONG, GroundType.LONG, GroundType.LONG));
		push("compile",new ArrowType(DynamicType.Q, DynamicType.Q));
		push("complex",new ArrowType(DynamicType.Q, GroundType.COMPLEX));
		push("delattr",DynamicType.Q);
		push("dict",DynamicType.Q);
		push("dir",DynamicType.Q);
		push("divmod",new ArrowType(GroundType.LONG, GroundType.LONG, DynamicType.Q));
		push("enumerate",DynamicType.Q);
		push("eval",new ArrowType(GroundType.STR, DynamicType.Q));
		push("execfile",new ArrowType(GroundType.STR, DynamicType.Q));
		push("file",new ArrowType(GroundType.STR, DynamicType.Q));
		push("filter",DynamicType.Q);
		push("float",new ArrowType(DynamicType.Q, GroundType.FLOAT));
		push("format",DynamicType.Q);
		push("frozenset",DynamicType.Q);
		push("getattr",DynamicType.Q);
		push("globals",DynamicType.Q);
		push("hash",new ArrowType(DynamicType.Q, GroundType.LONG));
		push("help",new ArrowType(DynamicType.Q, DynamicType.Q));
		push("hex",new ArrowType(GroundType.LONG, GroundType.STR));
		push("id",new ArrowType(DynamicType.Q, GroundType.LONG));
		push("input",new ArrowType(GroundType.STR, DynamicType.Q));
		push("int",new ArrowType(DynamicType.Q, GroundType.LONG));
		push("isinstance",new ArrowType(DynamicType.Q, DynamicType.Q, 
					GroundType.LONG));
		push("issubclass",new ArrowType(DynamicType.Q, DynamicType.Q, 
				GroundType.LONG));
		push("iter",DynamicType.Q);
		push("len",new ArrowType(DynamicType.Q, GroundType.LONG));
		push("list",DynamicType.Q);
		push("locals",DynamicType.Q);
		push("long",new ArrowType(DynamicType.Q, GroundType.LONG));
		push("map",DynamicType.Q);
		push("max",DynamicType.Q);
		push("memoryview",DynamicType.Q);
		push("min",DynamicType.Q);
		push("next",DynamicType.Q);
		push("object",DynamicType.Q);
		push("oct",new ArrowType(GroundType.LONG, GroundType.STR));
		push("open",DynamicType.Q);
		push("ord",new ArrowType(DynamicType.Q, GroundType.LONG));
		push("pow",new ArrowType(GroundType.LONG, GroundType.LONG, GroundType.LONG));
		push("print",new ArrowType(DynamicType.Q, UnitType.UNIT));
		push("property",DynamicType.Q);
		push("range",DynamicType.Q);
		push("raw_input",DynamicType.Q);
		push("reduce",DynamicType.Q);
		push("reload",DynamicType.Q);
		push("repr",new ArrowType(DynamicType.Q, GroundType.STR));
		push("reversed",DynamicType.Q);
		push("round",DynamicType.Q);
		push("set",DynamicType.Q);
		push("setattr",DynamicType.Q);
		push("slice",DynamicType.Q);
		push("sorted",DynamicType.Q);
		push("staticmethod",DynamicType.Q);
		push("str",new ArrowType(DynamicType.Q, GroundType.STR));
		push("sum",DynamicType.Q);
		push("super",DynamicType.Q);
		push("tuple",DynamicType.Q);
		push("type",DynamicType.Q);
		push("unichr",new ArrowType(GroundType.LONG, GroundType.STR));
		push("unicode",DynamicType.Q);
		push("vars",DynamicType.Q);
		push("xrange",DynamicType.Q);
		push("zip",DynamicType.Q);
		push("__import__",DynamicType.Q);
		
		push("apply",DynamicType.Q);
		push("buffer",DynamicType.Q);	
		push("coerce",new ArrowType(GroundType.LONG, GroundType.LONG, GroundType.LONG));
		push("intern",new ArrowType(DynamicType.Q, DynamicType.Q));	
	}
	
	public Type lookup(String name) throws TypeError {
		for (AliasBind bind : this)
			if(name.equals(bind.name))
				return bind.type;
		return null;
	}
	
	public void push(String name, Type ty) {
		push(new AliasBind(name,ty));
	}
	
	public void union(TypeEnvironment a, PythonTree loc) throws TypeError {
		for(AliasBind bind : a) {
			Type lu = lookup(bind.name);
			if(lu == null)
				push(bind);
			else if(lu != bind.type)
				throw new TypeError("Variable \"" + bind.name + "\" is given multiple different definitions: " + lu + " and " + bind.type, loc);
		}
	}
}

