// https://searchcode.com/api/result/121565780/

/***********************************************************************************************************************
 *
 * Mistral - open source imaging engine
 * Copyright (C) 2003-2012 by Tidalwave s.a.s.
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
 * WWW: http://mistral.imajine.org
 * SCM: https://bitbucket.org/tidalwave/mistral-src
 *
 **********************************************************************************************************************/
package org.imajine.image;

import java.io.Serializable;

/*******************************************************************************
 *
 * A cache of ImageModels used for distributed computing. The purpose of the
 * cache is - quite obviously - to reduce the need for moving image through the
 * network among different computing nodes. 
 *
 * @author  Fabrizio Giudici
 * @version $Id$
 *
 ******************************************************************************/
public abstract class ImageModelCache
  {
    /** The default class. */
    private static Class<?extends ImageModelCache> defaultClass = DefaultImageModelCache.class;
    
    /** The singleton instance. */
    private static ImageModelCache instance;

    /***************************************************************************
     *
     *
     **************************************************************************/
    protected ImageModelCache ()
      {
      }

    /***************************************************************************
     *
     * Sets the default implementation of the cache for the local VM.
     *
     * @param  defaultClass           the implementation class
     * @throws IllegalStateException  if a cache has been already instantiated
     *
     **************************************************************************/
    public static void setDefault (final Class<?extends ImageModelCache> defaultClass)
      {
//        if (instance != null)
//          {
//            throw new IllegalStateException("A local cache has been already instantiated");
//          }
        
        ImageModelCache.defaultClass = defaultClass;
        instance = null;
      }

    /***************************************************************************
     *
     * Returns the default instance of the cache on the local VM.
     *
     * @return  the local cache
     *
     **************************************************************************/
    public static synchronized ImageModelCache getInstance() // FIXME: rename to getDefault(), it's no more a singleton
      {
        try
          {
            if (instance == null)
              {
                instance = defaultClass.newInstance();
              }

            return instance;
          }
        catch (Exception e)
          {
            throw new RuntimeException(e);
          }
      }

    /***************************************************************************
     * 
     * Adds an ImageModel to the cache. The image is always added only in the
     * local cache. Remote caches will pull the images they lack on demand.
     *
     * @param  imageModel  the ImageModel to add
     *
     **************************************************************************/
    public abstract void store (ImageModel imageModel);

    /***************************************************************************
     *
     * Updates an ImageModel in the cache. This means that all remote copies of
     * this image will be invalidated.
     *
     * TODO: should investigate more complex cases. Maybe the remote workers
     * still want to work with a remote snapshot. Include versioning instead of
     * invalidating? 
     *
     * @param  imageModel  the ImageModel to update
     *
     **************************************************************************/
    public abstract void update (ImageModel imageModel);

    /***************************************************************************
     * 
     * Removes an ImageModel from the cache. According to the value of the 
     * <code>remote</code> parameter, the operation is performed only locally
     * or also to remote caches.
     *
     * @param  id      the id of the ImageModel to remove
     * @param  remove  true if the operation must be performed also remotely
     *
     **************************************************************************/
    public abstract void remove (Serializable id, boolean remote);

    /***************************************************************************
     *
     * Finds an ImageModel in the cache. According to the value of the 
     * <code>remote</code> parameter, the search is performed only locally
     * or also to remote caches.
     *
     * @param  id      the id of the ImageModel to remove
     * @param  remove  true if the operation must be performed also remotely
     *
     **************************************************************************/
    public abstract ImageModel retrieve (Serializable id, boolean remote);
    
    /***************************************************************************
     *
     * Returns true if there's a local copy with the given id.
     *
     * @param  id      the id of the ImageModel to remove
     * @return         true if there's a local copy    
     *
     **************************************************************************/
    public abstract boolean contains (Serializable id);
  }

