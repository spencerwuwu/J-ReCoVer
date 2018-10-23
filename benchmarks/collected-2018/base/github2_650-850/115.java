// https://searchcode.com/api/result/111498790/

/*******************************************************************************
 *Copyright (c) 2008 The Bioclipse Team and others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/*----------------------------------------------------------------------
  File    : MaxSrcExt.java
  Contents: Graph fragment extension management
            (breadth first search/maximum edge source extension)
  Author  : Christian Borgelt
  History : 2002.03.11 file created as file submol.java
            2002.04.02 function compareTo added
            2003.08.07 complete rewrite of extension functions
            2005.06.08 elimination of forward edges added
            2005.07.19 forward edges allowed, backward edges eliminated
            2005.07.21 element manager added, excluded check removed
            2005.07.23 order of the extensions inverted
            2005.08.11 class Extension made abstract
            2006.04.10 function compareTo for rings simplified
            2006.04.11 function compareEdge added (for comparing edges)
            2006.05.02 functions makeCanonic and makeWord added
            2006.05.03 function makeCanonic completed and debugged
            2006.05.10 function describe added (code word printing)
            2006.05.12 extension-specific ring filtering moved here
            2006.05.16 bug in function ring fixed (ring direction)
            2006.05.18 adapted to new field Extension.dst
            2006.05.31 function isCanonic extended, result changed
            2006.06.06 adapted to changed type of edge flags
            2006.06.07 function compareWord added
            2006.07.01 adaptation of ring extensions moved here
            2006.07.03 edge splicing for ring extension adaptation
            2006.07.04 second functions makeWord/compareWord added
            2006.07.06 function adaptRing redesigned completely
            2006.07.10 marking of non-ring edges added to adaptRing
            2007.03.02 default constructor without arguments added
            2007.03.23 bug in function adaptRing fixed (last fixed edge)
            2007.03.24 adapted to new functions of super-class
            2007.06.21 adapted to new class TypeMgr
            2007.10.19 bug in ring extension handling fixed
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for maximum source extensions.
 *  <p>Maximum source extensions are the restricted extensions of a
 *  breadth-first spanning tree canonical form. Only nodes having an
 *  index no less than the maximum source of an edge (where the source
 *  of an edge is the incident node with the smaller index) may be
 *  extended by adding an edge. Edges closing cycles must lead "forward",
 *  that is, must lead from a node with a smaller index to a node with
 *  a larger index. In addition, at the node with the maximum source
 *  index added edges must succeed all already incident edges that have
 *  this node as a source node.</p>
 *  <p>For comparing edges and constructing code words the following
 *  precedence order of the edge properties is used:
 *  <ul><li>source node index (ascending)</li>
 *      <li>edge attribute (ascending)</li>
 *      <li>node attribute (ascending)</li>
 *      <li>destination node index (ascending)</li></ul>
 *  Hence the general form of a code word is<br>
 *  a (i<sub>s</sub> b a i<sub>d</sub>)<sup>m</sup><br>
 *  where a is a node attribute, b an edge attribute, i<sub>s</sub>
 *  the index of the source node of an edge, i<sub>d</sub> the index
 *  of the destination node of an edge and m the number of edges.</p>
 *  @author Christian Borgelt
 *  @since  2003.08.06/2005.08.11 */
/*--------------------------------------------------------------------*/
public class MaxSrcExt extends Extension {

  /*------------------------------------------------------------------*/
  /** Create a maximum source extension object.
   *  @since  2007.03.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MaxSrcExt ()
  { this(EDGE, 256); }

  /*------------------------------------------------------------------*/
  /** Create a maximum source extension object.
   *  @param  mode the extension mode
   *               (e.g. <code>EDGE</code> or <code>EDGE|RING</code>)
   *  @param  max  the maximum fragment size (number of nodes)
   *  @since  2003.08.06/2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MaxSrcExt (int mode, int max)
  { super(mode, max); }

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
   *  @since  2003.08.06/2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
public void init (Fragment frag, Embedding emb)
  {                             /* --- initialize an extension */
    Node s, d, y;               /* to traverse the nodes */
    Edge e, x;                  /* to traverse the edges */

    emb.index();                /* mark the embedding in the graph */
    this.frag = frag;           /* note the (possibly new) fragment*/
    this.emb  = emb;            /* and the embedding to extend */
    this.src  = frag.src;       /* start with the first edge */
    this.idx  = -1;             /* of the previous source node */
    this.size = -1;             /* clear the ring size and flags */
    this.all  =  0;             /* (also extension type indicator) */
    this.pos1 = this.pmin = -1; /* clear ring variant variables */
    if (frag.idx < 0) return;   /* if no previous edge, abort */
    s = emb.nodes[frag.src];    /* get the current node and */
    e = emb.edges[frag.idx];    /* find the previous edge's index */
    while (s.edges[++this.idx] != e);
    d = (e.src != s) ? e.src : e.dst;
    while (--this.idx >= 0) {   /* check for equivalent edges */
      x = s.edges[this.idx];    /* among the preceding ones */
      if (x.type != e.type) break;
      y = (x.src != s) ? x.src : x.dst;
      if (y.type != d.type) break;
    }                           /* (equivalent processed edges */
  }  /* init() */               /*  must be considered again) */

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
   *  @since  2003.08.06/2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
public boolean next ()
  {                             /* --- create the next extension */
    Node s, d, p[];             /* to traverse/access the nodes */
    Edge e;                     /* to traverse/access the edges */
    
    /* --- continue an old extension --- */
    if (((this.mode & EQVARS) != 0)
    &&  (this.pos1 >= 0)        /* if there is another equivalent */
    &&   this.variant())        /* variant of the previous ring, */
      return true;              /* return "extension successful" */
    if (((this.mode & RING)   != 0)
    &&  (this.all != 0)         /* if there is another ring flag */
    &&   this.ring())           /* and some ring is admissible, */
      return true;              /* return "extension successful" */
    if (((this.mode & CHAIN)  != 0)
    &&  (this.size == 0)        /* if last extension was an edge and */
    &&   this.chain())          /* it can be extended into a chain, */
      return true;              /* return "extension successful" */

    /* --- find a new extension --- */
    p = this.emb.nodes;         /* get the nodes of the embedding */
    s = p[this.src];            /* and the current anchor node */
    while (true) {              /* find the next unprocessed edge */
      while (++this.idx >= s.deg) {
        if (++this.src >= p.length) {
          this.emb.mark(-1);    /* if node's last edge is processed, */
          return false;         /* go to the next extendable node */
        }                       /* and if there is none, abort */
        s = p[this.src];        /* get the new anchor node and */
        this.idx = -1;          /* start with the first edge */
      }
      e = s.edges[this.idx];    /* get the next edge of this node */
      if (e.mark != -1)         /* if the edge is in the embedding */
        continue;               /* or excluded, it cannot be added */
      d = (s != e.src) ? e.src : e.dst;
      if ((d.mark < 0)          /* if node is not in the embedding */
      &&  (p.length +this.frag.chcnt >= this.max))
        continue;               /* check whether a new node is ok */
      this.dst = (d.mark < 0) ? p.length : d.mark;
      if (this.dst <= this.src) /* skip edges closing a ring that */
        continue;               /* lead "backward" in the fragment */
      this.nodes[0] = s;        /* note the anchor node and the */
      this.edges[0] = e;        /* (first) edge of the extension */
      this.nodes[1] = d;        /* note the destination node */
      if (e.isInRing()          /* if a ring extension is possible */
      && ((this.mode & RING) != 0)) {
        this.all  = e.getRings();   /* note the ring flags and */
        this.curr = 1;          /* init. the current ring flag */
        if (this.ring()) return true;
        continue;               /* if some ring is admissible, */
      }                         /* return "extension successful" */
      if ((this.mode & EDGE) == 0)
        continue;               /* check for edge extensions */
      this.nodecnt = (d.mark < 0) ? 1 : 0;
      this.edgecnt = 1;         /* zero/one new node, one new edge */
      this.size    = 0;         /* clear the extension size */
      this.chcnt   = this.frag.chcnt;
      return true;              /* copy the chain counter and */
    }                           /* return "extension successful" */
  }  /* next() */

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

  @Override
protected boolean validRing ()
  {                             /* --- check a ring extension */
    int  i;                     /* loop variable */
    Node s, d;                  /* to traverse the ring nodes */
    Edge frst, last;            /* to access first and last edge */

    s = this.nodes[0];          /* get the anchor node (source) */
    for (i = this.size; --i > 0; ) {
      d = this.nodes[i];        /* traverse the ring nodes and */
      if ( (d.mark >= 0)        /* check whether the ring is */
      &&   (d.mark <  s.mark)   /* admissible for this anchor */
      &&  ((this.edges[i  ].mark < 0)
      ||   (this.edges[i-1].mark < 0)))
        break;                  /* (no ring node that is incident to */
    }                           /* a new edge (mark < 0) must have */
    if (i > 0) return false;    /* a smaller index than the anchor) */
    this.sym = false;           /* default: locally asymmetric */
    frst = this.edges[0];       /* check first and last ring edge */
    last = this.edges[this.size-1];  /* if only the first is new, */
    if (last.mark >= 0) return true; /* the ring direction is ok */
    if (last.type > frst.type) return true;   /* compare the */
    if (last.type < frst.type) return false;  /* edge types */
    d = (last.src != s) ? last.src : last.dst;
    s = this.nodes[1];          /* get the destination nodes */
    if (d.type    > s.type)    return true;   /* compare the */
    if (d.type    < s.type)    return false;  /* destination types */
    if ((d.mark >= 0) && (d.mark < this.dst))
      return false;             /* compare the destination indices */
    return this.sym = true;     /* note the local symmetry */
  }  /* validRing() */

  /*------------------------------------------------------------------*/
  /** Initialize the generation of equivalent ring extension variants.
   *  <p>If a ring start (and possibly also ends) with an edge that is
   *  equivalent to one or more edges already in the fragment (that is,
   *  edges that start at the same node, have the same type, and lead
   *  to nodes of the same type), these edges must be spliced with the
   *  already existing equivalent edges in the fragment. All possible
   *  ways of splicing the equivalent edges, which keep their individual
   *  order (that is, the order of the already existing edges and the
   *  order of the added edges), have to be tried. This function
   *  initializes this variant generation.</p>
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected void initVars ()
  {                             /* --- init. ring extension variants */
    int  i;                     /* loop variable */
    Edge e, r;                  /* to access/traverse the edges */
    Node s, d, x;               /* to access/traverse the nodes */

    this.pmin = this.emb.edges.length;
    this.pmax = -1;             /* init. the insertion position range */
    r = this.edges[0];          /* get the first edge of the ring */
    s = this.nodes[0];          /* and its source and */
    d = this.nodes[1];          /* destination node */
    for (i = s.deg; --i >= 0; ) {
      e = s.edges[i];           /* traverse the edges of the source */
      if (e.mark <= this.frag.idx) continue;
      if (e.type != r.type) continue;   /* skip uneligible edges */
      x = (e.src != s) ? e.src : e.dst; /* (fixed or wrong type) */
      if (x.mark <  s.mark) continue;   /* skip backward edges */
      if (x.type != d.type) continue;   /* (to preceding node) */
      if (e.mark < this.pmin) this.pmin = e.mark;
      if (e.mark > this.pmax) this.pmax = e.mark;
    }                           /* find range of equivalent edges */
    if (this.pmax < 0) {        /* if there are no equivalent edges, */
      this.pos1 = -1; return; } /* abort with unknown positions */
    this.pos1 = ++this.pmax;    /* compute the initial position(s) */
    this.pos2 = (this.sym) ? ++this.pmax : -1;
  }  /* initVars() */

  /*------------------------------------------------------------------*/
  /** Create the next ring extension variant.
   *  <p>If a ring start (and possibly also ends) with an edge that is
   *  equivalent to one or more edges already in the fragment (that is,
   *  edges that start at the same node, have the same type, and lead
   *  to nodes of the same type), these edges must be spliced with the
   *  already existing equivalent edges in the fragment. All possible
   *  ways of splicing the equivalent edges, which keep their individual
   *  order (that is, the order of the already existing edges and the
   *  order of the added edges), have to be tried. This function
   *  generates the next ring extension variant. Before it can be
   *  called, the function <code>initVars()</code> must have been
   *  invoked.</p>
   *  @return whether another ring variant was created
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected boolean variant ()
  {                             /* --- create ring extension variant */
    if  (--this.pos1 >= this.pmin) /* shift first edge to the left */
      return true;                 /* if shift is possible, abort */
    if ((  this.pos2 < 0)       /* check if second edge can be moved */
    ||  (--this.pos2 <= this.pmin)) {  /* if not (or there is none), */
      this.pos1 = -1; return false; }  /* all variants are created */
    this.pos1 = this.pos2-1;    /* place first edge before second */
    return true;                /* return 'next variant created' */
  }  /* variant() */

  /*------------------------------------------------------------------*/
  /** Reorder the edges of a fragment with a ring extension.
   *  <p>After a ring extension it may be necessary to reorder the
   *  edges of the resulting fragment, so that the edges get into the
   *  proper order w.r.t. the canonical form. In addition, it must be
   *  checked whether rings were added in the right order (if several
   *  rings were added). If not, the ring extension cannot be adapted
   *  and thus the function returns -1.</p>
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

  @Override
protected int adaptRing (Fragment frag, boolean check)
  {                             /* --- adapt and check ring extension */
    int     i, k, r, n;         /* loop variable, indices */
    Graph   graph;              /* the fragment as a graph */
    Edge    e, x;               /* to traverse the edges */
    boolean changed;            /* whether fragment was changed */

    graph = this.prepare(frag); /* mark rings in the fragment */
    this.nodes[0] = graph.nodes[0];
    this.nodes[0].mark = 0;     /* store and mark the root node */
    x = graph.edges[frag.idx];  /* get the first edge of the ring */
    k = frag.ris[frag.ris.length-3];
    if (k >= 0) {               /* if insertion position is known */
      changed = (k != frag.idx);
      for (i = 0; i < k; i++)   /* copy up to first insertion point */
        this.edges[i] = graph.edges[i];
      this.edges[x.mark = k++] = x;   /* store the first ring edge */
      r = frag.ris[frag.ris.length-2];
      if (r > 0) {              /* if last ring edge is equivalent */
        while (k < r)           /* copy up to second insertion point */
          this.edges[k++] = graph.edges[i++];
        this.edges[k++] = graph.edges[n = graph.edgecnt-1];
        if (r != n) changed = true;   /* store the last ring edge and */
      }                         /* update whether fragment is changed */
      r = frag.ris[frag.ris.length-1];
      while (k <= r)            /* copy remaining equivalent edges */
        this.edges[k++] = graph.edges[i++];
      for (k = n = 0; k <= r; k++) {
        e = this.edges[k];      /* traverse edges in the copied part */
        e.mark = k;             /* mark each edge as processed */
        if      (e.src.mark < 0) this.nodes[e.src.mark = ++n] = e.src;
        else if (e.dst.mark < 0) this.nodes[e.dst.mark = ++n] = e.dst;
      } }                       /* number and collect the nodes */
    else {                      /* if insertion position is unknown */
      for (n = i = 0; i < frag.idx; i++) {
        e = graph.edges[i];     /* traverse the edges ascendingly */
        if (((x.src.mark >= 0)  /* if incident to the marked part */
        ||   (x.dst.mark >= 0)) /* and no greater than the next edge */
        &&  (this.compareEdge(x, e, n+1) < 0))
          break;                /* the insertion position is found */
        this.edges[e.mark = i] = e; /* copy the edge to the buffer */
        if      (e.src.mark < 0) this.nodes[e.src.mark = ++n] = e.src;
        else if (e.dst.mark < 0) this.nodes[e.dst.mark = ++n] = e.dst;
      }                         /* mark and collect fixed nodes */
      this.edges[x.mark = r = i] = x;
      changed = (i != frag.idx);/* store the first ring edge */
      if      (x.src.mark < 0) this.nodes[x.src.mark = ++n] = x.src;
      else if (x.dst.mark < 0) this.nodes[x.dst.mark = ++n] = x.dst;
    }                           /* number and collect fixed nodes */
    for (k = r; ++k < graph.edgecnt; ) {
      if (i == frag.idx) i++;   /* copy the remaining edges */
      this.edges[k] = graph.edges[i++];
    }                           /* (complete the edge array) */
    if (check) {                /* if to check the ring order */
      this.makeWord(this.edges, ++r);   /* build code word prefix */
      this.word[r*4+1] = Integer.MAX_VALUE;
      if (this.makeCanonic(r, 0, n+1))  /* complete the code word */
        changed = true;         /* by making the fragment canonic */
      if (!this.validRingOrder(graph, x.mark+1))
        return -1;              /* check for a valid ring order */
    }                           /* (no ring can be added later) */
    if (!changed) return 1;     /* if fragment is unchanged, abort */
    this.makeMap(graph, n);     /* create a map for nodes and edges */
    return 0;                   /* return that fragment needs change */
  }  /* adaptRing() */

  /*------------------------------------------------------------------*/
  /** Compare two edges with the precedence order of the canonical form.
   *  <p>The precedence order of the edge properties is:
   *  <ul><li>source node index (ascending)</li>
   *      <li>edge attribute (ascending)</li>
   *      <li>node attribute (ascending)</li>
   *      <li>destination node index (ascending)</li></ul></p>
   *  <p>This function is meant to compare edges from the same graph
   *  at each point where the next edge needs to be selected, when the
   *  graph (or rather its edge array) is rebuilt. At such a point
   *  all nodes incident to already processed edges are numbered.
   *  However, one of the nodes incident to the compared edges may
   *  not have been numbered yet. As this would make it impossible to
   *  compare the edges, the next number to be given to a node is
   *  also passed to the function.</p>
   *  @param  b1   the first  edge to compare
   *  @param  b2   the second edge to compare
   *  @param  next the index with which to number the next node
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the first edge is less than, equal to, or greater
   *          than the second edge
   *  @since  2006.04.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected int compareEdge (Edge b1, Edge b2, int next)
  {                             /* --- compare two edges */
    Node s1, d1, s2, d2;        /* source and destination nodes */
    int  i1, i2;                /* indices of the destination nodes */

    s1 = b1.src; d1 = b1.dst;   /* get nodes of first edge */
    if ((d1.mark >= 0) && (d1.mark < s1.mark)) {
      s1 = d1; d1 = b1.src; }   /* exchange nodes if necessary */
    s2 = b2.src; d2 = b2.dst;   /* get nodes of second edge */
    if ((d2.mark >= 0) && (d2.mark < s2.mark)) {
      s2 = d2; d2 = b2.src; }   /* exchange nodes if necessary */
    if (s1.mark < s2.mark) return -1;  /* compare the indices */
    if (s1.mark > s2.mark) return +1;  /* of the source nodes */
    if (b1.type < b2.type) return -1;  /* compare the types */
    if (b1.type > b2.type) return +1;  /* of the two edges */
    if (d1.type < d2.type) return -1;  /* compare the types */ 
    if (d1.type > d2.type) return +1;  /* of the destination nodes */
    i1 = (d1.mark >= 0) ? d1.mark : next;
    i2 = (d2.mark >= 0) ? d2.mark : next;
    if (i1      < i2)      return -1;  /* compare the indices */
    if (i1      > i2)      return +1;  /* of the destination nodes */
    return 0;                   /* otherwise the edges are equal */
  }  /* compareEdge() */

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
   *  @since  2002.04.02/2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
public int compareTo (Fragment frag)
  {                             /* --- compare extension to fragment */
    int t1, t2;                 /* buffers for comparison */

    if (this.src < frag.src) return -1;  /* compare the indices */
    if (this.src > frag.src) return +1;  /* of the anchor nodes */
    t1 =      this.edges[    0   ].type;
    t2 = frag.list.edges[frag.idx].type;
    if (t1 < t2) return -1;     /* compare the types */
    if (t1 > t2) return +1;     /* of the (first) added edge */
    t1 =      this.nodes[    1   ].type;
    t2 = frag.list.nodes[frag.dst].type;
    if (t1 < t2) return -1;     /* compare the types */
    if (t1 > t2) return +1;     /* of the destination nodes */
    if (this.dst < frag.dst) return -1;  /* compare the indices */
    if (this.dst > frag.dst) return +1;  /* of the destination nodes */
    t1 = (this.size > 0) ? 1 : ((this.size < 0) ? -1 : 0);
    t2 = (frag.size > 0) ? 1 : ((frag.size < 0) ? -1 : 0);
    if (t1 > t2) return -1;     /* get the extension types */
    if (t1 < t2) return +1;     /* from the sizes and compare them */
    return (this.size <= 0)     /* compare ring ext. if necessary */
         ? 0 : this.compareRing(frag);
  }  /* compareTo() */

  /*------------------------------------------------------------------*/
  /** Create the (prefix of a) code word for a given edge array.
   *  @param  edges the array of edges for which to create the code word
   *  @param  n     the number of edges to consider
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected void makeWord (Edge[] edges, int n)
  {                             /* --- construct (part of) code word */
    int  i, k;                  /* loop variables, buffers */
    Edge e;                     /* to traverse the edges */

    e = edges[0];               /* get first edge and root node */
    this.word[0] = (e.src.mark < e.dst.mark) ? e.src.type : e.dst.type;
    for (i = k = 0; i < n; i++) {
      e = edges[i];             /* traverse the graph's edges */
      if (e.src.mark < e.dst.mark) {
        this.word[++k] = e.src.mark; /* if "forward" edge */
        this.word[++k] = e.type;     /* start with source node */
        this.word[++k] = e.dst.type;
        this.word[++k] = e.dst.mark; }
      else {                         /* if "backward" edge */
        this.word[++k] = e.dst.mark; /* start with dest.  node */
        this.word[++k] = e.type;
        this.word[++k] = e.src.type;
        this.word[++k] = e.src.mark;
      }                         /* describe an edge of the graph */
    }                           /* (four characters per edge) */
  }  /* makeWord() */

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given edge array.
   *  @param  edges the array of edges to compare to
   *  @param  n     the number of edges to consider
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the internal code word is less than, equal to, or
   *          greater than the code word of the given edges array
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected int compareWord (Edge[] edges, int n)
  {                             /* --- compare edges to code word */
    int  i, k;                  /* loop variables */
    Edge e;                     /* to traverse the edges */
    Node s, d;                  /* to traverse the nodes */

    e = edges[0];               /* compare the type of the root node */
    s = (e.src.mark < e.dst.mark) ? e.src : e.dst;
    if (s.type < this.word[0]) return -1;
    if (s.type > this.word[0]) return +1;
    for (i = k = 0; i < n; i++) {
      e = edges[i];             /* traverse the graph's edges */
      s = e.src; d = e.dst;     /* get the source and dest. nodes */
      if (s.mark > d.mark) { s = d; d = e.src; }
      if (s.mark < this.word[++k]) return -1;
      if (s.mark > this.word[  k]) return +1;
      if (e.type < this.word[++k]) return -1;
      if (e.type > this.word[  k]) return +1;
      if (d.type < this.word[++k]) return -1;
      if (d.type > this.word[  k]) return +1;
      if (d.mark < this.word[++k]) return -1;
      if (d.mark > this.word[  k]) return +1;
    }                           /* return sign of difference */
    return 0;                   /* otherwise return 'equal' */
  }  /* compareWord() */

  /*------------------------------------------------------------------*/
  /** Internal recursive function for the canonical form test.
   *  <p>In each recursive call to this function one edge is checked.
   *  If a possibility to construct a lexicographically smaller (prefix
   *  of a) code word is found or if all (prefixes of) code words that
   *  could be constructed are lexicographically greater, the function
   *  returns directly. Only if there is a possibility to construct an
   *  equal prefix, the function calls itself recursively.</p>
   *  @param  ei  the current edge index
   *  @param  ni  the current node index
   *  @param  cnt the number of already numbered nodes
   *  @return the lowest edge index at which the considered graph
   *          differs from the canonical form (in this recursion)
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected int isCanonic (int ei, int ni, int cnt)
  {                             /* --- check prefix words recursively */
    int  i, k, c, m, r;         /* loop variable, node index, buffer */
    Edge e;                     /* to traverse/access the edges */
    Node s, d;                  /* to traverse/access the nodes */

    c = (ei << 2) +1;           /* compute index in code word */
    for ( ; ni < this.word[c]; ni++) {
      s = this.nodes[ni];       /* check nodes before current source */
      for (i = s.deg; --i >= 0; )
        if (s.edges[i].mark < 0)/* if there is an unmarked edge */
          return ei;            /* from a node with a smaller index, */
    }                           /* the fragment is not canonical */
    r = this.size;              /* set the default result and */
    s = this.nodes[ni];         /* get the current source node */
    for (i = 0; i < s.deg; i++) {
      e = s.edges[i];           /* traverse the unmarked edges */
      if (e.mark >= 0)             continue;
      if (e.type < this.word[c+1]) return ei;  /* check the type */
      if (e.type > this.word[c+1]) return r;   /* of the edge */
      d = (e.src != s) ? e.src : e.dst;
      if (d.type < this.word[c+2]) return ei;  /* check the type */
      if (d.type > this.word[c+2]) return r;   /* of the dest. node */
      k = m = d.mark;           /* note the node marker and */
      if (m < 0) k = cnt;       /* get the destination node index */
      if (k      < this.word[c+3]) return ei;  /* check the index */
      if (k      > this.word[c+3]) continue;   /* of the dest. node */
      if (ei     >= r-1)           return r;   /* check edge index */
      e.mark = 0;               /* mark the matching edge (and node) */
      if (m < 0) { d.mark =  k; this.nodes[cnt++] = d; }
      k = this.isCanonic(ei+1, ni, cnt);
      if (m < 0) { d.mark = -1;            cnt--;      }
      e.mark = -1;              /* unmark edge (and node) again */
      if (k < r) { if (k < this.src) return k; r = k; }
    }                           /* evaluate the recursion result */
    return r;                   /* return the overall result */
  }  /* isCanonic() */

  /*------------------------------------------------------------------*/
  /** Internal recursive function for making a given graph canonic.
   *  <p>This function works in basically the same way as the analogous
   *  function <code>isCanonic()</code>, with the only difference that
   *  whenever a smaller (prefix of a) code word is found, the function
   *  is not terminated, but continues with the new (prefix of a) code
   *  word, thus constructing the lexicographically smallest code word.
   *  </p>
   *  @param  ei  the current edge index
   *  @param  ni  the current node index
   *  @param  cnt the number of already numbered nodes
   *  @return whether the considered graphs needs to be changed
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected boolean makeCanonic (int ei, int ni, int cnt)
  {                             /* --- construct canonic code word */
    int     i, k, c, m;         /* loop variable, node index, buffer */
    Edge    e;                  /* to traverse/access the edges */
    Node    s, d;               /* to traverse/access the nodes */
    boolean changed;            /* whether code word was changed */

    c = (ei << 2) +1;           /* compute index in code word */
    if (ei >= this.size) {      /* if full code word is constructed */
      i = this.word[c]; this.word[c] = 0;
      return (i != 0);          /* reinstall sentinel and return */
    }                           /* whether code word was changed */
    changed = false;            /* default: code word is unchanged */
    while (true) {              /* source node search loop */
      if ((ni >  this.word[c])  /* if beyond small enough source */
      ||  (ni >= cnt))          /* or beyond already numbered nodes, */
        return false;           /* abort the function */
      s = this.nodes[ni];       /* traverse possible source nodes */
      for (i = s.deg; --i >= 0; )
        if (s.edges[i].mark < 0) break;
      if (i >= 0) break;        /* check for an unmarked edge */
      ni++;                     /* if all edges are marked, */
    }                           /* go to the next node */
    if (ni < this.word[c]) {    /* if source node has smaller index, */
      this.word[c]   = ni;      /* replace the code word letter */
      this.word[c+1] = Integer.MAX_VALUE;
    }                           /* (so that the rest is replaced) */
    for (i = 0; i < s.deg; i++) {
      e = s.edges[i];           /* traverse the unmarked edges */
      if (e.mark >= 0) continue;
      if (e.type > this.word[c+1])
        return changed;         /* compare the edge type */
      if (e.type < this.word[c+1]) {
        this.word[c+1] = e.type;/* set new edge type */
        this.word[c+2] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      d = (e.src != s) ? e.src : e.dst;
      if (d.type > this.word[c+2])
        return changed;         /* compare destination node type */
      if (d.type < this.word[c+2]) {
        this.word[c+2] = d.type;/* set new destination node type */
        this.word[c+3] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      k = m = d.mark;           /* note the node marker and */
      if (m < 0) k = cnt;       /* get the destination node index */
      if (k      > this.word[c+3])
        continue;               /* compare destination node index */
      if (k      < this.word[c+3]) {
        this.word[c+3] = k;     /* set new destination node index */
        this.word[c+4] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      e.mark = 0;               /* mark the edge (and node) */
      if (m < 0) { d.mark =  k; this.nodes[cnt++] = d; }
      if (this.makeCanonic(ei+1, ni, cnt)) {
        this.edges[ei] = e; changed = true; }
      if (m < 0) { d.mark = -1;            cnt--;      }
      e.mark = -1;              /* recursively construct code word, */
    }                           /* then unmark edge (and node) again */
    return changed;             /* return whether edges were replaced */
  }  /* makeCanonic() */

  /*------------------------------------------------------------------*/
  /** Check whether a fragment contains unclosable rings.
   *  <p>If the output is restricted to fragments containing only
   *  closed rings, the restricted extensions of a breadth-first search
   *  spanning tree canonical form render all nodes not on the rightmost
   *  path unextendable. If such a node has only one incident ring
   *  edge, the ring of which this edge is part cannot be closed by
   *  future extensions. Hence neither this fragment nor any of its
   *  extensions can produce output and thus it can be pruned.</p>
   *  @param  frag the fragment to check for unclosable rings
   *  @return whether the given fragment contains unclosable rings
   *  @since  2006.05.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @Override
protected boolean hasUnclosableRings (Fragment frag)
  {                             /* --- check for uncloseable rings */
    int   i, k, n;              /* loop variable, edge counter */
    Graph graph;                /* the fragment as a graph */
    Node  node;                 /* to traverse the nodes */

    graph = frag.getAsGraph();  /* get the fragment as a graph */
    for (i = frag.src; --i >= 0; ) {
      node = graph.nodes[i];    /* traverse the unextendable nodes */
      for (n = 0, k = node.deg; --k >= 0; )
        if (node.edges[k].isInRing()) n++;
      if (n == 1) return true;  /* if there is a single ring edge, */
    }                           /* a ring cannot be closed anymore, */
    return false;               /* else all rings may be closable */
  }  /* hasUnclosableRings() */

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

  @Override
protected String describe (Graph graph, boolean create)
  {                             /* --- create a graph's code word */
    int          i, k, n;       /* loop variable, buffers */
    StringBuffer s;             /* created description */
    TypeMgr      nmgr, emgr;    /* node and edge type manager */

    if (create)                 /* construct the graph's code word */
      this.makeWord(graph, graph.edgecnt);
    nmgr = graph.getNodeMgr();  /* get the node type manager */
    emgr = graph.getEdgeMgr();  /* and the edge type manager */
    k = this.word[0];           /* get and decode type of root node */
    if (graph.coder != null) k = graph.coder.decode(k);
    s = new StringBuffer(nmgr.getName(k));
    n = graph.edgecnt << 2;     /* get the number of characters */
    for (i = 0; i < n; ) {      /* traverse the characters */
      s.append('|');            /* separator for edges */
      s.append(this.word[++i]); /* source node index */
      s.append(' ');            /* separator to edge type */
      s.append(emgr.getName(this.word[++i]));
      s.append(' ');            /* separator to node type */
      k = this.word[++i];       /* get and decode the node type */
      if (graph.coder != null) k = graph.coder.decode(k);
      s.append(nmgr.getName(k));
      s.append(' ');            /* separator to node index */
      s.append(this.word[++i]); /* destination node index */
    }                           /* store the edge descriptions */
    return s.toString();        /* return created string description */
  }  /* describe() */

}  /* class MaxSrcExt */

