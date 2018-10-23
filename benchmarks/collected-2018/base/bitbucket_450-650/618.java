// https://searchcode.com/api/result/45765637/

/***
 * Coalevo Project
 * http://www.coalevo.net
 *
 * (c) Dieter Wimberger
 * http://dieter.wimpi.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/
package net.coalevo.foundation.impl;

import net.coalevo.foundation.model.MessageAttributes;
import net.coalevo.foundation.model.Messages;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that provides messages.
 * <p/>
 * There is some intelligence placed in here, that should
 * help to reduce resource usage and speed up system.
 *
 * @author Dieter Wimberger (wimpi)
 * @version @version@ (@date@)
 */
class MessagesImpl
    implements Messages {

  private Marker m_LogMarker = MarkerFactory.getMarker(MessagesImpl.class.getName());

  private GenericObjectPool.Config m_PoolConfig;
  private GenericObjectPool m_MessageAttributes;
  private Map<String, MessageMap> m_LocalizedMessages = new HashMap<String, MessageMap>();
  private BundleMessageResources m_MessageResources;
  private ReentrantLock m_LoadingLock = new ReentrantLock();
  private Locale m_DefaultLocale;
  private Set<Locale> m_LoadedLocales = new HashSet<Locale>();

  public MessagesImpl(BundleMessageResources rsrc) {
    m_MessageResources = rsrc;
    //Default Attribute pool config
    GenericObjectPool.Config attrPoolConfig = new GenericObjectPool.Config();
    attrPoolConfig.maxActive = 15;
    attrPoolConfig.maxIdle = 5;
    attrPoolConfig.maxWait = -1;
    attrPoolConfig.testOnBorrow = false;
    attrPoolConfig.testOnReturn = false;
    attrPoolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
    m_MessageAttributes = new GenericObjectPool(new MessageAttributesImpl(), attrPoolConfig);

    //default template pool configurations
    m_PoolConfig = new GenericObjectPool.Config();
    m_PoolConfig.maxActive = 10;
    m_PoolConfig.maxIdle = 5;
    m_PoolConfig.maxWait = -1;
    m_PoolConfig.testOnBorrow = false;
    m_PoolConfig.testOnReturn = false;
    m_PoolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

    m_DefaultLocale = Locale.getDefault();
    load(m_DefaultLocale);
  }//constructor

  public MessagesImpl(BundleMessageResources rsrc, GenericObjectPool.Config attrPoolConfig, GenericObjectPool.Config msgPoolConfig) {
    m_MessageResources = rsrc;
    m_MessageAttributes = new GenericObjectPool(new MessageAttributesImpl(), attrPoolConfig);

    m_PoolConfig = msgPoolConfig;
    load(m_DefaultLocale);
  }//constructor

  public String get(String key) {
    return get(m_DefaultLocale, key, null);
  }//get

  public String get(String key, MessageAttributes attr) {
    return get(m_DefaultLocale, key, attr);
  }//get

  public String get(String key, String name, String val) {
    MessageAttributes attr = leaseAttributes();
    attr.add(name, val);
    return get(key, attr);
  }//get

  public String get(String key, String name1, String val1, String name2, String val2) {
    MessageAttributes attr = leaseAttributes();
    attr.add(name1, val1);
    attr.add(name2, val2);
    return get(key, attr);
  }//get

  public String get(Locale l, String key) {
    return get(l, key, null);
  }//get

  public String get(Locale l, String key, MessageAttributes attr) {
    try {
      return getTemplate(l, key).toString(attr);
    } catch (Exception ex) {
      Activator.log().error(m_LogMarker,"get(Locale,String,MessageAttributes)::key=" + key, ex);
      return "!!! NULL !!!";
    }
  }//get

  public String get(Locale l, String key, String name, String val) {
    MessageAttributes attr = leaseAttributes();
    attr.add(name, val);
    return get(l, key, attr);
  }//getMessage

  public String get(Locale l, String key, String name1, String val1, String name2, String val2) {
    MessageAttributes attr = leaseAttributes();
    attr.add(name1, val1);
    attr.add(name2, val2);
    return get(l, key, attr);
  }//getMessage

  public MessageAttributes leaseAttributes() {
    try {
      return (MessageAttributes) m_MessageAttributes.borrowObject();
    } catch (Exception ex) {
      return new MessageAttributesImpl();
    }
  }//leaseAttributes

  public MessagesImpl.MessageTemplate createTemplate() {
    return new MessageTemplate();
  }//createTemplate

  private MessageTemplate getTemplate(Locale l, String key)
      throws Exception {
    //1. ensure we have a locale
    if (l == null) {
      l = m_DefaultLocale;
    }
    if (!m_LoadedLocales.contains(l)) {
      load(l);
    }
    List<String> suffixes = m_MessageResources.getAvailableSuffixes(l);
    for (Iterator<String> iter = suffixes.iterator(); iter.hasNext();) {
      String suf = (String) iter.next();
      MessageMap msges = (MessageMap) m_LocalizedMessages.get(suf);
      MessageTemplate msg = msges.get(key);
      if (msg != null) {
        return msg;
      }
    }
    return null;
  }//getTemplate

  private void load(Locale l) {
    try {
      //acquire lock, just one locale can be loaded at once
      m_LoadingLock.lock();
      List<String> suffixes = m_MessageResources.getAvailableSuffixes(l);

      for (Iterator<String> iter = suffixes.iterator(); iter.hasNext();) {
        String suf = iter.next();
        //1. check if it was already loaded
        if (m_LocalizedMessages.containsKey(suf)) {
          continue;
        }
        //2. load the resource
        InputStream in = m_MessageResources.getResourceAsStream(suf);
        if (in == null) {
          Activator.log().debug(m_LogMarker,"load()::Locale = " + ((l != null) ? l.toString() : "NULL!"));
        }
        //XMLReader parser = XMLReaderFactory.createXMLReader();
        XMLReader parser = Activator.getServices().getSAXParserFactory(ServiceMediator.WAIT_UNLIMITED)
            .newSAXParser().getXMLReader();

        /*
        //Activate Namespaces
        //parser.setFeature("http://xml.org/sax/features/namespaces", true);
        if (new Boolean(System.getProperty("sax.parser.verify")).booleanValue()) {
          parser.setFeature("http://xml.org/sax/features/validation", true);
        } else {
          Activator.log().debug(m_LogMarker,"load()::Disabled DTD Validation.");
          parser.setFeature("http://xml.org/sax/features/validation", false);
        }
        */
        //log.debug("Have parser = " + parser.toString());
        InputSource src = new InputSource(in);
        //log.debug("Have InputSource = " + src.toString());
        //set content handler for the telnet templates
        MessageMap msgmap = new MessageMap();
        parser.setContentHandler(new MessageBundleHandler(this, msgmap));
        parser.parse(src);
        m_LocalizedMessages.put(suf, msgmap);
      }
      m_LoadedLocales.add(l);
      Activator.log().info(m_LogMarker,"Loaded Resource:" + m_MessageResources.getBaseName() + " in Locale:" + l.toString());
    } catch (Exception ex) {
      Activator.log().error(m_LogMarker,"load()", ex);
    } finally {
      m_LoadingLock.unlock();
    }
  }//load

  /**
   * Inner class implementing a language specific map
   * of templates, relating a key with a {@link MessagesImpl.MessageTemplate}
   * instance.
   */
  protected class MessageMap {

    private HashMap<String, MessageTemplate> m_Messages;
    private StringTemplateGroup m_Group;

    public MessageMap() {
      m_Messages = new HashMap<String, MessageTemplate>();
      m_Group = new StringTemplateGroup("templates");
    }//TemplateSet

    /**
     * Adds a pool for a template to this map.
     * The key will be retrieved from the {@link MessagesImpl.MessageTemplate}
     * instance itself.
     *
     * @param t the template to be added.
     */
    public void add(MessageTemplate t) {
      Activator.log().debug(m_LogMarker,"MessageMap::add():template " + t.getKey());
      //1. prepare template accordingly
      t.prepareTemplate(m_Group);
      //2. add template to hashmap
      m_Messages.put(t.getKey(), t);
    }//add

    /**
     * Returns a {@link MessagesImpl.MessageTemplate} instance if one
     * is mapped to the given key.
     *
     * @param key the key for the template as <tt>String</tt>
     * @return a {@link MessagesImpl.MessageTemplate} instance.
     * @throws Exception if the template cannot be retrieved, or
     *                   the given key does not map to a template.
     */
    public MessageTemplate get(String key) throws Exception {
      return (MessageTemplate) m_Messages.get(key);
    }//get

  }//MessageMap

  /**
   * Inner class implementing a <tt>MessageTemplate</tt>.
   * <p/>
   * This class will automatically cache static template
   * products, and pool dynamic templates for reuse.
   */
  public class MessageTemplate {

    private boolean m_Static;
    private String m_Key;
    private String m_Content;
    private StringTemplatePool m_STPool;
    private String m_Cached;

    public MessageTemplate() {
    }//constructor

    /**
     * Formats the template with the given attributes.
     * For static templates, the attributes reference
     * should be <tt>null</tt>.
     * Dynamic templates might be called without attributes,
     * with varying success.
     * Note that this method will release the passed in
     * {@link MessageAttributes} instance to the lokal
     * pool.
     *
     * @param attr a {@link MessageAttributes} instance.
     * @return the formatted string with internal markup.
     */
    public String toString(MessageAttributes attr) {
      if (m_Static) {
        return m_Cached;
      } else {
        //lease make string
        StringTemplate st = null;
        try {
          st = m_STPool.lease();
          if (attr != null) {
            for (Iterator iter = ((MessageAttributesImpl) attr).iterator(); iter.hasNext();) {
              Object[] attrs = (Object[]) iter.next();
              String key = attrs[0].toString();
              if (attrs[1] instanceof String) {
                st.setAttribute(key, attrs[1].toString());
              } else if (attrs[1] instanceof String[]) {
                String[] values = (String[]) attrs[1];
                int i = values.length;
                if (i == 2) {
                  st.setAttribute(key, values[0], values[1]);
                } else if (i == 3) {
                  st.setAttribute(key, values[0], values[1], values[2]);
                } else if (i == 4) {
                  st.setAttribute(key, values[0], values[1], values[2], values[3]);
                } else if (i == 5) {
                  st.setAttribute(key, values[0], values[1], values[2], values[3], values[4]);
                } else {
                  //Split of aggregate name
                  String agkey = key.substring(0,key.indexOf('.'));
                  //Split varnames
                  String[] varnames = key.substring(key.indexOf('{')+1,key.indexOf('}')).split(",");
                  HashMap<String,String> m = new HashMap<String,String>(20);
                  for (int n = 0; n < varnames.length; n++) {
                    m.put(varnames[n].trim(),values[n]);
                  }
                  st.setAttribute(agkey, m);
                }
              } else {
                //no attributes
                Activator.log().error(m_LogMarker,"toString()::Can only handle String and String[]::" + key);
              }

              //st.setAttribute(str[0], str[1]);
              try {
                m_MessageAttributes.returnObject(attr);
              } catch (Exception ex) {
                //ignore
              }
            }
          }
          return st.toString();
        } catch (Exception ex) {
          //ignore
        } finally {
          try {
            m_STPool.release(st);
          } catch (Exception ex) {
            //ignore
          }
        }
      }
      return "";
    }//toString

    /**
     * Returns the key to this <tt>MessageTemplate</tt>.
     *
     * @return the key as <tt>String</tt>.
     */
    public String getKey() {
      return m_Key;
    }//getKey

    /**
     * Sets the key for this <tt>MessageTemplate</tt>.
     *
     * @param key the key for the template.
     */
    public void setKey(String key) {
      m_Key = key;
    }//setKey

    /**
     * Tests if this template is static.
     * Static means, that it does not require any attributes
     * for generating the output.
     *
     * @return true if static, false otherwise.
     */
    public boolean isStatic() {
      return m_Static;
    }//isStatic

    /**
     * Sets the flag for this <tt>MessageTemplate</tt> that indicates
     * if static or dynamic.
     *
     * @param b true if static, false otherwise.
     * @see #isStatic()
     */
    public void setStatic(boolean b) {
      m_Static = b;
    }//setStatic

    /**
     * Returns the content for this <tt>MessageTemplate</tt>.
     *
     * @return the content as <tt>String</tt>.
     */
    public String getContent() {
      return m_Content;
    }//getContent

    /**
     * Sets the content of this <tt>MessageTemplate</tt>.
     *
     * @param content the content as StringTemplate template string.
     */
    public void setContent(String content) {
      m_Content = content;
    }//setContent

    /**
     * Prepares this template, applying the style, caching the static one,
     * and preparing the dynamic template's pool.
     * This method should really only be called by an {@link MessagesImpl.MessageMap} instance.
     * Each map has an associated language specific group to allow
     * cross references within the template file (although references need to
     * be defined before).
     *
     * @param group the group to be used for cross referencing templates.
     * @see MessagesImpl.MessageMap#add(MessagesImpl.MessageTemplate)
     */
    public void prepareTemplate(StringTemplateGroup group) {
      StringTemplate st = group.defineTemplate(m_Key, m_Content);
      if (m_Static) {
        //prepare cached version
        m_Cached = st.toString();
      } else {
        //prepare pool
        m_STPool = new StringTemplatePool(st);
      }
    }//prepareTemplate

  }//inner class MessageTemplate

  /**
   * FIFO implementation for template attributes.
   * This class implements a container that can be
   * used to set attributes on the actual template
   * (<tt>StringTemplate</tt> instance) in a deferred
   * manner (i.e. without direct access, so the lease/release pool
   * mechanism can all be handled in one place).
   */
  private static class MessageAttributesImpl
      extends BasePoolableObjectFactory
      implements MessageAttributes {

    private UnboundedFifoBuffer m_FIFOBuffer;

    /**
     * Constructs a new <tt>MessageAttributesImpl</tt> instance.
     */
    public MessageAttributesImpl() {
      m_FIFOBuffer = new UnboundedFifoBuffer(10);
    }//MessageAttributes

    public Object makeObject() throws Exception {
      return new MessageAttributesImpl();
    }//makeObject

    public void passivateObject(Object o) {
      ((UnboundedFifoBuffer) o).clear();
    }//passivateObject

    public void add(String name, String value) {
      m_FIFOBuffer.add(new Object[]{name, value});
    }//add

    public void add(String aggrname, String[] values) {
      m_FIFOBuffer.add(new Object[]{aggrname, values});
    }//add

    /**
     * Returns an ordered (FIFO) iterator over
     * the stored elements.
     *
     * @return an <tt>Iterator</tt> instance.
     */
    public Iterator iterator() {
      return m_FIFOBuffer.iterator();
    }//iterator

  }//inner class MessageAttributesImpl

  /**
   * Inner class implementing a pool of
   * <tt>StringTemplate</tt> instances.
   */
  private class StringTemplatePool {

    private ObjectPool m_Pool;

    public StringTemplatePool(StringTemplate st) {
      m_Pool = new GenericObjectPool(new StringTemplateFactory(st), m_PoolConfig);
    }//constructor

    public StringTemplate lease()
        throws Exception {
      return (StringTemplate) m_Pool.borrowObject();
    }//lease

    public void release(StringTemplate t) throws Exception {
      m_Pool.returnObject(t);
    }//release

  }//StringTemplatePool

  /**
   * Inner class implementing a factory for the
   * {@link StringTemplatePool} instances.
   */
  private static class StringTemplateFactory
      extends BasePoolableObjectFactory {

    private StringTemplate m_Template;

    public StringTemplateFactory(StringTemplate st) {
      m_Template = st;
    }//constructor

    public Object makeObject() throws Exception {
      return m_Template.getInstanceOf();
    }//makeObject


    public void passivateObject(Object o) {
      ((StringTemplate) o).reset();
    }//passivateObject

  }//class StringTemplateFactory

}//class MessagesImpl

