// https://searchcode.com/api/result/45765578/

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
package net.coalevo.foundation.util;

import net.coalevo.foundation.util.metatype.MetaTypeDictionary;
import org.apache.commons.collections.list.CursorableLinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Provides an utility class that can be used to handle
 * configuration updates.
 * <p/>
 * If a bundle contains more classes that have configurable
 * parameters, this mediator helps to reduce dependencies.
 * Each class can register an {@link ConfigurationUpdateHandler}
 * and will be notified when a configuration update occurs.
 * </p>
 *
 * @author Dieter Wimberger (wimpi)
 * @version @version@ (@date@)
 */
public class ConfigurationMediator {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationMediator.class);
  private MetaTypeDictionary m_Configuration;
  private CursorableLinkedList m_Handlers;

  /**
   * Constructs a new <tt>ConfigurationMediator</tt>.
   */
  public ConfigurationMediator() {
    m_Handlers = new CursorableLinkedList();
  }//Configuration

  /**
   * Returns the configuration associated with this
   * <tt>ConfigurationMediator</tt>.
   *
   * @return a {@link MetaTypeDictionary}.
   */
  public MetaTypeDictionary getConfiguration() {
    return m_Configuration;
  }//getConfiguration

  /**
   * Update the configuration associated with this
   * <tt>ConfigurationMediator</tt>.
   * <p/>
   * Note that this will cause notification of all registered
   * {@link ConfigurationUpdateHandler} instances.
   * </p>
   *
   * @param config a {@link MetaTypeDictionary}.
   */
  public void update(MetaTypeDictionary config) {
    m_Configuration = config;
    //1. notify services
    notifyHandlers();
  }//update

  /**
   * Adds a {@link ConfigurationUpdateHandler} to be
   * notified on configuration updates.
   *
   * @param h a {@link ConfigurationUpdateHandler}.
   */
  public void addUpdateHandler(ConfigurationUpdateHandler h) {
    if (m_Handlers.contains(h)) {
      return;
    } else {
      m_Handlers.addLast(h);
    }
  }//addUpdateHandler

  /**
   * Remove a {@link ConfigurationUpdateHandler} so
   * that it will no longer be notified on updates.
   *
   * @param h an {@link ConfigurationUpdateHandler}.
   */
  public void removeUpdateHandler(ConfigurationUpdateHandler h) {
    m_Handlers.remove(h);
  }//removeUpdateHandler

  /**
   * Notifies all registered {@link ConfigurationUpdateHandler}.
   */
  protected synchronized void notifyHandlers() {
    for (Iterator iter = m_Handlers.listIterator(); iter.hasNext();) {
      ConfigurationUpdateHandler h = (ConfigurationUpdateHandler) iter.next();
      try {
        h.update(m_Configuration);
      } catch (RuntimeException ex) {
        log.error("notifyHandlers()", ex);
      }
    }
  }//notifyHandlers

  /**
   * Removes all handlers from this <tt>ConfigurationMediator</tt>.
   */
  public void clearHandlers() {
    m_Handlers.clear();
  }//clearHandlers

}//class ConfigurationMediator
