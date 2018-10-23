// https://searchcode.com/api/result/102311731/

//
// Copyright (C) 2012 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.modelGenerator.sourceGenerator.generator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;


/**
 * Class used to reduce all whitespaces in strings
 * to a single whitespace and multiple newlines to
 * single ones.
 * This way tests on generated sources can ignore
 * formatting issues.
 * 
 * @author Matteo Ceccarello
 *
 */
@RunWith(Enclosed.class)
public class StringNormalizer {
  
  private static String blanks = "[ \\t]+";
  private static String newLines = "\\n+";
  
  public static String normalize(String s) {
    String normalized = s
        .replaceAll(blanks, " ")
        .replaceAll(newLines, "\n");
    return normalized;
  }
  
  public static class StringNormalizerTest {
    
    @Test
    public void test1(){
      String expected = "Hello\nworld I'm\n a test";
      String actual = normalize("Hello\n\n\nworld\t  \t I'm\n a test");
      assertEquals(expected, actual);
    }
    
  }

}

