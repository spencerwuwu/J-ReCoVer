// https://searchcode.com/api/result/55521828/

/**
------------------------------------
JOTWiki          Thibaut Colar
tcolar-wiki AT colar DOT net
Licence at http://www.jotwiki.net
------------------------------------
 */
package net.jotwiki.forms.setup;

import java.io.File;
import java.util.Hashtable;

import net.jot.logger.JOTLogger;
import net.jot.scheduler.JOTSchedulingOptions;
import net.jot.web.JOTFlowRequest;
import net.jot.web.forms.JOTFormConst;
import net.jot.web.forms.JOTGeneratedForm;
import net.jot.web.forms.ui.JOTFormCategory;
import net.jot.web.forms.ui.JOTFormCheckboxField;
import net.jot.web.forms.ui.JOTFormField;
import net.jot.web.forms.ui.JOTFormPasswordField;
import net.jot.web.forms.ui.JOTFormSubmitButton;
import net.jot.web.forms.ui.JOTFormTextField;
import net.jotwiki.JOTWikiFilter;
import net.jotwiki.WikiPreferences;
import net.jotwiki.WikiUtilities;
import net.jotwiki.db.WikiPermission;
import net.jotwiki.db.WikiUser;
public class GlobalSetup extends JOTGeneratedForm
{

  public final String DISPLAY_NAME = "display_name";
  public final String ADMIN_LOGIN = "setup_admin_login";
  public final String PASSWORD = "setup_password";
  public final String PASSWORD_CONFIRM = "setup_password_confirm";
  public final String STACK_TRACE = "setup_stack_trace";
  public final String CONSOLE_PRINT = "setup_console_print";
  public final String DEBUG_0 = "setup_debug0";
  public final String DEBUG_1 = "setup_debug1";
  public final String DEBUG_2 = "setup_debug2";
  public final String DEBUG_3 = "setup_debug3";
  public final String DEBUG_4 = "setup_debug4";
  public final String DEBUG_5 = "setup_debug5";
  public final String SITEMAP_START = "sitemap_start";
  public final String SITEMAP_SCHEDULE = "sitemap_schedule";
  public final String ADDTHIS_PUBLISHER = "addthis_publisher";
  public final String PURGE_LOGS = "purge_logs";
  public final String DB_BACKUPS = "db_backups";
  public final String MAIL_HOST = "mail_host";
  public final String MAIL_DOMAIN = "mail_domain";
  public final String MAIL_PORT = "mail_port";
  public final String MAIL_FROM = "mail_from";
  public final String ENCODE_EMAIL = "encode_email";
  public final String ENCODE_MAILTO = "encode_mailto";
  public final String INDEXING_AT_STARTUP = "index_startup";
  public final String INDEXING_SCHEDULE = "index_schedule";
  
  
  // sitemap generator schedule ?

  private WikiPreferences prefs;

  // Generates form help icon and message HTML 
  protected String getDescription(JOTFormField field, int spanCpt)
  {
    return WikiUtilities.getCustomFormDescription(field, spanCpt);
  }

  public void layoutForm(JOTFlowRequest request)
  {
    WikiUser admin = getAdmin();
    String password = admin == null ? "" : admin.getPassword();
    String name = admin == null ? "John Doe" : admin.getFirstName();

    setFormTitle("General Settings");
    setFormAction("submitsetup.do");

    addCategory(new JOTFormCategory("Administrator"));
    JOTFormField login = new JOTFormTextField(ADMIN_LOGIN, "Login", 20, "admin");
    //login.setDisabled(true);
    addFormField(login);
    get(ADMIN_LOGIN).setFlag("DISABLED");

    JOTFormTextField display = new JOTFormTextField(DISPLAY_NAME, "Display Name", 20, name);
    display.setHelp("Even though the Admin login is forced to 'admin', you can choose a different 'Display Name'<br>The Display Name is what is gonna be displayed as the 'Author' when you post a page while logged as 'admin'.");
    addFormField(display);
    addFormField(new JOTFormPasswordField(PASSWORD, "Password", 20, password));
    addFormField(new JOTFormPasswordField(PASSWORD_CONFIRM, "Confirm Password", 20, password));

    addCategory(new JOTFormCategory("Mail server"));
    String server = prefs.getDefaultedString(WikiPreferences.GLOBAL_MAIL_HOST, "");
    JOTFormTextField mailHost=new JOTFormTextField(MAIL_HOST, "SMTP Host:", 25, server);
    mailHost.setHelp("Enter the hostname of your smtp(mail) server or leave lank to disable mail.<br>This is used for example to email copy of posted comments.");
    addFormField(mailHost);

    String domain = prefs.getDefaultedString(WikiPreferences.GLOBAL_MAIL_DOMAIN, "mycompany.com");
    JOTFormTextField mailDomain=new JOTFormTextField(MAIL_DOMAIN, "Mail Domain:", 25, domain);
    mailDomain.setHelp("The domain you send mail from. ie 'mycompany.com'");
    addFormField(mailDomain);
    
    String from = prefs.getDefaultedString(WikiPreferences.GLOBAL_MAIL_FROM, "postmaster");
    JOTFormTextField mailFrom=new JOTFormTextField(MAIL_FROM, "Mail Sender:", 25, from);
    mailFrom.setHelp("The 'from' user for the email without the domain. ie: 'tomc'.<br>The user will also recieve the bounces and replies.");
    addFormField(mailFrom);

    String port = prefs.getDefaultedString(WikiPreferences.GLOBAL_MAIL_PORT, "25");
    JOTFormTextField mailPort=new JOTFormTextField(MAIL_PORT, "SMTP Port:", 3, port);
    addFormField(mailPort);
    
    addCategory(new JOTFormCategory("Logging"));
    boolean trace = prefs.getDefaultedBoolean(WikiPreferences.GLOBAL_STACK_TRACE, Boolean.TRUE).booleanValue();
    JOTFormField stackTrace = new JOTFormCheckboxField(STACK_TRACE, "Log java StackTraces", trace);
    stackTrace.setHelp("Wether to display the full java stack trace in the logs when an error occurs.");
    addFormField(stackTrace);
    boolean wantConsole = prefs.getDefaultedBoolean(WikiPreferences.GLOBAL_PRINT_TO_CONSOLE, Boolean.FALSE).booleanValue();
    JOTFormField console = new JOTFormCheckboxField(CONSOLE_PRINT, "Dump logs on console", wantConsole);
    console.setHelp("If you enable this, the logs will be dumped BOTH to the log file and on the system console.<br>This is practical for debugging as you will see the logs on the fly on the console without having to look/refresh the log file.");
    addFormField(console);

    String levels = prefs.getDefaultedString(WikiPreferences.GLOBAL_LOG_LEVELS, "2,3,4,5");
    String keepLogs = prefs.getDefaultedString(WikiPreferences.GLOBAL_KEEP_LOGS_FOR, "7");
    addFormField(new JOTFormCheckboxField(DEBUG_5, "Log critical errors", levels.indexOf("5") != -1));
    addFormField(new JOTFormCheckboxField(DEBUG_4, "Log errors", levels.indexOf("4") != -1));
    addFormField(new JOTFormCheckboxField(DEBUG_3, "Log Warnings", levels.indexOf("3") != -1));
    addFormField(new JOTFormCheckboxField(DEBUG_2, "Log Infos", levels.indexOf("2") != -1));
    String debugHelp = "<b>Warning:</b> If you enable minor debug, many things will be dumped to the logs, including potentially sensitive data such as passwords.<br><br>On top of that Enabling debug mode can reduce greatly performance.<br><br>So unless trying to resolve an issue keep both debugging options off.";
    JOTFormField debug1 = new JOTFormCheckboxField(DEBUG_1, "Log debug entries", levels.indexOf("1") != -1);
    debug1.setHelp(debugHelp);
    addFormField(debug1);
    JOTFormField debug0 = new JOTFormCheckboxField(DEBUG_0, "Log minor debug entries", levels.indexOf("0") != -1);
    debug0.setHelp(debugHelp);
    addFormField(debug0);
    addFormField(new JOTFormTextField(PURGE_LOGS, "Remove log files older than(days)", 3, keepLogs));

    addCategory(new JOTFormCategory("Spam settings"));
    boolean mailto = prefs.getDefaultedBoolean(WikiPreferences.GLOBAL_ENCODE_MAILTO, Boolean.TRUE).booleanValue();
    boolean email = prefs.getDefaultedBoolean(WikiPreferences.GLOBAL_ENCODE_EMAIL, Boolean.TRUE).booleanValue();
    JOTFormField mailtof=new JOTFormCheckboxField(ENCODE_MAILTO, "Encode mailto: links", mailto);
    mailtof.setHelp("To prevent spam, mailto: links will be encoded using javascript.<br>This helps preventing spam robots to harvest email addresses.");
    JOTFormField emailf=new JOTFormCheckboxField(ENCODE_EMAIL, "Encode email addresses in text", email);
    emailf.setHelp("Also encode any 'plain text' email addresses found in the page<br>This helps preventing spam robots to harvest email addresses.");
    addFormField(mailtof);
    addFormField(emailf);

    addCategory(new JOTFormCategory("Database settings"));
    String keepDbBackups=prefs.getDefaultedString(WikiPreferences.GLOBAL_KEEP_DB_BACKUPS_FOR, "15");
    JOTFormField database=new JOTFormTextField(DB_BACKUPS, "Remove DB backups older than(days)", 3, keepDbBackups);
    database.setHelp("When the internal Database is upgraded, it is first backed-up, in case something bad happens.<br>While this is handy in case of problem, those files could potentially be large, so it's a good idea to have them removed after a while to free disk space.");
    addFormField(database);
            
    addCategory(new JOTFormCategory("Search feature"));
    boolean indexAtStart=prefs.getDefaultedBoolean(WikiPreferences.GLOBAL_INDEXING_AT_STARTUP, Boolean.TRUE).booleanValue();
    String indexSchedule=prefs.getDefaultedString(WikiPreferences.GLOBAL_INDEXING_SCHEDULE, "* * * * 0,10,20,30,40,50");
    JOTFormField indexStart=new JOTFormCheckboxField(INDEXING_AT_STARTUP, "Update search index at startup", indexAtStart);
    JOTFormField indexWhen=new JOTFormTextField(INDEXING_SCHEDULE, "Update search index at startup", 20, indexSchedule);
    indexStart.setHelp("Wether to update the search index right away when jotwiki starts.");
    indexWhen.setHelp("Schedules when to update the search index.<br>Recommanded: min: every 5mn, max: once a day<br><br>"+JOTSchedulingOptions.getScheduleHelp());
    addFormField(indexStart);
    addFormField(indexWhen);
    
    addCategory(new JOTFormCategory("Google Sitemap Settings"));
    boolean startSm = prefs.getDefaultedBoolean(WikiPreferences.GLOBAL_SITEMAP_AT_STARTUP, Boolean.TRUE).booleanValue();
    String smSchedule = prefs.getDefaultedString(WikiPreferences.GLOBAL_SITEMAP_SCHEDULE, "* * * 1 0");
    String addThisPubID = prefs.getDefaultedString(WikiPreferences.GLOBAL_ADDTHIS_PUBLISHER, "");

    JOTFormCheckboxField sitemapAtStartup = new JOTFormCheckboxField(SITEMAP_START, "Build sitemap at startup?", startSm);
    sitemapAtStartup.setHelp("Enable if you want the sitemap to be rebuilt when jotwiki is restarted.");
    addFormField(sitemapAtStartup);

    JOTFormTextField sitemapSchedule = new JOTFormTextField(SITEMAP_SCHEDULE, "Sitemap schedule", 20, smSchedule);
    sitemapSchedule.setHelp("<b>You will want to restart the application after changing this setting!</b><br>If you want the sitemap to be rebuilt at a specific time (schedule).<br>It will only be rebuilt if there where any changes.<br><br>" + JOTSchedulingOptions.getScheduleHelp());
    addFormField(sitemapSchedule);

    addCategory(new JOTFormCategory("AddThis.com Feature"));

    JOTFormTextField addThis = new JOTFormTextField(ADDTHIS_PUBLISHER, "AddThis Publisher ID", 20, addThisPubID);
    addThis.setHelp("If you want to track what pages your user bookmark, you can go to addthis.com and create an account.<br>Then you can enter your publisher ID(aka login) here.Then when somebody uses the addthis '+' icon to bookmark your pages, the stats will be kept into your addThis account.<br><br>Leave blank if you don't have an addThis account.");
    addFormField(addThis);

    addSubmitButton(new JOTFormSubmitButton("Save Setup"));
  }

  /**
   * Retrieves the "admin" user object 
   * @return
   */
  private WikiUser getAdmin()
  {
    WikiUser user = null;
    try
    {
      user = (WikiUser) WikiUser.getUserByLogin(WikiUser.class, "admin");
    }
    catch (Exception e)
    {
    }
    return user;
  }

  public void updateProperties(JOTFlowRequest request)
  {
    prefs = WikiPreferences.getInstance();
  }

  public Hashtable validateForm(JOTFlowRequest request) throws Exception
  {
    Hashtable results = new Hashtable();
    if (!get(PASSWORD).getValue().equals(get(PASSWORD_CONFIRM).getValue()))
    {
      results.put("PASS", "Passwords are not matching.");
    }
    if (get(PASSWORD).getValue().length() < 5)
    {
      results.put("PASS_LEN", "Passwords must be 5+ characters.");
    }
    // validate sitemap schedule
    String sitemapSchedule = get(SITEMAP_SCHEDULE).getValue();
    if (sitemapSchedule != null && sitemapSchedule.length() > 0)
    {
      if (!JOTSchedulingOptions.isValid(sitemapSchedule))
      {
        results.put("3", "The sitemap schedule format is invalid.");
      }
    }
    String indexingSchedule = get(INDEXING_SCHEDULE).getValue();
    if (indexingSchedule != null && indexingSchedule.length() > 0)
    {
      if (!JOTSchedulingOptions.isValid(indexingSchedule))
      {
        results.put("3", "The indexing schedule format is invalid.");
      }
    }
    return results;
  }

  public void save(JOTFlowRequest request) throws Exception
  {
    //Save/update the admin user
    WikiUser admin = saveOrUpdateAdmin("admin", get(DISPLAY_NAME).getValue(), get(PASSWORD).getValue());

    // delete the runsetup file id present (we where in initial setup). 
    File setupFile = new File(WikiPreferences.getInstance().getRootFolder(), "runsetup.txt");
    
    if (setupFile.exists())
    {
      setupFile.delete();
      // also log off setup and log as admin instead.
      
    }

    //save the other options (properties)
    prefs.setString(WikiPreferences.GLOBAL_STACK_TRACE, get(STACK_TRACE).getValue());
    prefs.setString(WikiPreferences.GLOBAL_PRINT_TO_CONSOLE, get(CONSOLE_PRINT).getValue());
    String levels = "";
    if (get(DEBUG_0).getValue().equals(JOTFormConst.VALUE_CHECKED))
    {
      levels += "0,";
    }
    if (get(DEBUG_1).getValue().equals(JOTFormConst.VALUE_CHECKED))
    {
      levels += "1,";
    }
    if (get(DEBUG_2).getValue().equals(JOTFormConst.VALUE_CHECKED))
    {
      levels += "2,";
    }
    if (get(DEBUG_3).getValue().equals(JOTFormConst.VALUE_CHECKED))
    {
      levels += "3,";
    }
    if (get(DEBUG_4).getValue().equals(JOTFormConst.VALUE_CHECKED))
    {
      levels += "4,";
    }
    if (get(DEBUG_5).getValue().equals(JOTFormConst.VALUE_CHECKED))
    {
      levels += "5,";
    }
    if (levels.endsWith(","))
    {
      levels = levels.substring(0, levels.length() - 1);
    }
    prefs.setString(WikiPreferences.GLOBAL_LOG_LEVELS, levels);

    prefs.setString(WikiPreferences.GLOBAL_SITEMAP_AT_STARTUP, get(SITEMAP_START).getValue());
    prefs.setString(WikiPreferences.GLOBAL_SITEMAP_SCHEDULE, get(SITEMAP_SCHEDULE).getValue());
    prefs.setString(WikiPreferences.GLOBAL_ADDTHIS_PUBLISHER, get(ADDTHIS_PUBLISHER).getValue());
    prefs.setString(WikiPreferences.GLOBAL_KEEP_LOGS_FOR, get(PURGE_LOGS).getValue());
    prefs.setString(WikiPreferences.GLOBAL_KEEP_DB_BACKUPS_FOR, get(DB_BACKUPS).getValue());
    prefs.setString(WikiPreferences.GLOBAL_MAIL_HOST, get(MAIL_HOST).getValue());
    prefs.setString(WikiPreferences.GLOBAL_MAIL_DOMAIN, get(MAIL_DOMAIN).getValue());
    prefs.setString(WikiPreferences.GLOBAL_MAIL_FROM, get(MAIL_FROM).getValue());
    prefs.setString(WikiPreferences.GLOBAL_MAIL_PORT, get(MAIL_PORT).getValue());
    prefs.setString(WikiPreferences.GLOBAL_ENCODE_EMAIL, get(ENCODE_EMAIL).getValue());
    prefs.setString(WikiPreferences.GLOBAL_ENCODE_MAILTO, get(ENCODE_MAILTO).getValue());
    prefs.setString(WikiPreferences.GLOBAL_INDEXING_AT_STARTUP, get(INDEXING_AT_STARTUP).getValue());
    prefs.setString(WikiPreferences.GLOBAL_INDEXING_SCHEDULE, get(INDEXING_SCHEDULE).getValue());

    //saving new prefs
    prefs.save();

    // resarting the logger with the new settings
    synchronized (this)
    {
      JOTLogger.destroy();
      JOTWikiFilter.initLogger(prefs);
    }
  }

  /**
   * Updates the 'admin' user, ie: password, name etc...
   * @param login
   * @param first
   * @param password
   * @return
   * @throws java.lang.Exception
   */
  private WikiUser saveOrUpdateAdmin(String login, String first, String password) throws Exception
  {
    WikiUser user = null;
    if (WikiUser.isNewUser(WikiUser.class, login))
    {
      user = new WikiUser();
    }
    else
    {
      user = (WikiUser) WikiUser.getUserByLogin(WikiUser.class, login);
    }
    user.setLogin(login);
    user.setDescription("Default SuperAdmin user.");
    user.setFirstName(first);
    user.setLastName("");
    user.setPassword(password);
    user.setRemovable(false);
    user.save();
    return user;
  }

  public void refreshData(JOTFlowRequest request) throws Exception
  {
    updateProperties(request);
    super.refreshData(request);
  }

  public boolean validatePermissions(JOTFlowRequest request)
  {
          return WikiPermission.hasPermission(request, WikiPermission.SETUP);
  }
}

