// https://searchcode.com/api/result/12690296/

package example.conc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * It uses <tt>FutureResult</tt> to improve the concurrency of load on demand chache.
 * By placing FutureResult into the cache, rather than the result of the computation itself,
 * you can reduce that you hold the write lock on the cache. While it won't speed up the first
 * thread to place an itme in the cache, it <i>will</i> reduce the time that the first thread bocks
 * threads from accessing the cache. It will also make the result available earlier to other threads
 * since they can retrieve FutureTask from the cache.
 *
 *
 * zhu.tan@gmail.com
 * 04-Jan-2010
 */
public class FileCacheEx {

    // could use ConcurrentHashMap here for performance gain.
    private Map<String, Future<Integer>> cache = new HashMap<String, Future<Integer>>();
    private ExecutorService executor = Executors.newFixedThreadPool(3);

    public Integer get(final String name) throws ExecutionException, InterruptedException {
        Future<Integer> result;
        synchronized (cache) {
            result = cache.get(name);
            if (result == null) {
               return 0;
            }
        }
        return result.get();
    }

     public void put(final String name) throws ExecutionException, InterruptedException {
        Future<Integer> result;
        synchronized (cache) {
            result = cache.get(name);
            if (result == null) {
                result = executor.submit(new Callable<Integer>(){
                    @Override
                    public Integer call() throws Exception {
                        Thread.sleep(500);
                        return name.length();
                    }
                });
                cache.put(name, result);
            }
        }
    }
}

