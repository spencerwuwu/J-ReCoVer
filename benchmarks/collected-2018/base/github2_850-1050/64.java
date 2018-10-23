// https://searchcode.com/api/result/111498873/

/*******************************************************************************
 *Copyright (c) 2008 The Bioclipse Team and others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/*----------------------------------------------------------------------
  File    : Extension.java
  Contents: Graph fragment extension management
  Author  : Christian Borgelt
  History : 2002.03.11 file created as file submol.java
            2002.04.02 function compareTo added
            2003.02.19 extension by variable length chains added
            2003.08.01 first version of ring extensions added
            2003.08.05 ring flag processing optimized (field "all")
            2003.08.06 file split, this part renamed to Extension.java
            2003.08.07 complete rewrite of extension functions
            2005.06.09 chain edges required to be bridges
            2005.07.22 handling of incomplete rings and trimming added
            2005.07.24 orientation check added to ring extension
            2005.08.10 wrapper functions makeFragment added
            2005.08.11 made abstract, subclasses MaxSrcExt and RightExt
            2006.04.11 function compareEdge added
            2006.05.02 parameters of function isCanonic changed
            2006.05.03 functions makeCanonic and makeWord added
            2006.05.08 function makeCanonic generalized and debugged
            2006.05.10 function describe added (code word printing)
            2006.05.11 second function isCanonic added
            2006.05.12 function compareRing added (part of compareTo)
            2006.05.18 field Extension.dst added, chain simplified
            2006.05.31 function isCanonic extended, result changed
            2006.06.06 adapted to changed type of edge flags
            2006.06.07 function compareWord added
            2006.07.01 adaptation of ring extensions moved here
            2006.07.06 generation of equivalent ring variants added
            2006.08.08 bug in definition of Extension.curr fixed
            2006.08.10 bug in function chain fixed (trimmed edges)
            2006.10.29 function setChainTypes added, chain adapted
            2007.03.24 functions prepare and removeRings added
            2007.03.26 flag sym added (indicating local ring symmetry)
            2007.06.21 adapted to new class TypeMgr
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for creating extensions of fragments and embeddings.
 *  <p>An extension object records all relevant information about
 *  an extension of a fragment and also information about the next
 *  extension to be considered. Extensions are later turned into
 *  fragments and/or embeddings (depending on whether an equivalent
 *  fragment exists or not).</p>
 *  <p>The same extension object is reused to create extensions
 *  instead of creating a new extension object for each embedding.
 *  As a consequence the fragment and embedding to extend are not
 *  passed directly to a constructor, but to an initialization
 *  function.</p>
 *  <p>The field <code>size</code>is used to indicate the type of the
 *  current extension. A negative size indicates a chain extension,
 *  with the absolute value of the size being the chain length.
 *  A zero size indicates a single edge extension (the standard case).
 *  Finally, a positive size indicates a ring extension, with the size
 *  being the number of nodes/edges in the ring.</p>
 *  @author Christian Borgelt
 *  @since  2002.03.11 */
/*--------------------------------------------------------------------*/
public abstract class Extension {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** extension mode flag: single edge */
  public    static final int EDGE   = 1;
  /** extension mode flag: ring (must be marked) */
  public    static final int RING   = 2;
  /** extension mode flag: variable length chain */
  public    static final int CHAIN  = 4;
  /** extension mode flag: equivalent ring variants */
  public    static final int EQVARS = 8;
  /** the flag for a fixed edge in the ring order test */
  protected static final int FIXED  = Integer.MIN_VALUE;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the extension mode */
  protected int       mode;
  /** the maximum fragment size */
  protected int       max;
  /** the minimum ring size */
  protected int       rgmin;
  /** the maximum ring size */
  protected int       rgmax;
  /** the node type for chain extensions */
  protected int       cnode;
  /** the edge type for chain extensions */
  protected int       cedge;
  /** the fragment  that is extended */
  protected Fragment  frag;
  /** the embedding that is extended */
  protected Embedding emb;
  /** (relevant) nodes of the extension */
  protected Node[]    nodes;
  /** (relevant) edges of the extension */
  protected Edge[]    edges;
  /** the number of nodes in a ring/chain */
  protected int       size;
  /** the number of new nodes in ring */
  protected int       nodecnt;
  /** the number of new edges in ring */
  protected int       edgecnt;
  /** the number of variable length chains */
  protected int       chcnt;
  /** the index of the current anchor node */
  protected int       src;
  /** the current edge index in the anchor node */
  protected int       idx;
  /** the index of the current destination node */
  protected int       dst;
  /** all ring flags of the current edge */
  protected long      all;
  /** the current ring flag */
  protected long      curr;
  /** whether the current ring is locally symmetric */
  protected boolean   sym;
  /** the minimal position/current position index of a ring edge */
  protected int       pmin;
  /** the maximal position/position index of a ring edge */
  protected int       pmax;
  /** the current position 1 of equivalent edges for ring extensions */
  protected int       pos1;
  /** the current position 2 of equivalent edges for ring extensions */
  protected int       pos2;
  /** the code word for isCanonic/makeCanonic */
  protected int[]     word;

  /*------------------------------------------------------------------*/
  /** Initialize the extension variables.
   *  <p>Since <code>Extension</code> is an abstract class, this
   *  constructor cannot be called directly to create an instance.
   *  Rather it is meant as a common initialization routine for
   *  subclasses of this class.</p>
   *  @param  mode the extension mode
   *               (e.g. <code>EDGE</code> or <code>EDGE|RING</code>)
   *  @param  max  the maximum fragment size (number of nodes)
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Extension (int mode, int max)
  {                             /* -- create an extension structure */
    if ((mode & RING) == 0)     /* equivalent variants are needed */
      mode &= ~EQVARS;          /* only for ring extensions */
    this.mode  = mode;          /* note the extension mode and */
    this.max   = max;           /* the maximum fragment size */
    this.rgmin = this.rgmax =  0;
    this.cnode = this.cedge = -1;
    this.frag  = null;          /* init. the extension variables */
    this.nodes = new Node[256]; /* init. the node/edge arrays with */
    this.edges = new Edge[256]; /* a large number of edges/nodes */
    this.word  = new int[1024]; /* and the code word accordingly */
    this.pmin  = this.pos1 = -1;/* init. the variables for */
    this.pmax  = this.pos2 = -1;/* equivalent variants of rings */
  }  /* Extension() */

  /*------------------------------------------------------------------*/
  /** Set the ring sizes for ring extensions.
   *  <p>These ring sizes are actually not needed for creating ring
   *  extensions, but only for adapting them, which is needed only
   *  if canonical form pruning is used.</p>
   *  @param  rgmin the minimal ring size (number of nodes/edges)
   *  @param  rgmax the maximal ring size (number of nodes/edges)
   *  @since  2006.07.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setRingSizes (int rgmin, int rgmax)
  { this.rgmin = rgmin; this.rgmax = rgmax; }

  /*------------------------------------------------------------------*/
  /** Set the node and edge type for chain extensions.
   *  @param  node the type of the chain nodes
   *  @param  edge the type of the chain edges
   *  @since  2006.10.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setChainTypes (int node, int edge)
  { this.cnode = node; this.cedge = edge; }

  /*------------------------------------------------------------------*/
  /** Initialize the extension generation process.
   *  <p>Instead of creating a new extension object each time an
   *  embedding has to be extended, the same extension object is
   *  reused, thus greatly reducing the overhead for memory allocation.
   *  As a consequence, the extension object has to be reinitialized
   *  for each embedding that is to be extended.</p>
   *  @param  frag the fragment  to extend
   *  @param  emb  the embedding to extend
   *               (must be contained in the fragment)
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract void init (Fragment frag, Embedding emb);

  /*------------------------------------------------------------------*/
  /** Create the next extension.
   *  <p>Each time this function is called and returns
   *  <code>true</code>, a new extension has been created, which
   *  may then be compared to already existing fragments (function
   *  <code>compareTo()</code>) or turned into a new fragment
   *  (function <code>makeFragment()</code>) or a new embedding
   *  (function <code>makeEmbedding()</code>). When all extensions of
   *  the embedding passed to <code>init()</code> have been created,
   *  the function returns <code>false</code>.</p>
   *  @return whether another extension was created
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract boolean next ();

  /*------------------------------------------------------------------*/
  /** Create a ring extension.
   *  <p>Follow a ring flag through the edges of the graph the
   *  embedding to extend refers to and collect the new edges for
   *  the extension. All created rings are checked with the function
   *  <code>validRing()</code>, restricting certain rings to a specific
   *  form (thus avoiding some unnecessary canonical form tests).
   *  If no (further) ring can be created, the function returns
   *  <code>false</code>, otherwise <code>true</code>.</p>
   *  @return whether another ring extension was created
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean ring ()
  {                             /* --- create a ring extension */
    int  i;                     /* loop variable */
    Node s, d;                  /* to traverse the nodes of the ring */
    Edge r, e = null;           /* to traverse the edges of the ring */

    this.chcnt = this.frag.chcnt;     /* copy the chain counter */
    s = this.nodes[0];          /* get the anchor node (source) */
    while (this.all != 0) {     /* while there is another ring flag */
      while ((this.all & this.curr) == 0)
        this.curr <<= 1;        /* find the next ring flag and */
      this.all &= ~this.curr;   /* remove it (it is processed now) */
      this.size    = 1;         /* initialize the ring size */
      this.edgecnt = 1;         /* and the counters for */
      this.nodecnt = 0;         /* the new edges and nodes */
      r = this.edges[0];        /* get the first  ring edge */
      d = this.nodes[1];        /* and the second ring node */
      do {                      /* traverse the ring */
        for (i = d.deg; --i >= 0; ) {
          e = d.edges[i];       /* traverse the edges of the node */
          if ((e != r) && ((e.flags & this.curr) != 0))
            break;              /* find the next edge */
        }                       /* of the ring to be added */
        if ((i < 0) || (e.mark < -1))
          break;                /* if the ring is incomplete, abort */
        /* If r.mark < -1, the edge has been removed from the */
        /* graph by trimming and thus cannot be followed.  */
        this.nodes[this.size  ] = d;     /* collect the nodes */
        if (d.mark < 0) this.nodecnt++;  /* and count new ones */
        this.edges[this.size++] = e;     /* collect the edges */
        if (e.mark < 0) this.edgecnt++;  /* and count new ones */
        r = e;                  /* go to the next edge and node */
        d = (e.src != d) ? e.src : e.dst;
      } while (d != s);         /* while the ring is not closed */
      if (d != s) continue;     /* check whether the ring was closed */
      if (this.emb.nodes.length +this.chcnt +this.nodecnt > this.max)
        continue;               /* check the size of the fragment */
      if (!this.validRing())    /* check the structure of the ring */
        continue;               /* and abort if it is not valid */
      if ((this.mode & EQVARS) != 0)
        this.initVars();        /* init. equivalent variants */
      return true;              /* of the current ring and */
    }                           /* return "ring extension succeeded" */
    return false;               /* return "ring extension failed" */
  }  /* ring() */

  /*------------------------------------------------------------------*/
  /** Check whether the current ring extension is valid.
   *  <p>In order to reduce the number of generated fragments, rings
   *  are usually generated in only one form. It is checked whether
   *  the source of the first new ring edge is minimal over all edges
   *  of the ring (so that a ring is always attached to the node with
   *  minimal index) and whether the first and last edges of the ring
   *  allow to fix an orientation of the ring, only one of which is
   *  considered valid. Invalid rings are discarded in the function
   *  <code>ring()</code> that creates ring extensions.</p>
   *  @return whether the ring is valid (has the correct form)
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract boolean validRing ();

  /*------------------------------------------------------------------*/
  /** Initialize the generation of equivalent ring extension variants.
   *  <p>If a ring start (and possibly also ends) with an edge that is
   *  equivalent to one or more edges already in the fragment (that is,
   *  edges that start at the same node, have the same type, and lead
   *  to nodes of the same type), these edges must be spliced with the
   *  already existing equivalent edges in the fragment. All possible
   *  ways of splicing the equivalent edges have to be tried. This
   *  function initializes this variant generation.</p>
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract void initVars ();

  /*------------------------------------------------------------------*/
  /** Create the next ring extension variant.
   *  @return whether another ring variant was created
   *  @see    #initVars()
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract boolean variant ();

  /*------------------------------------------------------------------*/
  /** Reorder the edges of a fragment with a ring extension.
   *  <p>After a ring extension it may be necessary to reorder the edges
   *  of the resulting fragment, so that the edges get into the proper
   *  order w.r.t. the canonical form. In addition, it must be checked
   *  whether rings were added in the right order (if several rings
   *  were added). If not, the ring extension cannot be adapted and
   *  thus the function returns -1.</p>
   *  <p>This function does not actually reorganize the fragment if the
   *  ring extension can be adapted, but only stores the edges and nodes
   *  in their new order in internal arrays. In addition, it creates a
   *  map for reorganizing the nodes and edges, also in an internal
   *  buffer. Either of these may later be used to actually reorganize
   *  the fragment (as a sub-graph) as well as the embeddings. Note
   *  that these arrays and maps are not filled/created if the fragment
   *  need not be changed in any way. In this case the function returns
   *  +1, otherwise the result is 0.</p>
   *  @param  frag  the fragment to adapt
   *  @param  check whether to check the ring order
   *  @return whether the ring adaptation succeeded, that is:
   *          <p><table cellpadding=0 cellspacing=0>
   *          <tr><td>-1,&nbsp;</td>
   *              <td>if the ring adaptation failed,</td></tr>
   *          <tr><td align="right">0,&nbsp;</td>
   *              <td>if the ring adaptation succeeded,
   *              but the fragment needs to be modified,</td></tr>
   *          <tr><td>+1,&nbsp;</td>
   *              <td>if the ring extension
   *                  need not be adapted.</td></tr>
   *          </table></p>
   *  @since  2006.07.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract int adaptRing (Fragment frag, boolean check);

  /*------------------------------------------------------------------*/
  /** Compare two edges with the precedence order of the canonical form.
   *  <p>A canonical form usually allows to compare two edges in the
   *  necessary way by fixing a specific precedence order of the
   *  defining properties of the edges.</p>
   *  <p>This function is meant to compare edges from the same graph
   *  at each point where the next edge needs to be selected, when the
   *  graph (or rather its edge array) is rebuilt. At such a point
   *  all nodes incident to already processed edges are numbered.
   *  However, one of the nodes incident to the compared edges may
   *  not have been numbered yet. As this would make it impossible to
   *  compare the edges, the next number to be given to a node is
   *  also passed to the function.</p>
   *  @param  edge1 the first  edge to compare
   *  @param  edge2 the second edge to compare
   *  @param  next  the index with which to number the next node
   *  @return whether the first edge is smaller (-1) or greater than
   *          (+1) or equal to (0) the second edge
   *  @since  2006.04.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract int compareEdge (Edge edge1, Edge edge2, int next);

  /*------------------------------------------------------------------*/
  /** Prepare the rings of a fragment for a ring order test.
   *  @param  frag the fragment to prepare
   *  @return the fragment as a graph
   *  @since  2007.03.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Graph prepare (Fragment frag)
  {                             /* --- prepare for ring order test */
    int   i;                    /* loop variable */
    Graph graph;                /* the fragment as a graph */
    Edge  e;                    /* to traverse the edges */

    graph = frag.getAsGraph();  /* mark rings in the fragment */
    graph.markRings(0, this.rgmax, 0);
    for (i = graph.edgecnt; --i >= 0; ) {
      e = graph.edges[i];       /* traverse the fragment's edges */
      if (!e.isInRing() && (e.getRings() != 0))
        Extension.removeRings(e);
    }                           /* remove superfluous ring flags */
    graph.prepare();            /* prepare graph for processing */
    graph.mark(-1);             /* unmark all nodes and edges */
    this.initCanonic(graph, 0); /* init. the canonical form */
    return graph;               /* return the graph that */
  }  /* prepare() */            /* represents the fragment */

  /*------------------------------------------------------------------*/
  /** Remove the flags of all rings an edge is contained in.
   *  @param  edge the edge to process
   *  @since  2007.03.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected static void removeRings (Edge edge)
  {                             /* --- remove ring flags */
    int  i;                     /* loop variable */
    long rgs, cur;              /* ring flags */
    Node s, a;                  /* to traverse the ring nodes */
    Edge r, x = null;           /* to traverse the ring edges */

    rgs = edge.getRings();      /* traverse the ring flags */
    for (cur = 1; rgs != 0; cur <<= 1) {
      if ((rgs & cur) == 0) continue;
      rgs &= ~cur;              /* remove the processed ring flag */
      r = edge; s = r.src; a = r.dst;
      do {                      /* loop to collect the ring edges */
        for (i = a.deg; --i >= 0; ) {
          x = a.edges[i];       /* traverse the incident edges */
          if ((x != r) && ((x.flags & cur) != 0)) break;
        }                       /* find the next ring edge and */
        r = x; r.flags &= ~cur; /* remove the current ring flag */
        a = (r.src != a) ? r.src : r.dst;
      } while (a != s);         /* get the next ring node */
    }                           /* until the ring is closed */
  }  /* removeRings() */

  /*------------------------------------------------------------------*/
  /** Internal recursive function to check whether an edge is removable.
   *  <p>If the given edge (which must be a ring edge) is removable,
   *  then the rings it is contained in can be added later than the
   *  rings of the last edges that have been added. As a consequence
   *  the rings have not been added in the correct order and the last
   *  ring extension is invalid.</p>
   *  @param  edge the edge to check
   *  @return whether the edge is removable
   *  @since  2006.07.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private static boolean isRemovable (Edge edge)
  {                             /* --- check for a removable edge */
    int  i;                     /* loop variable */
    long rgs, cur;              /* ring flags */
    Node s, a;                  /* to traverse the ring nodes */
    Edge r, x = null;           /* to traverse the ring edges */

    rgs = edge.getRings();      /* traverse the ring flags */
    for (cur = 1; rgs != 0; cur <<= 1) {
      if ((rgs & cur) == 0) continue;
      rgs &= ~cur;              /* remove the processed ring flag */
      r = edge; s = r.src; a = r.dst;
      do {                      /* loop to collect the ring edges */
        for (i = a.deg; --i >= 0; ) {
          x = a.edges[i];       /* traverse the incident edges */
          if ((x != r) && ((x.flags & cur) != 0)) break;
        }                       /* find the next ring edge and */
        r = x;                  /* remove the current ring flag */
        if (--r.mark == FIXED)  /* if all ring flags are removed */
          return false;         /* from a fixed edge, abort */
        a = (r.src != a) ? r.src : r.dst;
      } while (a != s);         /* get the next ring node */
    }                           /* until the ring is closed */
    return true;                /* return 'edge is removable' */
  }  /* isRemovable() */

  /*------------------------------------------------------------------*/
  /** Check for a valid ring order.
   *  <p>This function presupposes that the internal edge buffer
   *  contains the graphs edges in adapted order.</p>
   *  @param  graph the graph to check
   *  @param  fixed the number of fixed edges
   *  @since  2007.03.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean validRingOrder (Graph graph, int fixed)
  {                             /* --- check the ring order */
    int  i, k, n;               /* loop variables, number of edges */
    long rgs, cur;              /* ring flags */
    Edge edge;                  /* to traverse the edges */

    n = graph.edgecnt;          /* mark the fixed edges */
    for (i = n; --i >= fixed; ) this.edges[i].mark = 0;
    for (++i;   --i >= 0;     ) this.edges[i].mark = FIXED;
    for (i = n; --i >= 0; ) {   /* traverse all edges, */
      edge = this.edges[i];     /* get their ring flags, and */
      rgs  = edge.getRings();   /* mark non-ring edges as fixed */
      if (rgs == 0) edge.mark = FIXED;
      for (cur = 1; rgs != 0; cur <<= 1)
        if ((rgs & cur) != 0) { rgs &= ~cur; edge.mark++; }
      this.word[i] = edge.mark; /* count the number of ring flags */
    }                           /* and buffer the edge markers */
    for (i = fixed; i < n; i++) {
      edge = this.edges[i];     /* traverse the non-fixed edges */
      if ((edge.mark & FIXED) != 0) continue;
      for (k = n; --k >= 0; )   /* restore the edge markers */
        this.edges[k].mark = this.word[k];
      if (Extension.isRemovable(edge))
        return false;           /* if an edge can be removed, then */
    }                           /* rings were added in a wrong order */
    return true;                /* otherwise the order is valid */
  }  /* validRingOrder() */

  /*------------------------------------------------------------------*/
  /** Create a variable length chain extension.
   *  <p>A variable length chain consists of nodes of the same type
   *  that are connected by edges of the same type. There must not
   *  be any branches. This function is called when the function
   *  <code>next()</code> detects a possible start of a chain.
   *  However, the check in <code>next()</code> is limited and thus
   *  it may be that no variable length chain can be created. In this
   *  case this function returns <code>false</code>.</p>
   *  @return whether a chain extension was created
   *  @since  2003.02.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean chain ()
  {                             /* --- create a chain extension */
    Node node;                  /* to traverse the nodes of the chain */
    Edge edge;                  /* to traverse the edges of the chain */

    edge = this.edges[0];       /* get the starting edge */
    if ((edge.type != this.cedge) || !edge.isBridge())
      return false;             /* first edge must be single, bridge */
    node = this.nodes[1];       /* get dest. node (first in chain) */
    while ((node.deg  == 2)     /* and traverse the chain */
    &&     (node.type == this.cnode)) {
      edge = node.edges[(node.edges[0] != edge) ? 0 : 1];
      if (edge.mark < -1) return false;
      if ((edge.type != this.cedge) || !edge.isBridge())
        break;                  /* edge must be single and a bridge */
      node = (node != edge.src) ? edge.src : edge.dst;
      this.size--;              /* go to the next edge and node and */
    }                           /* increase the chain length */
    if (this.size >= 0) {       /* if no extension chain was found, */
      this.size = -1; return false; }         /* abort the function */
    this.edges[1] = edge;       /* note the last edge of the chain */
    this.edgecnt  = 2;          /* there are always two new edges */
    this.nodes[1] = node;       /* note the node and its index */
    this.nodecnt  = 1;          /* and set the new node counter */
    this.dst      = this.emb.nodes.length;
    this.chcnt    = this.frag.chcnt +1;  /* increment chain counter */
    return (this.emb.nodes.length +this.chcnt +1 <= this.max);
  }  /* chain() */              /* return extension success */

  /*------------------------------------------------------------------*/
  /** Compare the current extension to a given fragment.
   *  <p>This function is used to determine whether the current
   *  extension is equivalent to a previously created one (and thus
   *  only an embedding has to be created from it and to be added to
   *  the corresponding fragment) or not (and thus a new fragment has
   *  to be created). It is designed as a comparison function, because
   *  the created fragments are kept as an ordered array, so that a
   *  binary search becomes possible.</p>
   *  @param  frag the fragment to compare to
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the fragment described by this extension is less
   *          than, equal to, or greater than the fragment given
   *          as an argument
   *  @since  2002.04.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract int compareTo (Fragment frag);

  /*------------------------------------------------------------------*/
  /** Compare the current ring extension to a fragment.
   *  <p>This is a sub-function of the function <code>compareTo</code>,
   *  which compares the current extension to a given fragment whatever
   *  the type of the extension may be. If both the current extension
   *  and the given fragment describe a ring extension, this function
   *  is called to compare them.</p>
   *  <p>This function assumes that the first edge of the ring together
   *  with its destination node have already been compared (namely in
   *  the function <code>compareTo</code>) and thus only compares the
   *  rest of the new ring edges.</p>
   *  @param  frag the fragment to compare to
   *  @return whether the current extension is smaller (-1) or greater
   *          than (+1) or equal to (0) the given fragment
   *  @since  2006.05.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareRing (Fragment frag)
  {                             /* --- compare a ring extension */
    int  i, k, n, e;            /* loop variable, buffers */
    int  t1, t2;                /* buffers for comparison */
    Edge edge, x;               /* to traverse the edges */
    Node node, y;               /* to traverse the nodes */

    e = frag.base.list.edges.length;
    i =      frag.list.edges.length -e;
    if (this.edgecnt < i) return -1;  /* compare the number */
    if (this.edgecnt > i) return +1;  /* of added edges */
    n = frag.base.list.nodes.length;
    i =      frag.list.nodes.length -n;
    if (this.nodecnt < i) return -1;  /* compare the number */
    if (this.nodecnt > i) return +1;  /* of added nodes */
    /* Note that the ring sizes must not be compared, because it is  */
    /* possible that exactly the same extension (in terms of the set */
    /* of new edges and nodes) results from rings of different size, */
    /* simply because the differing part is already in the fragment. */
    for (i = k = 0; ++i < this.size; ) {
      x = this.edges[i];        /* traverse the remaining edges */
      if (x.mark >= 0) continue;/* skip already contained edges */
      node = this.nodes[i];     /* get the source node */
      t1 = (node.mark >= 0) ? node.mark : n++;
      t2 = frag.ris[k++];       /* get/compute source node indices */
      if (t1 < t2) return -1;   /* compare the indices */
      if (t1 > t2) return +1;   /* of the source nodes */
      y = frag.list.nodes[t2];  /* and then their types */
      if (node.type < y.type) return -1;
      if (node.type > y.type) return +1;
      edge = frag.list.edges[++e]; /* get the corresponding edge */
      if (edge.type > x.type) return -1;  /* compare the types */
      if (edge.type < x.type) return +1;  /* of the added edges */
      node = this.nodes[(i+1) % this.size];
      t1 = (node.mark >= 0) ? node.mark : n;
      t2 = frag.ris[k++];       /* get/compute dest. node indices */
      if (t1 < t2) return -1;   /* compare the indices */
      if (t1 > t2) return +1;   /* of the destination nodes */
      y = frag.list.nodes[t2];  /* and then their types */
      if (node.type < y.type) return -1;
      if (node.type > y.type) return +1;
    }                           /* (compare all added edges) */
    i = frag.ris[k++];          /* compare first insertion position */
    if (this.pos1 < i) return -1;
    if (this.pos1 > i) return +1;
    i = frag.ris[k++];          /* compare second insertion position */
    if (this.pos2 < i) return -1;
    if (this.pos2 > i) return +1;
    return 0;                   /* otherwise the fragments are equal */
  }  /* compareRing() */

  /*------------------------------------------------------------------*/
  /** Create a fragment from the current extension.
   *  <p>This function is called when the current extension is not
   *  equal to an already existing fragment and thus a new fragment
   *  has to be created.</p>
   *  @return the current extension as a fragment
   *  @since  2005.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Fragment makeFragment ()
  { return new Fragment(this); }

  /*------------------------------------------------------------------*/
  /** Create an embedding from the current extension.
   *  <p>This function is called when the current extension is equal
   *  to an already existing fragment and thus only a new embedding
   *  has to be added to that fragment.</p>
   *  @return the current extension as an embedding
   *  @since  2006.10.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Embedding makeEmbedding ()
  { return new Embedding(this); }

  /*------------------------------------------------------------------*/
  /** Initialize a canonical form test or generation.
   *  <p>For a canonical form test or for the procedure that makes a
   *  graph canonical, the internal arrays have to have a certain
   *  size (depending on the size of the graph, that is, the number
   *  of its nodes and edges), so that they can hold the necessary data.
   *  This function ensures proper array sizes and also initializes some
   *  variables.</p>
   *  @param  graph the graph to make canonic or to check
   *  @param  fixed the number of fixed (immovable) edges
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void initCanonic (Graph graph, int fixed)
  {                             /* --- initialize for canonical form */
    int k, n;                   /* number of nodes/edges/characters */

    k = graph.nodecnt;          /* enlarge node array if necessary */
    if (k > this.nodes.length) this.nodes = new Node[k];
    k = graph.edgecnt;          /* enlarge edge array if necessary */
    if (k > this.edges.length) this.edges = new Edge[k];
    n = (k << 2) +2;            /* enlarge code word if necessary */
    if (n > this.word.length)  this.word  = new int[n];
    this.word[n-1] = 0;         /* place a sentinel in the code word */
    this.size      = k;         /* note the number of edges and */
    this.src       = fixed;     /* the number of fixed edges */
  }  /* initCanonic() */

  /*------------------------------------------------------------------*/
  /** Create the code word for a given graph.
   *  <p>The code word is created for the current order of the edges
   *  as it is found in the graph. As a consequence the resulting
   *  code word may or may not be the canonical code word. If the
   *  canonical code word is desired, the graph has to be made
   *  canonic by calling the function <code>makeCanonic()</code>.</p>
   *  @param  graph the graph for which to create the code word
   *  @return the number of generated "characters" (array entries)
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int makeWord (Graph graph)
  { return this.makeWord(graph, graph.edgecnt); }

  /*------------------------------------------------------------------*/
  /** Create the code word for the first edges of a given graph.
   *  <p>In other words, this function creates the prefix of the
   *  code word for the given graph, using only the first edges.
   *  If, however, <code>edgecnt == graph.edgecnt</code>, all edges
   *  are used and thus a full code word is created.</p>
   *  <p>The code word is created for the current order of the edges
   *  as it is found in the graph. As a consequence the resulting
   *  code word may or may not be the canonical code word. If the
   *  canonical code word is desired, the graph has to be made
   *  canonic by calling the function <code>makeCanonic()</code>.</p>
   *  @param  graph   the graph for which to create the code word
   *  @param  edgecnt the number of edges to consider
   *  @return the number of generated "characters" (array entries)
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int makeWord (Graph graph, int edgecnt)
  {                             /* --- construct (part of) code word */
    int n = (edgecnt << 2) +1;  /* compute number of characters */ 
    if (n > this.word.length)   /* if the word gets too long, */
      this.word = new int[n];   /* create a new buffer */
    for (int i = graph.nodecnt; --i >= 0; )
      graph.nodes[i].mark = i;  /* number the nodes of the graph */
    if (edgecnt <= 0) {         /* if there are no edges, abort */
      this.word[0] = graph.nodes[0].type; return 1; }
    this.makeWord(graph.edges, edgecnt);
    return n;                   /* return the number of characters */
  }  /* makeWord() */

  /*------------------------------------------------------------------*/
  /** Create the (prefix of a) code word for a given edge array.
   *  @param  edges the array of edges for which to create the code word
   *  @param  n     the number of edges to consider
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract void makeWord (Edge[] edges, int n);

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given graph.
   *  <p>This function assumes that <code>makeWord()</code> has been
   *  called before (for some other graph or a different form of the
   *  same graph) and has placed a code word into the internal code
   *  word buffer. This code word is then compared to the code word
   *  that would be created for the given graph (without explicitely
   *  generating the code word for the graph).</p>
   *  @param  graph the graph to compare to
   *  @return whether the internal code word is smaller (-1) or greater
   *          than (+1) or equal to (0) the code word of the graph
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareWord (Graph graph)
  { return this.compareWord(graph, graph.edgecnt); }

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given graph.
   *  <p>The comparison takes only the first <code>edgecnt</code>
   *  edges into account. Any remaining edges are not compared.
   *  If, however, <code>edgecnt == graph.edgecnt</code>, the full
   *  code words are compared.</p>
   *  <p>This function assumes that <code>makeWord()</code> has been
   *  called before and has placed a code word into the internal code
   *  word buffer. This code word is then compared to the code word
   *  that would be created for the given graph (without explicitely
   *  generating the code word for the graph).</p>
   *  @param  graph   the graph to compare to
   *  @param  edgecnt the number of edges to consider
   *  @return whether the internal code word is smaller (-1) or greater
   *          than (+1) or equal to (0) the code word of the graph
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareWord (Graph graph, int edgecnt)
  {                             /* --- compare graph to code word */
    for (int i = graph.nodecnt; --i >= 0; )
      graph.nodes[i].mark = i;  /* number the nodes of the graph */
    return this.compareWord(graph.edges, edgecnt);
  }  /* compareWord() */        /* compare the edges of the graph */

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given edge array.
   *  @param  edges the array of edges to compare to
   *  @param  n     the number of edges to consider
   *  @return whether the internal code word is smaller (-1) or greater
   *          than (+1) or equal to (0) the code word of the edges array
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract int compareWord (Edge[] edges, int n);

  /*------------------------------------------------------------------*/
  /** Internal recursive function for the canonical form test.
   *  <p>In each recursive call to this function one edge is checked.
   *  If a possibility to construct a lexicographically smaller (prefix
   *  of a) code word is found or if all (prefixes of) code words that
   *  could be constructed are lexicographically greater, the function
   *  returns directly. Only if there is a possibility to construct an
   *  equal prefix, the function calls itself recursively.</p>
   *  @param  bdi the current edge index
   *  @param  ati the current node index
   *  @param  cnt the number of already numbered nodes
   *  @return the lowest edge index at which the considered graph
   *          differs from the canonical form (in this recursion)
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract int isCanonic (int bdi, int ati, int cnt);

  /*------------------------------------------------------------------*/
  /** Check whether a given graph is canonic.
   *  <p>In addition, if the graph is not canonic, it is determined
   *  whether the canonical form differs from the form of the graph
   *  within the first <code>fixed</code> edges. Hence there are three
   *  possible outcomes: (1) the graph is in canonical form (return
   *  value 1), (2) the graph differs from the canonical form in the
   *  first <code>fixed</code> edges (return value -1), (3) the graph
   *  is not in canonical form, but does not differ in the first
   *  <code>fixed</code> edges (return value 0).</p>
   *  @param  graph the graph to check for canonical form
   *  @param  fixed the number of fixed edges
   *  @return -1, if the graph differs from the canonical form
   *              in the first <code>fixed</code> edges,<br>
   *           0, if it is not canonical, but does not differ
   *              in the first <code>fixed</code> edges,<br>
   *           1, if the graph is canonical.
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int isCanonic (Graph graph, int fixed)
  {                             /* --- check for canonical form */
    int  i, k, r, t;            /* loop variable, buffers */
    Node node;                  /* to traverse the nodes */

    if (graph.edgecnt <= 0)     /* if there are no edges, */
      return 1;                 /* the graph's word is minimal */
    this.initCanonic(graph, fixed); /* build initial code word */
    this.makeWord(graph, graph.edgecnt);
    graph.prepare();            /* prepare graph for the test */
    graph.mark(-1);             /* (sort edges and clear markers) */
    r = graph.edgecnt;          /* set the default result */
    k = graph.nodes[0].type;    /* get the root node's type */
    for (i = graph.nodecnt; --i >= 0; ) {
      node = graph.nodes[i];    /* traverse the nodes of the fragment */
      if (node.type > k) continue;   /* check one letter prefix words */
      if (node.type < k) break; /* (words are e.g. A (I_s B A I_d)* ) */
      this.nodes[node.mark = 0] = node; /* mark and note the root, */
      t = this.isCanonic(0,0,1);/* check prefix words recursively, */
      node.mark = -1;           /* then unmark the root node again */
      if (t < r) { if (t < fixed) return -1; r = t; }
    }                           /* evaluate the recursion result */
    return (r < graph.edgecnt) ? 0 : 1;
  }  /* isCanonic() */          /* return the overall result */

  /*------------------------------------------------------------------*/
  /** Internal recursive function for making a given graph canonic.
   *  <p>This function works in basically the same way as the analogous
   *  function <code>isCanonic()</code>, with the only difference that
   *  whenever a smaller (prefix of a) code word is found, the function
   *  is not terminated, but continues with the new (prefix of a) code
   *  word, thus constructing the lexicographically smallest code word.
   *  </p>
   *  @param  bdi the current edge index
   *  @param  ati the current node index
   *  @param  cnt the number of already numbered nodes
   *  @return whether the considered graphs needs to be changed
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract boolean makeCanonic (int bdi, int ati, int cnt);

  /*------------------------------------------------------------------*/
  /** Make a given graph canonic.
   *  <p>The form of the graph (that is, the order of its nodes
   *  and edges) is changed in such a way that it produces the
   *  lexicographically smallest code word. The first <code>keep</code>
   *  edges are left unchanged. If <code>keep = 0</code>, then all
   *  edges may change their positions, but the first node is kept.
   *  Only if <code>keep = -1</code> the graph may be completely
   *  reorganized.</p>
   *  <p>This function does not actually reorganize the graph, but
   *  only stores the found canonical order of the edges and nodes in
   *  internal arrays. In addition, it creates a map for reorganizing
   *  the nodes and edges, also in an internal buffer. Either of these
   *  may later be used to actually reorganize the graph as well
   *  as any embeddings (if the graph represents a fragment). Note
   *  that these arrays and maps are not filled/created if the graph
   *  is already in canonical form. In this case the function returns
   *  <code>false</code>, thus indicating that no reorganization is
   *  necessary.</p>
   *  @param  graph the graph to make canonic
   *  @param  keep  the number of edges to keep
   *  @return whether the graphs needs to be changed
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean makeCanonic (Graph graph, int keep)
  {                             /* --- turn graph into canonical form */
    int     i, n;               /* loop variables, buffers */
    Node    node, root;         /* to traverse the nodes, new root */
    Edge    edge;               /* to traverse the edges */
    boolean changed;            /* whether fragment was changed */

    n = (keep >= 0) ? keep : 0; /* get number of edges to keep */
    if (graph.edgecnt <= n)     /* if no edges are moveable, */
      return false;             /* the graph's word is minimal */
    graph.prepare();            /* prepare graph for processing */
    this.initCanonic(graph, 0); /* and init. the canonical form */
    root = graph.nodes[0];      /* note the old root node */
    if (keep < 0) {             /* if full freedom of reordering */
      for (i = 0; i < graph.nodecnt; i++) {
        node = graph.nodes[i];  /* traverse the graph's nodes */
        if (node.type < root.type) root = node;
      }                         /* find node with smallest type */
      changed = (root != graph.nodes[0]);
      if (changed) {            /* if the root has to be replaced, */
        this.word[0] = root.type;     /* set the initial character */
        this.word[1] = Integer.MAX_VALUE; }
      else {                    /* if the root node may be kept */
        System.arraycopy(graph.edges, 0, this.edges, 0, graph.edgecnt);
        this.makeWord(graph, graph.edgecnt);
      }                         /* construct an initial code word */
      graph.mark(-1);           /* unmark all nodes and edges */
      for (i = graph.nodecnt; --i >= 0; ) {
        node = graph.nodes[i];  /* traverse the graph's nodes */
        if (node.type != root.type)
          continue;             /* check one letter prefix words */
        node.mark = 0;          /* mark the potential root and */
        this.nodes[0] = node;   /* store it as the first node */
        if (this.makeCanonic(0, 0, 1)) { root = node; changed = true; }
        node.mark = -1;         /* construct code word recursively, */
      }                         /* then unmark the pot. root again */
      this.nodes[0] = root;     /* set the (new) root node and */
      root.mark = n = 0; }      /* mark it as the first node */
    else {                      /* if to keep some edges */
      System.arraycopy(graph.edges, 0, this.edges, 0, graph.edgecnt);
      this.makeWord(graph, graph.edgecnt);
      n = 0;                    /* construct an initial code word */
      for (i = keep; --i >= 0; ) {  /* traverse the edges to keep */
        edge = graph.edges[i]; edge.mark = 0;
        if (edge.src.mark > n) n = edge.src.mark;
        if (edge.dst.mark > n) n = edge.dst.mark;
      }                         /* find node with highest index */
      for (i = n+1; --i >= 0; ) /* copy the marked/visited nodes */
        this.nodes[i] = graph.nodes[i];
      for (i = graph.nodecnt; --i > n; )
        graph.nodes[i].mark = -1;  /* unmark the unvisited nodes */
      for (i = graph.edgecnt; --i >= keep; )
        graph.edges[i].mark = -1;  /* unmark the edges to reorder */
      changed = this.makeCanonic(keep, 0, n+1);
    }                           /* construct code word recursively */
    if (changed)                /* if the graph was changed, */
      this.makeMap(graph, n);   /* build node and edge maps */
    return changed;             /* return whether graph is changed */
  }  /* makeCanonic() */

  /*------------------------------------------------------------------*/
  /** Build a map for reordering the nodes and edges.
   *  <p>This map describes the transition from the original form to
   *  the canonical form and is built in the <code>word</code> array
   *  of this extension structure. The first <code>graph.edgecnt</code>
   *  fields of this array contain the new indices of the edges, the
   *  next <code>graph.nodecnt</code> fields the new indices of the
   *  nodes. The map is used to reorganize the embeddings of a
   *  fragment.</p>
   *  @param  graph the graph for which to build the map
   *  @param  n     the highest already fixed node index
   *  @since  2006.05.08 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void makeMap (Graph graph, int n)
  {                             /* --- build map for canonical form */
    int  i, k;                  /* loop variables */
    Edge edge;                  /* to traverse the edges */

    k = graph.edgecnt;          /* number edges and build edge map */
    for (i = k; --i >= 0; ) this.edges[i].mark = i;
    for (i = k; --i >= 0; ) this.word[i] = graph.edges[i].mark;
    for (i = 0; i < k; i++) {   /* traverse the edges and */
      edge = this.edges[i];     /* number the nodes accordingly */
      if      (edge.src.mark < 0)
        this.nodes[edge.src.mark = ++n] = edge.src;
      else if (edge.dst.mark < 0)
        this.nodes[edge.dst.mark = ++n] = edge.dst;
    }                           /* also sort nodes into new order */
    for (k += i = graph.nodecnt; --i >= 0; )  /* build node map */
      this.word[--k] = graph.nodes[i].mark;   /* and copy nodes */
  }  /* makeMap() */

  /*------------------------------------------------------------------*/
  /** Check whether a fragment contains unclosable rings.
   *  <p>If the output is restricted to fragments containing only closed
   *  rings, the restricted extensions (as they can be derived from a
   *  canonical form) render certain nodes unextendable. If such a node
   *  has only one incident ring edge, the ring of which this edge is
   *  part cannot be closed by future extensions. Hence neither this
   *  fragment nor any of its extensions can produce output and thus
   *  it can be pruned.</p>
   *  @param  frag the fragment to check for unclosable rings
   *  @return whether the given fragment contains unclosable rings
   *  @since  2006.05.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract boolean hasUnclosableRings (Fragment frag);

  /*------------------------------------------------------------------*/
  /** Create the code word for a given graph as a string.
   *  @param  graph the graph for which to create a code word
   *  @return a code word (as a string) for the given graph
   *  @since  2006.05.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected String describe (Graph graph)
  { return this.describe(graph, true); }

  /*------------------------------------------------------------------*/
  /** Create the code word for a given graph as a string.
   *  <p>This function allows for the code word of the graph already
   *  being available in the internal code word buffer. In this case
   *  the function should be called with <code>create == false</code>
   *  (the graph is only used to retrieve the number of edges).</p>
   *  @param  graph  the graph for which to create a code word string
   *  @param  create whether the code word needs to be created
   *  @return a code word (as a string) for the given graph
   *  @since  2006.05.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract String describe (Graph graph, boolean create);

}  /* class Extension */

