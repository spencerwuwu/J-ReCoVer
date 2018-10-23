// https://searchcode.com/api/result/17331957/

/*******************************************************************************
 * Copyright (c) 2005-2011 eBay Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.ebayopensource.dsf.javatojs.tests.cli;
import static com.ebay.junitnexgen.category.Category.Groups.FUNCTIONAL;
import static com.ebay.junitnexgen.category.Category.Groups.P1;
import static com.ebay.junitnexgen.category.Category.Groups.P5;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.ebayopensource.dsf.javatojs.cli.Java2Vjo;
import org.ebayopensource.dsf.javatojs.tests.cli.data.Boo;
import org.ebayopensource.dsf.javatojs.tests.cli.data.Goo;
import org.ebayopensource.dsf.javatojs.tests.cli.data.subdir.Foo;
import org.ebayopensource.dsf.javatojs.tests.cli.data.subdir.Hoo;
import org.ebayopensource.dsf.javatojs.tests.cli.data2.Doo;
import org.ebayopensource.dsf.javatojs.tests.data.build.Dependent;
import org.ebayopensource.dsf.javatojs.tests.data.build.subdirma.H;
import org.ebayopensource.dsf.javatojs.translate.TranslateCtx;
import org.ebayopensource.dsf.javatojs.util.JavaToJsHelper;
import org.ebayopensource.dsf.util.JavaSourceLocator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ebay.junitnexgen.category.Category;
import com.ebay.junitnexgen.category.Description;
import com.ebay.junitnexgen.category.ModuleInfo;

@ModuleInfo(value="DsfPrebuild",subModuleId="JavaToJs")
public class Java2VjoTests {

	private PrintStream sysout = System.out;
	private ByteArrayOutputStream buffer;
	private long dateInMillis;
	
	private static final String SRC_DIR_NAME = "src";

	private File workingDir;
	private File sourceDir;
	
	public Java2VjoTests() throws IOException, URISyntaxException {
		prepareSourceDir(this.getClass());
	}
	
	@Before
	public void initTests() throws IOException, InterruptedException {
		//clear buffer before each test and add delay so test does not run too fast and cause
		//get time before test run to compare against file creation times
		TranslateCtx.ctx().reset();
		dateInMillis = Calendar.getInstance().getTimeInMillis();
		buffer =  new ByteArrayOutputStream();
		TeePrintStream printStream = new TeePrintStream(buffer);
		
		// Kpatil : to reduce console log , set debug to flase 
		printStream.setDebug(true);
		System.setOut(printStream);
	}

	@After
	public  void cleanup() throws IOException{
		//change sysout back to console
		System.setOut(sysout);
		traverseDir(workingDir);
	}

	
	private void prepareSourceDir(Class<?> anchor) throws IOException,
			URISyntaxException {
		URL sourceUrl = JavaSourceLocator.getInstance().getSourceUrl(anchor);
		if (sourceUrl.getProtocol().equalsIgnoreCase("jar")) {
			final JarURLConnection conn = (JarURLConnection) sourceUrl
					.openConnection();
			final JarFile jarFile = conn.getJarFile();
			workingDir = new File(jarFile.getName() + "Extract");
			sourceDir = new File(workingDir, SRC_DIR_NAME);
			sourceDir.mkdirs();

			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				final JarEntry currentEntry = entries.nextElement();
				if (currentEntry.isDirectory()) {
					new File(sourceDir, currentEntry.getName()).mkdirs();
				} else {
					final File fileToBeWritten = new File(sourceDir,
							currentEntry.getName());

					if (!fileToBeWritten.getParentFile().exists()) {
						fileToBeWritten.getParentFile().mkdirs();
					}
					fileToBeWritten.createNewFile();
					final FileWriter fileWriter = new FileWriter(
							fileToBeWritten);
					fileWriter.write(JavaToJsHelper
							.readFromInputReader(new InputStreamReader(jarFile
									.getInputStream(currentEntry))));
					fileWriter.flush();
					fileWriter.close();

				}
			}

		} else {
			String packagePath = anchor.getPackage().getName()
					.replace('.', '/');
			String url = sourceUrl.toExternalForm();
			sourceDir = new File(new URL(url.substring(0, url
					.indexOf(packagePath))).toURI());
			workingDir = sourceDir.getParentFile();
			sourceDir.mkdirs();
		}
	}
	
	private File getDirectDir(Class<?> clz) {
		return new File(sourceDir, clz.getPackage().getName().replace('.', '/'));
	}
	
	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo usage with null arg")
	public void paramNull() throws IOException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Java2Vjo.main(null);
		Field field = Java2Vjo.class.getDeclaredField("s_help");
		field.setAccessible(true);
		assertTrue(buffer.size() > 0);
		String expected = buffer.toString().replaceAll("\r", "").replaceAll("\n", "");
		String actual = field.get(String.class).toString().replaceAll("\r", "").replaceAll("\n", "");
		assertEquals(expected, actual);

	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo usage with empty arg list")
	public void paramEmpty() throws IOException, SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		Java2Vjo.main(new String[0]);
		Field field = Java2Vjo.class.getDeclaredField("s_help");
		field.setAccessible(true);
		assertTrue(buffer.size() > 0);
		String expected = buffer.toString().replaceAll("\r", "").replaceAll("\n", "");
		String actual = field.get(String.class).toString().replaceAll("\r", "").replaceAll("\n", "");
		assertEquals(expected, actual);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo behavior with no file paths are specified")
	public void noFilePaths() throws IOException {
		String[] params = new String[] { "-verbose" };
		Java2Vjo.main(params);
		assertEquals("ERROR: No Valid File Paths Found!\n", buffer.toString()
				.replaceAll("\r", ""));
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo with -help arg")
	public void helpOnly() throws IOException, IllegalArgumentException,
			IllegalAccessException, SecurityException, NoSuchFieldException {
		String[] params = new String[] { "-help" };
		Java2Vjo.main(params);
		Field field = Java2Vjo.class.getDeclaredField("s_help");
		field.setAccessible(true);
		assertTrue(buffer.size() > 0);
		String expected = buffer.toString().replaceAll("\r", "").replaceAll("\n", "");
		String actual = field.get(String.class).toString().replaceAll("\r", "").replaceAll("\n", "");
		assertEquals(expected, actual);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo with other help args")
	public void helpWithOthers() throws IOException, SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		
		File filePath = getDirectDir(Dependent.class);
		String[] params = new String[] { filePath.getCanonicalPath(), "-ot", "-help" };
		Java2Vjo.main(params);
		Field field = Java2Vjo.class.getDeclaredField("s_help");
		field.setAccessible(true);
		assertTrue(buffer.size() > 0);
		String expected = buffer.toString().replaceAll("\r", "").replaceAll("\n", "");
		String actual = field.get(String.class).toString().replaceAll("\r", "").replaceAll("\n", "");
		assertEquals(expected, actual);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo usage with invalid file path")
	public void invalidFilePath() {
		String[] args = new String[] { "jfkladjfldkasf", "-verbose" };
		Java2Vjo.main(args);
		assertEquals(
				"File/Path Does Not Exist: jfkladjfldkasf\nERROR: No Valid File Paths Found!\n",
				buffer.toString().replaceAll("\r", ""));

	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo with invalid param")
	public void invalidParam() throws IOException {

		File filePath = getDirectDir(Dependent.class);
		String[] args = new String[] { filePath.getCanonicalPath(), "-vlade" };
		Java2Vjo.main(args);
		assertTrue(buffer.toString().indexOf("Ignoring unknown option: -vlade") > -1);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo with expected param")
	public void defaultTest() throws IOException {

		File filePath = getDirectDir(Goo.class);
		String[] args = new String[] { filePath.getCanonicalPath() };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		validate(list, buffer.toString(), true, false, false);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo with trace params, -verbose, -trace")
	public void verboseTrace() throws IOException {
		File filePath = getDirectDir(Goo.class);
		String[] args = new String[] { "-verbose", filePath.getCanonicalPath(), "-trace" };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		validate(list, buffer.toString(), true, true, true);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo with param to turn off jsr")
	public void noJsrGen() throws IOException, InterruptedException {
		
		File filePath = getDirectDir(H.class);
		String[] args = new String[] { "-verbose", filePath.getCanonicalPath(), "-nojsr" };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		validate(list, buffer.toString(), false, true, false);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo valid directory path as param")
	public void dirPath() throws IOException {
		File filePath = getDirectDir(H.class);
		String[] args = new String[] { "-verbose",
				filePath.getParentFile().getCanonicalPath(), "-nojsr" };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		validate(list, buffer.toString(), false, true, false);
	}

//	@Test
//	@Category( { P1, FUNCTIONAL })
//	@Description("Test Java2Vjo with a list of directories")
//	public void onDemandDiffDir() throws IOException {
//		
//		File filePath = getDirectDir(Boo.class);
//		String[] args = new String[] { filePath.getCanonicalPath(),
//				"-nojsr", "-verbose" };
//		Java2Vjo.main(args);
//		ArrayList<File> list = new ArrayList<File>();
//		list.add(new File(getDirectDir(Boo.class),Boo.class.getSimpleName()+".java"));
//		list.add(new File(getDirectDir(Foo.class),Foo.class.getSimpleName()+".java"));
//		list.add(new File(getDirectDir(Hoo.class),Hoo.class.getSimpleName()+".java"));
//		list.add(new File(getDirectDir(Doo.class),Doo.class.getSimpleName()+".java"));
//		//create own set of packages and files to test.
//		validate(list, buffer.toString(), false, true, false);
//	}

	@Test
	@Category( { P1, FUNCTIONAL })
	@Description("Test Java2Vjo with -tt option and using a file")
	public void targetTranslationFile() throws IOException {
		File filePath = getDirectDir(Boo.class);
		String[] args = new String[] { filePath.getCanonicalPath(), "-nojsr", "-verbose", "-tt" };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		validate(list, buffer.toString(), false, true, false);
	}

	@Test
	@Category( { P1, FUNCTIONAL })
	@Description("Test Java2Vjo with -tt option and using a directory")
	public void targetTranslationDir() throws IOException {
		File filePath = getDirectDir(Boo.class);
		String[] args = new String[] {filePath.getParentFile().getCanonicalPath(),
				"-verbose", "-tt", "-verbose" };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		list.add(new File(getDirectDir(Goo.class),Goo.class.getSimpleName()+".java"));
		list.add(new File(getDirectDir(Foo.class),Foo.class.getSimpleName()+".java"));
		list.add(new File(getDirectDir(Hoo.class),Hoo.class.getSimpleName()+".java"));
		//create own set of packages and files to test.
		validate(list, buffer.toString(), true, true, false);
	}

	@Test
	@Category( { P1, FUNCTIONAL })
	@Description("Test Java2Vjo with a list of files")
	public void multipleFiles() throws IOException {

		File filePath = getDirectDir(Boo.class);
		File filePath2 = getDirectDir(Goo.class);
		String[] args = new String[] { filePath.getCanonicalPath(), "-nojsr", "-verbose",
				filePath2.getCanonicalPath(), "-ot", "-tt" };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		list.add(filePath2);
		validate(list, buffer.toString(), false, true, false);
	}

	@Test
	@Category( { P1, FUNCTIONAL })
	@Description("Test Java2Vjo a mix of files and directories")
	public void dirFileCombo() throws IOException {
		File filePath = getDirectDir(Foo.class);
		File filePath2 = getDirectDir(Doo.class);
		String[] args = new String[] { filePath.getParentFile().getCanonicalPath(),
				"-verbose", "-tt", filePath2.getCanonicalPath() };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		list.add(new File(getDirectDir(Hoo.class),Hoo.class.getSimpleName()+".java"));
		list.add(filePath2);
		validate(list, buffer.toString(), true, true, false);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo with lots of empty space in the midst of args")
	public void addtlSpaces() throws IOException {

		File filePath = new File(getDirectDir(Foo.class),Foo.class.getSimpleName()+".java");
		String[] args = new String[] { "   " + filePath.getCanonicalPath(), " -verbose ", "-tt ", };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		validate(list, buffer.toString(), true, true, false);
	}

	@Test
	@Category( { P5, FUNCTIONAL })
	@Description("Test Java2Vjo for the case-insensitiveness of options")
	public void optionsCase() throws IOException {
		File filePath = new File(getDirectDir(Foo.class),Foo.class.getSimpleName()+".java");
		String[] args = new String[] { filePath.getCanonicalPath(), "-Verbose ", "-TT", };
		Java2Vjo.main(args);
		ArrayList<File> list = new ArrayList<File>();
		list.add(filePath);
		validate(list, buffer.toString(), true, true, false);
	}

	public  void validate(ArrayList<File> files, String output,
			boolean jsrGen, boolean verbose, boolean trace) throws MalformedURLException {
		//give time for file system to update files i/o to finish.
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (File file : files) {
			String jsPath = file.getAbsolutePath();
			if (jsPath.endsWith(".java")){
				jsPath = jsPath.substring(0, jsPath.length()-4) + "js";
			}
			File jsFile = new File(jsPath);
			//validate js file was created/updated after test was run.
			assertTrue(dateInMillis <= jsFile.lastModified());
			//validate js files written to output/logs
			if (verbose) {
				assertTrue(output.indexOf("Write " + jsFile.toURI().toURL()) > -1);
			} else {
				assertFalse(output.indexOf("Write " + jsFile.toURI().toURL()) > -1);
			}
			
			jsFile.deleteOnExit();
			
			if (jsrGen) {
				String jsrPath = file.getAbsolutePath();
				if (jsrPath.endsWith(".java")){
					jsrPath = jsrPath.substring(0, jsrPath.length()-5) + "Jsr.java";
				}
				File jsrFile = new File(jsrPath);
				//validate jsr file was created/updated after test was run.
				assertTrue(dateInMillis <= jsrFile.lastModified());
				//validate jsr files written to output/logs
				if (verbose) {
					assertTrue(output.indexOf("Write "
							+ jsrFile.toURI().toURL(). toString().replace(".js",
									"Jsr.java")) > -1);
				} else {
					assertFalse(output.indexOf("Write "
							+ jsrFile.toURI().toURL()) > -1);
				}
				
				jsrFile.deleteOnExit();
				
			} else {
				String jsrPath = file.toURI().toURL().toString();
				if (jsrPath.endsWith(".java")){
					jsrPath = jsrPath.substring(0, jsrPath.length()-5) + "Jsr.java";
				}
				File jsrFile = new File(jsrPath);
				assertFalse(dateInMillis <= jsrFile.lastModified());
				jsrFile.deleteOnExit();
			}
		}

		
	
		
		//validate trace file was created after test run.
		File traceFile = new File("v4trace.xml");
//		if (trace) {
//			assertTrue(dateInMillis <= traceFile.lastModified());
//		} else {
//			assertFalse(dateInMillis <= traceFile.lastModified());
//		}

		
		
		//Validate number of files written out is as expected in logs
		//also check to ensure no other possible files created.
//		int occurs = countOccurrences(output, "Write");
//		if (jsrGen && verbose) {
//			assertEquals(files.size() * 2, occurs);
//		} else if (verbose) {
//			assertEquals(files.size(), occurs);
//		} else {
//			assertEquals(0, occurs);
//		}
	}

	public int countOccurrences(String arg1, String arg2) {
		int count = 0;
		int index = 0;
		while ((index = arg1.indexOf(arg2, index)) != -1) {
			++index;
			++count;
		}
		return count;
	}

	public void traverseDir(final File f) throws IOException {
		if (f.isDirectory()) {
			final File[] childs = f.listFiles();
			for (File child : childs) {
				traverseDir(child);
			}
			return;
		}
		if (f.getName().endsWith(".js") || f.getName().endsWith("Jsr.java")) {
			f.delete();
		}
	}
}

