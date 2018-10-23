// https://searchcode.com/api/result/7299516/

package com.ader.io;


import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

import com.ader.testutilities.CreateDaisy202Book;

public class BookValidatorTest extends TestCase {

	BookValidator validator = new BookValidator();
	CreateDaisy202Book eBook;
	private String dummyValidPath;
	private String dummyValidTextFile;
	private String dummyValidDaisyBookFolder;
	private String dummyValidDaisy202IndexFile;
	private String dummySecondDaisyBookFolder;
	private String dummyValidDaisy202UpperCaseIndexFile;
	private String dummyValidBook;
	private String dummyEmptyFolder;
	
	
	@Override
	protected void setUp() throws Exception {
	
		// I don't like having to initialise the dummy values with a private method,
		// however as System.getProperty("java.io.tempdir") behaves differently
		// depending on the runtime OS this is the best I can come up with for now.
		initializeDummyValues();
		
		// TODO (jharty): We need to create the folders and files that will be
		// used by these tests. At first we can probably live with creating and
		// purging the folders and files for each test, but we should try to
		// streamline the file IO to reduce the overall execution time and,
		// ideally, keep the side-effects of our tests (e.g. when run on a real
		// device) to a minimum.

		// TODO (jharty): find out how to stop needing so many 'new File(...)' calls
		if (new File(dummyValidPath).exists() || new File(dummyValidPath).mkdirs()) {}
		if (new File(dummyEmptyFolder).exists() || new File(dummyEmptyFolder).mkdirs()) {}
		if (!new File(dummyValidTextFile).exists()) {
			// TODO (jharty): There MUST be a cleaner way to code this!
			File dummyFile = new File(dummyValidTextFile);
			FileOutputStream myFile = new FileOutputStream(dummyFile);
			new PrintStream(myFile).println("some junk text which should be ignored.");
			myFile.close();
		}
		// Check whether the folder already exists
		if (new File(dummyValidDaisyBookFolder).exists() || new File(dummyValidDaisyBookFolder).mkdirs()) {
			// If the ncc.html file doesn't exist, create it
			if(!new File(dummyValidDaisy202IndexFile).exists()) {
				// How about creating a helper method WriteableFile(...)? to make the code readable
				File validDaisy202BookOnDisk = new File(dummyValidDaisy202IndexFile);
				FileOutputStream out = new FileOutputStream(validDaisy202BookOnDisk); 
				eBook = new CreateDaisy202Book(out);
				eBook.writeXmlHeader();
				eBook.writeDoctype();
				eBook.writeXmlns();
				eBook.writeBasicMetadata();
				eBook.addLevelOne();
				eBook.writeEndOfDocument();
				out.close(); // Now, save the changes.
			}
		}

		super.setUp();
	}

	private void initializeDummyValues() {
		final String tmpdir = System.getProperty("java.io.tmpdir");
		if (tmpdir.endsWith(File.separator)) {
			dummyValidPath = System.getProperty("java.io.tmpdir") + "daisyreadertests" + File.separator;
		} else {
			dummyValidPath = System.getProperty("java.io.tmpdir") + File.separator + "daisyreadertests" + File.separator;
		}	
		dummyValidTextFile = dummyValidPath + "dummyfile.txt";
		dummyValidDaisyBookFolder = dummyValidPath + "validbook";
		dummyValidDaisy202IndexFile = dummyValidDaisyBookFolder 
			+ File.separator + "ncc.html";
		dummySecondDaisyBookFolder = dummyValidPath + "anotherbook";
		dummyValidDaisy202UpperCaseIndexFile = dummySecondDaisyBookFolder 
			+ File.separator + "NCC.HTML";
		dummyValidBook = dummyValidDaisyBookFolder;
		dummyEmptyFolder = dummyValidPath + "emptyfolder/";
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		File cleanup = new File(dummyValidPath);
		recursiveDelete(cleanup);
	}

	public void testShouldFailForInvalidFileSystemRoot() {
		assertFalse("an invalid path should fail", validator
				.validFileSystemRoot("nonexistent file system"));
	}

	public void testShouldPassForValidFileSystemRoot() {
		assertTrue("an valid path should pass", validator
				.validFileSystemRoot(dummyValidPath));
	}

	public void testShouldFailForFileWhichIsNotAFolder() {
		assertFalse("an valid path should pass", validator
				.validFileSystemRoot(dummyValidTextFile));
	}

	public void testFolderContainsBook() {
		assertTrue("This folder should contain a valid book", validator
				.containsBook(dummyValidBook));
	}
	
	public void testValidBookFound() {
		validator.validFileSystemRoot(dummyValidPath);
		// TODO (jharty): the following call looks inappropriate since
		// dummyValidPath points elsewhere. I'm guessing this test intends to
		// read an external book from the filesystem (rather than one we
		// create in these tests. We need to decide how much to rely on
		// external content as these tests mature.

		validator.findBooks(dummyValidPath);
		assertTrue("there should be at least one book in the book list",
				validator.getBookList().size() > 0);  
		assertEquals("The path for the valid book is incorrect",
				dummyValidBook, validator.getBookList().get(0));
	}
	
	public void testValidBookWithUpperCaseIndexFileFound() throws Exception {
		
		if (new File(dummySecondDaisyBookFolder).exists() || new File(dummySecondDaisyBookFolder).mkdirs()) {
			File validUpperCaseDaisy202BookOnDisk = new File(dummyValidDaisy202UpperCaseIndexFile);
			FileOutputStream out = new FileOutputStream(validUpperCaseDaisy202BookOnDisk); 
			eBook = new CreateDaisy202Book(out);
			eBook.writeXmlHeader();
			eBook.writeDoctype();
			eBook.writeXmlns();
			eBook.writeBasicMetadata();
			eBook.addLevelOne();
			eBook.writeEndOfDocument();
			out.close(); // Now, save the changes.
		}

		
		assertTrue("The newly created book should exist.",
				new File(dummyValidDaisy202UpperCaseIndexFile).exists());
		
		validator.validFileSystemRoot(dummySecondDaisyBookFolder);
		validator.findBooks(dummySecondDaisyBookFolder);

		assertTrue("there should be one book in the book list",
				validator.getBookList().size() == 1);  
		assertEquals("The path for the valid book is incorrect",
				dummySecondDaisyBookFolder, validator.getBookList().get(0));

	}
	
	public void testNullPathGetsEmptyList() {
		BookValidator bookValidator = new BookValidator();
		bookValidator.findBooks(null);
		assertEquals("Should get an empty list of folders for a 'null' path", 
				0, bookValidator.getBookList().size());
	}
	
	public void testEmptyPathGetsEmptyList() {
		BookValidator bookValidator = new BookValidator();
		bookValidator.findBooks("");
		assertEquals("Should get an empty list of folders for an empty path", 
				0, bookValidator.getBookList().size());
	}	
	
	public void testStartsWithDotGetsEmptyList() {
		BookValidator bookValidator = new BookValidator();
		bookValidator.findBooks(".anything");
		assertEquals("Should get an empty list of folders for a path that starts with .", 
				0, bookValidator.getBookList().size());
	}
	
	/*
	 * Recursively delete file or directory
	 * @param fileOrDir the file or dir to delete
	 * @return true iff all files are successfully deleted
	 * 
	 * This code based on an answer from:
	 * http://stackoverflow.com/questions/617414/create-a-temporary-directory-in-java
	 */
	private static boolean recursiveDelete(File fileOrDir)
	{
	    if(fileOrDir.isDirectory())
	    {
	        // recursively delete contents
	        for(File innerFile: fileOrDir.listFiles())
	        {
	            if(!BookValidatorTest.recursiveDelete(innerFile))
	            {
	                return false;
	            }
	        }
	    }

	    return fileOrDir.delete();
	}

}

