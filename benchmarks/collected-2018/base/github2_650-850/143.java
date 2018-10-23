// https://searchcode.com/api/result/3244829/

/*
 * Copyright (c) 2007-2010 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package cascading.flow;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cascading.operation.Operation;
import cascading.pipe.Group;
import cascading.pipe.Operator;
import cascading.pipe.Pipe;
import cascading.tap.Tap;
import cascading.tap.TempHfs;
import cascading.tap.hadoop.Hadoop18TapUtil;
import cascading.tap.hadoop.MultiInputFormat;
import cascading.tap.hadoop.TapIterator;
import cascading.tuple.Fields;
import cascading.tuple.IndexTuple;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntryIterator;
import cascading.tuple.TuplePair;
import cascading.tuple.hadoop.CoGroupingComparator;
import cascading.tuple.hadoop.CoGroupingPartitioner;
import cascading.tuple.hadoop.GroupingComparator;
import cascading.tuple.hadoop.GroupingPartitioner;
import cascading.tuple.hadoop.GroupingSortingComparator;
import cascading.tuple.hadoop.IndexTupleCoGroupingComparator;
import cascading.tuple.hadoop.ReverseGroupingSortingComparator;
import cascading.tuple.hadoop.ReverseTupleComparator;
import cascading.tuple.hadoop.TupleComparator;
import cascading.tuple.hadoop.TupleSerialization;
import cascading.util.Util;
import org.apache.hadoop.mapred.JobConf;
import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Class FlowStep is an internal representation of a given Job to be executed on a remote cluster. During
 * planning, pipe assemblies are broken down into "steps" and encapsulated in this class.
 * <p/>
 * FlowSteps are submitted in order of dependency. If two or more steps do not share the same dependencies and all
 * can be scheduled simultaneously, the {@link #getSubmitPriority()} value determines the order in which
 * all steps will be submitted for execution. The default submit priority is 5.
 * <p/>
 * This class is for internal use, there are no stable public methods.
 */
public class FlowStep implements Serializable
  {
  /** Field LOG */
  private static final Logger LOG = Logger.getLogger( FlowStep.class );

  /** Field properties */
  private Map<Object, Object> properties = null;
  /** Field parentFlowName */
  private String parentFlowName;

  /** Field submitPriority */
  private int submitPriority = 5;

  /** Field name */
  String name;
  /** Field id */
  private int id;
  /** Field graph */
  final SimpleDirectedGraph<FlowElement, Scope> graph = new SimpleDirectedGraph<FlowElement, Scope>( Scope.class );

  /** Field sources */
  final Map<Tap, String> sources = new HashMap<Tap, String>();   // all sources and all sinks must have same scheme
  /** Field sink */
  protected Tap sink;
  /** Field mapperTraps */
  private final Map<String, Tap> mapperTraps = new HashMap<String, Tap>();
  /** Field reducerTraps */
  private final Map<String, Tap> reducerTraps = new HashMap<String, Tap>();
  /** Field tempSink */
  TempHfs tempSink; // used if we need to bypass
  /** Field group */
  private Group group;

  protected FlowStep( String name, int id )
    {
    this.name = name;
    this.id = id;
    }

  /**
   * Method getId returns the id of this FlowStep object.
   *
   * @return the id (type int) of this FlowStep object.
   */
  public int getID()
    {
    return id;
    }

  /**
   * Method getName returns the name of this FlowStep object.
   *
   * @return the name (type String) of this FlowStep object.
   */
  public String getName()
    {
    return name;
    }

  public void setName( String name )
    {
    if( name == null || name.isEmpty() )
      throw new IllegalArgumentException( "step name may not be null or empty" );

    this.name = name;
    }

  /**
   * Method getParentFlowName returns the parentFlowName of this FlowStep object.
   *
   * @return the parentFlowName (type Flow) of this FlowStep object.
   */
  public String getParentFlowName()
    {
    return parentFlowName;
    }

  /**
   * Method setParentFlowName sets the parentFlowName of this FlowStep object.
   *
   * @param parentFlowName the parentFlowName of this FlowStep object.
   */
  public void setParentFlowName( String parentFlowName )
    {
    this.parentFlowName = parentFlowName;
    }

  /**
   * Method getStepName returns the stepName of this FlowStep object.
   *
   * @return the stepName (type String) of this FlowStep object.
   */
  public String getStepName()
    {
    return String.format( "%s[%s]", getParentFlowName(), getName() );
    }

  /**
   * Method getSubmitPriority returns the submitPriority of this FlowStep object.
   * <p/>
   * 10 is lowest, 1 is the highest, 5 is the default.
   *
   * @return the submitPriority (type int) of this FlowStep object.
   */
  public int getSubmitPriority()
    {
    return submitPriority;
    }

  /**
   * Method setSubmitPriority sets the submitPriority of this FlowStep object.
   * <p/>
   * 10 is lowest, 1 is the highest, 5 is the default.
   *
   * @param submitPriority the submitPriority of this FlowStep object.
   */
  public void setSubmitPriority( int submitPriority )
    {
    this.submitPriority = submitPriority;
    }

  public Group getGroup()
    {
    return group;
    }

  protected void setGroup( Group group )
    {
    this.group = group;
    }

  public Map<String, Tap> getMapperTraps()
    {
    return mapperTraps;
    }

  public Map<String, Tap> getReducerTraps()
    {
    return reducerTraps;
    }

  /**
   * Method getProperties returns the properties of this FlowStep object.
   *
   * @return the properties (type Map<Object, Object>) of this FlowStep object.
   */
  public Map<Object, Object> getProperties()
    {
    if( properties == null )
      properties = new Properties();

    return properties;
    }

  /**
   * Method setProperties sets the properties of this FlowStep object.
   *
   * @param properties the properties of this FlowStep object.
   */
  public void setProperties( Map<Object, Object> properties )
    {
    this.properties = properties;
    }

  /**
   * Method hasProperties returns {@code true} if there are properties associated with this FlowStep.
   *
   * @return boolean
   */
  public boolean hasProperties()
    {
    return properties != null && !properties.isEmpty();
    }

  protected JobConf getJobConf() throws IOException
    {
    return getJobConf( null );
    }

  protected JobConf getJobConf( JobConf parentConf ) throws IOException
    {
    JobConf conf = parentConf == null ? new JobConf() : new JobConf( parentConf );

    // set values first so they can't break things downstream
    if( hasProperties() )
      {
      for( Map.Entry entry : getProperties().entrySet() )
        conf.set( entry.getKey().toString(), entry.getValue().toString() );
      }

    // disable warning
    conf.setBoolean( "mapred.used.genericoptionsparser", true );

    conf.setJobName( getStepName() );

    conf.setOutputKeyClass( Tuple.class );
    conf.setOutputValueClass( Tuple.class );

    conf.setMapperClass( FlowMapper.class );
    conf.setReducerClass( FlowReducer.class );

    // set for use by the shuffling phase
    TupleSerialization.setSerializations( conf );

    initFromSources( conf );

    initFromSink( conf );

    initFromTraps( conf );

    if( sink.getScheme().getNumSinkParts() != 0 )
      {
      // if no reducer, set num map tasks to control parts
      if( getGroup() != null )
        conf.setNumReduceTasks( sink.getScheme().getNumSinkParts() );
      else
        conf.setNumMapTasks( sink.getScheme().getNumSinkParts() );
      }

    conf.setOutputKeyComparatorClass( TupleComparator.class );

    if( getGroup() == null )
      {
      conf.setNumReduceTasks( 0 ); // disable reducers
      }
    else
      {
      // must set map output defaults when performing a reduce
      conf.setMapOutputKeyClass( Tuple.class );
      conf.setMapOutputValueClass( Tuple.class );

      // handles the case the groupby sort should be reversed
      if( getGroup().isSortReversed() )
        conf.setOutputKeyComparatorClass( ReverseTupleComparator.class );

      addComparators( conf, "cascading.group.comparator", getGroup().getGroupingSelectors() );

      if( getGroup().isGroupBy() )
        addComparators( conf, "cascading.sort.comparator", getGroup().getSortingSelectors() );

      if( !getGroup().isGroupBy() )
        {
        conf.setPartitionerClass( CoGroupingPartitioner.class );
        conf.setMapOutputKeyClass( IndexTuple.class ); // allows groups to be sorted by index
        conf.setMapOutputValueClass( IndexTuple.class );
        conf.setOutputKeyComparatorClass( IndexTupleCoGroupingComparator.class ); // sorts by group, then by index
        conf.setOutputValueGroupingComparator( CoGroupingComparator.class );
        }

      if( getGroup().isSorted() )
        {
        conf.setPartitionerClass( GroupingPartitioner.class );
        conf.setMapOutputKeyClass( TuplePair.class );

        if( getGroup().isSortReversed() )
          conf.setOutputKeyComparatorClass( ReverseGroupingSortingComparator.class );
        else
          conf.setOutputKeyComparatorClass( GroupingSortingComparator.class );

        // no need to supply a reverse comparator, only equality is checked
        conf.setOutputValueGroupingComparator( GroupingComparator.class );
        }
      }

    // perform last so init above will pass to tasks
    conf.setInt( "cascading.flow.step.id", id );
    conf.set( "cascading.flow.step", Util.serializeBase64( this ) );

    return conf;
    }

  private void addComparators( JobConf conf, String property, Map<String, Fields> map ) throws IOException
    {
    Iterator<Fields> fieldsIterator = map.values().iterator();

    if( !fieldsIterator.hasNext() )
      return;

    Fields fields = fieldsIterator.next();

    if( fields.hasComparators() )
      {
      conf.set( property, Util.serializeBase64( fields ) );
      return;
      }

    // use resolved fields if there are no comparators.
    Set<Scope> previousScopes = getPreviousScopes( getGroup() );

    fields = previousScopes.iterator().next().getOutValuesFields();

    if( fields.size() != 0 ) // allows fields.UNKNOWN to be used
      conf.setInt( property + ".size", fields.size() );

    return;
    }

  private void initFromTraps( JobConf conf ) throws IOException
    {
    initFromTraps( conf, getMapperTraps() );
    initFromTraps( conf, getReducerTraps() );
    }

  private void initFromTraps( JobConf conf, Map<String, Tap> traps ) throws IOException
    {
    if( !traps.isEmpty() )
      {
      JobConf trapConf = new JobConf( conf );

      for( Tap tap : traps.values() )
        tap.sinkInit( trapConf );
      }
    }

  private void initFromSources( JobConf conf ) throws IOException
    {
    JobConf[] fromJobs = new JobConf[ sources.size() ];
    int i = 0;

    for( Tap tap : sources.keySet() )
      {
      fromJobs[ i ] = new JobConf( conf );
      tap.sourceInit( fromJobs[ i ] );
      fromJobs[ i ].set( "cascading.step.source", Util.serializeBase64( tap ) );
      i++;
      }

    MultiInputFormat.addInputFormat( conf, fromJobs );
    }

  private void initFromSink( JobConf conf ) throws IOException
    {
    // init sink first so tempSink can take precedence
    if( sink != null )
      sink.sinkInit( conf );

    // tempSink exists because sink is writeDirect
    if( tempSink != null )
      tempSink.sinkInit( conf );
    }

  public TapIterator openSourceForRead( JobConf conf ) throws IOException
    {
    return new TapIterator( sources.keySet().iterator().next(), conf );
    }

  public TupleEntryIterator openSinkForRead( JobConf conf ) throws IOException
    {
    return sink.openForRead( conf );
    }

  public Tap getMapperTrap( String name )
    {
    return getMapperTraps().get( name );
    }

  public Tap getReducerTrap( String name )
    {
    return getReducerTraps().get( name );
    }

  /**
   * Method getPreviousScopes returns the previous Scope instances. If the flowElement is a Group (specifically a CoGroup),
   * there will be more than one instance.
   *
   * @param flowElement of type FlowElement
   * @return Set<Scope>
   */
  public Set<Scope> getPreviousScopes( FlowElement flowElement )
    {
    assertFlowElement( flowElement );

    return graph.incomingEdgesOf( flowElement );
    }

  /**
   * Method getNextScope returns the next Scope instance in the graph. There will always only be one next.
   *
   * @param flowElement of type FlowElement
   * @return Scope
   */
  public Scope getNextScope( FlowElement flowElement )
    {
    assertFlowElement( flowElement );

    Set<Scope> set = graph.outgoingEdgesOf( flowElement );

    if( set.size() != 1 )
      throw new IllegalStateException( "should only be one scope after current flow element: " + flowElement + " found: " + set.size() );

    return set.iterator().next();
    }

  public Set<Scope> getNextScopes( FlowElement flowElement )
    {
    assertFlowElement( flowElement );

    return graph.outgoingEdgesOf( flowElement );
    }

  private void assertFlowElement( FlowElement flowElement )
    {
    if( !graph.containsVertex( flowElement ) )
      {
      String message = "unable to find %s in plan, class and serializable fields must implement #hashCode() and #equals()";

      if( flowElement instanceof Pipe )
        message = Util.formatTrace( (Pipe) flowElement, String.format( message, "pipe" ) );
      else if( flowElement instanceof Tap )
        message = Util.formatTrace( (Tap) flowElement, String.format( message, "tap" ) );

      throw new IllegalStateException( message );
      }
    }

  public FlowElement getNextFlowElement( Scope scope )
    {
    return graph.getEdgeTarget( scope );
    }

  public String getSourceName( Tap source )
    {
    return sources.get( source );
    }

  public Collection<Operation> getAllOperations()
    {
    Set<FlowElement> vertices = graph.vertexSet();
    List<Operation> operations = new ArrayList<Operation>(); // operations impl equals, so two instance may be the same

    for( FlowElement vertice : vertices )
      {
      if( vertice instanceof Operator )
        operations.add( ( (Operator) vertice ).getOperation() );
      }

    return operations;
    }

  public boolean containsPipeNamed( String pipeName )
    {
    Set<FlowElement> vertices = graph.vertexSet();

    for( FlowElement vertice : vertices )
      {
      if( vertice instanceof Pipe && ( (Pipe) vertice ).getName().equals( pipeName ) )
        return true;
      }

    return false;
    }

  /**
   * Method clean removes any temporary files used by this FlowStep instance. It will log any IOExceptions thrown.
   *
   * @param jobConf of type JobConf
   */
  public void clean( JobConf jobConf )
    {
    if( tempSink != null )
      {
      try
        {
        tempSink.deletePath( jobConf );
        }
      catch( Exception exception )
        {
        // sink all exceptions, don't fail app
        logWarn( "unable to remove temporary file: " + tempSink, exception );
        }
      }

    if( sink instanceof TempHfs )
      {
      try
        {
        sink.deletePath( jobConf );
        }
      catch( Exception exception )
        {
        // sink all exceptions, don't fail app
        logWarn( "unable to remove temporary file: " + sink, exception );
        }
      }
    else
      {
      cleanTap( jobConf, sink );
      }

    for( Tap tap : getMapperTraps().values() )
      cleanTap( jobConf, tap );

    for( Tap tap : getReducerTraps().values() )
      cleanTap( jobConf, tap );

    }

  private void cleanTap( JobConf jobConf, Tap tap )
    {
    try
      {
      Hadoop18TapUtil.cleanupTap( jobConf, tap );
      }
    catch( IOException exception )
      {
      // ignore exception
      }
    }

  @Override
  public boolean equals( Object object )
    {
    if( this == object )
      return true;
    if( object == null || getClass() != object.getClass() )
      return false;

    FlowStep flowStep = (FlowStep) object;

    if( name != null ? !name.equals( flowStep.name ) : flowStep.name != null )
      return false;

    return true;
    }

  @Override
  public int hashCode()
    {
    return name != null ? name.hashCode() : 0;
    }

  @Override
  public String toString()
    {
    StringBuffer buffer = new StringBuffer();

    buffer.append( getClass().getSimpleName() );
    buffer.append( "[name: " ).append( getName() ).append( "]" );

    return buffer.toString();
    }

  protected FlowStepJob createFlowStepJob( JobConf parentConf ) throws IOException
    {
    return new FlowStepJob( this, getName(), getJobConf( parentConf ) );
    }

  protected final boolean isInfoEnabled()
    {
    return LOG.isInfoEnabled();
    }

  protected final boolean isDebugEnabled()
    {
    return LOG.isDebugEnabled();
    }

  protected void logDebug( String message )
    {
    LOG.debug( "[" + Util.truncate( getParentFlowName(), 25 ) + "] " + message );
    }

  protected void logInfo( String message )
    {
    LOG.info( "[" + Util.truncate( getParentFlowName(), 25 ) + "] " + message );
    }

  protected void logWarn( String message )
    {
    LOG.warn( "[" + Util.truncate( getParentFlowName(), 25 ) + "] " + message );
    }

  protected void logWarn( String message, Throwable throwable )
    {
    LOG.warn( "[" + Util.truncate( getParentFlowName(), 25 ) + "] " + message, throwable );
    }

  protected void logError( String message, Throwable throwable )
    {
    LOG.error( "[" + Util.truncate( getParentFlowName(), 25 ) + "] " + message, throwable );
    }
  }

