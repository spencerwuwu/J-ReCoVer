// https://searchcode.com/api/result/951637/

import net.sourceforge.javaocr.ocrPlugins.mseOCR.CharacterRange;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.OCRScanner;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImage;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImageLoader;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author MTO
 */
public class GameOCRBusiness {
   private static OCRScanner ocrScanner = new OCRScanner();
   private JFrame frame;

   public GameOCRBusiness() {
      frame = new JFrame();
      frame.setVisible(false);
      initTraining(ocrScanner);
   }

   public Plade recognize(BufferedImage input) throws Exception {
      BufferedImage resultImage = stripImageForColors(input);
      BufferedImage removePgp = removeUnwantedData(resultImage);

      String text = ocrScanner.scan(removePgp, 0, 52, 0, 0, null);
      String[] lines = text.split("\n");
      Plade plade = new Plade();
      for (String line : lines) {
         ArrayList<Integer> numbers = new ArrayList<Integer>();
         String[] numberStrings = line.split(" ");
         for (String numberString : numberStrings) {
            String resultnumber = numberString.trim();
            if (resultnumber.length() != 0) {
               try {
                  numbers.add(Integer.parseInt(resultnumber));
               } catch (Exception e) {
                  // Ignore
               }
            }
         }
         if (numbers.size() != 0) {
            plade.addLine(numbers);
         }
      }

      return plade;
   }


   private BufferedImage removeUnwantedData(BufferedImage image) throws Exception {
      HashSet<Rectangle> areas = new HashSet<Rectangle>();

      Rectangle currentArea = null;
      for(int x=0;x<image.getWidth();x++) {
         for(int y=0;y<image.getHeight();y++) {
            int rgb = image.getRGB(x, y);
            if(rgb==-1) {
               if(currentArea!=null) {
                  areas.add(currentArea);
                  currentArea=null;
               }
               //
            } else {
               if(currentArea==null) {
                  currentArea = new Rectangle(x, y, 1, 1);
               }
               currentArea.add(new Rectangle(x, y, 1, 1));
            }
         }
      }

      if(currentArea!=null) {
         areas.add(currentArea);
      }

      for (Rectangle area : areas) {
         area.grow(1,0);
      }

      HashSet<Rectangle> result = new HashSet<Rectangle>();
      result.addAll(areas);

      while(true) {
         for (Rectangle outerArea : result) {
            if(outerArea.getX()==0 || outerArea.getY()==0) {
               continue;
            }
            for (Rectangle innerArea : result) {
               if(innerArea.getX()==0 || innerArea.getY()==0) {
                  continue;
               }
               if(innerArea.equals(outerArea)) {
                  continue;
               }
               if(outerArea.intersects(innerArea)) {
                  outerArea.setRect(outerArea.union(innerArea));
                  innerArea.setBounds(new Rectangle(0,0,0,0));
               }
            }
         }

         HashSet<Rectangle> newResult = new HashSet<Rectangle>();
         for (Rectangle rectangle : new HashSet<Rectangle>(areas)) {
            if(rectangle.getX()!=0) {
               newResult.add(rectangle);
            }
         }

         if(newResult.size()==result.size()) {
            // We couldn't reduce the number of rectangles ...
            break;
         }
         result.clear();
         result.addAll(newResult);
      }

      HashMap<Double, ArrayList<Rectangle>> sizeMap = new HashMap<Double, ArrayList<Rectangle>>();
      for (Rectangle rectangle : result) {
         double heightKey = ((int)rectangle.getHeight() / 5);
         ArrayList<Rectangle> rectangles = sizeMap.get(heightKey);
         if(rectangles==null) {
            rectangles = new ArrayList<Rectangle>();
            sizeMap.put(heightKey, rectangles);
         }
         rectangles.add(rectangle);
      }

      Graphics2D graphics = image.createGraphics();
      graphics.setColor(Color.WHITE);

      for (Double aDouble : sizeMap.keySet()) {
         ArrayList<Rectangle> rectangles = sizeMap.get(aDouble);

         boolean fail = false;
         try {
            for (Rectangle next : rectangles) {
               BufferedImage subimage = image.getSubimage((int) next.getX(), (int) next.getY(), (int) next.getWidth(), (int) next.getHeight());
               String scan = ocrScanner.scan(subimage, 0, 0, subimage.getWidth(), subimage.getHeight(), null);
               if(scan.length()!=1) {
                  fail = true;
                  break;
               }
            }
         } catch (Exception e) {
            // Ignore if it fails
            fail = true;
         }
         if(fail) {
            for (Rectangle rectangle : rectangles) {
               graphics.fill(rectangle);
            }
         }
      }

      return image;
   }



   public static BufferedImage stripImageForColors(BufferedImage image) {
      BufferedImage resultImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
      for (int x = 0; x < image.getWidth(); x++) {
         for (int y = 0; y < image.getHeight(); y++) {
            int rgb = image.getRGB(x, y);
            if (rgb == Color.BLACK.getRGB()) {
               resultImage.setRGB(x, y, Color.BLACK.getRGB());
            } else {
               resultImage.setRGB(x, y, Color.WHITE.getRGB());
            }
         }
      }
      return resultImage;
   }


   private void initTraining(OCRScanner scanner) {
      try {
         TrainingImageLoader loader = new TrainingImageLoader();
         HashMap<Character, ArrayList<TrainingImage>> hashMap = new HashMap<Character, ArrayList<TrainingImage>>();
         for (int i = 0; i < 10; i++) {
            File file = new File(SystemConfiguration.TRAINING_DIRECTORY, "char" + i + ".png");
            loader.load(frame, file.getAbsolutePath(), new CharacterRange('0' + i, '0' + i), hashMap);
         }
         scanner.addTrainingImages(hashMap);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

}

