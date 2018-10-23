// https://searchcode.com/api/result/3617440/

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

package com.pietschy.gwt.pectin.demo.client.basic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.pietschy.gwt.pectin.client.bean.BeanModelProvider;
import com.pietschy.gwt.pectin.client.bean.NestedTypes;
import com.pietschy.gwt.pectin.client.form.FieldModel;
import com.pietschy.gwt.pectin.client.form.FormModel;
import com.pietschy.gwt.pectin.client.form.ListFieldModel;
import com.pietschy.gwt.pectin.client.function.Join;
import com.pietschy.gwt.pectin.client.function.Reduce;
import com.pietschy.gwt.pectin.client.value.ValueModel;
import com.pietschy.gwt.pectin.demo.client.domain.Address;
import com.pietschy.gwt.pectin.demo.client.domain.Gender;
import com.pietschy.gwt.pectin.demo.client.domain.Person;
import com.pietschy.gwt.pectin.demo.client.domain.Wine;

import java.util.List;

/**
 *
 */
public class BasicFormModel extends FormModel
{
   @NestedTypes(Address.class)
   public static abstract class PersonProvider extends BeanModelProvider<Person> {}

   private PersonProvider personProvider = GWT.create(PersonProvider.class);

   protected final FieldModel<String> givenName;
   protected final FieldModel<String> surname;
   protected final FieldModel<String> fullName;
   protected final FieldModel<Integer> lettersInName;
   protected final FieldModel<Gender> gender;
   protected final ListFieldModel<Wine> favoriteWines;
   protected final ListFieldModel<String> favoriteCheeses;
   protected final FieldModel<String> addressOne;
   protected final FieldModel<String> addressTwo;
   protected final FieldModel<String> suburb;
   protected final FieldModel<String> postCode;

   protected final ValueModel<Boolean> dirty;

   // one day I'll get around to writing a proper gui-action for these but
   // until now we'll just use a click handler.
   protected final ClickHandler saveHandler = new SaveHandler();
   protected final ClickHandler revertHandler = new RevertHandler();


   public BasicFormModel()
   {
      // Create our models...
      givenName = fieldOfType(String.class).boundTo(personProvider, "givenName");
      surname = fieldOfType(String.class).boundTo(personProvider, "surname");
      gender = fieldOfType(Gender.class).boundTo(personProvider, "gender");
      
      // some list fields
      favoriteWines = listOfType(Wine.class).boundTo(personProvider, "favoriteWines");
      favoriteCheeses = listOfType(String.class).boundTo(personProvider, "favoriteCheeses");
      
      // Some computed fields
      // (you can also compute regular models using the static methods of
      // the Functions class.  I.e. ValueModel m = Functions.computedFrom(..).using(..)
      fullName = fieldOfType(String.class)
         .computedFrom(givenName, surname)
         .using(new Join(" "));

      lettersInName = fieldOfType(Integer.class)
         .computedFrom(givenName, surname)
         .using(new CharacterCounter());

      // nested properties...
      addressOne= fieldOfType(String.class).boundTo(personProvider, "address.addressOne");
      addressTwo= fieldOfType(String.class).boundTo(personProvider, "address.addressTwo");
      suburb = fieldOfType(String.class).boundTo(personProvider, "address.suburb");
      postCode = fieldOfType(String.class).boundTo(personProvider, "address.postCode");

      dirty = personProvider.dirty();

   }

   public void setPerson(Person person)
   {
      personProvider.setValue(person);
   }

   // I'd prefer to use a proper gui-action here, but for now we'll
   // just use click handlers.
   private class SaveHandler implements ClickHandler
   {
      public void onClick(ClickEvent event)
      {
         personProvider.commit();
      }
   }
   
   private class RevertHandler implements ClickHandler
   {
      public void onClick(ClickEvent event)
      {
         personProvider.revert();
      }
   }

   private static class CharacterCounter implements Reduce<Integer, String>
   {
      public Integer compute(List<? extends String> source)
      {
         int total = 0;
         for (String name : source)
         {
            if (name != null)
            {
               // I'm ignoring the fact that there could be
               // whitespace in the name..
               total += name.trim().length();
            }
         }
         
         return total;
      }
   }
}

