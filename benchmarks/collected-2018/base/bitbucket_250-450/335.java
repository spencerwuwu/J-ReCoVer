// https://searchcode.com/api/result/134595152/

package ca.uwaterloo.coordinator.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import ca.uwaterloo.coordinator.services.job.Job;
import ca.uwaterloo.coordinator.services.project.Project;
import ca.uwaterloo.coordinator.services.project.ProjectPhase;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ProjectServiceTest {

	JedisPool pool;
	Jedis jedis;
	
	JedisConnectionFactory factory;
	RedisTemplate<String, Object> template;
	
	ProjectService projectService;
	JobService jobService;
	
	@Before
	public void setUp() throws Exception{
		pool = new JedisPool(new JedisPoolConfig(), "localhost");
		jedis = pool.getResource();
		jedis.flushDB();
		pool.returnResource(jedis);
		
		factory = new JedisConnectionFactory();
		factory.setUsePool(true);
		factory.setPort(6379);
		factory.afterPropertiesSet();
		
		template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(factory);
		template.afterPropertiesSet();
		
		RedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<Object>(Object.class);
	
		projectService = new ProjectService();
		projectService.setSerializer(serializer);
		projectService.setResearcherUrl("http://localhost:8080/MRResearcher/volunteer");
		ReflectionTestUtils.setField(projectService, "template", template);
		
		jobService = new JobService();
		ReflectionTestUtils.setField(jobService, "template", template);
		ReflectionTestUtils.setField(jobService, "projectService", projectService);
	}
	
	@After
	public void tearDown(){
		pool.returnResource(jedis);
		pool.destroy();
	}
	
	@Test
	public void test_createProject_default_oneworkunit() throws IOException, JSONException {
		jedis.flushDB();
		
		final String projectID = "testproject";
		projectService.setWorkUnitSizeInKB(2*1024);
		projectService.createExampleProjectOnResearcher(projectID, 2); //in Megs
		projectService.createProject(projectID);
		
		Project project = projectService.getProject(projectID);
		List<Job> availJobs = jobService.getAvailableJobs(projectID);
		List<Job> completeJobs = jobService.getCompleteJobs(projectID);
		
		Job job =  jobService.peekAtNextJob(projectID);
		assertEquals(1, availJobs.size());
		assertEquals(0, completeJobs.size());
		assertEquals(ProjectPhase.PRE_INITIALIZED, project.getPhase());
		assertEquals(0, job.getChunkStart());
		assertEquals(project.getChunks()-1, job.getChunkEnd());
		
		projectService.startProject(projectID);
		
		Job prevJob = job;
		job = jobService.getNextJob(projectID);
		project = projectService.getProject(projectID);
		availJobs = jobService.getAvailableJobs(projectID);
		completeJobs = jobService.getCompleteJobs(projectID);
		
		assertEquals(1, availJobs.size());
		assertEquals(0, completeJobs.size());
		assertEquals(ProjectPhase.MAP, project.getPhase());
		assertEquals(0, job.getChunkStart());
		assertEquals(project.getChunks()-1, job.getChunkEnd());
		
		assertEquals(prevJob.getId(), job.getId());
		
		jobService.finishJob(projectID, job.getId(), null);
		job = jobService.peekAtNextJob(projectID);
		project = projectService.getProject(projectID);
		availJobs = jobService.getAvailableJobs(projectID);
		completeJobs = jobService.getCompleteJobs(projectID);
		
		assertEquals(0, availJobs.size());
		assertEquals(1, completeJobs.size());
		assertEquals(ProjectPhase.REDUCE, project.getPhase());
		assertNull(job);
	}

	@Test
	public void test_createProject_default_twoworkunits() throws IOException, JSONException {
		jedis.flushDB();
		
		final String projectID = "testproject";
		projectService.setWorkUnitSizeInKB(1*1024);
		projectService.createExampleProjectOnResearcher(projectID, 2); //in Megs
		projectService.createProject(projectID);
		
		Project project = projectService.getProject(projectID);
		List<Job> availJobs = jobService.getAvailableJobs(projectID);
		List<Job> completeJobs = jobService.getCompleteJobs(projectID);
		Job job = jobService.peekAtNextJob(projectID);
		
		assertEquals(2, availJobs.size());
		assertEquals(0, completeJobs.size());
		assertEquals(ProjectPhase.PRE_INITIALIZED, project.getPhase());
		assertEquals(availJobs.get(0).getId(), job.getId());
		assertEquals(0, availJobs.get(0).getChunkStart());
		assertEquals(project.getChunks()-1, availJobs.get(1).getChunkEnd());
		
		projectService.startProject(projectID);
		
		Job prevJob = job;
		job = jobService.getNextJob(projectID);
		jobService.finishJob(projectID, job.getId(), null);
		project = projectService.getProject(projectID);
		availJobs = jobService.getAvailableJobs(projectID);
		completeJobs = jobService.getCompleteJobs(projectID);
		
		assertEquals(1, availJobs.size());
		assertEquals(1, completeJobs.size());
		assertEquals(ProjectPhase.MAP, project.getPhase());
		
		job = jobService.getNextJob(projectID);
		jobService.finishJob(projectID, job.getId(), null);		
		project = projectService.getProject(projectID);
		availJobs = jobService.getAvailableJobs(projectID);
		completeJobs = jobService.getCompleteJobs(projectID);
		
		assertEquals(0, availJobs.size());
		assertEquals(2, completeJobs.size());
		assertEquals(ProjectPhase.REDUCE, project.getPhase());	
	}

	@Test
	public void test_createProject_default_twoworkunits_before_finish() throws IOException, JSONException {
		jedis.flushDB();
		
		final String projectID = "testproject";
		projectService.setWorkUnitSizeInKB(1*1024);
		projectService.createExampleProjectOnResearcher(projectID, 2); //in Megs
		projectService.createProject(projectID);
		
		Project project = projectService.getProject(projectID);
		List<Job> availJobs = jobService.getAvailableJobs(projectID);
		List<Job> completeJobs = jobService.getCompleteJobs(projectID);
		
		Job job;
		Job job1 = availJobs.get(0);
		Job job2 = availJobs.get(1);
		projectService.startProject(projectID);
		job = jobService.getNextJob(projectID);
		assertEquals(job1.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job2.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job1.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job2.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job1.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job2.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job1.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job2.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job1.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job2.getId(), job.getId());
		
		jobService.finishJob(projectID, job2.getId(), null);
		jobService.finishJob(projectID, job1.getId(), null);
		
		availJobs = jobService.getAvailableJobs(projectID);
		completeJobs = jobService.getCompleteJobs(projectID);
		assertEquals(job2.getId(), completeJobs.get(0).getId());
		assertEquals(job1.getId(), completeJobs.get(1).getId());
		
		jobService.finishJob(projectID, job2.getId(), null);
		jobService.finishJob(projectID, job2.getId(), null);
		jobService.finishJob(projectID, job2.getId(), null);

		jobService.finishJob(projectID, job1.getId(), null);
		jobService.finishJob(projectID, job1.getId(), null);
		jobService.finishJob(projectID, job1.getId(), null);

	}
	
	@Test
	public void test_createProject_default_bigworkunits_checkchunks() throws IOException, JSONException {
		jedis.flushDB();

		final String projectID = "testproject";
		projectService.setWorkUnitSizeInKB(1*1024);
		projectService.createExampleProjectOnResearcher(projectID, 10); //in Megs
		projectService.createProject(projectID);
		projectService.startProject(projectID);
		
		Project project = projectService.getProject(projectID);
		List<Job> availJobs = jobService.getAvailableJobs(projectID);
		List<Job> completeJobs = jobService.getCompleteJobs(projectID);
		Job job;
		
		Job job10 = availJobs.get(0);
		Job job11 = availJobs.get(1);		
		Job job12 = availJobs.get(2);
		Job job13 = availJobs.get(3);	
		Job job14 = availJobs.get(4);
		Job job15 = availJobs.get(5);	
		Job job16 = availJobs.get(6);
		Job job17 = availJobs.get(7);	
		Job job18 = availJobs.get(8);
		Job job19 = availJobs.get(9);			
		
		projectService.startProject(projectID);
		job = jobService.getNextJob(projectID);
		assertEquals(job10.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job11.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job12.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job13.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job14.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job15.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job16.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job17.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job18.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job19.getId(), job.getId());
		
		job = jobService.getNextJob(projectID);
		assertEquals(job10.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job11.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job12.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job13.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job14.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job15.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job16.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job17.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job18.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job19.getId(), job.getId());
		
		job = jobService.getNextJob(projectID);
		assertEquals(job10.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job11.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job12.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job13.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job14.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job15.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job16.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job17.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job18.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job19.getId(), job.getId());
		
		jobService.finishJob(projectID, job10.getId(), null);
		jobService.finishJob(projectID, job11.getId(), null);
		jobService.finishJob(projectID, job12.getId(), null);
		jobService.finishJob(projectID, job13.getId(), null);
		jobService.finishJob(projectID, job14.getId(), null);
		
		availJobs = jobService.getAvailableJobs(projectID);
		completeJobs = jobService.getCompleteJobs(projectID);
		
		assertEquals(5, availJobs.size());
		assertEquals(5, completeJobs.size());
	}

	@Test
	public void test_createProject_default_bigworkunits_checkchunks_oddnumber() throws IOException, JSONException {
		jedis.flushDB();

		final String projectID = "testproject";
		projectService.setWorkUnitSizeInKB(1*1025);
		projectService.createExampleProjectOnResearcher(projectID, 10); //in Megs
		projectService.createProject(projectID);
		projectService.startProject(projectID);
		
		Project project = projectService.getProject(projectID);
		List<Job> availJobs = jobService.getAvailableJobs(projectID);
		List<Job> completeJobs = jobService.getCompleteJobs(projectID);
		Job job;

		assertEquals(10, availJobs.size());
		assertEquals(0, completeJobs.size());
		
		Job job10 = availJobs.get(0);
		Job job11 = availJobs.get(1);		
		Job job12 = availJobs.get(2);
		Job job13 = availJobs.get(3);	
		Job job14 = availJobs.get(4);
		Job job15 = availJobs.get(5);	
		Job job16 = availJobs.get(6);
		Job job17 = availJobs.get(7);	
		Job job18 = availJobs.get(8);		
		Job job19 = availJobs.get(9);
		
		projectService.startProject(projectID);
		job = jobService.getNextJob(projectID);
		assertEquals(job10.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job11.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job12.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job13.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job14.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job15.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job16.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job17.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job18.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job19.getId(), job.getId());
		
		job = jobService.getNextJob(projectID);
		assertEquals(job10.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job11.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job12.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job13.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job14.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job15.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job16.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job17.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job18.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job19.getId(), job.getId());
		
		job = jobService.getNextJob(projectID);
		assertEquals(job10.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job11.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job12.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job13.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job14.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job15.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job16.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job17.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job18.getId(), job.getId());
		job = jobService.getNextJob(projectID);
		assertEquals(job19.getId(), job.getId());
		
		jobService.finishJob(projectID, job10.getId(), null);
		jobService.finishJob(projectID, job11.getId(), null);
		jobService.finishJob(projectID, job12.getId(), null);
		jobService.finishJob(projectID, job13.getId(), null);
		jobService.finishJob(projectID, job14.getId(), null);
		
		availJobs = jobService.getAvailableJobs(projectID);
		completeJobs = jobService.getCompleteJobs(projectID);
		
		assertEquals(5, availJobs.size());
		assertEquals(5, completeJobs.size());
	}
}

