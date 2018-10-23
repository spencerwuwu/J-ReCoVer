// https://searchcode.com/api/result/3617557/

/*
 * Copyright 2009 Andrew Pietsch 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may 
 * obtain a copy of the License at 
 *      
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions 
 * and limitations under the License. 
 */

package com.pietschy.gwt.pectin.client.list;


import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.pietschy.gwt.pectin.client.function.Reduce;
import com.pietschy.gwt.pectin.client.value.IsValueChangeEventWithValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * AbstractComputedValueModel Tester.
 *
 * @author andrew
 * @version $Revision$, $Date$
 * @created August 15, 2009
 * @since 1.0
 */
public class ReducingValueModelTest
{
   private ArrayListModel<String> source;
   private ReducingValueModel<String, String> subject;
   private Reduce<String, String> concat = new Concat();
   private Reduce<String, String> reverseConcat = new ConcatReverse();

   @BeforeMethod
   public void setUp()
   {
      source = new ArrayListModel<String>();
      subject = new ReducingValueModel<String, String>(source, concat);
   }


   @Test(dataProvider = "testData")
   public void getValue(Reduce<String, String> function, String valueA, String valueB, String result)
   {
      subject.setFunction(function);
      source.setElements(Arrays.asList(valueA, valueB));
      assertEquals(subject.getValue(), result);
   }

   @DataProvider
   public Object[][] testData()
   {
      return new Object[][]
         {
            {concat, "a", "b", "ab"},
            {concat, "c", "d", "cd"},
            {reverseConcat, "a", "b", "ba"},
            {reverseConcat, "c", "d", "dc"},
         };
   }

   @Test
   @SuppressWarnings("unchecked")
   public void functionChangeFiresValueChange()
   {
      ValueChangeHandler<String> changeHandler = mock(ValueChangeHandler.class);

      source.setElements(Arrays.asList("a", "b"));

      assertEquals(subject.getValue(), "ab");

      subject.addValueChangeHandler(changeHandler);
      subject.setFunction(reverseConcat);

      verify(changeHandler, times(1)).onValueChange(argThat(new IsValueChangeEventWithValue<String>("ba")));
   }

   @Test
   @SuppressWarnings("unchecked")
   public void sourceChangeFiresValueChange()
   {
      ValueChangeHandler<String> changeHandler = mock(ValueChangeHandler.class);

      subject.addValueChangeHandler(changeHandler);

      source.setElements(Arrays.asList("a"));
      verify(changeHandler, times(1)).onValueChange(argThat(new IsValueChangeEventWithValue<String>("a")));

      source.setElements(Arrays.asList("a", "b"));
      verify(changeHandler, times(1)).onValueChange(argThat(new IsValueChangeEventWithValue<String>("ab")));
   }


   @Test
   @SuppressWarnings("unchecked")
   public void recomputeAfterRunningValueChanges()
   {
      ValueChangeHandler<String> changeHandler = mock(ValueChangeHandler.class);

      subject.setFunction(concat);
      source.setElements(Arrays.asList("a", "b"));
      assertEquals(subject.getValue(), "ab");

      subject.addValueChangeHandler(changeHandler);

      subject.recomputeAfterRunning(new Runnable()
      {
         public void run()
         {
            source.setElements(Arrays.asList("ignored"));
            source.setElements(Arrays.asList("ignored"));
            source.setElements(Arrays.asList("c", "d"));
         }
      });

      verify(changeHandler, times(1)).onValueChange(argThat(new IsValueChangeEventWithValue<String>("cd")));

   }

   @Test
   @SuppressWarnings("unchecked")
   public void recomputeAfterRunningFunctionChange()
   {
      ValueChangeHandler<String> changeHandler = mock(ValueChangeHandler.class);

      subject.setFunction(concat);
      source.setElements(Arrays.asList("a", "b"));
      assertEquals(subject.getValue(), "ab");

      subject.addValueChangeHandler(changeHandler);

      subject.recomputeAfterRunning(new Runnable()
      {
         public void run()
         {
            subject.setFunction(reverseConcat);
         }
      });

      verify(changeHandler, times(1)).onValueChange(argThat(new IsValueChangeEventWithValue<String>("ba")));

   }

   @Test
   @SuppressWarnings("unchecked")
   public void recomputeAfterWorksOkWithReEntrantCall()
   {
      ValueChangeHandler<String> changeHandler = mock(ValueChangeHandler.class);

      subject.setFunction(concat);
      source.setElements(Arrays.asList("", ""));
      assertEquals(subject.getValue(), "");

      subject.addValueChangeHandler(changeHandler);


      subject.recomputeAfterRunning(new Runnable()
      {
         public void run()
         {
            source.setElements(Arrays.asList("a"));
            subject.recomputeAfterRunning(new Runnable()
            {
               public void run()
               {
                  source.setElements(Arrays.asList("a", "b"));
               }
            });
            subject.setFunction(reverseConcat);
         }
      });

      verify(changeHandler, times(1)).onValueChange(argThat(new IsValueChangeEventWithValue<String>("ba")));

      // and events should be happening as normal again.
      subject.setFunction(concat);
      verify(changeHandler, times(1)).onValueChange(argThat(new IsValueChangeEventWithValue<String>("ab")));
   }

   private static class Concat implements Reduce<String, String>
   {
      public String compute(List<? extends String> source)
      {
         String result = "";
         for (String s : source)
         {
            if (s != null)
            {
               result += s;
            }
         }
         return result;
      }
   }

   private static class ConcatReverse implements Reduce<String, String>
   {
      public String compute(List<? extends String> source)
      {
         String result = "";
         for (String s : source)
         {
            if (s != null)
            {
               result = s + result;
            }
         }
         return result;
      }
   }
}
