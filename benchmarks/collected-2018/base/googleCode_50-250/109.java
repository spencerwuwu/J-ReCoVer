// https://searchcode.com/api/result/12624530/

/**
 * File: ShortReadGraphLoadProcessor.java
 * Created by: mhaimel
 * Created on: Apr 20, 2010
 * CVS:  $Id: ShortReadGraphLoadProcessor.java 1.0 Apr 20, 2010 9:48:26 AM mhaimel Exp $
 */
package uk.ac.ebi.curtain.processor.load.impl;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.ebi.curtain.model.graph.curtain.CurtainGraph;
import uk.ac.ebi.curtain.model.graph.curtain.impl.FillCurtainGraph;
import uk.ac.ebi.curtain.util.io.MapEntryBean;
import uk.ac.ebi.curtain.util.io.MapEntryWriter;
import uk.ac.ebi.curtain.utils.CurtainUncheckedException;
import uk.ac.ebi.curtain.utils.concurrent.UtilsCollect;
import uk.ac.ebi.curtain.utils.data.DataEntry;
import uk.ac.ebi.curtain.utils.data.MetaData;
import uk.ac.ebi.curtain.utils.data.ReadType;
import uk.ac.ebi.curtain.utils.file.FileInfo;
import uk.ac.ebi.curtain.utils.file.maq.MaqFlag;
import uk.ac.ebi.curtain.utils.match.MatchDetails;
import uk.ac.ebi.velvet.model.Strand;

/**
 * @author mhaimel
 *
 */
public class ShortReadGraphLoadProcessor extends AbstractMatchLoadProcessor<DataEntry<File>, String> {
	private static final int _DEFAULT_MIN_MAPPING_QUALITY = 15;
	private final AtomicReference<MapEntryWriter> mapEntryWriter = new AtomicReference<MapEntryWriter>();
	private final AtomicReference<FillCurtainGraph> fillGraph = new AtomicReference<FillCurtainGraph>();
	private final AtomicBoolean pairedEntries = new AtomicBoolean(false);
	private final AtomicInteger minQuality = new AtomicInteger(_DEFAULT_MIN_MAPPING_QUALITY);

	@Override
	protected DataEntry<File> getDataEntries() {
		return getInput();
	}

	@Override
	protected void init(FileInfo fi,Integer cat, Integer inslen) {
		getLog().debug("Use MapEntry writer " + mapEntryWriter.get());
		CurtainGraph graph = getContext().getGraph();
		if(!fi.getReadType().isIn(ReadType.getShort())){
			throw new IllegalArgumentException("This parser is NOT able to load "+ fi + "!!! Only to following are allowed: " + Arrays.toString(ReadType.getShort()));
		}
		if(fi.getReadType().isPaired()){
			FillCurtainGraph filler = new FillCurtainGraph(cat, graph, inslen);
			filler.setDistanceInfo(
					getContext().getArguments().getDistanceInfo());
			fillGraph.set(filler);
			pairedEntries.set(true);
		} else {
			pairedEntries.set(false);
		}
	}
	
	protected int getMinQual(){
		return minQuality.get();
	}
	
	public void setMinQual(int minimumMappingQuality){
		this.minQuality.set(minimumMappingQuality);
	}
	
	@Override
	protected String buildReturnObject() {
		return "Finished Loading "+getDataEntries();
	}
	
	public void setMapEntryWriter(MapEntryWriter mapEntryWriter) {
		this.mapEntryWriter.set(mapEntryWriter);
	}
	
	protected void unmappedEntry(MatchDetails details, MetaData meta, File file){
		// do nothing
	}
	
	
	
	@Override
	protected void storeParsedEntry(MatchDetails details, MetaData meta, File file) {
		boolean mapped = true;
		if(!details.getMapped()){
			if(pairedEntries.get()){
				if(!details.getOtherMapped()){
					mapped = false;
				}
			} else {
				mapped = false;
			}
		}
		if(!mapped){
			unmappedEntry(details, meta, file);
			return;
		}
		Integer ctgIdx = getCtgIdx().getIdentifier(details.getReference());
		if(null == ctgIdx){
			throw new IllegalStateException("Contig identifier not registered: " + details.getReference());
		}
		if(pairedEntries.get()){
			storePairedData(ctgIdx,details,meta);
		} else {
			MapEntryBean bean = toMapEntryBean(details, meta.getCategory());
			storeSingleData(ctgIdx,details,bean); // TODO move to new storage
		}
	}

	private MapEntryBean toMapEntryBean(MatchDetails details, Integer category) {
		MaqFlag flag = MaqFlag.getFlat(details.getPaired(), details.getMapped(), details.getOtherMapped(), details.getDifferentContig());
		MapEntryBean bean = new MapEntryBean(category,flag,details);
		return bean;
	}

	private final ConcurrentMap<String, MatchDetails> read2details = UtilsCollect.newConcurrentMap();
	private Map<String, Integer> read2ctg = new HashMap<String, Integer>(); 

	private void storePairedData(Integer ctgIdx, MatchDetails details, MetaData meta){
		if(!details.getPaired()){
			throw new CurtainUncheckedException("`Unpaired` flag in `Paired` file!!!");
		}
		if(!details.getMapped() && !details.getOtherMapped()){
			// ignore - both unmapped - should NOT happen at this point
			return;
		} 
		if(isBadMap(details)){
			if(details.getAlignBlockNumber() > 1){
				// keep first to clear the other pair
				return;
			}
		}
		/********************************************************************************************/		
		MatchDetails otherDetails = read2details.remove(details.getReadId());
		Integer otherCtgIdx = read2ctg.remove(details.getReadId());
		if(null == otherCtgIdx){
			if(!isBadMap(details)){
				read2details.put(details.getReadId(), details);
				// to reduce storage
			}
			read2ctg.put(details.getReadId(), ctgIdx);
			return;
		}
		if(null == otherDetails){
			return; // other pair had partial matches
		}
		if(isBadMap(details) || isBadMap(otherDetails)){
			return; // Ignore
		}	
		MatchDetails fwdDetails = details;
		Integer fwdCtg = ctgIdx;
		MatchDetails revDetails = otherDetails;
		Integer revCtg = otherCtgIdx;
		if(!fwdDetails.getReadOrientation().equals(Strand.FWD)){
			fwdDetails = otherDetails;
			revDetails = details;	
			fwdCtg = otherCtgIdx;
			revCtg = ctgIdx;
		}
		
		Integer category = meta.getCategory();
		_dostoreData(fwdCtg,fwdDetails,toMapEntryBean(fwdDetails, category));
		_dostoreData(revCtg,revDetails,toMapEntryBean(revDetails, category));
		if(!fwdCtg.equals(revCtg) && !fwdDetails.getDifferentContig()){
			getLog().debug("Problem");
		} else if(fwdCtg.equals(revCtg) && fwdDetails.getDifferentContig()){
			getLog().debug("Problem");
		}
		
	}

	private boolean isBadMap(MatchDetails details) {
		if(details.getAlignBlocks() > 1 ){
			return true; // only the first one - bad short read mapping
		}
		return false;
	}

	private void storeSingleData(Integer ctgIdx, MatchDetails details, MapEntryBean bean) {
		if(bean.getMapQual() >= getMinQual()){
//			_dostoreData(ctgIdx, details, bean); TODO removed
		}
	}

	private void _dostoreData(Integer ctgIdx, MatchDetails details, MapEntryBean bean) {
		storeMapping(bean);
		addToGraph(ctgIdx, details);
	}

	private void addToGraph(Integer ctgIdx, MatchDetails details) {
		if(pairedEntries.get()){
			FillCurtainGraph filler = fillGraph.get();
			filler.mapEntry(Long.valueOf(ctgIdx.longValue()), details);
		}
	}

	private void storeMapping(MapEntryBean bean) {
		mapEntryWriter.get().write(bean);
	}

	@Override
	protected void postStore() {
		getLog().debug("Remaining: " + this.read2details.size());
		// TODO Auto-generated method stub
	}
}

