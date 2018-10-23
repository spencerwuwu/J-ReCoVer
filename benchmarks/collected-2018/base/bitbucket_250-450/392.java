// https://searchcode.com/api/result/122438805/

/***********************************************************************************************************************
 *
 * blueMarine Metadata - open source media workflow
 * Copyright (C) 2007-2011 by Tidalwave s.a.s. (http://www.tidalwave.it)
 *
 ***********************************************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
 *
 * WWW: http://bluemarine.tidalwave.it
 * SCM: https://kenai.com/hg/bluemarine~metadata-src
 *
 **********************************************************************************************************************/
package it.tidalwave.metadata.persistence;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.concurrent.LinkedBlockingQueue;
import it.tidalwave.util.logging.Logger;
import org.openide.util.RequestProcessor;
import org.openide.loaders.DataObject;
import it.tidalwave.metadata.persistence.query.Criterion;

/*******************************************************************************
 *
 * This class holds a pair "property = value".
 * 
 * @author  Fabrizio Giudici
 * @version $Id$
 *
 ******************************************************************************/
public final class PropertyAndValue<T>
  {
    /***************************************************************************
     *
     * 
     **************************************************************************/
    public static class TemporarilyUnavailableException extends Exception
      {
        private static final long serialVersionUID = -4437803436375069579L;
      }
    
    private final static String CLASS = PropertyAndValue.class.getName();
    private final static Logger logger = Logger.getLogger(CLASS);

    private final static SerialDataObjectCounter DATA_OBJECT_COUNTER = new SerialDataObjectCounter();
    
    @Nonnull
    private final MetadataProperty property;
    
    @CheckForNull
    private final T value;

    @Nonnegative
    private int dataObjectCount;
    
    private boolean dataObjectCountAvailable;
    
    private boolean dataObjectCountComputeInProgress;
    
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    
    static
      {
        RequestProcessor.getDefault().post(DATA_OBJECT_COUNTER);
      }
    
    /***************************************************************************
     *
     * A queue consumer that ensures that only one count query is executed at
     * a given time. This is important to reduce the hit on the database as well
     * as the transaction contention chances.
     * 
     **************************************************************************/
    static class SerialDataObjectCounter implements Runnable
      {
        private final LinkedBlockingQueue<PropertyAndValue> queue = new LinkedBlockingQueue<PropertyAndValue>();
        
        public void post (@Nonnull final PropertyAndValue propertyAndValue)
          {
            final boolean b = queue.offer(propertyAndValue);
            assert b;
          }
        
        public void run() 
          {
            Thread.currentThread().setName("DataObjectCounter");

            while (!Thread.interrupted())
              {
                PropertyAndValue propertyAndValue;
                
                try 
                  {
                    propertyAndValue = queue.take();
                  }
                catch (InterruptedException e)
                  {
                    return;
                  }
                
                try
                  {
                    final Criterion criterion = Criterion.matches(propertyAndValue);
                    propertyAndValue.dataObjectCount = criterion.findDataObjectCount();
                    
                    synchronized (propertyAndValue)
                      {
                        propertyAndValue.dataObjectCountAvailable = true;
                        propertyAndValue.dataObjectCountComputeInProgress = false;
                      }
                    
                    propertyAndValue.propertyChangeSupport.firePropertyChange("dataObjectCount", null, propertyAndValue.dataObjectCount); // FIXME: change prop name
                  }
                catch (Throwable t)
                  {
                    logger.warning("DataObjectCounter background task failed");
                    logger.throwing(CLASS, "DataObjectCounter.run()", t);
                  }
              }
          }
      }  
   
    /***************************************************************************
     *
     * 
     **************************************************************************/
    protected PropertyAndValue (@Nonnull final MetadataProperty property, 
                                @CheckForNull final T value)
      {
        this.property = property;
        this.value = value;
      }

    /***************************************************************************
     *
     * Returns the bound property.
     * 
     * @return  the property
     * 
     **************************************************************************/
    @Nonnull
    public MetadataProperty getProperty()
      {
        return property;
      }
    
    /***************************************************************************
     *
     * Returns the bound value.
     * 
     * @return  the value
     * 
     **************************************************************************/
    @CheckForNull
    public T getValue()
      {
        return value;
      }

    /***************************************************************************
     *
     * Registers a {@link PropertyChangeListener}.
     * 
     * @param  propertyChangeListener   the listener
     * 
     **************************************************************************/
    public void addPropertyChangeListener (@Nonnull final PropertyChangeListener propertyChangeListener)
      {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
      }
    
    /***************************************************************************
     *
     * Unregisters a {@link PropertyChangeListener}.
     * 
     * @param  propertyChangeListener   the listener
     * 
     **************************************************************************/
    public void removePropertyChangeListener (@Nonnull final PropertyChangeListener propertyChangeListener)
      {
        propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
      }
    
    /***************************************************************************
     *
     * Returns the count of {@link DataObject}s that are currently bound with
     * this property and value. This method throws an {@TemporaryUnavailableException} 
     * if the value is not ready yet; in this case, a background thread will be 
     * started to compute it and make available later; it can be retrieved
     * asynchronously by means of a {@link PropertyChangeListener}.
     * 
     * @return  the number of bound objects
     * @throws  TemporaryUnavailableException  if the result is not available yet
     * 
     **************************************************************************/
    @Nonnegative
    public synchronized int getDataObjectCount()
      throws TemporarilyUnavailableException
      {
        logger.fine("getDataObjectCount()");
        
        if (dataObjectCountAvailable)
          {
            logger.finest(">>>> getDataObjectCount() returning immediately");
            return dataObjectCount;    
          }
        
        if (!dataObjectCountComputeInProgress)
          {
            logger.finest(">>>> getDataObjectCount() starting background task");
            dataObjectCountComputeInProgress = true;
            DATA_OBJECT_COUNTER.post(this);
          }
              
        throw new TemporarilyUnavailableException();
      }
    
    public synchronized void invalidateDataObjectCount()
      {
        dataObjectCountAvailable = false;
      }
    
    /***************************************************************************
     *
     * {@inheritDoc}
     * 
     **************************************************************************/
    @Override
    public boolean equals (@CheckForNull final Object object) 
      {
        if (object == null) 
          {
            return false;
          }
        
        if (getClass() != object.getClass()) 
          {
            return false;
          }
        
        final PropertyAndValue other = (PropertyAndValue)object;
        
        if (!this.property.equals(other.property))
          {
            return false;
          }
        
        if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) 
          {
            return false;
          }
        
        return true;
      }

    /***************************************************************************
     *
     * {@inheritDoc}
     * 
     **************************************************************************/
    @Override
    public int hashCode() 
      {
        int hash = 5 * 53 + this.property.hashCode();
        hash = 53 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
      }
    
    /***************************************************************************
     *
     * {@inheritDoc}
     * 
     **************************************************************************/
    @Override 
    @Nonnull
    public String toString()
      {
        return String.format("PropertyAndValue[%s=%s]", property.getPropertyName(), value);   
      }
  }

