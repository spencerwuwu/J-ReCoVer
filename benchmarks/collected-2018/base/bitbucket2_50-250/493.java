// https://searchcode.com/api/result/63926017/

package com.plainject.core.ioc;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.matcher.Matcher;

/**
 * An extension of AbstractModule that provides
 * support for member injection of instances
 * constructed at bind-time; in particular,
 * itself and MethodInterceptors.
 */
public abstract class ExtendedModule extends AbstractModule {

	 /**
	  * Overridden version of bindInterceptor that,
	  * in addition to the standard behavior,
	  * arranges for field and method injection of
	  * each MethodInterceptor in {@code interceptors}.
	  * 
	  * @param classMatcher   the class matcher.
	  * @param methodMatcher the method matcher.
	  * @param interceptors  the interceptor array.
	  */
	 @Override 
	 public void bindInterceptor(Matcher<? super Class<?>> classMatcher,
	                                       Matcher<? super Method> methodMatcher,
	                                       MethodInterceptor... interceptors) {
		 registerForInjection(interceptors);
		 super.bindInterceptor(classMatcher, methodMatcher, interceptors);
	 }

	 /**
	  * Arranges for this module and each of the given
	  * objects (if any) to be field and method injected
	  * when the Injector is created. It is safe to call
	  * this method more than once, and it is safe
	  * to call it more than once on the same object(s).
	  */
	 protected <T> void registerForInjection(T... objects) {
		 ensureSelfInjection();
		 if (objects != null) {
			 for (T object : objects) {
				 if (object != null) {
					 toBeInjected.add(object);
				 }
			 }
		 }
	 }

	 @SuppressWarnings("unused")
	 @Inject 
	 private void injectRegisteredObjects(Injector injector) {
		 for (Object injectee : toBeInjected) {
			 injector.injectMembers(injectee);
		 }
	 }

	 private void ensureSelfInjection() {
		 if (!selfInjectionInitialized) {
			 bind(ExtendedModule.class)
			 	//.annotatedWith(getUniqueAnnotation())
			 	.toInstance(this);
			 selfInjectionInitialized = true;
		 }
	 }

	 private final Set<Object> toBeInjected = new HashSet<Object>();
	  
	 private boolean selfInjectionInitialized = false;  

	 /**
	  * Hack to ensure unique Keys for binding different
	  * instances of ExtendedModule. The prefix is chosen
	  * to reduce the chances of a conflict with some other
	  * use of @Named. A better solution would be to invent
	  * an Annotation for just this purpose.
	  */
//	 private static Annotation getUniqueAnnotation() {
//		 return named("ExtendedModule-" + count.incrementAndGet());
//	 }

}

