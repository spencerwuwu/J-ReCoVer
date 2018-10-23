// https://searchcode.com/api/result/818312/

/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.tdb.solver;


import java.util.Iterator;
import java.util.List;

import org.openjena.atlas.iterator.Filter ;
import org.openjena.atlas.iterator.Iter ;
import org.openjena.atlas.iterator.NullIterator ;
import org.openjena.atlas.iterator.RepeatApplyIterator ;
import org.openjena.atlas.iterator.Transform ;
import org.openjena.atlas.lib.Action ;
import org.openjena.atlas.lib.Tuple ;



import com.hp.hpl.jena.graph.Node;

import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;

import com.hp.hpl.jena.tdb.nodetable.NodeTable;
import com.hp.hpl.jena.tdb.nodetable.NodeTupleTable;
import com.hp.hpl.jena.tdb.store.NodeId;

public class StageMatchTuple extends RepeatApplyIterator<BindingNodeId>
{
    private final NodeTupleTable nodeTupleTable ;
    private final Tuple<Node> patternTuple ;

    private final ExecutionContext execCxt ;
    private boolean anyGraphs ;
    private Filter<Tuple<NodeId>> filter ;

    public StageMatchTuple(NodeTupleTable nodeTupleTable, Iterator<BindingNodeId> input, 
                            Tuple<Node> tuple, boolean anyGraphs, 
                            Filter<Tuple<NodeId>> filter, 
                            ExecutionContext execCxt)
    {
        super(input) ;
        this.filter = filter ;
        this.nodeTupleTable = nodeTupleTable ; 
        this.patternTuple = tuple ;
        this.execCxt = execCxt ;
        this.anyGraphs = anyGraphs ; 
    }

    /** Prepare a pattern (tuple of nodes), and an existing binding of NodeId, into NodesIds and Variables. 
     *  A variable in the pattern is replaced by its binding or null in the Nodeids.
     *  A variable that is not bound by the binding is placed in the var array.
     */
    public static void prepare(NodeTable nodeTable, Tuple<Node> patternTuple, BindingNodeId input, NodeId ids[], Var[] var)
    {
        // Process the Node to NodeId conversion ourselves because
        // we wish to abort if an unknown node is seen.
        for ( int i = 0 ; i < patternTuple.size() ; i++ )
        {
            Node n = patternTuple.get(i) ;
            // Substitution and turning into NodeIds
            // Variables unsubstituted are null NodeIds
            NodeId nId = idFor(nodeTable, input, n) ;
            if ( NodeId.doesNotExist(nId) )
                new NullIterator<BindingNodeId>() ;
            ids[i] = nId ;
            if ( nId == null )
                var[i] = asVar(n) ;
        }
    }
    
    @Override
    protected Iterator<BindingNodeId> makeNextStage(final BindingNodeId input)
    {
        // ---- Convert to NodeIds 
        NodeId ids[] = new NodeId[patternTuple.size()] ;
        // Variables for this tuple after subsitution
        final Var[] var = new Var[patternTuple.size()] ;

        prepare(nodeTupleTable.getNodeTable(), patternTuple, input, ids, var) ;
        
        // Go directly to the tuple table
        Iterator<Tuple<NodeId>> iterMatches = nodeTupleTable.getTupleTable().find(Tuple.create(ids)) ;
        
        // ** Allow a triple or quad filter here.
        if ( filter != null )
            iterMatches = Iter.filter(iterMatches, filter) ;
        
        // If we want to reduce to RDF semantics over quads,
        // we need to reduce the quads to unique triples. 
        // We do that by having the graph slot as "any", then running
        // through a distinct-ifier. 
        // Assumes quads are GSPO - zaps the first slot.
        // Assumes that tuples are not shared.
        if ( anyGraphs )
        {
            iterMatches = Iter.operate(iterMatches, quadsToTriples) ;
            iterMatches = Iter.distinct(iterMatches) ;  // WRT only three varying slots.
        }
        
        // Map Tuple<NodeId> to BindingNodeId
        Transform<Tuple<NodeId>, BindingNodeId> binder = new Transform<Tuple<NodeId>, BindingNodeId>()
        {
            //@Override
            public BindingNodeId convert(Tuple<NodeId> tuple)
            {
                BindingNodeId output = new BindingNodeId(input) ;
                for ( int i = 0 ; i < var.length ; i++ )
                {
                    Var v = var[i] ;
                    if ( v == null )
                        continue ;
                    NodeId id = tuple.get(i) ;
                    if ( reject(output, v,id) )
                        return null ;
                    output.put(v, id) ;
                }
                return output ;
            }
        } ;
        
        return Iter.iter(iterMatches).map(binder).removeNulls() ;
    }
    
    // -- Mutating "transform in place"
    private static Action<Tuple<NodeId>> quadsToTriples = new Action<Tuple<NodeId>>(){
        //@Override
        public void apply(Tuple<NodeId> item)
        { item.tuple()[0] = NodeId.NodeIdAny ; }
    } ;
    
    // -- Copying
    private static Transform<Tuple<NodeId>,Tuple<NodeId>> projectToTriples = new Transform<Tuple<NodeId>,Tuple<NodeId>>(){
        //@Override
        public Tuple<NodeId> convert(Tuple<NodeId> item)
        {
            // Zap graph node id.
            Tuple<NodeId> t2 = Tuple.create(NodeId.NodeIdAny,    // Can't be null - gets bound to a daft variable.
                                            item.get(1),
                                            item.get(2),
                                            item.get(3)) ;
            return t2 ;
        } } ;
    
    
    private static Iterator<Tuple<NodeId>> print(Iterator<Tuple<NodeId>> iter)
    {
        if ( ! iter.hasNext() )
            System.err.println("<empty>") ;
        else
        {
            List<Tuple<NodeId>> r = Iter.toList(iter) ;
            String str = Iter.asString(r, "\n") ;
            System.err.println(str) ;
            // Reset iter
            iter = Iter.iter(r) ;
        }
        return iter ;
    }
    
    private static boolean reject(BindingNodeId output , Var var, NodeId value)
    {
        if ( ! output.containsKey(var) )
            return false ;
        
        if ( output.get(var).equals(value) )
            return false ;

        return true ;
    }
    
    private static Var asVar(Node node)
    {
        if ( Var.isVar(node) )
            return Var.alloc(node) ;
        return null ;
    }

    /** Return null for variables, and for nodes, the node id or NodeDoesNotExist */
    private static NodeId idFor(NodeTable nodeTable, BindingNodeId input, Node node)
    {
        if ( Var.isVar(node) )
        {
            NodeId n = input.get((Var.alloc(node))) ;
            // Bound to NodeId or null. 
            return n ;
        } 
        // May return NodeId.NodeDoesNotExist which must not be null. 
        return nodeTable.getNodeIdForNode(node) ;
    }
}
/*
 * (c) Copyright 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
