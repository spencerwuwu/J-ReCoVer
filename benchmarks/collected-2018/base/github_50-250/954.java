// https://searchcode.com/api/result/66192930/

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrencyApp {

    static private int numActiveThreads;
    static private long tic;
    static private ExecutorService threadPool;

    public interface AsyncCallback<T> {
        void call(T data);
    }

    public interface Reducer<S,T> {
        T reduce(S current, T result);
    }

    public static class Algorithms {

        public static <S,T> T reduce(Collection<S> items, Reducer<S,T> reducer, T initial) {

            for (S item : items) {
                initial = reducer.reduce(item, initial);
            }
            return initial;
        }
    }

    private static class BlurImage implements Callable<Object> {

        private final int start;
        private final int step;
        private final AsyncCallback callback;
        private final BufferedImage original;
        private final BufferedImage processed;

        private final Reducer<Integer, Integer> sumIntegers = new Reducer<Integer, Integer>() {
            public Integer reduce(Integer current, Integer result) {
                return result + current;
            }
        };

        public BlurImage(BufferedImage original, BufferedImage processed, int start, int step, AsyncCallback<BufferedImage> callback) {
            this.original = original;
            this.processed = processed;
            this.start = start;
            this.step = step;
            this.callback = callback;
        }

        public Object call() {

            int imageWidth  = original.getWidth();
            int imageHeight = original.getHeight();

            System.out.println("started");

            int red;
            int green;
            int blue;
            int colors[] = new int[8];
            Integer colorBias[] = {1, 5, 1};

            int sumColorBias = Algorithms.reduce(Arrays.asList(colorBias), sumIntegers, 0) / 3;

            int end = Math.min(start + 1 + step, imageWidth - 2);

            for (int x = start + 1; x < end; x++) {
                for (int y = 1; y < imageHeight - 2; y++) {

                    red   = 0;
                    green = 0;
                    blue  = 0;

                    colors[0] = original.getRGB(x - 1, y - 1);
                    colors[1] = original.getRGB(x    , y - 1);
                    colors[2] = original.getRGB(x + 1, y - 1);
                    colors[3] = original.getRGB(x - 1, y);
                    colors[4] = original.getRGB(x + 1, y);
                    colors[5] = original.getRGB(x - 1, y + 1);
                    colors[6] = original.getRGB(x    , y + 1);
                    colors[7] = original.getRGB(x + 1, y + 1);

                    for (int color : colors) {
                        red     += colorBias[0] * ((color & 0x00ff0000) >> 16);
                        green   += colorBias[1] * ((color & 0x0000ff00) >> 8);
                        blue    += colorBias[2] *  (color & 0x000000ff);
                    }

                    int newColor = (Math.min(255, (red   / (8 * sumColorBias))) << 16)
                                 + (Math.min(255, (green / (8 * sumColorBias))) << 8) 
                                 +  Math.min(255, (blue  / (8 * sumColorBias)));

                    processed.setRGB(x, y, newColor);
                }
            }

            callback.call(processed);
            return null;
        }
    }

    private static final AsyncCallback render = new AsyncCallback<BufferedImage>() {

        public void call(BufferedImage data) {
            numActiveThreads--;

            if (numActiveThreads != 0) return;

            long toc = System.currentTimeMillis() - tic;

            JFrame frame = new JFrame();
            frame.getContentPane().add(new JLabel(new ImageIcon(data)), BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);
            frame.setTitle("Time taken: " + toc + "ms");
        }
    };

    private static int calcStepSize(int total, int numThreads, int currentThread) {

        int step = total / numThreads;
        int rem  = total % numThreads;

        if (currentThread >= rem) return step;
        return step + 1;
    }

    private static ExecutorService getThreadPool() {
        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(getNumProcessors());
        }
        return threadPool;
    }

    private static int getNumProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void main(String[] args) {

        BufferedImage original  = null;
        BufferedImage processed = null;
        URL url;

        try {
            url = new URL("http://dl.dropbox.com/u/2746641/DSC00224.JPG");
            original  = ImageIO.read(url);
            processed = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        int numThreads = getNumProcessors();
        numActiveThreads = numThreads;

        tic = System.currentTimeMillis();

        Collection<Callable<Object>> apps = new ArrayList<Callable<Object>>();

        for (int thread = 0; thread < numThreads; thread++) {
            int step = calcStepSize(original.getWidth(), numThreads, thread);
            apps.add(new BlurImage(original, processed, thread * step, (thread + 1) * step, render));
        }

        try {
            getThreadPool().invokeAll(apps);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
