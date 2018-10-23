// https://searchcode.com/api/result/102311811/

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
package gov.nasa.jpf.modelGenerator.sourceGenerator.util;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to informations available through reflection 
 * without the hassle of dealing with {@link ClassNotFoundException} and similar.
 * 
 * Moreover this class provides some shorthand methods to access more concisely 
 * informations about:
 * 
 *   - The implemented interfaces (if any)
 *   - The superclass
 *   - The modifiers of the class
 *   
 * This class provides also string representation of all these informations,
 * dealing with generics. For example, among the implemented interfaces
 * of class `java.util.LinkedList` there is `java.util.List` that makes 
 * use of generics. The method {@link #getImplementedInterfacesStrings()}
 * will return a string that contains `java.util.List<E>`, with `E` being
 * the correct type parameter for `java.util.LinkedList<E>`.
 * 
 * Finally {@link ClassReflectionInfo} performs some sort of caching of already
 * loaded classes, in order to reduce the time of future accesses.
 * 
 * @author Matteo Ceccarello
 */
public class ClassReflectionInfo {
  
  static {
    cache = new HashMap<String, ClassReflectionInfo>();
  }
  
  /**
   * The cache used to store the already accessed {@link ClassReflectionInfo}s.
   */
  static Map<String, ClassReflectionInfo> cache;

  /**
   * Public factory method. This is the entry point of the class, that
   * loads the class if it is not in the cache, otherwise it will get it from there.
   * 
   * @param className the class we want to load
   * @return the {@link ClassReflectionInfo} object giving access to the underlying 
   *         {@link Class} informations.
   */
  public static ClassReflectionInfo getClassInfo(String className){
    Class<?> clazz = handleBaseClasses(className);
    if (clazz == null) {
      try {
        if (className.endsWith("[]"))
          clazz = loadArrayClass(className);
        else
          clazz = Class.forName(className);
      } catch (ClassNotFoundException e) {
        fail(className, e);
      }
    }
    return getClassInfo(clazz);
  }
  
  /**
   * Public factory method. This is the entry point of the class, that
   * loads the class if it is not in the cache, otherwise it will get it from there.
   * 
   * @param clazz the class we want to load
   * @return the {@link ClassReflectionInfo} object giving access to the underlying 
   *         {@link Class} informations.
   */
  public static ClassReflectionInfo getClassInfo(Class<?> clazz){
    ClassReflectionInfo cri = cache.get(clazz.getCanonicalName());
    if(cri == null){
      cri = new ClassReflectionInfo(clazz);
      cache.put(clazz.getCanonicalName(), cri);
    }
    return cri;
  }
  
  /**
   * Builds an error message and throws a {@link RuntimeException} that
   * will most likely abort the execution of the program.
   * 
   * The error message contains informations about the current class path.
   * 
   * @param requestedClassName the class we wanted but that we can't find
   * @param e the exception to be used as the cause of the {@link RuntimeException}
   */
  static void fail(String requestedClassName, Exception e) {
    String message = "\nFailed to load class: "+requestedClassName
        + "\nThe current classpath is: \n";
    for (String entry : System.getProperty("java.class.path").split(":")) {
      message += "\t"+entry+"\n";
    }
    throw new RuntimeException(message, e);
  }
  
  /*-------------------------------------------------------------------------*
   *                          Non static part                                |
   *-------------------------------------------------------------------------*/
  
  private Class<?> cls;
  
  private boolean isGeneric;
  private TypeVariable<?>[] typeParameters;
  
  private boolean implementInterfaces;
  private ClassReflectionInfo[] implementedInterfaces;
  
  /** True if this class subclasses a class other than Object */
  private boolean isSubclass;
  private ClassReflectionInfo superClass;  
  
  /**
   * Builds a new {@link ClassReflectionInfo}. Can be called only by 
   * the factory method.
   */
  ClassReflectionInfo(String className) {
    Class<?> baseClass = handleBaseClasses(className);
    if (baseClass != null) {
      this.cls = baseClass;
    } else {
      try {
        if (className.endsWith("[]"))
          this.cls = loadArrayClass(className);
        else
          this.cls = Class.forName(className);
      } catch (ClassNotFoundException e) {
        fail(className, e);
      }
    }
    init();
  }
  
  /**
   * Loads an array class (like `java.util.String[].class`).
   * 
   * *Attention*: call only if sure that className ends with `[]`,
   * since no check is performed about this. This is for performance reasons,
   * since the method is most likely to be called only by {@link #ClassReflectionInfo(String)}
   * that already performs the check.
   */
  static Class<?> loadArrayClass(String className) throws ClassNotFoundException {
    // strip trailing parentheses
    String name = "[L"+ className.substring(0, className.length()-2) + ";";
    return Class.forName(name);
  }
  
  /**
   * Builds a new {@link ClassReflectionInfo}. Can be called only by 
   * the factory method.
   */
  ClassReflectionInfo(Class<?> cls){
    this.cls = cls;
    init();
  }
  
  /**
   * Initializes all the variables. Called by the constructors.
   */
  private void init(){
    this.typeParameters = cls.getTypeParameters();
    this.isGeneric = (typeParameters.length > 0) ? true : false;
    
    Class<?>[] ifaces = cls.getInterfaces();
    this.implementedInterfaces = new ClassReflectionInfo[ifaces.length];
    for (int i = 0; i < ifaces.length; i++) {
      implementedInterfaces[i] = getClassInfo(ifaces[i]);
    }
    this.implementInterfaces = (implementedInterfaces.length > 0) ? true : false;
    
    Class<?> sc = cls.getSuperclass();
    this.superClass = (sc != null) ? getClassInfo(sc) : null;
    if(sc != null && !"java.lang.Object".equals(sc.getCanonicalName()))
      this.isSubclass =true;
    else this.isSubclass = false;
  }

  /**
   * Returns the class for base types (like `int` or `boolean[]`).
   * @param className the name of the base type we want o load
   * @return the corresponding class or null if className does not
   *         represent a base type (i.e. is an object reference) 
   */
  protected static Class<?> handleBaseClasses(String className){
    if("byte".equals(className))
      return byte.class;
    if("short".equals(className))
      return short.class;
    if("int".equals(className))
      return int.class;
    if("long".equals(className))
      return long.class;
    if("float".equals(className))
      return float.class;
    if("double".equals(className))
      return double.class;
    if("boolean".equals(className))
      return boolean.class;
    if("char".equals(className))
      return char.class;
    if("void".equals(className))
      return void.class;
    
 // deal with arrays of base types
    if("byte[]".equals(className))
      return byte[].class;
    if("short[]".equals(className))
      return short[].class;
    if("int[]".equals(className))
      return int[].class;
    if("long[]".equals(className))
      return long[].class;
    if("float[]".equals(className))
      return float[].class;
    if("double[]".equals(className))
      return double[].class;
    if("boolean[]".equals(className))
      return boolean[].class;
    if("char[]".equals(className))
      return char[].class;
    
    return null;
  }

  /**
   * This method can't be named getClass() in order not to override
   * {@link Object#getClass()}.
   * @return the underlying {@link Class} instance.
   */
  public Class<?> getCls() {
    return cls;
  }

  public String getClassName() {
    return cls.getName();
  }

  /**
   * @return true if the class makes use of parameterized types.
   */
  public boolean isGeneric() {
    return isGeneric;
  }

  public TypeVariable<?>[] getTypeParameters() {
    return typeParameters;
  }

  /**
   * Returns the type parameters as a string like `<K, V>`
   */
  public String getTypeParametersString() {
    if(isGeneric)
      return Arrays.toString(typeParameters).replace('[', '<').replace(']', '>');
    return "";
  }

  /**
   * @return true if the underlying class implements interfaces.
   */
  public boolean implementsInterfaces() {
    return implementInterfaces;
  }

  public ClassReflectionInfo[] getImplementedInterfaces() {
    return implementedInterfaces;
  }
  
  /**
   * Builds a string array containing all the implemented interfaces, with the correct
   * parameterized types if necessary.
   */
  public String[] getImplementedInterfacesStrings(){
    Type[] ifaceTypes = cls.getGenericInterfaces();
    String ifaces[] = new String[ifaceTypes.length];
    for (int i = 0; i<ifaceTypes.length; i++) {
      String implementedIface = ifaceTypes[i].toString();
      int idx = implementedIface.indexOf("interface");
      if(idx >= 0)
        implementedIface = implementedIface.substring(idx+10); // 10 = "interface".length() + 1
      ifaces[i] = implementedIface;
    }
    
    return ifaces;
  } 
  
  public boolean isSubclass() {
    return isSubclass;
  }

  public ClassReflectionInfo getSuperClass() {
    return superClass;
  }
  
  public boolean isInterface() {
    return cls.isInterface();
  }
  
  public boolean isEnum() {
    return cls.isEnum();
  }
  
  public boolean isAbstract() {
    return Modifier.isAbstract(cls.getModifiers());
  }
  
  /**
   * Builds a string with the superclass name, 
   * with type parameters if necessary.
   * The returned string has no leading `extends` keyword.
   */
  public String getSuperClassString() {
    String sc = cls.getGenericSuperclass().toString();
    int idx = sc.indexOf("class");
    if(idx >= 0)
      sc = sc.substring(idx+6); // 6 = "class".length() + 1
    return sc;
  }

  /**
   * @return a string containing all modifiers, like `public`, `final`
   *         and so on.
   */
  public String getModifiers() {
    int mod = this.cls.getModifiers();
    return Modifier.toString(mod);
  }
  
  @Override
  public String toString() {
    return getClassName() + getTypeParametersString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cls == null) ? 0 : cls.getCanonicalName().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ClassReflectionInfo other = (ClassReflectionInfo) obj;
    if (cls == null) {
      if (other.cls != null)
        return false;
    } else if (!cls.getCanonicalName().equals(other.cls.getCanonicalName()))
      return false;
    return true;
  }
  
  /*------------------------------------------------------------*
   *                      Static methods                        |
   *------------------------------------------------------------*/
  
  /**
   * Get the default return value for the given type.
   * 
   *  - For numeric types the default is 0
   *  - For `char` is `'\u0000'`
   *  - For `boolean` is `false`
   *  - For object references is `null`
   *  
   * @param typeName
   * @return
   */
  public static String getDefaultValue(String typeName) {
    // deal with base types
    if("byte".equals(typeName) 
        || "short".equals(typeName) 
        || "int".equals(typeName)
        || "long".equals(typeName)
        || "float".equals(typeName)
        || "double".equals(typeName))
     return "0";
    else if("boolean".equals(typeName))
      return "false";
    else if("char".equals(typeName))
      return "\'\\u0000\'";   
    else if(!"void".equals(typeName))
      return "null";
    return "";
  }
  
}

