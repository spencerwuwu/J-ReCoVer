// https://searchcode.com/api/result/134595154/

package ca.uwaterloo.coordinator.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.util.SafeEncoder;

import ca.uwaterloo.coordinator.metrics.RunningTime;
import ca.uwaterloo.coordinator.metrics.RuntimeProjectDetails;
import ca.uwaterloo.coordinator.services.project.Project;
import ca.uwaterloo.coordinator.services.project.ProjectPhase;
import ca.uwaterloo.coordinator.services.redis.dao.AvailableJobsKey;
import ca.uwaterloo.coordinator.services.redis.dao.JobsInfoKey;
import ca.uwaterloo.coordinator.services.redis.dao.Keys;
import ca.uwaterloo.coordinator.services.redis.dao.ProjectInfoKey;

@Service
public class ProjectService {
	
	public static int jobID = 0;

	public int workUnitSizeInKB = 2048;

	private String researcherUrl;
	
	private RedisSerializer<Object>	serializer;
	
	private ConcurrentHashMap<String, RuntimeProjectDetails> projectRuntimeDetailsMap = new ConcurrentHashMap<String, RuntimeProjectDetails>();
	
	@Autowired
	private RedisTemplate<String, Object> template;
	
	public void setSerializer(final RedisSerializer<Object> serializer) {
		this.serializer = serializer;
	}
	
	public void setResearcherUrl(final String researcherUrl){
		this.researcherUrl = researcherUrl;
	}
	
	public void setWorkUnitSizeInKB(final int workUnitSize){
		this.workUnitSizeInKB = workUnitSize;
	}
	
	public void createExampleProjectOnResearcher(String projectID, int sizeInMegabytes) throws IOException, JSONException{
		URL url = new URL(createExampleUrl(projectID,researcherUrl,sizeInMegabytes));
		URLConnection connection = url.openConnection();
		String line;
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		JSONObject json = new JSONObject(builder.toString());		
		
		assert json.getString("result").equalsIgnoreCase("OK");
	}
	
	public Project createProject(String projectID){
		return createProject(projectID, researcherUrl);
	}
	
	private Project createProject(String projectID, String url) {
		//
		// Get some details about the Researcher portal
		JSONObject chunkInfo = queryResearcherService(projectID, url);
		final RedisConnectionFactory factory = template.getConnectionFactory();
		final RedisConnection conn = factory.getConnection();
		final Jedis jedis = (Jedis) conn.getNativeConnection();
		Project project = null;
		try {
			Pipeline p = jedis.pipelined();
			
			createProjectProfile(p, 
					projectID,
					url,
					chunkInfo.getInt("chunks"), 
					chunkInfo.getDouble("sizePerChunk"));
			
			createWorkUnits(p, 
					projectID, 
					chunkInfo.getInt("chunks"), 
					chunkInfo.getDouble("sizePerChunk"));
			
			p.sync();
			
			project = new Project();
			project.setId(projectID);
			project.setChunks(chunkInfo.getInt("chunks"));
			project.setKbPerChunk(chunkInfo.getDouble("sizePerChunk"));
			project.setPhase(ProjectPhase.PRE_INITIALIZED);
			project.setUrl(url);
			
		}catch( JSONException e ){
			e.printStackTrace();
		} finally {
			RedisConnectionUtils.releaseConnection(conn, factory);
		}
		return project;
	}
	
	private void createProjectProfile(Pipeline p, 
			String projectID,
			String url,
			long chunkNum,
			double chunkSize) {
		
		ProjectInfoKey projectInfoKey = Keys.projectInfoKey(projectID);
		
		p.hset(SafeEncoder.encode(projectInfoKey.key()), 
				SafeEncoder.encode(projectInfoKey.chunksStr()), 
				serializer.serialize(chunkNum));
		p.hset(SafeEncoder.encode(projectInfoKey.key()), 
				SafeEncoder.encode(projectInfoKey.kbPerChunkStr()), 
				serializer.serialize(chunkSize));
		p.hset(projectInfoKey.key(), 
				projectInfoKey.phaseStr(), 
				ProjectPhase.PRE_INITIALIZED.phaseStr());
		p.hset(projectInfoKey.key(), 
				projectInfoKey.urlStr(), 
				url);
		p.hset(projectInfoKey.key(), 
				projectInfoKey.workUnitSizeStr(), 
				""+workUnitSizeInKB);
	}

	private void createWorkUnits(Pipeline p, String projectID, long chunkNum,
			double chunkSize) {

		AvailableJobsKey availableJobsKey = Keys.availableJobsKey(projectID);

		int workUnitSize = workUnitSizeInKB;

		int chunksPerWorkUnit;
		int remainderChunks;
		if (workUnitSize < chunkSize) {
			chunksPerWorkUnit = 1;
		} else {
			chunksPerWorkUnit = (int) Math.min(chunkNum, (long) (workUnitSize / chunkSize)); //check to see chunksPerWorkUnit are not bigger than chunNum
		}

		int workUnits = (int) (chunkNum / chunksPerWorkUnit);
		remainderChunks = (int)(chunkNum - (chunksPerWorkUnit*workUnits));
		
		if(remainderChunks > 0){
			workUnits++;
		}
		
		for (int i = 0; i < workUnits; i++) {
			String currJobID = "job"+(++jobID);
			int chunkStart = i * chunksPerWorkUnit;
			int chunkEnd = (int) Math.min(((i+1) * chunksPerWorkUnit) - 1, chunkNum);
			
			//Add to jobs info job info HASH
			JobsInfoKey jobInfoKey = Keys.jobsInfoKey(projectID, currJobID);
			p.hset(SafeEncoder.encode(jobInfoKey.key()), 
					SafeEncoder.encode(jobInfoKey.chunkStartStr()), 
					serializer.serialize(chunkStart));
			p.hset(SafeEncoder.encode(jobInfoKey.key()), 
					SafeEncoder.encode(jobInfoKey.chunkEndStr()), 
					serializer.serialize(chunkEnd));
			
			//Add to available jobs SORTED SET
			p.zadd(availableJobsKey.key(), 0, jobInfoKey.key());
			
		}

	}
	
	private String createExampleUrl(String projectID, String serviceUrl, int megabyteNumber){
		return serviceUrl+"/"+projectID+"/chunks/create?size="+megabyteNumber+"M";
	}
	
	private String chunksUrl(String projectID, String serviceUrl){
		return serviceUrl+"/"+projectID+"/chunks";
	}

	private JSONObject queryResearcherService(String projectID, String serviceUrl) {
		JSONObject json = null;
		try {
			URL url = new URL(chunksUrl(projectID,serviceUrl));
			URLConnection connection = url.openConnection();
			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			json = new JSONObject(builder.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}

	public Project getProject(String projectID) {
		final RedisConnectionFactory factory = template.getConnectionFactory();
		final RedisConnection conn = factory.getConnection();
		final Jedis jedis = (Jedis) conn.getNativeConnection();
		Project project;
		try {
			
			project = new Project();
			project.setId(projectID);
			ProjectInfoKey key = Keys.projectInfoKey(projectID);
			Map<String, String> map = jedis.hgetAll(key.key());
			if(!map.isEmpty()){
				project.setKbPerChunk(Double.parseDouble(map.get(key.kbPerChunkStr())));
				project.setChunks(Long.parseLong(map.get(key.chunksStr())));
				project.setPhase(ProjectPhase.fromString(map.get(key.phaseStr())));
				project.setUrl(map.get(key.urlStr()));
			}
		} finally {
			RedisConnectionUtils.releaseConnection(conn, factory);
		}
		
		return project;
	}
	
	public ConcurrentHashMap<String, RuntimeProjectDetails> projectRuntimeDetailsMap(){
		return projectRuntimeDetailsMap;
	}
	
	public void startProject(String projectID){
		projectRuntimeDetailsMap.put(projectID, new RuntimeProjectDetails());
		projectRuntimeDetailsMap.get(projectID).getRunningTime().start();
		changePhase(projectID, ProjectPhase.MAP);
	}

	public void changePhase(String projectID, ProjectPhase phase) {
		Project project = getProject(projectID);
		if(project.getPhase().next() == phase){
			
			if(phase == ProjectPhase.REDUCE){
				projectRuntimeDetailsMap.get(projectID).getRunningTime().stop();
			}
			
			final RedisConnectionFactory factory = template.getConnectionFactory();
			final RedisConnection conn = factory.getConnection();
			final Jedis jedis = (Jedis) conn.getNativeConnection();		
			try{			
				ProjectInfoKey projectInfoKey = Keys.projectInfoKey(projectID);
				jedis.hset(projectInfoKey.key(), 
						projectInfoKey.phaseStr(), 
						phase.phaseStr());		
			} finally {
				RedisConnectionUtils.releaseConnection(conn, factory);
			}					
		}
	}
}

