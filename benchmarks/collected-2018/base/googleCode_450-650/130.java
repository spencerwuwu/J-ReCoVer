// https://searchcode.com/api/result/13967822/


/*
 * Copyright (c) 1998 - 2005 Versant Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Versant Corporation - initial API and implementation
 */
package com.versant.core.storagemanager;

import com.versant.core.common.config.ConfigInfo;
import com.versant.core.common.config.ConfigParser;
import com.versant.core.metadata.parser.MetaDataParser;
import com.versant.core.metadata.ModelMetaData;
import com.versant.core.common.BindingSupportImpl;
import com.versant.core.common.Utils;
import com.versant.core.util.BeanUtils;
import com.versant.core.util.PropertiesLoader;
import com.versant.core.util.classhelper.ClassHelper;
import com.versant.core.logging.LogEventStore;
import com.versant.core.storagemanager.logging.LoggingStorageManagerFactory;

import com.versant.core.compiler.ClassCompiler;
import com.versant.core.compiler.PizzaClassCompiler;
import com.versant.core.compiler.ClassSpec;

import com.versant.core.server.CompiledQueryCache;

import java.util.*;
import java.util.zip.Deflater;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.io.*;

/**
 * Bean to create StorageManagerFactory's. This needs some more work to avoid
 * referencing the SMF implementation classes directly as it does now.
 */
public class StorageManagerFactoryBuilder {

    private ConfigInfo config;
    private ClassLoader loader;
    private LogEventStore pes;
    private StorageCache cache;
    private boolean onlyMetaData;
    private boolean fullInit = true;
    private boolean continueAfterMetaDataError;

    private ClassCompiler classCompiler;

    private HashMap classSpecs;
    private CompiledQueryCache compiledQueryCache;
    private boolean keepHyperdriveBytecode;
    private Map hyperdriveBytecode;
    private int hyperdriveBytecodeMaxSize;

    public StorageManagerFactoryBuilder() {
    }

    /**
     * Create a SMF based on our properties.
     */
    public StorageManagerFactory createStorageManagerFactory() {

        if (config == null) {
            throw BindingSupportImpl.getInstance().internal(
                    "config property not set");
        }


        if (loader == null) {
            throw BindingSupportImpl.getInstance().internal(
                    "loader property not set");
        }
        if (Utils.isStringEmpty(config.url)) {
            if (Utils.isStringEmpty(config.props.getProperty(
                    ConfigParser.OPTION_CONNECTION_FACTORY_NAME)) &&
                    !Utils.isDataSource(config.props.getProperty(
                            ConfigParser.STD_CON_DRIVER_NAME), loader)) {

                throw BindingSupportImpl.getInstance().internal(
                        "javax.jdo.option.ConnectionURL property is required " +
                        "if using a JDBC Driver");

            } else if (Utils.isStringEmpty(config.db)){
                throw BindingSupportImpl.getInstance().internal(
                        ConfigParser.STORE_DB +
                        " property is required if using a JDBC DataSource");
            }

        }


        if (cache == null) {
            cache = new NOPStorageCache();
        }
        if (pes == null) {
            pes = new LogEventStore();
            BeanUtils.setProperties(pes, config.perfProps);
        }
        if (config.jdoMetaData == null) {
            MetaDataParser p = new MetaDataParser();
            config.jdoMetaData = p.parse(config.jdoResources, loader);
        }
        if (config.hyperdrive) {
            classSpecs = new HashMap();
        } else {
            classSpecs = null;
        }
        compiledQueryCache = new CompiledQueryCache(
                config.compiledQueryCacheSize);
        StorageManagerFactory smf = createSmfForURL();
        ModelMetaData jmd = smf.getModelMetaData();
        jmd.forceClassRegistration();
        cache.setJDOMetaData(jmd);
        smf = new LoggingStorageManagerFactory(smf, pes);


        if (config.hyperdrive && !classSpecs.isEmpty()) {
            if (config.hyperdriveSrcDir != null) {
                writeGeneratedSourceCode(classSpecs, config.hyperdriveSrcDir);
            }
            if (!onlyMetaData || config.hyperdriveClassDir != null) {
                compileAndInitGeneratedClasses(config.hyperdriveClassDir, jmd);
            }
        }


        if (!onlyMetaData) {
            smf.init(fullInit, loader);
        }
        return smf;
    }

    private StorageManagerFactory createSmfForURL() {
        StorageManagerFactory smf;
        Properties p;

        try {
            if (!Utils.isStringEmpty(config.db)){
                p = PropertiesLoader.loadPropertiesForDB(loader, "openaccess",
                        config.db);
            } else {
                p = PropertiesLoader.loadPropertiesForURL(loader, "openaccess",
                    config.url);
            }
        } catch (IOException e) {
            throw BindingSupportImpl.getInstance().invalidOperation(
                    e.toString(), e);
        }


        String resName = p.getProperty(PropertiesLoader.RES_NAME_PROP);
        String smfClassName = p.getProperty("smf");
        if (smfClassName == null) {
            throw BindingSupportImpl.getInstance().internal(
                    "No 'smf' property in " + resName);
        }
        try {
            Class cls = ClassHelper.get().classForName(smfClassName, true, loader);
            Constructor cons = cls.getConstructor(
                new Class[]{StorageManagerFactoryBuilder.class});
            smf = (StorageManagerFactory)cons.newInstance(new Object[]{this});
        } catch (Throwable e) {
            if (BindingSupportImpl.getInstance().isError(e) && !BindingSupportImpl.getInstance().isOutOfMemoryError(e)) {
                throw (Error)e;
            }
            if (BindingSupportImpl.getInstance().isOwnException(e)) {
                throw (RuntimeException)e;
            }
            if( e instanceof InvocationTargetException && ((InvocationTargetException)e).getTargetException() != null )
            {
            	Throwable inner = ((InvocationTargetException)e).getTargetException();
				if (BindingSupportImpl.getInstance().isOwnException(inner))
					throw (RuntimeException)inner;
				else
					throw BindingSupportImpl.getInstance().internal( inner.toString(),
																	 inner );
            }
            else
            {
            	throw BindingSupportImpl.getInstance().internal(e.toString(), e);
            }
        }
        return smf;
    }

    /**
     * Compile, load and init all of the dynamically generated classes. This
     * will only compile classes that cannot be loaded from our classloader.
     */

    private void compileAndInitGeneratedClasses(String hyperdriveClassDir,
            ModelMetaData jmd) {
        // attempt to load each class and create a single String of source
        // code for those which cannot be loaded
        ArrayList toInit = new ArrayList();
        ArrayList notGenerated = new ArrayList();
        Map toCompile = new HashMap();
        for (Iterator i = classSpecs.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry)i.next();
            String name = (String)e.getKey();
            ClassSpec spec = (ClassSpec)e.getValue();
            try {
                toInit.add(Class.forName(name, false, loader));
                notGenerated.add(name);
            } catch (ClassNotFoundException x) {
                toCompile.put(name, spec.toSrcCode());
            }
            i.remove(); // remove one at a time to reduce max mem usage
        }
        classSpecs = null;

        // compile classes we could not load
        if (!toCompile.isEmpty()) {
            if (classCompiler == null) {
                classCompiler = new PizzaClassCompiler();
            }
            Map compiled = classCompiler.compile(toCompile, loader);
            classCompiler = null;
            if (hyperdriveClassDir != null) {
                writeClassFiles(compiled, hyperdriveClassDir);
            }
            loadCompiledClasses(compiled, toInit);
            if (keepHyperdriveBytecode) {
                hyperdriveBytecode = compiled;
            }
        }

        initHyperdriveClasses(toInit, jmd);

        if (keepHyperdriveBytecode) {
            // put in null bytecode for all classes loaded and not generated
            if (hyperdriveBytecode == null) {
                hyperdriveBytecode = new HashMap();
            }
            for (int i = notGenerated.size() - 1; i >= 0; i--) {
                hyperdriveBytecode.put(notGenerated.get(i), null);
            }
        }
    }


    /**
     * Invoke the initStatics method on each Class in toInit that has one and
     * check the return value see if the class has been previously initialized.
     * If it has then throw an exception.
     */

    public static void initHyperdriveClasses(List toInit, ModelMetaData jmd) {
        for (Iterator i = toInit.iterator(); i.hasNext(); ) {
            Class cls = (Class)i.next();
            Object res = null;
            try {
                Method m = cls.getMethod("initStatics",
                        new Class[]{ModelMetaData.class});
                res = m.invoke(null, new Object[]{jmd});
            } catch (NoSuchMethodException e) {
                // ignore
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                throw BindingSupportImpl.getInstance().internal(t.toString(), t);
            } catch (Exception x) {
                throw BindingSupportImpl.getInstance().internal(x.toString(), x);
            }
            if (res instanceof Boolean) {
                if (!((Boolean)res).booleanValue()) {
                    throw BindingSupportImpl.getInstance().internal(
                        cls.getName() + " is in use (try versant.useClassloader=true)");
                }
            }
        }
    }


    /**
     * Load and init the dynamically compiled classes. This is done in
     * alphabetical order to make sure that any class load problems are
     * detirministic. If keepHyperdriveBytecode is true then the bytecode
     * for each class is compressed and kept in the map, otherwise it is
     * discarded.
     */

    private void loadCompiledClasses(Map map, Collection toInit) {
        ArrayList names = new ArrayList(map.keySet());
        Collections.sort(names);
        Method defineClass = null;
        try {
            defineClass = ClassLoader.class.getDeclaredMethod("defineClass",
                    new Class[]{String.class, byte[].class, Integer.TYPE,
                                Integer.TYPE});
        } catch (NoSuchMethodException e) {
            // not possible really
            throw BindingSupportImpl.getInstance().internal(e.toString(), e);
        }
        defineClass.setAccessible(true);
        Deflater def = null;
        byte[] outbuf = null;
        if (keepHyperdriveBytecode) {
            def = new Deflater(9);
            hyperdriveBytecodeMaxSize = 0;
            for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                byte[] bytecode = (byte[])((Map.Entry)i.next()).getValue();
                if (bytecode.length > hyperdriveBytecodeMaxSize) {
                    hyperdriveBytecodeMaxSize = bytecode.length;
                }
            }
            outbuf = new byte[hyperdriveBytecodeMaxSize + 128];
        }
        for (Iterator i = names.iterator(); i.hasNext(); ) {
            Object className = i.next();
            byte[] bytecode = (byte[])map.get(className);
            try {
                Class cls = (Class)defineClass.invoke(loader, new Object[]{null,
                        bytecode, new Integer(0), new Integer(bytecode.length)});
                toInit.add(cls);
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                throw BindingSupportImpl.getInstance().internal(t.toString(), t);
            } catch (Exception x) {
                throw BindingSupportImpl.getInstance().internal(x.toString(), x);
            }
            if (keepHyperdriveBytecode) {
                def.reset();
                def.setInput(bytecode);
                def.finish();
                int sz = def.deflate(outbuf);
                bytecode = new byte[sz];
                System.arraycopy(outbuf, 0, bytecode, 0, sz);
                map.put(className, bytecode);
            } else {
                map.remove(className);
            }
        }
    }


    /**
     * Write .class files for all classes in c to dir.
     */

    private void writeClassFiles(Map map, String dir) {
        for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry)i.next();
            String className = (String)e.getKey();
            byte[] bytecode = (byte[])e.getValue();
            File f = new File(dir, className + ".class");
            try {
                FileOutputStream o = new FileOutputStream(f);
                o.write(bytecode, 0, bytecode.length);
                o.close();
            } catch (IOException x) {
                throw BindingSupportImpl.getInstance().runtime(
                        "Error writing to " + f + ": " + x, x);
            }
        }
    }


    /**
     * Write out src for all generated classes to dir.
     */

    private void writeGeneratedSourceCode(HashMap classSpecs, String dir) {
        for (Iterator i = classSpecs.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry)i.next();
            ClassSpec spec = (ClassSpec)e.getValue();
            File f = new File(dir, spec.getName() + ".java");
            try {
                FileWriter o = new FileWriter(f);
                o.write(spec.toSrcCode());
                o.close();
            } catch (IOException x) {
                throw BindingSupportImpl.getInstance().runtime(
                        "Error writing to " + f + ": " + x, x);
            }
        }
    }


    public ConfigInfo getConfig() {
        return config;
    }

    public void setConfig(ConfigInfo config) {
        this.config = config;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public LogEventStore getLogEventStore() {
        return pes;
    }

    public void setLogEventStore(LogEventStore logEventStore) {
        this.pes = logEventStore;
    }

    public StorageCache getCache() {
        return cache;
    }

    public void setCache(StorageCache cache) {
        this.cache = cache;
    }

    public boolean isOnlyMetaData() {
        return onlyMetaData;
    }

    /**
     * If this flag is true then the SMF is created just to access the
     * complete JDO meta data. It will not attempt to connect to a datastore.
     */
    public void setOnlyMetaData(boolean onlyMetaData) {
        this.onlyMetaData = onlyMetaData;
    }

    public boolean isFullInit() {
        return fullInit;
    }

    /**
     * If this flag is true then the SMF is completely initialized ready for
     * use e.g. the JDBC store will populate the keygen tables. This is ignored
     * if the onlyMetaData flag is set.
     */
    public void setFullInit(boolean fullInit) {
        this.fullInit = fullInit;
    }


    public ClassCompiler getClassCompiler() {
        return classCompiler;
    }

    public void setClassCompiler(ClassCompiler classCompiler) {
        this.classCompiler = classCompiler;
    }


    public boolean isContinueAfterMetaDataError() {
        return continueAfterMetaDataError;
    }

    /**
     * If this flag is set then the store should attempt to recover from
     * meta data errors and continue instead of throwing an exception. The
     * errors must be added to the meta data. This is to support the
     * Workbench and other tools.
     */
    public void setContinueAfterMetaDataError(boolean continueAfterMetaDataError) {
        this.continueAfterMetaDataError = continueAfterMetaDataError;
    }

    /**
     * StorageManagerFactory's supporting hyperdrive code generation must add
     * dynamically generated classes to this map during construction. This
     * maps class name -> ClassSpec.
     */
    public HashMap getClassSpecs() {
        return classSpecs;
    }

    /**
     * StorageManager's must store CompiledQueries in this cache. 
     */
    public CompiledQueryCache getCompiledQueryCache() {
        return compiledQueryCache;
    }

    /**
     * If this property is true then the bytecode for generated hyperdrive
     * classes is avilable via {@link #getHyperdriveBytecode()} after the
     * call to {@link #createStorageManagerFactory()} .
     */
    public void setKeepHyperdriveBytecode(boolean keepHyperdriveBytecode) {
        this.keepHyperdriveBytecode = keepHyperdriveBytecode;
    }

    public boolean isKeepHyperdriveBytecode() {
        return keepHyperdriveBytecode;
    }

    /**
     * If keepHyperdriveBytecode is true and hyperdrive classes were
     * generated then this maps each class name to its compressed byte[]
     * bytecode or null if the class was loaded from our classloader and not
     * compiled at runtime. Otherwise it is null.
     *
     * @see #setKeepHyperdriveBytecode(boolean)
     * @see #getHyperdriveBytecodeMaxSize
     */
    public Map getHyperdriveBytecode() {
        return hyperdriveBytecode;
    }

    /**
     * If keepHyperdriveBytecode is true and hyperdrive classes were
     * generated then this is the length in bytes of the uncompressed
     * bytecode for the biggest class. This is useful for sizing
     * decompression buffers.
     *
     * @see #setKeepHyperdriveBytecode(boolean)
     */
    public int getHyperdriveBytecodeMaxSize() {
        return hyperdriveBytecodeMaxSize;
    }

}


