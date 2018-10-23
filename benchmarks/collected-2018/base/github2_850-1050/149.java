// https://searchcode.com/api/result/98810862/

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.session;
import static org.apache.hadoop.hive.metastore.MetaStoreUtils.DEFAULT_DATABASE_NAME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.common.JavaUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.ql.MapRedStats;
import org.apache.hadoop.hive.ql.exec.Utilities;
import org.apache.hadoop.hive.ql.exec.tez.TezSessionPoolManager;
import org.apache.hadoop.hive.ql.exec.tez.TezSessionState;
import org.apache.hadoop.hive.ql.history.HiveHistory;
import org.apache.hadoop.hive.ql.history.HiveHistoryImpl;
import org.apache.hadoop.hive.ql.history.HiveHistoryProxyHandler;
import org.apache.hadoop.hive.ql.log.PerfLogger;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.HiveUtils;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.HiveAuthorizationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizer;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizerFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveMetastoreClientFactoryImpl;
import org.apache.hadoop.hive.ql.util.DosToUnix;
import org.apache.hadoop.hive.shims.ShimLoader;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * SessionState encapsulates common data associated with a session.
 *
 * Also provides support for a thread static session object that can be accessed
 * from any point in the code to interact with the user and to retrieve
 * configuration information
 */
public class SessionState {
  private static final Log LOG = LogFactory.getLog(SessionState.class);

  protected ClassLoader parentLoader;

  /**
   * current configuration.
   */
  protected HiveConf conf;

  /**
   * silent mode.
   */
  protected boolean isSilent;

  /**
   * verbose mode
   */
  protected boolean isVerbose;

  /**
   * Is the query served from HiveServer2
   */
  private boolean isHiveServerQuery = false;

  /*
   * HiveHistory Object
   */
  protected HiveHistory hiveHist;

  /**
   * Streams to read/write from.
   */
  public InputStream in;
  public PrintStream out;
  public PrintStream info;
  public PrintStream err;
  /**
   * Standard output from any child process(es).
   */
  public PrintStream childOut;
  /**
   * Error output from any child process(es).
   */
  public PrintStream childErr;

  /**
   * Temporary file name used to store results of non-Hive commands (e.g., set, dfs)
   * and HiveServer.fetch*() function will read results from this file
   */
  protected File tmpOutputFile;

  /**
   * type of the command.
   */
  private HiveOperation commandType;

  private String lastCommand;

  private HiveAuthorizationProvider authorizer;

  private HiveAuthorizer authorizerV2;

  public enum AuthorizationMode{V1, V2};

  private HiveAuthenticationProvider authenticator;

  private CreateTableAutomaticGrant createTableGrants;

  private Map<String, MapRedStats> mapRedStats;

  private Map<String, String> hiveVariables;

  // A mapping from a hadoop job ID to the stack traces collected from the map reduce task logs
  private Map<String, List<List<String>>> stackTraces;

  // This mapping collects all the configuration variables which have been set by the user
  // explicitly, either via SET in the CLI, the hiveconf option, or a System property.
  // It is a mapping from the variable name to its value.  Note that if a user repeatedly
  // changes the value of a variable, the corresponding change will be made in this mapping.
  private Map<String, String> overriddenConfigurations;

  private Map<String, List<String>> localMapRedErrors;

  private TezSessionState tezSessionState;

  private String currentDatabase;

  private final String CONFIG_AUTHZ_SETTINGS_APPLIED_MARKER =
      "hive.internal.ss.authz.settings.applied.marker";

  /**
   * Lineage state.
   */
  LineageState ls;

  private PerfLogger perfLogger;

  private final String userName;

  /**
   * Get the lineage state stored in this session.
   *
   * @return LineageState
   */
  public LineageState getLineageState() {
    return ls;
  }

  public HiveConf getConf() {
    return conf;
  }

  public void setConf(HiveConf conf) {
    this.conf = conf;
  }

  public File getTmpOutputFile() {
    return tmpOutputFile;
  }

  public void setTmpOutputFile(File f) {
    tmpOutputFile = f;
  }

  public boolean getIsSilent() {
    if(conf != null) {
      return conf.getBoolVar(HiveConf.ConfVars.HIVESESSIONSILENT);
    } else {
      return isSilent;
    }
  }

  public boolean isHiveServerQuery() {
    return this.isHiveServerQuery;
  }

  public void setIsSilent(boolean isSilent) {
    if(conf != null) {
      conf.setBoolVar(HiveConf.ConfVars.HIVESESSIONSILENT, isSilent);
    }
    this.isSilent = isSilent;
  }

  public boolean getIsVerbose() {
    return isVerbose;
  }

  public void setIsVerbose(boolean isVerbose) {
    this.isVerbose = isVerbose;
  }

  public void setIsHiveServerQuery(boolean isHiveServerQuery) {
    this.isHiveServerQuery = isHiveServerQuery;
  }

  public SessionState(HiveConf conf) {
    this(conf, null);
  }

  public SessionState(HiveConf conf, String userName) {
    this.conf = conf;
    this.userName = userName;
    isSilent = conf.getBoolVar(HiveConf.ConfVars.HIVESESSIONSILENT);
    ls = new LineageState();
    overriddenConfigurations = new HashMap<String, String>();
    overriddenConfigurations.putAll(HiveConf.getConfSystemProperties());
    // if there isn't already a session name, go ahead and create it.
    if (StringUtils.isEmpty(conf.getVar(HiveConf.ConfVars.HIVESESSIONID))) {
      conf.setVar(HiveConf.ConfVars.HIVESESSIONID, makeSessionId());
    }
    parentLoader = JavaUtils.getClassLoader();
  }

  public void setCmd(String cmdString) {
    conf.setVar(HiveConf.ConfVars.HIVEQUERYSTRING, cmdString);
  }

  public String getCmd() {
    return (conf.getVar(HiveConf.ConfVars.HIVEQUERYSTRING));
  }

  public String getQueryId() {
    return (conf.getVar(HiveConf.ConfVars.HIVEQUERYID));
  }

  public Map<String, String> getHiveVariables() {
    if (hiveVariables == null) {
      hiveVariables = new HashMap<String, String>();
    }
    return hiveVariables;
  }

  public void setHiveVariables(Map<String, String> hiveVariables) {
    this.hiveVariables = hiveVariables;
  }

  public String getSessionId() {
    return (conf.getVar(HiveConf.ConfVars.HIVESESSIONID));
  }

  /**
   * Singleton Session object per thread.
   *
   **/
  private static ThreadLocal<SessionState> tss = new ThreadLocal<SessionState>();

  /**
   * start a new session and set it to current session.
   */
  public static SessionState start(HiveConf conf) {
    SessionState ss = new SessionState(conf);
    return start(ss);
  }

  /**
   * Sets the given session state in the thread local var for sessions.
   */
  public static void setCurrentSessionState(SessionState startSs) {
    tss.set(startSs);
    Thread.currentThread().setContextClassLoader(startSs.getConf().getClassLoader());
  }

  public static void detachSession() {
    tss.remove();
  }

  /**
   * set current session to existing session object if a thread is running
   * multiple sessions - it must call this method with the new session object
   * when switching from one session to another.
   */
  public static SessionState start(SessionState startSs) {

    setCurrentSessionState(startSs);

    if(startSs.hiveHist == null){
      if (startSs.getConf().getBoolVar(HiveConf.ConfVars.HIVE_SESSION_HISTORY_ENABLED)) {
        startSs.hiveHist = new HiveHistoryImpl(startSs);
      }else {
        //Hive history is disabled, create a no-op proxy
        startSs.hiveHist = HiveHistoryProxyHandler.getNoOpHiveHistoryProxy();
      }
    }

    if (startSs.getTmpOutputFile() == null) {
      // set temp file containing results to be sent to HiveClient
      try {
        startSs.setTmpOutputFile(createTempFile(startSs.getConf()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // Get the following out of the way when you start the session these take a
    // while and should be done when we start up.
    try {
      //Hive object instance should be created with a copy of the conf object. If the conf is
      // shared with SessionState, other parts of the code might update the config, but
      // Hive.get(HiveConf) would not recognize the case when it needs refreshing
      Hive.get(new HiveConf(startSs.conf)).getMSC();
      ShimLoader.getHadoopShims().getUGIForConf(startSs.conf);
      FileSystem.get(startSs.conf);
    } catch (Exception e) {
      // catch-all due to some exec time dependencies on session state
      // that would cause ClassNoFoundException otherwise
      throw new RuntimeException(e);
    }

    if (HiveConf.getVar(startSs.getConf(), HiveConf.ConfVars.HIVE_EXECUTION_ENGINE)
        .equals("tez") && (startSs.isHiveServerQuery == false)) {
      try {
        if (startSs.tezSessionState == null) {
          startSs.tezSessionState = new TezSessionState(startSs.getSessionId());
        }
        if (!startSs.tezSessionState.isOpen()) {
          startSs.tezSessionState.open(startSs.conf); // should use conf on session start-up
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      LOG.info("No Tez session required at this point. hive.execution.engine=mr.");
    }
    return startSs;
  }

  /**
   * Setup authentication and authorization plugins for this session.
   */
  private void setupAuth() {

    if (authenticator != null) {
      // auth has been initialized
      return;
    }

    try {
      authenticator = HiveUtils.getAuthenticator(conf,
          HiveConf.ConfVars.HIVE_AUTHENTICATOR_MANAGER);
      authenticator.setSessionState(this);

      authorizer = HiveUtils.getAuthorizeProviderManager(conf,
          HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER, authenticator, true);

      if (authorizer == null) {
        // if it was null, the new authorization plugin must be specified in
        // config
        HiveAuthorizerFactory authorizerFactory = HiveUtils.getAuthorizerFactory(conf,
            HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER);

        authorizerV2 = authorizerFactory.createHiveAuthorizer(new HiveMetastoreClientFactoryImpl(),
            conf, authenticator);

        authorizerV2.applyAuthorizationConfigPolicy(conf);
        // create the create table grants with new config
        createTableGrants = CreateTableAutomaticGrant.create(conf);
      }

    } catch (HiveException e) {
      throw new RuntimeException(e);
    }

    if(LOG.isDebugEnabled()){
      Object authorizationClass = getAuthorizationMode() == AuthorizationMode.V1 ?
          getAuthorizer() : getAuthorizerV2();
          LOG.debug("Session is using authorization class " + authorizationClass.getClass());
    }
    return;
  }

  /**
   * @param conf
   * @return per-session temp file
   * @throws IOException
   */
  private static File createTempFile(HiveConf conf) throws IOException {
    String lScratchDir =
        HiveConf.getVar(conf, HiveConf.ConfVars.LOCALSCRATCHDIR);

    File tmpDir = new File(lScratchDir);
    String sessionID = conf.getVar(HiveConf.ConfVars.HIVESESSIONID);
    if (!tmpDir.exists()) {
      if (!tmpDir.mkdirs()) {
        //Do another exists to check to handle possible race condition
        // Another thread might have created the dir, if that is why
        // mkdirs returned false, that is fine
        if(!tmpDir.exists()){
          throw new RuntimeException("Unable to create log directory "
              + lScratchDir);
        }
      }
    }
    File tmpFile = File.createTempFile(sessionID, ".pipeout", tmpDir);
    tmpFile.deleteOnExit();
    return tmpFile;
  }

  /**
   * get the current session.
   */
  public static SessionState get() {
    return tss.get();
  }

  /**
   * get hiveHitsory object which does structured logging.
   *
   * @return The hive history object
   */
  public HiveHistory getHiveHistory() {
    return hiveHist;
  }

  /**
   * Create a session ID. Looks like:
   *   $user_$pid@$host_$date
   * @return the unique string
   */
  private static String makeSessionId() {
    return UUID.randomUUID().toString();
  }

  public String getLastCommand() {
    return lastCommand;
  }

  public void setLastCommand(String lastCommand) {
    this.lastCommand = lastCommand;
  }

  /**
   * This class provides helper routines to emit informational and error
   * messages to the user and log4j files while obeying the current session's
   * verbosity levels.
   *
   * NEVER write directly to the SessionStates standard output other than to
   * emit result data DO use printInfo and printError provided by LogHelper to
   * emit non result data strings.
   *
   * It is perfectly acceptable to have global static LogHelper objects (for
   * example - once per module) LogHelper always emits info/error to current
   * session as required.
   */
  public static class LogHelper {

    protected Log LOG;
    protected boolean isSilent;

    public LogHelper(Log LOG) {
      this(LOG, false);
    }

    public LogHelper(Log LOG, boolean isSilent) {
      this.LOG = LOG;
      this.isSilent = isSilent;
    }

    public PrintStream getOutStream() {
      SessionState ss = SessionState.get();
      return ((ss != null) && (ss.out != null)) ? ss.out : System.out;
    }

    public PrintStream getInfoStream() {
      SessionState ss = SessionState.get();
      return ((ss != null) && (ss.info != null)) ? ss.info : getErrStream();
    }

    public PrintStream getErrStream() {
      SessionState ss = SessionState.get();
      return ((ss != null) && (ss.err != null)) ? ss.err : System.err;
    }

    public PrintStream getChildOutStream() {
      SessionState ss = SessionState.get();
      return ((ss != null) && (ss.childOut != null)) ? ss.childOut : System.out;
    }

    public PrintStream getChildErrStream() {
      SessionState ss = SessionState.get();
      return ((ss != null) && (ss.childErr != null)) ? ss.childErr : System.err;
    }

    public boolean getIsSilent() {
      SessionState ss = SessionState.get();
      // use the session or the one supplied in constructor
      return (ss != null) ? ss.getIsSilent() : isSilent;
    }

    public void printInfo(String info) {
      printInfo(info, null);
    }

    public void printInfo(String info, String detail) {
      if (!getIsSilent()) {
        getInfoStream().println(info);
      }
      LOG.info(info + StringUtils.defaultString(detail));
    }

    public void printError(String error) {
      printError(error, null);
    }

    public void printError(String error, String detail) {
      getErrStream().println(error);
      LOG.error(error + StringUtils.defaultString(detail));
    }
  }

  private static LogHelper _console;

  /**
   * initialize or retrieve console object for SessionState.
   */
  public static LogHelper getConsole() {
    if (_console == null) {
      Log LOG = LogFactory.getLog("SessionState");
      _console = new LogHelper(LOG);
    }
    return _console;
  }

  /**
   *
   * @return username from current SessionState authenticator. username will be
   *         null if there is no current SessionState object or authenticator is
   *         null.
   */
  public static String getUserFromAuthenticator() {
    if (SessionState.get() != null && SessionState.get().getAuthenticator() != null) {
      return SessionState.get().getAuthenticator().getUserName();
    }
    return null;
  }

  static void validateFiles(List<String> newFiles) throws IllegalArgumentException {
    SessionState ss = SessionState.get();
    Configuration conf = (ss == null) ? new Configuration() : ss.getConf();

    LogHelper console = getConsole();
    for (String newFile : newFiles) {
      try {
        if (Utilities.realFile(newFile, conf) == null) {
          String message = newFile + " does not exist";
          throw new IllegalArgumentException(message);
        }
      } catch (IOException e) {
        String message = "Unable to validate " + newFile;
        throw new IllegalArgumentException(message, e);
      }
    }
  }

  static void registerJars(List<String> newJars) throws IllegalArgumentException {
    LogHelper console = getConsole();
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      ClassLoader newLoader = Utilities.addToClassPath(loader, newJars.toArray(new String[0]));
      Thread.currentThread().setContextClassLoader(newLoader);
      SessionState.get().getConf().setClassLoader(newLoader);
      console.printInfo("Added " + newJars + " to class path");
    } catch (Exception e) {
      String message = "Unable to register " + newJars;
      throw new IllegalArgumentException(message, e);
    }
  }

  static boolean unregisterJar(List<String> jarsToUnregister) {
    LogHelper console = getConsole();
    try {
      Utilities.removeFromClassPath(jarsToUnregister.toArray(new String[0]));
      console.printInfo("Deleted " + jarsToUnregister + " from class path");
      return true;
    } catch (Exception e) {
      console.printError("Unable to unregister " + jarsToUnregister
          + "\nException: " + e.getMessage(), "\n"
              + org.apache.hadoop.util.StringUtils.stringifyException(e));
      return false;
    }
  }

  /**
   * ResourceType.
   *
   */
  public static enum ResourceType {
    FILE,

    JAR {
      @Override
      public void preHook(Set<String> cur, List<String> s) throws IllegalArgumentException {
        super.preHook(cur, s);
        registerJars(s);
      }
      @Override
      public void postHook(Set<String> cur, List<String> s) {
        unregisterJar(s);
      }
    },
    ARCHIVE;

    public void preHook(Set<String> cur, List<String> s) throws IllegalArgumentException {
      validateFiles(s);
    }
    public void postHook(Set<String> cur, List<String> s) {
    }
  };

  public static ResourceType find_resource_type(String s) {

    s = s.trim().toUpperCase();

    try {
      return ResourceType.valueOf(s);
    } catch (IllegalArgumentException e) {
    }

    // try singular
    if (s.endsWith("S")) {
      s = s.substring(0, s.length() - 1);
    } else {
      return null;
    }

    try {
      return ResourceType.valueOf(s);
    } catch (IllegalArgumentException e) {
    }
    return null;
  }

  private final HashMap<ResourceType, Set<String>> resource_map =
      new HashMap<ResourceType, Set<String>>();

  public String add_resource(ResourceType t, String value) throws RuntimeException {
    return add_resource(t, value, false);
  }

  public String add_resource(ResourceType t, String value, boolean convertToUnix)
      throws RuntimeException {
    List<String> added = add_resources(t, Arrays.asList(value), convertToUnix);
    if (added == null || added.isEmpty()) {
      return null;
    }
    return added.get(0);
  }

  public List<String> add_resources(ResourceType t, List<String> values)
      throws RuntimeException {
    // By default don't convert to unix
    return add_resources(t, values, false);
  }

  public List<String> add_resources(ResourceType t, List<String> values, boolean convertToUnix)
      throws RuntimeException {
    Set<String> resourceMap = getResourceMap(t);

    List<String> localized = new ArrayList<String>();
    try {
      for (String value : values) {
        localized.add(downloadResource(value, convertToUnix));
      }

      t.preHook(resourceMap, localized);

    } catch (RuntimeException e) {
      getConsole().printError(e.getMessage(), "\n"
          + org.apache.hadoop.util.StringUtils.stringifyException(e));
      throw e;
    }

    getConsole().printInfo("Added resources: " + values);
    resourceMap.addAll(localized);

    return localized;
  }

  private Set<String> getResourceMap(ResourceType t) {
    Set<String> result = resource_map.get(t);
    if (result == null) {
      result = new HashSet<String>();
      resource_map.put(t, result);
    }
    return result;
  }

  /**
   * Returns  true if it is from any external File Systems except local
   */
  public static boolean canDownloadResource(String value) {
    // Allow to download resources from any external FileSystem.
    // And no need to download if it already exists on local file system.
    String scheme = new Path(value).toUri().getScheme();
    return (scheme != null) && !scheme.equalsIgnoreCase("file");
  }

  private String downloadResource(String value, boolean convertToUnix) {
    if (canDownloadResource(value)) {
      getConsole().printInfo("converting to local " + value);
      File resourceDir = new File(getConf().getVar(HiveConf.ConfVars.DOWNLOADED_RESOURCES_DIR));
      String destinationName = new Path(value).getName();
      File destinationFile = new File(resourceDir, destinationName);
      if (resourceDir.exists() && ! resourceDir.isDirectory()) {
        throw new RuntimeException("The resource directory is not a directory, resourceDir is set to" + resourceDir);
      }
      if (!resourceDir.exists() && !resourceDir.mkdirs()) {
        throw new RuntimeException("Couldn't create directory " + resourceDir);
      }
      try {
        FileSystem fs = FileSystem.get(new URI(value), conf);
        fs.copyToLocalFile(new Path(value), new Path(destinationFile.getCanonicalPath()));
        value = destinationFile.getCanonicalPath();

        // add "execute" permission to downloaded resource file (needed when loading dll file)
        FileUtil.chmod(value, "ugo+rx", true);
        if (convertToUnix && DosToUnix.isWindowsScript(destinationFile)) {
          try {
            DosToUnix.convertWindowsScriptToUnix(destinationFile);
          } catch (Exception e) {
            throw new RuntimeException("Caught exception while converting file " +
                destinationFile + " to unix line endings", e);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to read external resource " + value, e);
      }
    }
    return value;
  }

  public void delete_resources(ResourceType t, List<String> value) {
    Set<String> resources = resource_map.get(t);
    if (resources != null && !resources.isEmpty()) {
      t.postHook(resources, value);
      resources.removeAll(value);
    }
  }

  public Set<String> list_resource(ResourceType t, List<String> filter) {
    Set<String> orig = resource_map.get(t);
    if (orig == null) {
      return null;
    }
    if (filter == null) {
      return orig;
    } else {
      Set<String> fnl = new HashSet<String>();
      for (String one : orig) {
        if (filter.contains(one)) {
          fnl.add(one);
        }
      }
      return fnl;
    }
  }

  public void delete_resources(ResourceType t) {
    Set<String> resources = resource_map.get(t);
    if (resources != null && !resources.isEmpty()) {
      delete_resources(t, new ArrayList<String>(resources));
      resource_map.remove(t);
    }
  }

  public String getCommandType() {
    if (commandType == null) {
      return null;
    }
    return commandType.getOperationName();
  }

  public HiveOperation getHiveOperation() {
    return commandType;
  }

  public void setCommandType(HiveOperation commandType) {
    this.commandType = commandType;
  }

  public HiveAuthorizationProvider getAuthorizer() {
    setupAuth();
    return authorizer;
  }

  public void setAuthorizer(HiveAuthorizationProvider authorizer) {
    this.authorizer = authorizer;
  }

  public HiveAuthorizer getAuthorizerV2() {
    setupAuth();
    return authorizerV2;
  }

  public HiveAuthenticationProvider getAuthenticator() {
    setupAuth();
    return authenticator;
  }

  public void setAuthenticator(HiveAuthenticationProvider authenticator) {
    this.authenticator = authenticator;
  }

  public CreateTableAutomaticGrant getCreateTableGrants() {
    setupAuth();
    return createTableGrants;
  }

  public void setCreateTableGrants(CreateTableAutomaticGrant createTableGrants) {
    this.createTableGrants = createTableGrants;
  }

  public Map<String, MapRedStats> getMapRedStats() {
    return mapRedStats;
  }

  public void setMapRedStats(Map<String, MapRedStats> mapRedStats) {
    this.mapRedStats = mapRedStats;
  }

  public void setStackTraces(Map<String, List<List<String>>> stackTraces) {
    this.stackTraces = stackTraces;
  }

  public Map<String, List<List<String>>> getStackTraces() {
    return stackTraces;
  }

  public Map<String, String> getOverriddenConfigurations() {
    if (overriddenConfigurations == null) {
      overriddenConfigurations = new HashMap<String, String>();
    }
    return overriddenConfigurations;
  }

  public void setOverriddenConfigurations(Map<String, String> overriddenConfigurations) {
    this.overriddenConfigurations = overriddenConfigurations;
  }

  public Map<String, List<String>> getLocalMapRedErrors() {
    return localMapRedErrors;
  }

  public void addLocalMapRedErrors(String id, List<String> localMapRedErrors) {
    if (!this.localMapRedErrors.containsKey(id)) {
      this.localMapRedErrors.put(id, new ArrayList<String>());
    }

    this.localMapRedErrors.get(id).addAll(localMapRedErrors);
  }

  public void setLocalMapRedErrors(Map<String, List<String>> localMapRedErrors) {
    this.localMapRedErrors = localMapRedErrors;
  }

  public String getCurrentDatabase() {
    if (currentDatabase == null) {
      currentDatabase = DEFAULT_DATABASE_NAME;
    }
    return currentDatabase;
  }

  public void setCurrentDatabase(String currentDatabase) {
    this.currentDatabase = currentDatabase;
  }

  public void close() throws IOException {
    JavaUtils.closeClassLoadersTo(conf.getClassLoader(), parentLoader);
    File resourceDir =
        new File(getConf().getVar(HiveConf.ConfVars.DOWNLOADED_RESOURCES_DIR));
    LOG.debug("Removing resource dir " + resourceDir);
    try {
      if (resourceDir.exists()) {
        FileUtils.deleteDirectory(resourceDir);
      }
    } catch (IOException e) {
      LOG.info("Error removing session resource dir " + resourceDir, e);
    } finally {
      detachSession();
    }

    try {
      if (tezSessionState != null) {
        TezSessionPoolManager.getInstance().close(tezSessionState);
      }
    } catch (Exception e) {
      LOG.info("Error closing tez session", e);
    } finally {
      tezSessionState = null;
    }
  }

  public AuthorizationMode getAuthorizationMode(){
    setupAuth();
    if(authorizer != null){
      return AuthorizationMode.V1;
    }else if(authorizerV2 != null){
      return AuthorizationMode.V2;
    }
    //should not happen - this should not get called before this.start() is called
    throw new AssertionError("Authorization plugins not initialized!");
  }

  public boolean isAuthorizationModeV2(){
    return getAuthorizationMode() == AuthorizationMode.V2;
  }

  /**
   * @param resetPerfLogger
   * @return  Tries to return an instance of the class whose name is configured in
   *          hive.exec.perf.logger, but if it can't it just returns an instance of
   *          the base PerfLogger class

   */
  public PerfLogger getPerfLogger(boolean resetPerfLogger) {
    if ((perfLogger == null) || resetPerfLogger) {
      try {
        perfLogger = (PerfLogger) ReflectionUtils.newInstance(conf.getClassByName(
            conf.getVar(ConfVars.HIVE_PERF_LOGGER)), conf);
      } catch (ClassNotFoundException e) {
        LOG.error("Performance Logger Class not found:" + e.getMessage());
        perfLogger = new PerfLogger();
      }
    }
    return perfLogger;
  }

  public TezSessionState getTezSession() {
    return tezSessionState;
  }

  public void setTezSession(TezSessionState session) {
    this.tezSessionState = session;
  }

  public String getUserName() {
    return userName;
  }

  /**
   * If authorization mode is v2, then pass it through authorizer so that it can apply
   * any security configuration changes.
   */
  public void applyAuthorizationPolicy() throws HiveException {
    if(!isAuthorizationModeV2()){
      // auth v1 interface does not have this functionality
      return;
    }

    // avoid processing the same config multiple times, check marker
    if (conf.get(CONFIG_AUTHZ_SETTINGS_APPLIED_MARKER, "").equals(Boolean.TRUE.toString())) {
      return;
    }

    authorizerV2.applyAuthorizationConfigPolicy(conf);
    // set a marker that this conf has been processed.
    conf.set(CONFIG_AUTHZ_SETTINGS_APPLIED_MARKER, Boolean.TRUE.toString());

  }
}

