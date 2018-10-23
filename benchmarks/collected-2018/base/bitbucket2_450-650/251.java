// https://searchcode.com/api/result/45949865/

package org.kite9.diagram.builders.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.kite9.diagram.builders.Filter;
import org.kite9.diagram.builders.formats.PropositionFormat;
import org.kite9.diagram.builders.krmodel.NounFactory;
import org.kite9.diagram.builders.krmodel.NounPart;
import org.kite9.diagram.builders.krmodel.Relationship;
import org.kite9.diagram.builders.krmodel.Tie;
import org.kite9.diagram.builders.krmodel.Relationship.RelationshipType;
import org.kite9.framework.alias.Aliaser;
import org.kite9.framework.model.AbstractHandle;
import org.kite9.framework.model.AnnotationHandle;
import org.kite9.framework.model.MemberHandle;
import org.kite9.framework.model.MethodHandle;
import org.kite9.framework.model.ProjectModel;

public class ClassBuilder extends AnnotatedElementBuilder<Class<?>> {

	public ClassBuilder(List<Tie> forX, ProjectModel model, Aliaser a) {
		super(forX, model, a);
	}

	public ClassBuilder showVisibility(PropositionFormat f) {
		for (Tie t : ties) {
			NounPart subject = NounFactory.createNewSubjectNounPart(t);
			Class<?> c = getRepresented(t);
			if (Modifier.isPublic(c.getModifiers())) {
				f.write(subject, JavaRelationships.VISIBILITY, createNoun(new JavaModifier("public")));
			} else if (Modifier.isPrivate(c.getModifiers())) {
				f.write(subject, JavaRelationships.VISIBILITY, createNoun(new JavaModifier("private")));
			} else if (Modifier.isProtected(c.getModifiers())) {
				f.write(subject, JavaRelationships.VISIBILITY, createNoun(new JavaModifier("protected")));
			}
		}
		return this;
	}

	public ClassBuilder showStatic(PropositionFormat f) {
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			NounPart subject = NounFactory.createNewSubjectNounPart(t);
			if (c.getEnclosingClass() != null) {
				// inner class
				if (Modifier.isStatic(c.getModifiers())) {
					f.write(subject, JavaRelationships.MODIFIER, createNoun(new JavaModifier("static")));
				}
			}
		}
		return this;
	}

	public ClassBuilder show(PropositionFormat f) {
		return (ClassBuilder) super.show(f);
	}

	public ClassBuilder showFinal(PropositionFormat f) {
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			NounPart subject = NounFactory.createNewSubjectNounPart(t);
			if (Modifier.isFinal(c.getModifiers())) {
				f.write(subject, JavaRelationships.MODIFIER, createNoun(new JavaModifier("final")));
			}
		}
		return this;
	}

	/**
	 * Creates a helper to allow you to manipulate the superclasses of the
	 * classes that this builder manages.
	 * 
	 * @param f
	 *            An optional filter to reduce the number of interfaces being
	 *            considered.
	 */
	public ClassBuilder withSuperClasses(Filter<? super Class<?>> f) {
		return new ClassBuilder(packContent(ties, f, new ClassContentSelector<Class<?>>() {

			public Class<?>[] contents(Class<?> c) {
				return new Class<?>[] { c.getSuperclass() };
			}

			public Set<? extends Class<?>> traverse(Class<?> c) {
				throw new UnsupportedOperationException("Not implemented for superClass selector");
			}

		}, false, JavaRelationships.EXTENDS), model, a);
	}

	/**
	 * Creates a helper to allow you to manipulate the interfaces this class
	 * implements or extends
	 * 
	 * @param f
	 *            An optional filter to reduce the number of interfaces being
	 *            considered.
	 * 
	 * @param traverse
	 *            Set to true if you want interfaces declared by superclasses
	 *            and superinterfaces too
	 */
	public ClassBuilder withInterfaces(Filter<? super Class<?>> f, boolean traverse) {
		return new ClassBuilder(packContent(ties, f, new ClassContentSelector<Class<?>>() {

			public Class<?>[] contents(Class<?> c) {
				return c.getInterfaces();
			}

			public Set<? extends Class<?>> traverse(Class<?> c) {
				return superTraverse(c);
			}

		}, traverse, JavaRelationships.IMPLEMENTS), model, a);
	}

	/**
	 * Creates a helper to allow you to manipulate methods on this class.
	 * 
	 * @param f
	 *            An optional filter to reduce the number of methods being
	 *            considered.
	 * 
	 * @oparam traverse Set to true if you want declarations from superclasses
	 *         and superinterfaces too
	 */
	public MethodBuilder withMethods(Filter<? super Method> f, boolean traverse) {
		return new MethodBuilder(packContent(ties, f, new ClassContentSelector<Method>() {
			public Method[] contents(Class<?> c) {
				return c.getDeclaredMethods();
			}

			public Set<? extends Class<?>> traverse(Class<?> c) {
				return superTraverse(c);
			}

		}, traverse, JavaRelationships.METHOD), model, a);
	}

	/**
	 * Creates a helper to allow you to manipulate inner classes on this class.
	 * 
	 * @param f
	 *            An optional filter to reduce the number of methods being
	 *            considered.
	 * 
	 * @oparam traverse Set to true if you want declarations from superclasses
	 *         too
	 */
	public ClassBuilder withInnerClasses(Filter<? super Class<?>> f, boolean traverse) {
		return new ClassBuilder(packContent(ties, f, new ClassContentSelector<Class<?>>() {
			public Class<?>[] contents(Class<?> c) {
				return c.getDeclaredClasses();
			}

			public Set<? extends Class<?>> traverse(Class<?> c) {
				return Collections.singleton(c.getSuperclass());
			}
		}, traverse, JavaRelationships.INNER_CLASS), model, a);
	}

	/**
	 * Creates a helper to allow you to manipulate subclasses within the project
	 * of the current class or classes.
	 * 
	 * @param f
	 *            Optional filter to reduce the number of returned classes
	 * @param traverse
	 *            set to true if you want the entire subclass tree (i.e.
	 *            sub-sub-classes etc)s
	 * @return
	 */
	public ClassBuilder withSubClasses(Filter<? super Class<?>> f, boolean traverse) {
		final ClassLoader cl = getCurrentClassLoader();
		return new ClassBuilder(packContent(ties, f, new ClassContentSelector<Class<?>>() {
			public Class<?>[] contents(Class<?> c) {
				return MemberHandle.hydrateClasses(model.getSubclasses(MemberHandle.convertClassName(c)), cl).toArray(
						new Class<?>[] {});
			}

			public Set<Class<?>> traverse(Class<?> c) {
				return MemberHandle.hydrateClasses(model.getSubclasses(MemberHandle.convertClassName(c)), cl);
			}
		}, traverse, JavaRelationships.EXTENDED_BY), model, a);

	}

	protected interface ClassContentSelector<T> extends ContentSelector<T, Class<?>> {

	}

	/**
	 * Creates a helper to allow you to manipulate fields on this class.
	 * 
	 * @param f
	 *            An optional filter to reduce the number of methods being
	 *            considered.
	 * @oparam traverse Set to true if you want declarations from superclasses
	 *         too
	 */
	public FieldBuilder withFields(Filter<? super Field> f, boolean traverse) {
		return new FieldBuilder(packContent(ties, f, new ClassContentSelector<Field>() {
			public Field[] contents(Class<?> c) {
				return c.getDeclaredFields();
			}

			public Set<? extends Class<?>> traverse(Class<?> c) {
				return Collections.singleton(c.getSuperclass());
			}
		}, traverse, JavaRelationships.FIELD), model, a);
	}

	private Set<Class<?>> superTraverse(Class<?> c) {
		LinkedHashSet<Class<?>> out = new LinkedHashSet<Class<?>>();
		if (c.getSuperclass() != null)
			out.add(c.getSuperclass());
		for (int i = 0; i < c.getInterfaces().length; i++) {
			out.add(c.getInterfaces()[i]);
		}
		return out;
	}

	/**
	 * This is a helper method used to create a list of ties correctly, by
	 * applying a ContentSelector.
	 */
	protected <Y> List<Tie> packContent(Collection<Tie> in, Filter<? super Y> f, ClassContentSelector<Y> ccs,
			boolean traverse, Relationship r) {
		List<Tie> out = new ArrayList<Tie>();
		packContentInner(in, f, ccs, traverse, r, out);
		return out;
	}

	protected <Y> void packContentInner(Collection<Tie> in, Filter<? super Y> f, ClassContentSelector<Y> ccs,
			boolean traverse, Relationship r, List<Tie> out) {
		for (Tie t : in) {
			Class<?> c = getRepresented(t);
			NounPart subject = NounFactory.createNewSubjectNounPart(t);
			if (model.withinModel(MemberHandle.convertClassName(c))) {
				for (Y y : ccs.contents(c)) {
					if ((y!=null) && ((f == null) || (f.accept(y)))) {
						out.add(new Tie(subject, r, createNoun(y)));
					}
				}

				if (traverse) {
					packContentInner2(subject, ccs.traverse(c), f, ccs, traverse, r, out);
				}
			}
		}
	}

	protected <Y> void packContentInner2(NounPart subject, Collection<? extends Class<?>> in, Filter<? super Y> f,
			ClassContentSelector<Y> ccs, boolean traverse, Relationship r, List<Tie> out) {
		for (Class<?> c : in) {
			if (model.withinModel(MemberHandle.convertClassName(c))) {
				for (Y y : ccs.contents(c)) {
					if ((f == null) || (f.accept(y))) {
						out.add(new Tie(subject, r, createNoun(y)));
					}
				}

				if (traverse) {
					packContentInner2(subject, ccs.traverse(c), f, ccs, traverse, r, out);
				}
			}
		}
	}

	@Override
	public ClassBuilder reduce(Filter<? super Class<?>> f) {
		return new ClassBuilder(reduceInner(f), model, a);
	}

	/**
	 * Assumes that the classes in this builder are annotations, and provides
	 * you with a classbuilder of classes that have declared these annotations.
	 */
	public ClassBuilder withAnnotatedClasses(Filter<? super Class<?>> f) {
		final ClassLoader cl = getCurrentClassLoader();
		return new ClassBuilder(packContent(ties, f, new ClassContentSelector<Class<?>>() {
			public Class<?>[] contents(Class<?> c) {
				Set<String> classNames = model.getClassesWithAnnotation(MemberHandle.convertClassName(c));
				Class<?>[] out = new Class<?>[classNames.size()];
				int i = 0;
				for (String name : classNames) {
					out[i++] = MemberHandle.hydrateClass(name, cl);
				}
				return out;
			}

			public Set<? extends Class<?>> traverse(Class<?> c) {
				return Collections.singleton(c.getSuperclass());
			}
		}, false, JavaRelationships.ANNOTATION_OF), model, a);
	}

	/**
	 * Returns ties for all of the annotations which reference this class
	 */
	public AnnotationBuilder withReferencingAnnotations(Filter<? super Annotation> f) {
		final ClassLoader cl = getCurrentClassLoader();
		return new AnnotationBuilder(packContent(ties, f, new ClassContentSelector<Annotation>() {

			public Annotation[] contents(Class<?> c) {
				Set<AnnotationHandle> handles = model.getAnnotationReferences(MemberHandle.convertClassName(c));
				Annotation[] out = new Annotation[handles.size()];
				int i = 0;
				for (AnnotationHandle h : handles) {
					out[i++] = h.hydrate(cl);
				}
				return out;

			}

			public Set<? extends Class<?>> traverse(Class<?> c) {
				return null;
			}

		}, false, JavaRelationships.REFERENCED_BY), model, a);

	}

	/**
	 * Returns ties for all of the annotated elements which reference this
	 * class, via an annotation.
	 * 
	 */
	public AnnotatedElementBuilder<?> withReferencingAnnotatedElements(Filter<? super Class<? extends Annotation>> f) {
		final ClassLoader cl = getCurrentClassLoader();
		List<Tie> out = new ArrayList<Tie>();
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			NounPart subject = NounFactory.createNewSubjectNounPart(t);
			Set<AnnotationHandle> handles = model.getAnnotationReferences(MemberHandle.convertClassName(c));
			for (AnnotationHandle h : handles) {
				AnnotatedElement object = h.getAnnotatedItem().hydrate(cl);
				Annotation relation = h.hydrate(cl);
				if ((f==null) || (f.accept(relation.annotationType()))) {
					String annAlias = a.getObjectAlias(relation);
					Relationship rel = new Relationship(annAlias+"(r)", new Relationship(annAlias));
					Tie newTie = new Tie(subject, rel, createNoun(object));
					out.add(newTie);
				}
			}
		}
		
		return new AnnotatedElementBuilder<AnnotatedElement>(out, model, a);
	}

	/**
	 * Returns classes calling this one
	 */
	public ClassBuilder withCallingClasses(Filter<? super Class<?>> f, boolean traverse) {
		final ClassLoader cl = getCurrentClassLoader();
		List<Tie> ties2 = new ArrayList<Tie>();
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			Set<Class<?>> todo = traverse ? superTraverse(c) : new LinkedHashSet<Class<?>>();
			todo.add(c);
			for (Class<?> class1 : todo) {
				for (Method m : class1.getDeclaredMethods()) {
					for (MemberHandle mh : model.getCalledBy(new MethodHandle(m))) {
						if (mh instanceof MethodHandle) {
							Class<?> dc = ((MethodHandle) mh).hydrateClass(cl);
							if ((f == null) || (f.accept(dc))) {
								ties2.add(new Tie(NounFactory.createNewSubjectNounPart(t), new MethodCallRelationship(m,
										RelationshipType.PASSIVE), createNoun(dc)));
							}
						}
					}
				}
			}
		}

		return new ClassBuilder(ties2, model, a);

	}

	/**
	 * Returns classes calling this one
	 */
	public MethodBuilder withCallingMethods(Filter<? super Method> f, boolean traverse) {
		final ClassLoader cl = getCurrentClassLoader();
		List<Tie> ties2 = new ArrayList<Tie>();
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			Set<Class<?>> todo = traverse ? superTraverse(c) : new LinkedHashSet<Class<?>>();
			todo.add(c);
			for (Class<?> class1 : todo) {
				for (Method m : class1.getDeclaredMethods()) {
					for (MemberHandle mh : model.getCalledBy(new MethodHandle(m))) {
						if (mh instanceof MethodHandle) {
							Method dc = ((MethodHandle) mh).hydrate(cl);
							if ((f == null) || (f.accept(dc))) {
								ties2.add(new Tie(NounFactory.createNewSubjectNounPart(t), JavaRelationships.CALLED_BY, createNoun(dc)));
							}
						}
					}
				}
			}
		}

		return new MethodBuilder(ties2, model, a);

	}

	/**
	 * Returns classes which are called by this one
	 */
	public ClassBuilder withCalledClasses(Filter<? super Class<?>> f, boolean traverse) {
		final ClassLoader cl = getCurrentClassLoader();
		List<Tie> ties2 = new ArrayList<Tie>();
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			Set<Class<?>> todo = traverse ? superTraverse(c) : new LinkedHashSet<Class<?>>();
			todo.add(c);
			for (Class<?> class1 : todo) {
				for (Method m : class1.getDeclaredMethods()) {
					for (MemberHandle mh : model.getCalls(new MethodHandle(m))) {
						if (mh instanceof MethodHandle) {
							Method m2 = ((MethodHandle) mh).hydrate(cl);
							Class<?> dc = ((MethodHandle) mh).hydrateClass(cl);
							if ((f == null) || (f.accept(dc))) {
								ties2.add(new Tie(NounFactory.createNewSubjectNounPart(t), new MethodCallRelationship(m2),
										createNoun(dc)));
							}
						}
					}
				}
			}
		}

		return new ClassBuilder(ties2, model, a);
	}

	/**
	 * Returns methods which are called by this class
	 */
	public MethodBuilder withCalledMethods(Filter<? super Method> f, boolean traverse) {
		final ClassLoader cl = getCurrentClassLoader();
		List<Tie> ties2 = new ArrayList<Tie>();
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			Set<Class<?>> todo = traverse ? superTraverse(c) : new LinkedHashSet<Class<?>>();
			todo.add(c);
			for (Class<?> class1 : todo) {
				for (Method m : class1.getDeclaredMethods()) {
					for (MemberHandle mh : model.getCalls(new MethodHandle(m))) {
						if (mh instanceof MethodHandle) {
							Method m2 = ((MethodHandle) mh).hydrate(cl);
							if ((f == null) || (f.accept(m2))) {
								ties2.add(new Tie(NounFactory.createNewSubjectNounPart(t), JavaRelationships.CALLS, createNoun(m2)));
							}
						}
					}
				}
			}
		}

		return new MethodBuilder(ties2, model, a);
	}
	
	/**
	 * Returns classes which this class depends on to work.
	 */
	public ClassBuilder withDependencies(Filter<? super Class<?>> f, boolean traverse) {
		final ClassLoader cl = getCurrentClassLoader();
		List<Tie> ties2 = new ArrayList<Tie>();
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			Set<Class<?>> todo = traverse ? superTraverse(c) : new LinkedHashSet<Class<?>>();
			todo.add(c);
			for (Class<?> class1 : todo) {
				String className = AbstractHandle.convertClassName(class1);
				for (String depName : model.getDependsOnClasses(className)) {
					Class<?> depClass = AbstractHandle.hydrateClass(depName, cl);
					if ((f == null) || (f.accept(depClass))) {
						ties2.add(new Tie(NounFactory.createNewSubjectNounPart(t), JavaRelationships.REQUIRES,
								createNoun(depClass)));
					}
				}
			}
		}

		return new ClassBuilder(ties2, model, a);
	}
	
	/**
	 * Returns classes which require this class to work.
	 */
	public ClassBuilder withDependants(Filter<? super Class<?>> f, boolean traverse) {
		final ClassLoader cl = getCurrentClassLoader();
		List<Tie> ties2 = new ArrayList<Tie>();
		for (Tie t : ties) {
			Class<?> c = getRepresented(t);
			Set<Class<?>> todo = traverse ? superTraverse(c) : new LinkedHashSet<Class<?>>();
			todo.add(c);
			for (Class<?> class1 : todo) {
				String className = AbstractHandle.convertClassName(class1);
				for (String depName : model.getDependedOnClasses(className)) {
					Class<?> depClass = AbstractHandle.hydrateClass(depName, cl);
					if ((f == null) || (f.accept(depClass))) {
						ties2.add(new Tie(NounFactory.createNewSubjectNounPart(t), JavaRelationships.REQUIRED_BY,
								createNoun(depClass)));
					}
				}
			}
		}

		return new ClassBuilder(ties2, model, a);
	}
}

