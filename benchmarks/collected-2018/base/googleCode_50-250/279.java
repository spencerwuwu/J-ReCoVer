// https://searchcode.com/api/result/3617433/

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

package com.pietschy.gwt.pectin.demo.client.metadata;

import com.google.gwt.core.client.GWT;
import com.pietschy.gwt.pectin.client.bean.BeanModelProvider;
import com.pietschy.gwt.pectin.client.form.FieldModel;
import com.pietschy.gwt.pectin.client.form.FormModel;
import com.pietschy.gwt.pectin.client.form.FormattedFieldModel;
import com.pietschy.gwt.pectin.client.form.ListFieldModel;
import com.pietschy.gwt.pectin.client.format.IntegerFormat;
import com.pietschy.gwt.pectin.client.function.Function;
import com.pietschy.gwt.pectin.client.function.Reduce;
import com.pietschy.gwt.pectin.client.value.ValueModel;
import com.pietschy.gwt.pectin.demo.client.domain.Person;
import com.pietschy.gwt.pectin.demo.client.domain.Protocol;
import com.pietschy.gwt.pectin.demo.client.domain.Wine;

import java.util.List;

import static com.pietschy.gwt.pectin.client.condition.Conditions.is;
import static com.pietschy.gwt.pectin.client.condition.Conditions.valueOf;
import static com.pietschy.gwt.pectin.client.form.metadata.MetadataPlugin.*;
import static com.pietschy.gwt.pectin.client.function.Functions.computedFrom;


/**
 * Created by IntelliJ IDEA.
 * User: andrew
 * Date: Aug 10, 2009
 * Time: 4:25:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetadataFormModel extends FormModel
{
   protected final FieldModel<String> givenName;
   protected final FieldModel<String> surname;
   protected final FieldModel<String> loginId;
   protected final FieldModel<String> password;
   protected final FieldModel<Boolean> revealPassword;

   protected final FieldModel<String> nickName;
   protected final FieldModel<Boolean> hasNickName;
   protected final FieldModel<Boolean> wineLover;
   protected final ListFieldModel<Wine> favoriteWines;
   protected final FieldModel<Boolean> hasFavoriteWines;
   protected final ListFieldModel<String> favoriteCheeses;
   protected final FieldModel<Boolean> cheeseLover;

   protected final FieldModel<Protocol> protocol;
   protected final FormattedFieldModel<Integer> port;
   protected final FieldModel<Integer> defaultPort;
   private ValueModel<String> defaultLoginId;

   public static abstract class PersonProvider extends BeanModelProvider<Person> {}
   private PersonProvider personProvider = GWT.create(PersonProvider.class);
   
   
   public MetadataFormModel()
   {
      // Create our field models..
      givenName = fieldOfType(String.class).boundTo(personProvider, "givenName");
      surname = fieldOfType(String.class).boundTo(personProvider, "surname");
      // some form only fields as I'm too lazy to add them to Person.
      loginId = fieldOfType(String.class).create();
      password = fieldOfType(String.class).createWithValue("secret");
      revealPassword = fieldOfType(Boolean.class).createWithValue(false);

      // compute our default login name from the first and last names.
      defaultLoginId = computedFrom(givenName, surname).using(new LoginIdGenerator());

      // now the static watermarks
      watermark(givenName).with("Enter your first name");
      watermark(surname).with("Enter your last name");

      // and a dynamic one...
      watermark(loginId).with(defaultLoginId);

      // now create our protocol fields.  This example is a bit more complex as the watermark and
      // default port values are derived from another field that can be changed by the user.
      protocol = fieldOfType(Protocol.class).createWithValue(Protocol.FTP);
      port = formattedFieldOfType(Integer.class).using(new IntegerFormat()).create();

      // the default port field is computed from the currently selected protocol.
      defaultPort = fieldOfType(Integer.class).computedFrom(protocol).using(new ProtocolToPortFunction());

      // only allow editing of the port when a protocol is selected.
      enable(port).when(valueOf(protocol).isNotNull());

      // and we use the default port as the watermark.  If we need special formatting we could
      // use withValueOf(defaultPort).formattedBy(..).
      watermark(port).withValueOf(defaultPort);

      // only display the default port if the user has entered a non null value that isn't the default.
      hide(defaultPort).when(valueOf(port).isNull().or(valueOf(port).isSameAs(defaultPort)));


      // now configure our nick name
      hasNickName = fieldOfType(Boolean.class).createWithValue(false);
      nickName = fieldOfType(String.class).boundTo(personProvider, "nickName");

      enable(nickName).when(hasNickName);


      // and our wine lists
      wineLover = fieldOfType(Boolean.class).boundTo(personProvider, "wineLover");
      hasFavoriteWines = fieldOfType(Boolean.class).createWithValue(false);
      favoriteWines = listOfType(Wine.class).boundTo(personProvider, "favoriteWines");

      enable(hasFavoriteWines).when(wineLover);
      enable(favoriteWines).when(is(wineLover).and(hasFavoriteWines));

      cheeseLover = fieldOfType(Boolean.class).boundTo(personProvider, "cheeseLover");
      favoriteCheeses = listOfType(String.class).boundTo(personProvider, "favoriteCheeses");

      show(favoriteCheeses).when(cheeseLover);
   }
   
   public void setPerson(Person person)
   {
      personProvider.setValue(person);
   }

   private static class ProtocolToPortFunction implements Function<Integer, Protocol>
   {
      public Integer compute(Protocol source)
      {
         return source != null ? source.getDefaultPort() : null;
      }
   }

   public class LoginIdGenerator implements Reduce<String, String>
   {
      private String separator;

      public LoginIdGenerator()
      {
         this(".");
      }

      public LoginIdGenerator(String separator)
      {
         this.separator = separator;
      }

      public String compute(List<? extends String> source)
      {
         StringBuilder buf = null;
         for (String value : source)
         {
            if (notBlankOrNull(value))
            {
               if (buf == null)
               {
                  buf = new StringBuilder();
               }
               else
               {
                  buf.append(separator);
               }

               buf.append(value.toLowerCase());
            }
         }
         return buf != null ? buf.toString() : "";
      }

      private boolean notBlankOrNull(String value)
      {
         return value != null && !value.trim().isEmpty();
      }
   }

}

