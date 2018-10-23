// https://searchcode.com/api/result/3617556/

package com.pietschy.gwt.pectin.client.function;

import com.pietschy.gwt.pectin.client.list.ArrayListModel;
import com.pietschy.gwt.pectin.client.value.Converter;
import com.pietschy.gwt.pectin.client.value.MutableValueModel;
import com.pietschy.gwt.pectin.client.value.ValueHolder;
import com.pietschy.gwt.pectin.client.value.ValueModel;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static com.pietschy.gwt.pectin.client.function.Functions.computedFrom;
import static com.pietschy.gwt.pectin.client.function.Functions.convert;
import static org.testng.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: andrew
 * Date: Jul 16, 2010
 * Time: 12:55:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class FunctionsTest
{
   private StringIntegerConverter converter;
   private ValueHolder<Integer> source;
   private Function<String,Integer> function;
   private Reduce<Integer,Integer> sum;


   @BeforeMethod
   protected void setUp() throws Exception
   {
      converter = new StringIntegerConverter();
      source = new ValueHolder<Integer>(0);
      function = new Function<String, Integer>()
      {
         public String compute(Integer source)
         {
            return String.valueOf(source);
         }
      };
      sum = new Reduce<Integer, Integer>()
      {
         public Integer compute(List<? extends Integer> source)
         {
            int result = 0;
            for (Integer integer : source)
            {
               result += integer;
            }
            return result;
         }
      };
   }

   @Test
   public void testConvertMutable() throws Exception
   {
      MutableValueModel<String> result = convert(source).using(converter);
      assertEquals(result.getValue(), "0");
      source.setValue(1);
      assertEquals(result.getValue(), "1");
   }

   @Test
   public void testConvertFromNonMutableUsingFunction() throws Exception
   {
      // make sure we use the non mutable variant
      ValueModel<String> result = Functions.convert((ValueModel<Integer>) source).using(function);
      Assert.assertFalse(result instanceof MutableValueModel);

      assertEquals(result.getValue(), "0");
      source.setValue(1);
      assertEquals(result.getValue(), "1");
   }

   @Test
   public void testComputedFromNonMutableUsingConverter() throws Exception
   {
      // make sure we use the non mutable variant
      ValueModel<String> result = Functions.convert((ValueModel<Integer>) source).using(converter);
      Assert.assertFalse(result instanceof MutableValueModel);

      assertEquals(result.getValue(), "0");
      source.setValue(1);
      assertEquals(result.getValue(), "1");
   }

   @Test
   public void testComputedFromValueList() throws Exception
   {
      ValueHolder<Integer> sourceA = new ValueHolder<Integer>(1);
      ValueHolder<Integer> sourceB = new ValueHolder<Integer>(2);

      ValueModel<Integer> result = computedFrom(sourceA, sourceB).using(sum);
      assertEquals(result.getValue(), new Integer(3));
      sourceA.setValue(3);
      assertEquals(result.getValue(), new Integer(5));
   }

   @Test
   public void testComputedFromList() throws Exception
   {
      ArrayListModel<Integer> source = new ArrayListModel<Integer>();
      ValueModel<Integer> result = computedFrom(source).using(sum);
      assertEquals(result.getValue(), new Integer(0));
      source.add(1);
      assertEquals(result.getValue(), new Integer(1));
      source.add(2);
      assertEquals(result.getValue(), new Integer(3));
   }

   private static class StringIntegerConverter implements Converter<String, Integer>
   {
      public String fromSource(Integer value)
      {
         return String.valueOf(value);
      }

      public Integer toSource(String value)
      {
         return Integer.parseInt(value);
      }
   }
}

