// https://searchcode.com/api/result/126212642/

package net.sf.pdfsplice.xperimental;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PRIndirectReference;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStream;

public class AnalysePdf {

	String basedir = "test-data/";
	
	//String FIN = basedir + "input-data/eff_comcast_report.pdf";
	String FIN = basedir + "input-data/eff_comcast_report.attach.pdf";
	
	String FOUTDir = basedir + "output-data/analyse/";
	
	public AnalysePdf() {
	}
	
	Set<PdfReader> readersAnalysed = new HashSet<PdfReader>();;
	
	void reduce() throws IOException, DocumentException {
		PdfReader reader = new PdfReader(FIN);
		
		File outDir = new File(FOUTDir);
		
		if(!outDir.isDirectory()) {
			outDir.mkdirs();
		}
		
		analyseReader(reader);
		// we retrieve the total number of pages
	}
	
	void analyseReader(PdfReader reader) throws IOException {
		if(readersAnalysed.contains(reader)) {
			System.out.println("reader already analysed... "+reader);
			return;
		}
		readersAnalysed.add(reader);
		
		int n = reader.getNumberOfPages();
		// we retrieve the size of the first page
		Rectangle psize = reader.getPageSize(1);
		float width = psize.getWidth();
		float height = psize.getHeight();
		System.out.println(">>>> There are " + n + " pages in the document. width="+width+" height="+height);
		
		//Rectangle rfinal = new Rectangle(width, height);
		
		// creation of a document-object
		Document document = new Document();
		//Document document = new Document(rfinal);
		// create a writer that listens to the document
		//PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(FOUT));
		// open the document
		document.open();
		// add content from source
		//PdfContentByte cb = writer.getDirectContent();
		int i = 0;
		int p = 0;
		//System.out.println("There are " + n + " pages in the document. width="+width+" height="+height);
		//float widthOut = width/cols;
		//float heightOut = height/rows;
		
		reader.getInfo();
		
		int idxObj = 1;
		//PdfObject pdfobj = reader.getPdfObject(idxObj);
		PdfObject pdfobj = reader.getCatalog();
		
		while(pdfobj!=null) {
			System.out.println("==================== "+idxObj);
			//analyseObj(idxObj, pdfobj, "");
			analyseStream(idxObj, pdfobj);
			
			idxObj++;
			pdfobj = reader.getPdfObject(idxObj);
		}
		
		//analyseStream(pdfobj);

		/*while (i < n) {
			i++;
			PdfImportedPage page1 = writer.getImportedPage(reader, i);
		}*/
	}
	
	void analyseObj(int idxObj, PdfObject pdfobj, String prepend) {
		System.out.println(prepend+idxObj+": "+pdfobj);
		if(pdfobj.isDictionary()) {
			System.out.println(prepend+"[dict:"+idxObj+"]: "+pdfobj);
			Set<PdfName> keys = ((PdfDictionary)pdfobj).getKeys();
			for(PdfName pn: keys) {
				//System.out.println(prepend+"  - "+pn+": "+((PdfDictionary)pdfobj).get(pn));
				//System.out.println(prepend+"  - "+pn);
				analyseObj(idxObj, ((PdfDictionary)pdfobj).get(pn), prepend+"    ");
			}
		}
		else if(pdfobj.isStream()) {
			PdfStream pdfstream = (PdfStream) pdfobj;
			int l = pdfstream.length();
			int rl = pdfstream.getRawLength();
			int bl = -1;
			byte[] bytes = pdfstream.getBytes();
			if(bytes!=null) {
				bl = bytes.length; //pdfstream.getBytes().length;
			}
			
			System.out.println(prepend+"[stream:"+idxObj+"]: l="+l+"; rl="+rl+"; b="+bl);
		}
		else if(pdfobj.isIndirect()) {
			PRIndirectReference pdfindref = (PRIndirectReference) pdfobj; 
			byte[] barr = pdfindref.getBytes();
			int size=0;
			if(barr!=null) {
				size = barr.length;
			}
			//System.out.println(prepend+"[indirect:"+idxObj+"]: "+pdfindref); //+" (size="+size+")");
			////PdfReader readerX = pdfindref.getReader();
			////analyseReader(readerX);
		}
		else if(pdfobj.isArray()) {
			PdfArray parr = (PdfArray) pdfobj;
			System.out.println(prepend+"[array:"+idxObj+"]: "+parr+" (size="+parr.size()+")");
			for(int i=0;i<parr.size();i++) {
				PdfObject obj = parr.getPdfObject(i);
				//System.out.println(prepend+"  - "+obj);
				analyseObj(idxObj, obj, prepend+"    ");
			}
		}
		else if(pdfobj.isBoolean() || pdfobj.isName() || pdfobj.isNumber() || pdfobj.isString()) {
			//System.out.println(prepend+"[type:"+idxObj+":"+pdfobj.getClass().getSimpleName()+"]: "+pdfobj);
		}
		else {
			//System.out.println(prepend+"[etc:"+idxObj+":"+pdfobj.getClass().getName()+"]: "+pdfobj);
		}
	}
	
	int idFile = 0;
	
	/**
	 * 
	 * outputs all streams found to filesystem.
	 * 
	 * @param idxObj
	 * @param pdfobj
	 * @return
	 * @throws IOException
	 */
	boolean analyseStream(int idxObj, PdfObject pdfobj) throws IOException {
		boolean retFinal = false;
		if(pdfobj.isDictionary()) {
			//System.out.println("[dict:"+idxObj+"]: "+pdfobj);
			Set<PdfName> keys = ((PdfDictionary)pdfobj).getKeys();
			for(PdfName pn: keys) {
				boolean ret = analyseStream(idxObj, ((PdfDictionary)pdfobj).get(pn));
				if(ret) { System.out.println("   dict: "+pdfobj); retFinal = true; }
			}
		}
		if(pdfobj.isArray()) {
			//System.out.println("[array:"+idxObj+"]: "+pdfobj);
			PdfArray parr = (PdfArray) pdfobj;
			for(int i=0;i<parr.size();i++) {
				PdfObject obj = parr.getPdfObject(i);
				boolean ret = analyseStream(idxObj, obj);
				if(ret) { System.out.println("   arr: "+pdfobj); retFinal = true; }
			}
		}
		if(pdfobj.isStream()) {
			++idFile;
			System.out.println("isStream: ["+idxObj+" ; "+idFile+"]"+pdfobj);
			PRStream stream = (PRStream) PdfReader.getPdfObject(pdfobj);
			//System.out.println("PRStream: "+stream);
			FileOutputStream fos = new FileOutputStream(FOUTDir+"f" + idFile + ".stream");
			fos.write(PdfReader.getStreamBytes(stream));
			fos.flush();
			fos.close();
			
			return true;
		}
		/*else {
			//System.out.println("zzz: "+pdfobj);
		}*/
		
		return retFinal;
	}
	
	/**
	 * @param args
	 * @throws DocumentException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, DocumentException {
		AnalysePdf rp = new AnalysePdf();
		rp.reduce();
	}

}

