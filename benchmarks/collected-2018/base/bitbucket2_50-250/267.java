// https://searchcode.com/api/result/46076535/

//Copyright (C) 2010  Novabit Informationssysteme GmbH
//
//This file is part of Nuclos.
//
//Nuclos is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Nuclos is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Nuclos.  If not, see <http://www.gnu.org/licenses/>.
package org.nuclos.tools.ruledoc.doclet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.nuclos.tools.ruledoc.javaToHtml.HTMLFromSourceGenerator;

import com.sun.tools.doclets.internal.toolkit.builders.AbstractBuilder;
import com.sun.tools.doclets.internal.toolkit.util.*;

/**
 * The class with "start" method, calls individual Writers.
 *
 *
 *
 * @author novabit
 *
 */
public class CommonDoclet extends AbstractNovabitDoclet {

	public ConfigurationImpl configuration = configuration();

	public static final String BUILD_DATE = System.getProperty("java.version");

	public static final String SOURCE_OUTPUT_DIR_NAME = "src-html/";

	public static boolean start(RootDoc root) {
		CommonDoclet doclet = new CommonDoclet();
		return doclet.start(doclet, root);
	}

	@Override
	public ConfigurationImpl configuration() {
		return ConfigurationImpl.getInstance();
	}

	@Override
	protected void generateOtherFiles(RootDoc root, ClassTree classtree)
			throws Exception {
		super.generateOtherFiles(root, classtree);

		if (configuration.linksource) {
			HTMLFromSourceGenerator
					.convertRoot(
							configuration,
							root,
							ConfigurationImpl
									.includeTrailingFilePathDelimiter(SOURCE_OUTPUT_DIR_NAME));
		}

		boolean nodeprecated = configuration.nodeprecated;
		String configdestdir = configuration.destDirName;
		String confighelpfile = configuration.helpfile;
		String configstylefile = configuration.stylesheetfile;
		performCopy(configdestdir, confighelpfile);
		performCopy(configdestdir, configstylefile);
		Util.copyResourceFile(configuration, "inherit.gif", false);
		// do early to reduce memory footprint

		AllClassesFrameWriter.generate(configuration, new IndexBuilder(
				configuration, nodeprecated, true));

		FrameOutputWriter.generate(configuration);

		if (configuration.helpfile.length() == 0 && !configuration.nohelp) {
			HelpWriter.generate(configuration);
		}
		if (configuration.stylesheetfile.length() == 0) {
			StylesheetWriter.generate(configuration);
		}
	}

	@Override
	protected void generateClassFiles(ClassDoc[] arr, ClassTree classtree) {
		Arrays.sort(arr);
		for (int i = 0; i < arr.length; i++) {
			if (!(configuration.isGeneratedDoc(arr[i]) && arr[i].isIncluded())) {
				continue;
			}
			ClassDoc prev = (i == 0) ? null : arr[i - 1];
			ClassDoc curr = arr[i];
			ClassDoc next = (i + 1 == arr.length) ? null : arr[i + 1];
			try {
				if (curr.isAnnotationType()) {
					AbstractBuilder annotationTypeBuilder = configuration
							.getBuilderFactory().getAnnotationTypeBuilder(
							(AnnotationTypeDoc) curr, prev, next);
					annotationTypeBuilder.build();
				}
				else {
					AbstractBuilder classBuilder = configuration
							.getBuilderFactory().getClassBuilder(curr, prev,
							next, classtree);
					classBuilder.build();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new DocletAbortException();
			}
		}
	}

	@Override
	protected void generatePackageFiles(ClassTree classtree) throws Exception {
		PackageDoc[] packages = configuration.packages;
		if (packages.length > 1) {
			PackageIndexFrameWriter.generate(configuration);
		}
		PackageDoc prev = null, next;
		for (int i = 0; i < packages.length; i++) {
			PackageFrameWriter.generate(configuration, packages[i]);
			next = (i + 1 < packages.length && packages[i + 1].name().length() > 0) ? packages[i + 1]
					: null;
			// If the next package is unnamed package, skip 2 ahead if possible
			next = (i + 2 < packages.length && next == null) ? packages[i + 2]
					: next;
			AbstractBuilder packageSummaryBuilder = configuration
					.getBuilderFactory().getPackageSummaryBuilder(packages[i],
					prev, next);
			packageSummaryBuilder.build();

			prev = packages[i];
		}
	}

	public static int optionLength(String option) {
		// Construct temporary configuration for check
		return (ConfigurationImpl.getInstance()).optionLength(option);
	}

	public static boolean validOptions(String options[][],
			DocErrorReporter reporter) {
		// Construct temporary configuration for check
		return (ConfigurationImpl.getInstance())
				.validOptions(options, reporter);
	}

	private void performCopy(String configdestdir, String filename) {
		try {
			String destdir = (configdestdir.length() > 0) ? configdestdir
					+ File.separatorChar : "";
			if (filename.length() > 0) {
				File helpstylefile = new File(filename);
				String parent = helpstylefile.getParent();
				String helpstylefilename = (parent == null) ? filename
						: filename.substring(parent.length() + 1);
				File desthelpfile = new File(destdir + helpstylefilename);
				if (!desthelpfile.getCanonicalPath().equals(
						helpstylefile.getCanonicalPath())) {
					configuration.message.notice((SourcePosition) null,
							"doclet.Copying_File_0_To_File_1", helpstylefile
							.toString(), desthelpfile.toString());
					Util.copyFile(desthelpfile, helpstylefile);
				}
			}
		}
		catch (IOException exc) {
			configuration.message
					.error((SourcePosition) null,
							"doclet.perform_copy_exception_encountered", exc
							.toString());
			throw new DocletAbortException();
		}
	}
}

