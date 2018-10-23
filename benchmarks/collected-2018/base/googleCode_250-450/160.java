// https://searchcode.com/api/result/13951237/

/*
 * Copyright (c) 2006 Israfil Consulting Services Corporation
 * Copyright (c) 2006 Christian Edward Gruber
 * All Rights Reserved
 * 
 * This software is licensed under the Berkeley Standard Distribution license,
 * (BSD license), as defined below:
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this 
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of Israfil Consulting Services nor the names of its contributors 
 *    may be used to endorse or promote products derived from this software without 
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE.
 * 
 * $Id: AbstractFlexMojo.java 610 2008-01-15 16:49:22Z christianedwardgruber $
 */
package net.israfil.mojo.flex2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Base class for flex2/actionscript3 plugin
 *
 * @author <a href="cgruber@israfil.net">Christian Edward Gruber</a>
 * @author <a href="tspurway@gmail.com">Tim Spurway</a>
 * @version $Id: AbstractFlexMojo.java 610 2008-01-15 16:49:22Z christianedwardgruber $
 */
public abstract class AbstractFlexMojo extends AbstractMojo {

	/**
	 * The directory in which Flex (command-line or builder) is installed.
	 * 
	 * @parameter expression="${flex.home}"
	 * @required 
	 * 
	 */
	protected File flexHome;

	/**
	 * The location of the flex configuration file.  
	 * 
	 * @parameter expression="${flex.config}"
	 */
	protected File flexConfig;
    
    /**
     * Location of the flex sources.
     *
     * @parameter expression="${flex.compiler.source}" default="src/main/flex"
     */
    protected File source;
	
	/**
	 * The directory into which to place the resulting artifact.  note that this is a read-only 
	 * parameter, satisfied from the build directory.  Attempts to configure this should use the 
	 * standard &lt;build&gt;&lt;directory/&gt;&lt;/build&gt; approach.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 * @readonly
	 */
	protected File outputDirectory;

    /**
	 * Name of the generated compiled binary file.
	 * 
	 * @parameter expression="${project.build.finalName}"
	 * @required
	 */
	protected String finalName;
   

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    protected List pluginArtifacts;
    
    /**
     * These are additional JVM options used when invoking the Flex compiler
     *
     * @parameter expression="${flex.java.options}"
     */
    protected String[] javaOpts;

    /**
     * @parameter expression="${flex.extraParameters}" 
     */
    protected Parameter[] extraParameters;
    
	/**
	 * Classifier to add to the artifact generated. If given, the artifact will
	 * be an attachment instead.
	 * 
	 * @parameter
	 */
	protected String classifier;

    protected abstract File getOutputFile();

    protected abstract String getCompilerClass();    

    protected abstract String getFileExtension();
    
    protected File getFile( File basedir, String finalName, String classifier ) {
        if ( classifier != null && !classifier.equals("") ) {
        	if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" )){
        		classifier = "-" + classifier;
        	}
            return new File( basedir, finalName + classifier + ".jar" );
        } else {
        	return new File( basedir, finalName + "." + getFileExtension() );
        }
    }

    /**
     * Override and implement this method to take the existing java parameters
     * and add any final information requried to provide source files to the
     * compiler.  
     * @param parameterList
     */
    protected List prepareParameters() throws MojoFailureException, MojoExecutionException {
    	List parameters = new ArrayList();
    	
		try {
			parameters.add("+flexlib=" + new File(flexHome,"frameworks").getCanonicalPath());			
		} catch (IOException e) {
			throw new MojoExecutionException("Config file does not exist.", e);
		}
    	
		if (flexConfig != null) {
			parameters.add("-load-config");
			try {
				parameters.add(flexConfig.getCanonicalFile().getAbsolutePath());
			} catch (IOException e) {
				throw new MojoExecutionException("Config file does not exist.", e);
			}
		}
		
		parameters.add( "-source-path" );
		
		try {
			parameters.add( source.getCanonicalPath());
		} catch (IOException e) {
			throw new MojoExecutionException("Source path doesn't exist.", e);
		}
    	
    	
    	return parameters;
    }

    protected void finalizeParameters(List parameters) throws MojoExecutionException, MojoFailureException {
    	getLog().info("Adding Extra Parameters: " + Arrays.asList(extraParameters));
		Iterator extraParmsIter = Arrays.asList(extraParameters).iterator();
		while (extraParmsIter.hasNext()) {
			Parameter p = (Parameter)extraParmsIter.next();
			getLog().debug("Adding parameter " + p.getName());
			parameters.add("-" + p.getName());
			if (p.getValues() != null) {
				Iterator values = Arrays.asList(p.getValues()).iterator();
				while (values.hasNext()) {
					String value = (String)values.next();
					getLog().debug("Adding parameter value" + value);
					parameters.add(value);					
				}
			}
		}
    }

    
    protected abstract String getExecutableJar();
    
	protected String getCommandClassPath()  throws MojoExecutionException, MojoFailureException {
		StringBuffer compilerClasspath = new StringBuffer();
		try {
    		addClasspathEntry(compilerClasspath,new File(new File(flexHome,"lib"),getExecutableJar()).getCanonicalPath());
			
    		// get support jar.
    		for (Iterator i = pluginArtifacts.iterator(); i.hasNext() ;) {
				Artifact artifact = (Artifact)i.next();
				getLog().debug("Trying artifact to add to classpath: " + artifact);
				if (artifact.getGroupId().equals("net.israfil.mojo") && artifact.getArtifactId().equals("maven-flex2-plugin-support")) {
					addClasspathEntry(compilerClasspath, artifact.getFile().getCanonicalFile().getAbsolutePath());
					break;
				}
			}
		} catch (IOException e) { 
			throw new MojoExecutionException("Could not generate a canonical path for library.",e); 
		}	

		return compilerClasspath.toString();
    }
    
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		if (!flexHome.exists())
			throw new MojoExecutionException(flexHome + " does not exist.  flex.home property must be set.");


		this.getLog().debug("Creating output directory: " + outputDirectory);
		outputDirectory.mkdirs();
		
		
		// HACK: why is this not being set in the defaults???
		if (source == null) source = new File(project.getBasedir(),"src/main/flex");
		if ( !source.exists() ) 				
			throw new MojoExecutionException("Source directory " + source + " does not exist.");
		
		//
		// Setup the Command-line of java execution.
		//
		List commandLineArgs = new ArrayList();
        
		//Add java opts, checking for two opts which are set in the 
		//basic flex shell scripts, and setting them if they're not 
		//overridden.
		//TODO: Test this.
		boolean maxMemIsSet = false;
		boolean sunIoCachesIsSet = false;
        for( int i = 0; i < javaOpts.length; i++ ) {
            commandLineArgs.add( javaOpts[i] );
            if (javaOpts[i].startsWith("-Xmx")) maxMemIsSet = true;
            else if (javaOpts[i].startsWith("-Dsun.io.useCanonCaches")) sunIoCachesIsSet = true;
        }
        if (!maxMemIsSet) commandLineArgs.add("-Xmx384m");
        if (!sunIoCachesIsSet) commandLineArgs.add("-Dsun.io.useCanonCaches=false");
        
		commandLineArgs.add("-classpath");
		commandLineArgs.add(getCommandClassPath());
		
		//
		// Setup the Java parameters.
		//
		
		List parameters = prepareParameters();
    	finalizeParameters(parameters); // stuff

    	
    	int result = -1;

		try {
			source = source.getCanonicalFile(); // try to reduce size of line.
		} catch (IOException e) {}

        try {
            result = executeJavaCommand("java", source.getAbsolutePath(), commandLineArgs, getCompilerClass(), parameters);
            if ( result != 0 ) 
                throw new MojoExecutionException( "Result of " + getCompilerClass() + " execution is: '" + result + "'." );
        } catch ( CommandLineException e ) {
            throw new MojoExecutionException( "Command execution failed.", e );
        }
        
        postProcess();
		
	}
	
	/**
	 * Executed near the end of execute().
	 */
	protected void postProcess() {
		// Point Maven2 at the outbound file.
        File outFile = getOutputFile();
		if (classifier != null) {
			projectHelper.attachArtifact(project, classifier, outFile);
		} else {
			project.getArtifact().setFile(outFile);
		}
	};
	
	protected void addClasspathEntry(StringBuffer compilerClasspath, String path) {
		getLog().debug("Adding " + path + " to path.");
		if (compilerClasspath.length() != 0) {
			compilerClasspath.append(System.getProperty("path.separator"));
		} 
		compilerClasspath.append(path);
	}
	
	protected int executeJavaCommand(String cmd, String cwd, List commandLineArgs, String className, List programArgs) throws CommandLineException {
		
    	// Attempt to invoke the compiler
		// create the array for the method sig consisting of one
		// string array representing command-line params.
		getLog().debug("Invoking " + className + " with parameters: " + programArgs);
		
        Commandline cl = new Commandline();
        cl.setExecutable( cmd );
        Iterator clargsIterator = commandLineArgs.iterator();
        while ( clargsIterator.hasNext() ) 
        	cl.createArgument().setValue( (String)clargsIterator.next() );

        cl.createArgument().setValue( StreamedParameterExecutableWrapper.class.getCanonicalName() );
        
        cl.createArgument().setValue( className );
        
        InputStream argIs = StringStreamUtil.prepareInputStreamFromStrings(programArgs);
        
        cl.setWorkingDirectory( cwd );
        
        StreamConsumer consumer = new StreamConsumer() {
            public void consumeLine( String line ) { getLog().info( line ); }
        };
        
        getLog().debug("Command line: " + cl);	
        
        return CommandLineUtils.executeCommandLine( cl, argIs, consumer, consumer );
        
	}

}

