// https://searchcode.com/api/result/7299503/

package com.ader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.ader.smil.AudioElement;
import com.ader.smil.SmilFile;
import com.ader.testutilities.CreateDaisy202Book;

public class GenerateDaisy202BookTest extends TestCase {

	private ByteArrayOutputStream out;
	private CreateDaisy202Book eBook;
	
	@Override
	protected void setUp() {
		out = new ByteArrayOutputStream();
		try {
			eBook = new CreateDaisy202Book(out);
		} catch (NotImplementedException e) {
			throw new RuntimeException(e);
		}
		eBook.writeXmlHeader();
		eBook.writeDoctype();
		eBook.writeXmlns();
		eBook.writeBasicMetadata();
	}
	/**
	 * Note: this test confirms we are able to insert incorrect content e.g.
	 * for testing. It confirms the code we're testing is *NOT* intended for
	 * real-world use.
	 */
	@SmallTest
	public void testAbilityToInjectEmptySmilFile() {
		eBook.addSmilFileEntry(1, "", "");
		eBook.writeEndOfDocument();

		XMLParser anotherParser = createDaisyBookIndex();
		
		NavCentre nc = anotherParser.processNCC();
		assertEquals("Expected a 1:1 match of NavPoints and sections", 0, nc.count());
	}
	
	@MediumTest
	public void testAbilityToInjectSingleItemSmilFile() throws Exception {
		SmilFile singleEntry = new SmilFile();
		singleEntry.load("/sdcard/testfiles/singleEntry.smil");
		List <AudioElement> audio = singleEntry.getAudioSegments();
		String id = audio.get(0).getId();
		assertEquals("ID of the audio element incorrect", "audio_0001", id);
		eBook.addSmilFileEntry(1, "singleEntry.smil", id);
		eBook.writeEndOfDocument();

		XMLParser anotherParser = createDaisyBookIndex();
		
		NavCentre nc = anotherParser.processNCC();
		assertEquals("Expected a 1:1 match of NavPoints and sections", 1, nc.count());
	}
	
	@SmallTest
	public void testAbilityToAddVariousValidLevels() {
		final String sections = "123456";
		eBook.addTheseLevels(sections);
		eBook.writeEndOfDocument();
		
		XMLParser anotherParser = createDaisyBookIndex();
		NavCentre nc = anotherParser.processNCC();
		assertEquals(
				String.format("Expected a section for each section added.", sections.length()),
				sections.length(), nc.count());
	}

	@SmallTest
	public void testExceptionThrownForInvalidLevels() {
		final String sections = "1234567";
		try {
			eBook.addTheseLevels(sections);
			fail(String.format("An exception should be thrown as %s contains an invalid level: 7",
					sections));
			
		} catch (IllegalArgumentException iae) {
			// pass, this is the expected result.
		}
	}
	
	@SmallTest
	public void testExceptionThrownForInvalidContents() {
		final String invalidContents = "abc";
		try {
			eBook.addTheseLevels(invalidContents);
			fail(String.format("An exception should be thrown as %s contains invalid contents", 
					invalidContents));
		} catch (IllegalArgumentException iae) {
			// pass, this is the expected result
		}
	}
	/**
	 * Simple helper method to reduce code duplication.
	 * @return a parser that represents the index of a DAISY 2.02 book.
	 */
	private XMLParser createDaisyBookIndex() {
		ByteArrayInputStream newBook = new ByteArrayInputStream(out.toByteArray());
		XMLParser anotherParser = new XMLParser(newBook);
		return anotherParser;
	}
}

