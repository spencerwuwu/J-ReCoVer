// https://searchcode.com/api/result/119275756/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.lstachowiak.hadoop.sample1;

import java.util.Arrays;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author wookasz
 */
public class HttpMethodReducerTest {

    private HttpMethodReducer cut;

    @Before
    public void setUp() {
        cut = new HttpMethodReducer();
    }

    @Test
    public void returnsCountOfGetMethods() throws Exception {
        Text getMethod = new Text(HttpMethod.GET.name());
        Iterable<IntWritable> list = Arrays.asList(new IntWritable(1), 
                                                new IntWritable(1), 
                                                new IntWritable(1), 
                                                new IntWritable(1));
        
        Context context = mock(Context.class);
        
        cut.reduce(getMethod, list, context);

        verify(context).write(getMethod, new IntWritable(4));
    }
    
    
    @Test
    public void returnsCountOfHeadMethods() throws Exception {
        Text headMethod = new Text(HttpMethod.HEAD.name());
        Iterable<IntWritable> list = Arrays.asList();
        
        Context context = mock(Context.class);
        
        cut.reduce(headMethod, list, context);

        verify(context).write(headMethod, new IntWritable(0));
    }
}

