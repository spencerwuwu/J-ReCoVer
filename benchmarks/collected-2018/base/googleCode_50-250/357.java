// https://searchcode.com/api/result/13740160/

/*
 * Code for blog.techhead.biz
 * Distributed under BSD-style license
 */

package biz.techhead.funcy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static biz.techhead.funcy.Tuples.T0;
import static biz.techhead.funcy.Tuples.T2;

/**
 * There is really no purpose to this class other than to help reduce the
 * occurence of carpal tunnel.  However, to obtain this benefit, you also
 * sacrifice thread safety.
 * 
 * <p>In this class, there is no need to directly override {@code call}.
 * Instead, simply create one no-args method with a void return type
 * (it need not even be public) and SugarFunc will call it. This further
 * reduces code size, and even adds a nice little self-documenting touch.
 * 
 * <p>This idea was inspired in part by the following article:
 * http://gleichman.wordpress.com/2008/02/10/blocks-an-alternative-for-closures/
 * 
 * <p><code>
 * FuncE<Boolean,Integer,RuntimeException> p1 =
 *         new Func<Boolean,Integer>() {
 *             public Boolean call(Integer in) {
 *                 return (in % 2) == 0; } };
 *
 * FuncE<Boolean,Integer,RuntimeException> p2 =
 *         new Predicate<Integer>() { void even() { out = ((in % 2) == 0); } };
 * </code>
 *
 * <p>Please note, you should never use the abstract types Predicate, Mapper,
 * Reducer, etc. as the expected type in a public interface.  Instead use
 * {@code FuncE}.
 *
 * @author Jonathan Hawkes <jhawkes at techhead.biz>
 */
public abstract class SugarFunc<O,I>
        extends Restarts.RestartableFuncE<O,I,RuntimeException>
        implements Func<O,I> {
    
    public abstract static class Predicate<T> extends SugarFunc<Boolean,T> {}
    public abstract static class Mapper<I,O> extends SugarFunc<O,I> {}
    public abstract static class Reducer<T> extends SugarFunc<T,T2<T,T>> {}
    public abstract static class Handler<T> extends SugarFunc<Void,T> {}
    public abstract static class Expression<T> extends SugarFunc<T,T0> {}
    public abstract static class Statement extends SugarFunc<Void,T0> {}
    
    public SugarFunc() { findImpl(); }

    private void findImpl() {
        Method[] methods = getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getReturnType()==void.class) {
                if (method.getParameterTypes().length==0) {
                    method.setAccessible(true);
                    impl = method;
                    return;
                }
            }
        }
        throw new RuntimeException("No suitable implementation method found.");
    }
    
    protected void call() {
        try {
            impl.invoke(this, (Object[]) null);
        } catch (InvocationTargetException ex) {
            Throwable t = ex.getTargetException();
            if (t instanceof Error) throw (Error) t;
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new CheckedBaggageException(t);
        } catch (IllegalAccessException ex) {
            // method.setAccessible(true) has been called
            throw new AssertionError();
        }
    }
    
    @Override public O call(I in) {
        this.in = in;
        call();
        return out;
    }
    
    protected I in;
    protected O out;
    private Method impl;
}

