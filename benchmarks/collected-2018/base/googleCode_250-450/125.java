// https://searchcode.com/api/result/11640472/

package org.testng.internal;

import org.testng.ClassMethodMap;
import org.testng.IMethodInstance;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestMethodFilter;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.thread.ThreadUtil;
import org.testng.xml.XmlSuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FIXME: reduce contention when this class is used through parallel invocation due to
 * invocationCount and threadPoolSize by not invoking the @BeforeClass and @AfterClass
 * which are already invoked on the original method.
 * 
 * This class implements Runnable and will invoke the ITestMethod passed in its
 * constructor on its run() method.
 * 
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class TestMethodWorker implements IMethodWorker {
  // Map of the test methods and their associated instances
  // It has to be a set because the same method can be passed several times
  // and associated to a different instance
  protected IMethodInstance[] m_testMethods;
  protected IInvoker m_invoker = null;
  protected Map<String, String> m_parameters = null;
  protected XmlSuite m_suite = null;
//  protected Map<ITestClass, Set<Object>> m_invokedBeforeClassMethods = null;
//  protected Map<ITestClass, Set<Object>> m_invokedAfterClassMethods = null;
  protected ITestNGMethod[] m_allTestMethods;
  protected List<ITestResult> m_testResults = new ArrayList<ITestResult>();
  protected ConfigurationGroupMethods m_groupMethods = null;
  protected ClassMethodMap m_classMethodMap = null;
  private ITestContext m_testContext = null;
  
  public TestMethodWorker(IInvoker invoker, 
                          IMethodInstance[] testMethods,
                          XmlSuite suite,
                          Map<String, String> parameters,
                          ITestNGMethod[] allTestMethods,
                          ConfigurationGroupMethods groupMethods,
                          ClassMethodMap classMethodMap,
                          ITestContext testContext)
  {
    m_invoker = invoker;
    m_testMethods = testMethods;
    m_suite = suite;
    m_parameters = parameters;
//    m_invokedBeforeClassMethods = invokedBeforeClassMethods;
//    m_invokedAfterClassMethods = invokedAfterClassMethods;
    m_allTestMethods = allTestMethods;
    m_groupMethods = groupMethods;
    m_classMethodMap = classMethodMap;
    m_testContext = testContext;
  }
  
  /**
   * Retrieves the maximum specified timeout of all ITestNGMethods to
   * be run.
   * 
   * @return the max timeout or 0 if no timeout was specified
   */
  public long getMaxTimeOut() {
    long result = 0;
    for (IMethodInstance mi : m_testMethods) {
      ITestNGMethod tm = mi.getMethod();
      if (tm.getTimeOut() > result) {
        result = tm.getTimeOut();
      }
    }
    
    return result;
  }
  
  @Override
  public String toString() {
    return "[Worker on thread:" + Thread.currentThread().getId() + " " 
    + m_testMethods[0].getMethod()
    + " " + m_testMethods[0].getInstances()[0]
    + "]";
  }
  
  /**
   * Run all the ITestNGMethods passed in through the constructor.
   * 
   * @see java.lang.Runnable#run()
   */
  public void run() {
    // Using an index here because we need to tell the invoker
    // the index of the current method
	  ITestMethodFilter filter=this.m_testContext.getSuite().getTestMethodFilter();
    for (int indexMethod = 0; indexMethod < m_testMethods.length; indexMethod++) {
      ITestNGMethod tm = m_testMethods[indexMethod].getMethod();
  
      ITestClass testClass = tm.getTestClass();
      if(filter==null||(filter!=null&&filter.canRunClass(testClass)))
    	  invokeBeforeClassMethods(testClass, m_testMethods[indexMethod]);
      else
    	  continue;
      //
      // Invoke test method
      //
      try {
    	  if(filter==null||(filter!=null&&filter.canRunMethod(m_testMethods[indexMethod])))
    		  invokeTestMethods(tm, m_testMethods[indexMethod].getInstances(), m_testContext);
          else
        	  continue;        
        
        if(org.testng.TestNG.isFail&&org.testng.TestNG.StopOnFailure)
        	break;
      }
      finally {
        invokeAfterClassMethods(testClass, m_testMethods[indexMethod]);
      }
    }
  }
  
  protected void invokeTestMethods(ITestNGMethod tm, Object[] instances,
      ITestContext testContext) 
  {
    // Potential bug here:  we look up the method index of tm among all
    // the test methods (not very efficient) but if this method appears
    // several times and these methods are run in parallel, the results
    // are unpredictable...  Need to think about this more (and make it
    // more efficient)
    List<ITestResult> testResults = 
        m_invoker.invokeTestMethods(tm, 
            m_allTestMethods, 
            indexOf(tm, m_allTestMethods), 
            m_suite, 
            m_parameters, 
            m_groupMethods,
            instances,
            testContext);
    
    if (testResults != null) {
      m_testResults.addAll(testResults);        
    }
  }
  
  //
  // Invoke the before class methods if not done already
  //
  protected void invokeBeforeClassMethods(ITestClass testClass, IMethodInstance mi) {
    // if no BeforeClass than return immediately
    // used for parallel case when BeforeClass were already invoked
    if( (null == m_classMethodMap) || (null == m_classMethodMap.getInvokedBeforeClassMethods())) {
      return;
    }
    ITestNGMethod[] classMethods= testClass.getBeforeClassMethods();
    if(null == classMethods || classMethods.length == 0) {
      return;
    }
    
    // the whole invocation must be synchronized as other threads must
    // get a full initialized test object (not the same for @After)
    Map<ITestClass, Set<Object>> invokedBeforeClassMethods= m_classMethodMap.getInvokedBeforeClassMethods();
    synchronized(invokedBeforeClassMethods) {
      Set<Object> instances= invokedBeforeClassMethods.get(testClass);
      if(null == instances) {
        instances= new HashSet<Object>();
        invokedBeforeClassMethods.put(testClass, instances);
      }
      for(Object instance: mi.getInstances()) {
        if (! instances.contains(instance)) {  
          instances.add(instance);
          m_invoker.invokeConfigurations(testClass,
                                         testClass.getBeforeClassMethods(),
                                         m_suite,
                                         m_parameters,
                                         null, /* no parameter values */
                                         instance);
        }
      }
    }
  }
  
  protected void invokeAfterClassMethods(ITestClass testClass, IMethodInstance mi) {
    // if no BeforeClass than return immediately
    // used for parallel case when BeforeClass were already invoked
    if( (null == m_classMethodMap) || (null == m_classMethodMap.getInvokedAfterClassMethods()) ) {
      return;
    }
    ITestNGMethod[] afterClassMethods= testClass.getAfterClassMethods();
    
    if(null == afterClassMethods || afterClassMethods.length == 0) {
      return;
    }
    
    //
    // Invoke after class methods if this test method is the last one
    //
    List<Object> invokeInstances= new ArrayList<Object>();
    ITestNGMethod tm= mi.getMethod();
    if (m_classMethodMap.removeAndCheckIfLast(tm)) {
      Map<ITestClass, Set<Object>> invokedAfterClassMethods= m_classMethodMap.getInvokedAfterClassMethods();
      synchronized(invokedAfterClassMethods) {
        Set<Object> instances = invokedAfterClassMethods.get(testClass);
        if(null == instances) {
          instances= new HashSet<Object>();
          invokedAfterClassMethods.put(testClass, instances);
        }
        for(Object inst: mi.getInstances()) {
          if(! instances.contains(inst)) {
            invokeInstances.add(inst);
          }
        }
      }
      
      for(Object inst: invokeInstances) {
        m_invoker.invokeConfigurations(testClass,
                                       afterClassMethods,
                                       m_suite,
                                       m_parameters,
                                       null, /* no parameter values */
                                       inst);
      }
    }
  }
  
  
  protected int indexOf(ITestNGMethod tm, ITestNGMethod[] allTestMethods) {
    for (int i = 0; i < allTestMethods.length; i++) {
      if (allTestMethods[i] == tm) return i;
    }
    return -1;
  }

  public List<ITestResult> getTestResults() {
    return m_testResults;
  }
  
  private void ppp(String s) {
    Utils.log("TestMethodWorker", 2, ThreadUtil.currentThreadInfo() + ":" + s);
  }

  public void setAllTestMethods(ITestNGMethod[] allTestMethods) {
    m_allTestMethods = allTestMethods;
  }
}

class SingleTestMethodWorker extends TestMethodWorker {
  private static final ConfigurationGroupMethods EMPTY_GROUP_METHODS=
    new ConfigurationGroupMethods(new ITestNGMethod[0], 
        new HashMap<String, List<ITestNGMethod>>(), new HashMap<String, List<ITestNGMethod>>());
  
  public SingleTestMethodWorker(IInvoker invoker, 
                                MethodInstance testMethod,
                                XmlSuite suite,
                                Map<String, String> parameters,
                                ITestNGMethod[] allTestMethods,
                                ITestContext testContext)
  {
    super(invoker,
          new MethodInstance[] {testMethod},
          suite,
          parameters,
          allTestMethods,
          EMPTY_GROUP_METHODS,
          null,
          testContext);
  }

  protected void invokeAfterClassMethods(ITestClass testClass, ITestNGMethod tm) {
    // HINT: do nothing
  }

  protected void invokeBeforeClassMethods(ITestClass testClass) {
    // HINT: do nothing
  }
}
