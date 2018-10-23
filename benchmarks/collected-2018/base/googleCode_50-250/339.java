// https://searchcode.com/api/result/4332680/

package it.unina.cloudclusteringnaive;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.taskqueue.TaskOptions.Builder.*;


public class StartJobViaCodeServlet extends HttpServlet{
	
	
	 /**
     * Starts a map/reduce job that updates {@link Subscriber} entities with 
     * dates from a given status snapshot.
     */
    public static void startJobWithName(String name) {            
            
            TaskOptions task = buildStartJob(name);
//            addJobParam(task, StatusBlobInputFormat.SAMPLE_ID_PARAMETER, statusBlob.getSampleId());
//            addJobParam(task, StatusBlobInputFormat.DATE_PARAMETER, statusBlob.getDate().getMillis());
//            
            Queue queue = QueueFactory.getDefaultQueue();
            queue.add(task);
    }


    private static TaskOptions buildStartJob(String jobName) {
            return TaskOptions.Builder.withUrl("/mapreduce/command/start_job").method(Method.POST)
            .header("X-Requested-With", "XMLHttpRequest") // hack: we need to fix appengine-mapper so we can properly call start_job without need to pretend to be an ajaxmethod
            .param("name", jobName);
    }
    
//    private static void addJobParam(TaskOptions task, String paramName, String paramValue ) {
//            task.param("mapper_params." + paramName, paramValue);
//    }
//    
//    private static void addJobParam(TaskOptions task, String paramName, long value) {
//            addJobParam(task, paramName, Long.toString(value));
//    }
//    
    
	
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req,resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		startJobWithName("Generate Leave1Out");
		resp.sendRedirect("mapreduce/status");
		
	}
	
	
	

}

