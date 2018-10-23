// https://searchcode.com/api/result/127473763/

package org.baseparadigm;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

import org.baseparadigm.MEdge.MEdgeBuilder;
import org.baseparadigm.i.Bytes;
import org.baseparadigm.i.CidScheme;
import org.baseparadigm.i.ContentId;
import org.baseparadigm.i.Graph;
import org.baseparadigm.i.HasAs;
import org.baseparadigm.i.HasBytes;
import org.baseparadigm.i.HasContentId;
import org.baseparadigm.i.Repo;
import org.baseparadigm.i.ResolvableId;
import org.baseparadigm.i.SomeContent;
import org.baseparadigm.i.ToByteArray;
import org.baseparadigm.i.Traversal;
import org.baseparadigm.i.HasAs.AsTypeException;

public class Util {


    /**
     * {@code IterableAsType} helps with iteration eg:
     * {@code for (I i : new IterableAsType(someContent, I.class, true)}
     * 
     * @param <I>
     *            the class of instances returned from {@link Iterator#next()}
     */
    public static class IterableAsType<I>
             implements Iterable<I> {
        private final Iterable<HasAs> table;
        private final Class<I> clazz;
        private final boolean throwExceptions;

        /**
         * @param table the iterable providing instances to convert
         * @param iClass
         *            the class the loop desires to iterate over
         * @param throwExceptions
         *            if true {@link AsTypeException}s will be thrown during
         *            iteration, or alternatively, if false nulls will be
         *            returned for content that fails to convert to the class.
         * @return an iterable whose iterator will return instances of the given
         *         class
         */
        @SuppressWarnings("unchecked")
        public IterableAsType(final Iterable<? extends HasAs> table
                            , final Class<I> iClass
                            , final boolean throwExceptions) {
            this.clazz = iClass; this.table = (Iterable<HasAs>) table; this.throwExceptions = throwExceptions;
        }
        /** defaults {@code throwExceptions} to {@code true} */
        public IterableAsType(Iterable<? extends HasAs> table, Class<I> iClass) {
            this(table, iClass, true); }
        @Override public Iterator<I> iterator() {
            return new IteratorAsType(); }
        private class IteratorAsType
           implements Iterator<I> {
            private final Iterator<HasAs> thit = table.iterator();
            @Override public boolean hasNext() {
                return thit.hasNext(); }
            @Override public I next() {
                if (throwExceptions)
                    return thit.next().asType(clazz);
                else try {
                    return thit.next().asType(clazz);
                } catch (final Exception e) { return null; }
            }
            @Override public void remove() {
                thit.remove(); }
        }
    }
    
    /** saves you a few characters over constructing an {@link IterableAsType#IterableAsType(Iterable, Class, boolean)} at the beginning of a for loop. */
    public static <I extends HasAs> Iterable<I> eachAsType(
              final Iterable<? extends HasAs> table
            , final Class<I> iClass
            , final boolean throwExceptions) {
        return new IterableAsType<I>(table, iClass, throwExceptions);
    }
    /** saves you a few characters over constructing an {@link IterableAsType#IterableAsType(Iterable, Class)} at the beginning of a for loop. */
    public static <I extends HasAs> Iterable<I> eachAsType(
              final Iterable<? extends HasAs> table
            , final Class<I> iClass) {
        return new IterableAsType<I>(table, iClass);
    }
    
    public static <NT extends HasContentId> Traversal<NT> traverse(final Graph<NT> g) {
        assert false : "write me";
        return null;
    }
    
    // TODO are these map/reduce types actually used or useful anywhere?
    public interface Emitter<T> {
        void emit(T e);
    }

    public interface Mapper<IN, OUT> {
        void mapOne(IN q, Emitter<OUT> ec);
    }

    /**
     * extending this can simplify subclasses.
     * abstract simply because there's no reason to instantiate one.
     */
    public static abstract class HcidWrapper
                      implements HasContentId {

        public final HasContentId wrapped;

        public HcidWrapper(HasContentId wrapped) { this.wrapped = wrapped; }
        @Override public int compareTo(HasContentId o) {
            return wrapped.compareTo(o); }
        @Override public CidScheme getCidScheme() {
            return wrapped.getCidScheme(); }
        @Override public Bytes getBytes() {
            return wrapped.getBytes(); }
        @Override public <T> T asType(Class<T> clazz) {
            return wrapped.asType(clazz); }
        @Override public ResolvableId getId() {
            return wrapped.getId(); }
        @Override public String toString() {
            return getClass().getSimpleName() +": "+ wrapped.toString(); }
    }
    
    public static <IN, OUT> Collection<OUT> map(final Util.Mapper<IN, OUT> fil, final Iterable<IN> ite) {
        final List<OUT> ret = new LinkedList<>();
        final Emitter<OUT> emitter = new Emitter<OUT>() {
            @Override public void emit(final OUT e) {
                ret.add(e);
            }
        };
        for (final IN edge : ite)
            fil.mapOne(edge, emitter);
        return ret;
    }

    public static class SingleValueRepo
             implements Repo {
        private final HasContentId hasCid;
        private final ResolvableId rcid;
        public SingleValueRepo(final ContentId cid, final HasBytes tent) {
            if (cid instanceof ResolvableId) {
                rcid = (ResolvableId) cid;
            } else {
                rcid = new ResolvableId() {
                    @Override public Bytes getBytes() {
                        return cid.getBytes(); }
                    @Override public int compareTo(final ContentId o) {
                        return cid.compareTo(o); }
                    @Override public CidScheme getCidScheme() {
                        return cid.getCidScheme(); }
                    @Override public Repo getRepo() {
                        return SingleValueRepo.this; }
                    @Override public HasContentId resolve() {
                        return hasCid; }
                    @Override public ContentIdAsContent asContent() {
                        return new CaContentId(this); }
                };
            }
            if (tent instanceof HasContentId) {
                hasCid = (HasContentId) tent;
            } else {
                hasCid = new HasContentId() {
                    @Override public int compareTo(final HasContentId arg0) {
                        return cid.compareTo(arg0.getId()); }
                    @Override public CidScheme getCidScheme() {
                        return cid.getCidScheme(); }
                    @Override public Bytes getBytes() {
                        return tent.getBytes(); }
                    @Override public ResolvableId getId() {
                        return rcid; }
                    @Override public <T> T asType(final Class<T> clazz) {
                        throw new UnsupportedOperationException(); }
                };
            }
        }
        @Override
        public CidScheme getCidScheme() { return rcid.getCidScheme(); }
        @Override
        public boolean containsKey(final ContentId cid) { return rcid.equals(cid); }
        @Override
        public HasContentId get(final ContentId cid) {
            if (containsKey(cid))
                return hasCid;
            return null;
        }
        @Override
        public ResolvableId put(final HasBytes value) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The known content id schemes. New content id schemes register here.
     */
    public static final SortedMap<BigInteger, CidScheme> availableCidSchemes = new ConcurrentSkipListMap<>();

    public static Object firstNotNull(final Object[] oarr) {
        for (final Object i : oarr)
            if (i != null) return i;
        return null;
    }
    
    /**
     * returns null if not found
     */
    public static HasContentId firstHcid(final Object[] oarr) {
        for (final Object i : oarr)
            if (i instanceof HasContentId) return (HasContentId) i;
        return null;
    }

    public static final Charset defaultCharset = Charset.forName("UTF8");
    
    public static Bytes drain(final InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r = 0;
        for (final byte[] buf = new byte[512];
                (r = is.read(buf)) >= 0;
                baos.write(buf, 0, r)) {
            if (r == 0)
                Thread.yield();
        }
        assert is.read() == -1;
        return new ByteChunk(baos.toByteArray());
    }

    public static Bytes drain(final File f) throws IOException {
        return Util.drain(new BufferedInputStream(new FileInputStream(f)));
    }
    
    private static final String HEXES = "0123456789ABCDEF";
    public static CharSequence toHex( final byte[] raw ) {
      if ( raw == null )
        return null;
      final StringBuilder hex = new StringBuilder( 2 * raw.length );
      for ( final byte b : raw ) {
        hex.append(HEXES.charAt((b & 0xF0) >> 4))
           .append(HEXES.charAt((b & 0x0F)));
      }
      return hex;
    }
    public static byte[] fromHex( final CharSequence hex ) {
        if ( hex == null )
            return null;
        // http://stackoverflow.com/a/140861
        final int len = hex.length();
        assert len %2 == 0;
        final byte[] ret = new byte[ len / 2 ];
        for ( int i = 0; i < len ; i += 2 ) {
            ret[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i+1), 16));
        }
        return ret;
    }
    static {
        assert "ABCD098700FFE5".equals(toHex(fromHex("ABCD098700FFE5")).toString()) : "ABCD098700FFE5 does not equal "+ toHex(fromHex("ABCD098700FFE5"));
    }
    
    /**
     * Box a byte array in a ToByteArray.
     * tba(b).toByteArray() == b
     */
    public static ToByteArray tba(final byte[] b) {
        return new ToByteArray() {
            @Override
            public byte[] toByteArray() {
                return b;
            }
        };
    }

    public static Comparator<ContentId> contentIdComparator = new Comparator<ContentId>() {
        @Override
        public int compare(final ContentId o1, final ContentId o2) {
            assert o1.getCidScheme().equals(o2.getCidScheme());
            final byte[] bytea = o1.getBytes().toByteArray();
            final byte[] cytea = o2.getBytes().toByteArray();
            for (int i = 0;
                    i < bytea.length;
                    i++) {
                final int unsigA = 0xFF & bytea[i];
                final int unsigB = 0xFF & cytea[i];
                if (unsigA < unsigB)
                    return -1;
                else if (unsigA > unsigB)
                    return 1;
            }
            return 0;
        }
    };

    /**
     * If assertions are enabled, setting this to false will enable the more rigorous
     * assertions at potentially great cost.
     */
    public static boolean avoidIntensiveAssertions = true;

    public static byte[] toByteArray(final BigInteger bi, final int len) {
        final byte[] keyBytes = new byte[len];
        java.util.Arrays.fill(keyBytes, (bi.signum() == -1
                ? (byte)0xFF // negative values get padded with 0xFFs
                : (byte)0x00 // positive values get padded with zeros
                ));
        final byte[] bia = bi.toByteArray();
        System.arraycopy(bia, 0, keyBytes, keyBytes.length - bia.length, bia.length);
        return keyBytes;
    }

    private static final Map<CidScheme, Repo> defaultRepos = new ConcurrentHashMap<>();
    
    // a stack of repos per CidScheme per thread
    private static final ThreadLocal<Map<CidScheme, Deque<Repo>>> tlDefaultRepos = new ThreadLocal<Map<CidScheme, Deque<Repo>>>() {
        @Override
        protected Map<CidScheme, Deque<Repo>> initialValue() {
            return new HashMap<>();
        }
    };
    /**
     * If there isn't a thread local default, this method synchronizes with setDefaultRepo.
     * @return the thread local default repo if one has been set or else the static default.
     */
    public static Repo getDefaultRepo(final CidScheme cids) {
        final Deque<Repo> tl = tlDefaultRepos.get().get(cids);
        if (tl != null && (!tl.isEmpty()))
            return tl.peek();
        return defaultRepos.get(cids);
    }
    /**
     * A useful utility for setting the default for content ids to resolve against. use popThreadLocalDefaultRepo(CidScheme) when done.
     */
    public static void pushThreadLocalDefaultRepo(final Repo repo) {
        final Map<CidScheme, Deque<Repo>> mcs = tlDefaultRepos.get();
        Deque<Repo> dr;
        if (mcs.containsKey(repo.getCidScheme())) {
            dr = mcs.get(repo.getCidScheme());
            assert dr != null : mcs +" should not allow null values";
        } else {
            dr = new LinkedList<Repo>();
            mcs.put(repo.getCidScheme(), dr);
        }
        dr.push(repo);
    }
    public static Repo popThreadLocalDefaultRepo(final CidScheme cids) {
        final Deque<Repo> dr = tlDefaultRepos.get().get(cids);
        if (dr != null && (!dr.isEmpty()))
            return dr.pop();
        return null;
    }
    public static void setDefaultRepo(final Repo repo) {
        defaultRepos.put(repo.getCidScheme(), repo);
    }
    public static void unsetDefaultRepo(final CidScheme cids) {
        defaultRepos.remove(cids);
    }

    /**
     * Essentially just {@link Repo#get(ContentId)} but it throws an exception if {@code get} returns null.
     * @param r the repo to retrieve from
     * @param cid the id for the content to retrieve
     * @return the content from r
     * @throws NotInRepoException if the content is not in the repo
     */
    public static byte[] repoGet(final Repo r, final ContentId cid) throws NotInRepoException {
        final HasContentId gotten = r.get(cid);
        if (gotten == null)
            throw new NotInRepoException(cid.toString());
        return gotten.getBytes().toByteArray();
    }


    private static final AtomicReference<CidScheme> defaultCids =
            new AtomicReference<CidScheme>(CommonsCidScheme.instance());

    // a stack of repos per CidScheme per thread
    private static final ThreadLocal<Deque<CidScheme>> tlDefaultCidSchemes = new ThreadLocal<Deque<CidScheme>>() {
        @Override
        protected Deque<CidScheme> initialValue() {
            final Deque<CidScheme> ret = new LinkedList<>();
            ret.push(defaultCids.get());
            return ret;
        }
    };
    /**
     * If there isn't a thread local default, this method synchronizes with setDefaultRepo.
     * @return the thread local default repo if one has been set or else the static default.
     */
    public static CidScheme getDefaultCidScheme() {
        final Deque<CidScheme> tl = tlDefaultCidSchemes.get();
        if (tl != null && (!tl.isEmpty()))
            return tl.peek();
        return defaultCids.get();
    }
    public static void pushThreadLocalDefaultCidScheme(final CidScheme cids) {
        tlDefaultCidSchemes.get().push(cids);
    }
    public static CidScheme popThreadLocalDefaultCidScheme() {
        final Deque<CidScheme> dr = tlDefaultCidSchemes.get();
        if (dr != null && (!dr.isEmpty()))
            return dr.pop();
        return null;
    }
    public static void setDefaultCidScheme(final CidScheme cids) {
        defaultCids.set(cids);
    }
    public static void unsetDefaultCidScheme() {
        defaultCids.set(null);
    }

    /**
     * the label edges returned from typeEdgeFor will have
     */
    static final HasBytes JAVA_CLASS_TYPE_LABEL = new CharSequenceByteChunk("java class");
    /**
     * 
     */
    public static MEdge<HasContentId> typeEdgeFor(final HasContentId valueType) {
        return new MEdgeBuilder<HasContentId>(valueType.getCidScheme()).known(valueType)
                .label(JAVA_CLASS_TYPE_LABEL)
                .discoverable(valueType.getClass().getName()).finishMEdge();
    }

    // maybe this will be needed TODO
//    public static Graph<HasContentId> collectNecessary(Iterable<HasContentId> value, HasContentId... more) {
//        assert value != null || more.length != 0;
//        EdgeSetBuilder<HasContentId> necessaryGraphBuilder = new EdgeSetBuilder<>();
//        if (value != null)
//            for(HasContentId i : value)
//                necessaryGraphBuilder.addAll(i.getNecessaryGraph());
//        for (HasContentId i : more)
//            necessaryGraphBuilder.add(typeEdgeFor(i));
//        return necessaryGraphBuilder.finishEdgeSet();
//    }
}

