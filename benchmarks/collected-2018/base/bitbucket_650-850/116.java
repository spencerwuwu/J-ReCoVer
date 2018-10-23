// https://searchcode.com/api/result/121883021/

package nexj.core.testing.unit;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

import nexj.core.admin.etl.DataLoader;
import nexj.core.meta.Event;
import nexj.core.meta.Metadata;
import nexj.core.meta.MetadataException;
import nexj.core.meta.Repository;
import nexj.core.meta.integration.Channel;
import nexj.core.meta.integration.channel.jms.MessageQueue;
import nexj.core.meta.testing.unit.UnitTest;
import nexj.core.meta.testing.unit.UnitTestCase;
import nexj.core.rpc.jms.JMSSender;
import nexj.core.runtime.Context;
import nexj.core.runtime.InvocationContext;
import nexj.core.runtime.ThreadContextHolder;
import nexj.core.scripting.Function;
import nexj.core.scripting.GlobalEnvironment;
import nexj.core.scripting.Intrinsic;
import nexj.core.scripting.Machine;
import nexj.core.scripting.Machine.PCodeLocation;
import nexj.core.scripting.PCodeFunction;
import nexj.core.scripting.Pair;
import nexj.core.util.IOUtil;
import nexj.core.util.Logger;
import nexj.core.util.ObjUtil;
import nexj.core.util.Suspendable;
import nexj.core.util.URLUtil;
import nexj.core.util.XMLUtil;
import nexj.core.util.auth.SimplePrincipal;

/**
 * Plays the unit test.
 */
public class UnitTestPlayer
{
   // constants

   /**
    * The name of the resource with global definitions for loading before the tests.
    */
   protected final static String INITIAL_SCRIPT = "unittest.scm";

   /**
    * The resource URL of the initial script 
    */
   protected final static String INITIAL_SCRIPT_URL= "syslibrary:unittest";

   /**
    * The name of the method on the user class for granting debug privileges.
    */
   protected final static String GRANT_DEBUG = "grantDebugPrivileges";

   // attributes

   /**
    * The system user for initialization.
    */
   protected String m_sSystemUser;

   /**
    * Whether debugging is enabled.
    */
   protected boolean m_bDebug;

   /**
    * Whether to recreate the schema.
    */
   protected boolean m_bSchemaResetEnabled = true;

   // associations

   /**
    * The test locale.
    */
   protected Locale m_locale = Locale.getDefault();
   
   /**
    * The global scripting environment.
    */
   protected GlobalEnvironment m_globalEnv;
   
   /**
    * The list of unit tests to run sorted by URL of dump file
    */
   protected List m_unitTestList;

   /**
    * The J2EE container (if any) to be suspended during test case initialization
    * and resumed prior to running the test case.
    */
   protected Suspendable m_container;

   /**
    * The test logger.
    */
   protected final UnitTestLogger m_unitTestLogger;

   /**
    * The class logger.
    */
   protected final static Logger s_logger = Logger.getLogger(UnitTestPlayer.class);

   /**
    * The system log enabler.
    */
   protected final static Logger s_enabler = Logger.getSystemLogger(UnitTestPlayer.class);

   /**
    * Whether the thread is the main unit test thread. Boolean.TRUE or null.
    */
   protected static Thread s_mainThread;

   // constructors
   
   /**
    * Constructs the player with a unit test logger.
    * @param logger The unit test logger.
    */
   public UnitTestPlayer(UnitTestLogger logger, Suspendable container)
   {
      m_unitTestLogger = logger;
      m_container = container;
   }

   // operations

   /**
    * Sets the system user for initialization.
    * @param sSystemUser The system user for initialization to set.
    */
   public void setSystemUser(String sSystemUser)
   {
      m_sSystemUser = sSystemUser;
   }

   /**
    * @return The system user for initialization.
    */
   public String getSystemUser()
   {
      return m_sSystemUser;
   }

   /**
    * Sets the test locale.
    * @param locale The test locale to set.
    */
   public void setLocale(Locale locale)
   {
      m_locale = locale;
   }

   /**
    * @return The test locale.
    */
   public Locale getLocale()
   {
      return m_locale;
   }

   /**
    * Set the debug mode.
    * @param bDebug True to enable debugging
    */
   public void setDebug(boolean bDebug)
   {
      m_bDebug = bDebug;
   }

   /**
    * @param bSchemaResetEnabled true to perform schema recreate.
    */
   public void setSchemaResetEnabled(boolean bSchemaResetEnabled)
   {
      m_bSchemaResetEnabled = bSchemaResetEnabled;
   }

   /**
    * @return Whether script debugging is enabled
    */
   public boolean isDebug()
   {
      return m_bDebug;
   }

   /**
    * @return The global environment.
    */
   public GlobalEnvironment getGlobalEnvironment()
   {
      return m_globalEnv;
   }

   /**
    * Runs a single unit test
    * @param unitTest Unit test metadata
    */
   public void run(UnitTest unitTest)
   {
      run(Collections.singletonList(unitTest));
   }

   /**
    * Runs a collection of unit tests
    * @param unitTestCollection A collection of unit test metadata
    */
   public void run(Collection unitTestCollection)
   {
      m_unitTestList = new ArrayList(unitTestCollection);

      Collections.sort(m_unitTestList, new Comparator()
      {
         public int compare(Object left, Object right)
         {
            UnitTest leftUnitTest = (UnitTest)left;
            UnitTest rightUnitTest = (UnitTest)right;
            URL leftDumpURL = getDumpURL(leftUnitTest);
            URL rightDumpURL = getDumpURL(rightUnitTest);
            
            if (leftDumpURL == null)
            {
               return (rightDumpURL == null) ? 0 : -1;
            }
            
            int n = leftDumpURL.toString().compareTo(rightDumpURL.toString());

            if (n != 0)
            {
               return n;
            }

            return leftUnitTest.getName().compareToIgnoreCase(rightUnitTest.getName());
         }
      });

      run();
   }

   /**
    * Get whether the current thread is the main test thread.
    * @return Whether the current thread is the main test thread.
    */
   public static boolean isMainThread()
   {
      synchronized (UnitTestPlayer.class)
      {
         return s_mainThread == Thread.currentThread();
      }
   }

   /**
    * Runs the unit test.
    * @see java.lang.Runnable#run()
    */
   protected void run()
   {
      Context contextSaved = ThreadContextHolder.getContext();
      boolean bLogEnabledSaved = Logger.isEnabled();

      synchronized (UnitTestPlayer.class)
      {
         s_mainThread = Thread.currentThread();
      }

      try
      {
         Metadata metadata = Repository.getMetadata();
         boolean bFirstRun = true;
         int nTestCaseCount = 0;
         int nFailureCount = 0;
         int nErrorCount = 0;
         m_unitTestLogger.begin();

         // Now that support has been added for a real JMS engine in Teee, we must set JMSSender
         // to not use it as most unit tests don't expect it and rely on synchronous message delivery. 
         for (Iterator itr = metadata.getChannelIterator(); itr.hasNext(); )
         {
            Channel channel = (Channel)itr.next();

            if (channel instanceof MessageQueue)
            {
               MessageQueue mq = (MessageQueue)channel;

               if (mq.isSendable())
               {
                  ((JMSSender)mq.getSender().getInstance(null)).setEnabled(false);
               }
            }
         }

         boolean bReady = false;

         for (int nUnitTest = 0; nUnitTest < m_unitTestList.size(); ++nUnitTest)
         {
            boolean bInit = m_globalEnv == null;

            s_enabler.enable();

            if (bInit)
            {
               m_globalEnv = new GlobalEnvironment(metadata.getGlobalEnvironment());
            }

            InvocationContext context = (InvocationContext)metadata.getComponent("System.InvocationContext").getInstance(null);

            context.setAudited(false);
            context.initialize(null, m_globalEnv);
            context.setLocale(m_locale);

            Machine machine = context.getMachine();

            if (bInit)
            {
               InputStream istream = null;
               Reader reader = null;

               try
               {
                  istream = URLUtil.openResource(UnitTest.class, INITIAL_SCRIPT);
                  reader = IOUtil.openBufferedReader(istream, XMLUtil.ENCODING);
                  Intrinsic.load(reader, INITIAL_SCRIPT_URL, machine);
               }
               catch (IOException e)
               {
                  throw new MetadataException("err.meta.resourceOpen", new Object[]{INITIAL_SCRIPT}, e);
               }
               finally
               {
                  IOUtil.close(reader);
                  IOUtil.close(istream);
               }
            }

            UnitTest utest = (UnitTest)m_unitTestList.get(nUnitTest);

            for (Iterator argItr = new ArgumentIterator(context, utest); argItr.hasNext(); )
            {
               Object[] variableArray = (Object[])argItr.next();
               String sTestArguments = formatLoopVariables(variableArray, utest.getLoopCount());
               DataLoader dataLoader = new DataLoader(context);
               URL dumpURL = getDumpURL(utest);
               boolean bResetDB = dumpURL != null;

               m_unitTestLogger.begin(utest, sTestArguments);

               if (bResetDB && !bReady)
               {
                  if (m_container != null)
                  {
                     m_container.suspend();
                  }

                  bReady = true;

                  if (nUnitTest == 0)
                  {
                     try
                     {
                        if (m_bSchemaResetEnabled)
                        {
                           dataLoader.recreateSchema((Set)null);
                        }
                        else
                        {
                           dataLoader.deleteData((Set)null);
                        }
                     }
                     catch (Throwable e)
                     {
                        m_unitTestLogger.err(utest, e);

                        return;
                     }
                  }
                  else
                  {
                     bFirstRun = false;
                  }
               }

               byte nMode = utest.getMode();
               int nTestCase = 0;

               if (utest.hasInitializer())
               {
                  ++nTestCase;
               }

               if (utest.hasFinalizer())
               {
                  ++nTestCase;
               }

               Object[] functionObjArray = null;

               if (utest.getFunction() != null)
               {
                  functionObjArray = (Object[])context.getMachine().invoke(utest.getFunction(), variableArray);
               }

               boolean bFirstTestCase = true;

               for (Iterator itr = utest.getUnitTestCaseIterator(); itr.hasNext();
                  bFirstRun = false, bFirstTestCase = false, bResetDB = dumpURL != null && nMode != UnitTest.MODE_DIRTY)
               {
                  UnitTestCase testCase = (UnitTestCase)itr.next();

                  if (!isEnabled(utest, testCase))
                  {
                     nTestCase++;
                     continue;
                  }

                  if (bResetDB)
                  {
                     if (!bFirstTestCase && m_container != null)
                     {
                        m_container.suspend();
                     }

                     if (!bFirstRun)
                     {
                        try
                        {
                           dataLoader.deleteData((Set)null);
                        }
                        catch (Throwable e)
                        {
                           m_unitTestLogger.err(utest, e);

                           return;
                        }
                     }

                     InputStream in = null;

                     try
                     {
                        in = new BufferedInputStream(URLUtil.openStream(dumpURL));
                        dataLoader.importData(in, true);
                     }
                     catch (Throwable e)
                     {
                        m_unitTestLogger.err(utest, e);

                        return;
                     }
                     finally
                     {
                        IOUtil.close(in);
                        in = null;
                     }
                  }

                  context.complete(true);
                  context.getMachine().cleanup();
                  context.initUnitOfWork();

                  final InvocationContext testContext = (InvocationContext)metadata
                     .getComponent("System.InvocationContext").getInstance(null);

                  try
                  {
                     testContext.setAudited(false);
                     testContext.getGlobalCache().clear();
                     testContext.initialize((m_sSystemUser == null) ? null : new SimplePrincipal(m_sSystemUser),
                        new GlobalEnvironment(m_globalEnv));

                     if (m_container != null && m_sSystemUser != null)
                     {
                        Event grantDebugEvent = testContext.getUserClass().findEvent(GRANT_DEBUG, 0);

                        if (grantDebugEvent != null)
                        {
                           boolean bSecure = testContext.isSecure();

                           try
                           {
                              testContext.setSecure(false);
                              grantDebugEvent.invoke(testContext.getUser(), (Pair)null, testContext.getMachine());
                           }
                           finally
                           {
                              testContext.setSecure(bSecure);
                           }
                        }
                     }

                     if (bResetDB && m_container != null)
                     {
                        m_container.resume();

                        // Suspend the thread if in script debugging mode
                        if (m_bDebug)
                        {
                           PCodeFunction nextFunction = (PCodeFunction)((utest.hasInitializer()) ? functionObjArray[0]
                              : functionObjArray[nTestCase]);

                           s_logger.debug("Waiting for script debugger to connect");

                           try
                           {
                              Class clazz = Class.forName("nexj.core.scripting.debugger.server.GenericDebugSessionManager");
                              
                              Object debugSessionManager = clazz.getMethod("getInstance", null).invoke(null, null);
                              
                              clazz.getMethod("suspendOnStartup", new Class[]{PCodeLocation.class})
                                 .invoke(debugSessionManager, new Object[]{new PCodeLocation(nextFunction, 0)});
                           }
                           catch(Throwable t)
                           {
                              ObjUtil.rethrow(t);
                           }
                        }

                        ThreadContextHolder.setContext(testContext);
                     }

                     Logger.setEnabled(bLogEnabledSaved);

                     if (utest.hasInitializer())
                     {
                        try
                        {
                           testContext.getMachine().invoke((Function)functionObjArray[0], (Object[])null);
                        }
                        catch (Throwable e)
                        {
                           m_unitTestLogger.err(testCase, e);
                           ++nErrorCount;

                           continue;
                        }
                     }

                     m_unitTestLogger.begin(testCase);

                     try
                     {
                        nTestCaseCount++;
                        testContext.getMachine().invoke((Function)functionObjArray[nTestCase++], (Object[])null);
                        m_unitTestLogger.end(testCase);
                     }
                     catch (UnitTestAssertionException e)
                     {
                        m_unitTestLogger.fail(testCase, e);
                        ++nFailureCount;
                     }
                     catch (Throwable e)
                     {
                        m_unitTestLogger.err(testCase, e);
                        ++nErrorCount;
                     }

                     if (utest.hasFinalizer())
                     {
                        try
                        {
                           testContext.getMachine().invoke(
                              (Function)functionObjArray[(!utest.hasInitializer()) ? 0 : 1],
                              (Object[])null);
                        }
                        catch (Throwable e)
                        {
                           m_unitTestLogger.err(testCase, e);
                           ++nErrorCount;
                        }
                     }
                  }
                  finally
                  {
                     s_enabler.enable();
                     testContext.complete(false);
                     testContext.getGlobalCache().clear();
                     ThreadContextHolder.setContext(context); // context of DataLoader
                  }

                  bReady = false;
               }

               m_unitTestLogger.end(utest, sTestArguments);
            }

            context.complete(false); // free any memory retained by this context
         }

         m_unitTestLogger.end(nTestCaseCount, m_unitTestList.size(), nErrorCount, nFailureCount);
      }
      catch (Throwable e)
      {
         m_unitTestLogger.err(e);
      }
      finally
      {
         Logger.setEnabled(bLogEnabledSaved);
         ThreadContextHolder.setContext(contextSaved);

         synchronized (UnitTestPlayer.class)
         {
            s_mainThread = null;
         }
      }
   }

   /**
    * Decides whether the particular unit test case should be ignored
    * @param utest The unit tests
    * @param testCase The test case
    * @return True if the unit test case should be ignored
    */
   protected boolean isEnabled(UnitTest utest, UnitTestCase testCase)
   {
      return true;
   }

   /**
    * Return a dump URL for the given unit test
    * @param utest The unit test
    * @return A dump URL; can be null.
    */
   protected URL getDumpURL(UnitTest utest)
   {
      return utest.getDumpURL();
   }

   /**
    * Formats the loop variables as a string for display to user.
    * 
    * @param variableArray The test variables. Loop variables at beginning of array.
    * @param nLoopCount The number of loop variables.
    * @return A string representation of the loop variables.
    */
   protected static String formatLoopVariables(Object[] variableArray, int nLoopCount)
   {
      if (nLoopCount <= 0 || variableArray == null)
      {
         return null;
      }

      StringBuilder buf = new StringBuilder("[");

      for (int i = 0; i < nLoopCount; i++)
      {
         if (i > 0)
         {
            buf.append(", ");
         }

         buf.append(variableArray[i].toString());
      }

      buf.append(']');

      return buf.toString();
   }

   // inner classes

   /**
    * Iterates the unit test arguments.
    */
   protected static class ArgumentIterator implements Iterator
   {
      // attributes

      /**
       * The number of loops.
       */
      protected int m_nLoopCount;

      /**
       * True until the first item in the iteration has been returned.
       */
      protected boolean m_bFirst = true;

      // associations

      /**
       * The context for executing the loop value expressions.
       */
      protected InvocationContext m_context;

      /**
       * The unit test whose loops are being iterated.
       */
      protected UnitTest m_utest;

      /**
       * The iterator stack.
       */
      protected Iterator[] m_iteratorStack;

      /**
       * The next values to be returned by this iterator; null if at end of iteration.
       */
      protected Object[] m_nextValueArray;

      /**
       * The current values returned by this iterator; stored in a member variable to reduce
       * object creation.
       */
      protected Object[] m_currentValueArray;

      // constructors

      /**
       * Creates a new iterator over the loops of the given unit test. If the unit test has no
       * loops, then this iterator returns a single element filled with null values for the
       * non-loop variables.
       * 
       * @param context The context for executing the loop value expressions.
       * @param utest The unit test whose loops will be iterated.
       */
      public ArgumentIterator(InvocationContext context, UnitTest utest)
      {
         m_context = context;
         m_utest = utest;
         m_nLoopCount = utest.getLoopCount();
         int nVariableCount = utest.getAllVariableCount();

         if (m_nLoopCount > 0)
         {
            m_iteratorStack = new Iterator[m_nLoopCount];
            m_nextValueArray = new Object[nVariableCount];
         }

         m_currentValueArray = new Object[nVariableCount];

         internalNext();
      }

      // operations

      /**
       * @see java.util.Iterator#hasNext()
       */
      public boolean hasNext()
      {
         return (m_nextValueArray != null) || (m_nLoopCount == 0 && m_bFirst);
      }

      /**
       * @see java.util.Iterator#next()
       */
      public Object next()
      {
         if (m_bFirst)
         {
            m_bFirst = false;

            if (m_nLoopCount == 0)
            {
               return m_currentValueArray;
            }
         }

         if (m_nextValueArray == null)
         {
            throw new NoSuchElementException();
         }

         System.arraycopy(m_nextValueArray, 0, m_currentValueArray, 0, m_nextValueArray.length);
         internalNext();

         return m_currentValueArray;
      }

      /**
       * Advances to next values in this iteration.
       */
      protected void internalNext()
      {
         int nCurrentLoop = (m_bFirst) ? 0 : m_nLoopCount - 1;

         while (nCurrentLoop < m_nLoopCount)
         {
            Iterator itr = m_iteratorStack[nCurrentLoop];

            if (itr == null)
            {
               Function loopFunction = m_utest.getLoopFunction(nCurrentLoop);
               Object[] loopFunctionArgumentArray = new Object[nCurrentLoop];

               System.arraycopy(m_nextValueArray, 0, loopFunctionArgumentArray, 0, nCurrentLoop);

               Object result = m_context.getMachine().invoke(loopFunction, loopFunctionArgumentArray);

               itr = Intrinsic.getIterator(result);

               if (itr == null)
               {
                  throw new IllegalStateException("Loop function result not iterable");
               }

               m_iteratorStack[nCurrentLoop] = itr;
            }

            if (itr.hasNext())
            {
               m_nextValueArray[nCurrentLoop] = itr.next();
               nCurrentLoop++;
            }
            else
            {
               m_iteratorStack[nCurrentLoop--] = null;

               if (nCurrentLoop < 0)
               {
                  m_nextValueArray = null;

                  return;
               }
            }
         }
      }

      /**
       * @see java.util.Iterator#remove()
       */
      public void remove()
      {
         throw new UnsupportedOperationException();
      }
   }
}

