// https://searchcode.com/api/result/125457318/

package contrail.avro;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import contrail.CompressedRead;
import contrail.ContrailConfig;
import contrail.DestForLinkDir;
import contrail.EdgeDestNode;
import contrail.KMerEdge;
import contrail.GraphNodeData;
import contrail.ReadState;
import contrail.ReporterMock;

import contrail.sequences.Alphabet;
import contrail.sequences.DNAAlphabetFactory;
import contrail.sequences.DNAUtil;
import contrail.sequences.Sequence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.avro.mapred.AvroCollector;
import org.apache.avro.mapred.Pair;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.junit.Test;

public class TestBuildGraphAvro {

  /**
   * Generate a list of all edges coming from the forward read. 
   * 
   * @param read: String representing the read
   * @param K: Length of the KMers. Generated edges will have length K+1;
   * @param edges: HasMap String -> Int. Edges are added to this hash map 
   *    The keys are strings representing the K + 1 length sequences
   *    corresponding to the edges. We consider only edges coming from
   *    the forward direction of the read and not its reverse complement.
   *    The integer represents a count indicating how often that edge appeared.
   */
  public void allEdgesForForwardRead(
      String read, int K, HashMap<String, java.lang.Integer> edges) {    
    for (int i = 0; i <= read.length() - K - 1; i++) {
      String e = read.substring(i, i + K + 1);      
      if (!edges.containsKey(e)) {
        edges.put(e, new java.lang.Integer(0));
      }
      edges.put(e, edges.get(e) + 1);
    }
  }

  /**
   * Generate a list of all edges coming from the read and its reverse complement. 
   * 
   * @param read: String representing the read
   * @return HasMap String -> Int. The keys are strings representing the K+1
   *    sequences corresponding to the edges. We consider both the read
   *    and its reverse complement. The integer represents a count indicating
   *    how often that edge appeared.
   */
  public HashMap<String, java.lang.Integer> allEdgesForRead(String read, int K) {
    HashMap<String, java.lang.Integer> edges = 
        new HashMap<String, java.lang.Integer>();

    allEdgesForForwardRead(read, K, edges);

    Sequence seq = new Sequence(read, DNAAlphabetFactory.create());
    String rc_str = DNAUtil.reverseComplement(seq).toString(); 
    allEdgesForForwardRead(rc_str, K, edges);
    return edges;
  }

  /**
   * Return a random a random string of the specified length using the given
   * alphabet.
   * 
   * @param length
   * @param alphabet
   * @return
   */
  public static String randomString(int length, Alphabet alphabet) {
    // Generate a random sequence of the indicated length;
    char[] letters = new char[length];
    for (int pos = 0; pos < length; pos++) {
      // Randomly select the character in the alpahbet.
      int rnd_int = (int) Math.floor(Math.random() * (alphabet.size() -1));
      letters[pos] = alphabet.validChars()[rnd_int];        
    }
    return String.valueOf(letters);
  }

  /**
   * Return a random letter r or f with equal probability
   */
  public static String randomDir() {
    double rnd = Math.random();
    String letter = rnd > .5 ? "f" : "r";
    return letter;
  }
  
  /**
   * Helper class which contains the test data for the map phase.
   */
  public static class MapTestData {
    private CompressedRead read;
    private int K;
    private String uncompressed;
    /**
     * Create a specific test case.
     * @param K: Length of the KMer.
     * @param uncompressed: The uncompressed sequence to read.
     */
    public MapTestData(int K, String uncompressed) {  
      Alphabet alphabet = DNAAlphabetFactory.create();
      Sequence seq = new Sequence(uncompressed, alphabet);
      read = new CompressedRead();
      read.setDna(ByteBuffer.wrap(seq.toPackedBytes(), 0, seq.numPackedBytes()));
      read.setId("read1");
      read.setLength(seq.size());
      this.K = K;
      this.uncompressed = uncompressed;
    }
    
    /**
     * Create a random test consisting of a random length read and random
     * value for K.
     * @return An instance of MapTestData cntaining the data for the test. 
     */
    public static MapTestData RandomTest() {
      final int MAX_K = 30;
      final int MIN_K = 2;
      final int MAX_LENGTH = 100;      
      Alphabet alphabet = DNAAlphabetFactory.create();
      int K = (int)Math.ceil(Math.random()*MAX_K) + 1;
      K = K > MIN_K ? K : MIN_K;
      // Create a compressed read.
      int length = (int) (Math.floor(Math.random() * MAX_LENGTH) + K + 1);
      String uncompressed = randomString(length, alphabet);    
      return new MapTestData(K, uncompressed);
    }
    
    public CompressedRead getRead() {
      return read;
    }
    
    public String getUncompressed() {
      return uncompressed;
    }
    
    public int getK() {
      return K;
    }
  }
  
  @Test
  // TODO(jlewi): We should probably test that chunk is set correctly.
  public void TestMap() {    
    int ntrials = 10;
    Alphabet alphabet = DNAAlphabetFactory.create();

    for (int trial = 0; trial < ntrials; trial++) {
      MapTestData test_data = MapTestData.RandomTest();
      
      AvroCollectorMock<Pair<ByteBuffer, KMerEdge>> collector_mock = 
          new AvroCollectorMock<Pair<ByteBuffer, KMerEdge>>();
  
      ReporterMock reporter_mock = new ReporterMock();
      Reporter reporter = (Reporter) reporter_mock;
  
      BuildGraphAvro.BuildGraphMapper mapper = 
          new BuildGraphAvro.BuildGraphMapper();      
      ContrailConfig.PREPROCESS_SUFFIX = 0;
      ContrailConfig.TEST_MODE = true;
      ContrailConfig.K = test_data.getK();
      
      int K = test_data.getK();
      JobConf job = new JobConf(BuildGraphAvro.BuildGraphMapper.class);
      ContrailConfig.initializeConfiguration(job);
      mapper.configure(job);
        
      try {
        mapper.map(
            test_data.getRead(), 
            (AvroCollector<Pair<ByteBuffer, KMerEdge>>)collector_mock, reporter);
      }
      catch (IOException exception){
        fail("IOException occured in map: " + exception.getMessage());
      }
  
      // Keep track of the full K + 1 strings corresponding to the
      // edges we read. We keep a count of each edge because it could
      // appear more than once
      HashMap<String, java.lang.Integer> edges = 
          new HashMap<String, java.lang.Integer>();
  
      // Reconstruct all possible edges coming from the outputs of the mapper.
      for (Iterator<Pair<ByteBuffer, KMerEdge>> it = collector_mock.data.iterator();
          it.hasNext(); ) {
        Pair<ByteBuffer, KMerEdge> pair = it.next();
  
        Sequence canonical_key = 
            new Sequence(DNAAlphabetFactory.create(), (int)ContrailConfig.K);      
        canonical_key.readPackedBytes(pair.key().array(), test_data.getK());
  
        Sequence rc_key = DNAUtil.reverseComplement(canonical_key);
  
        KMerEdge edge = pair.value();      
  
        // Check data in edge is valid.
        assertEquals(edge.getLinkDir().length(), 2);
  
        Sequence last_base = new Sequence(alphabet);
        last_base.readPackedBytes(edge.getLastBase().array(), 1);
  
        // Reconstruct the K+1 string this edge came from.
        Sequence edge_seq = new Sequence(alphabet);  
        if (edge.getLinkDir().charAt(0) == 'f') {
          edge_seq.add(canonical_key);        
        } else if (edge.getLinkDir().charAt(0) == 'r')  {
          edge_seq.add(rc_key);       
        } else {
          fail ("link dir should only consist of the letters r and f");
        }
        edge_seq.add(last_base);
  
        Sequence dest_kmer = edge_seq.subSequence(1, test_data.getK() + 1);
        // Check that the direction of the destination node is properly encoded.
        // If the sequence and its reverse complement are equal then the link 
        // direction could be either 'r' or 'f' because of DNAUtil.flip_link.
        if (dest_kmer.equals(DNAUtil.reverseComplement(dest_kmer))) {
          assertTrue(edge.getLinkDir().charAt(1) == 'f' || 
                     edge.getLinkDir().charAt(1) == 'r');
        } else {
          assertEquals(DNAUtil.canonicaldir(dest_kmer), 
              edge.getLinkDir().charAt(1));
        }
        
  
        {
          // Add the edge (K + 1) sequence that would ahve genereated this
          // KMerEdge to the list of edges.
          String edge_str = edge_seq.toString();
          if (!edges.containsKey(edge_str)) {
            edges.put(edge_str, 0);        
          }
          edges.put(edge_str, edges.get(edge_str) + 1);
        }
  
        String uncompressed = test_data.getUncompressed();
        // Check the state as best we can.
        if (edge.getState() == ReadState.END5) {
          assertEquals(edge.getLinkDir().charAt(0), 'f');
          // The first characters in the string should match this edge. 
          assertEquals(canonical_key.toString(), uncompressed.substring(0, K));        
        } else if (edge.getState() == ReadState.END6) {
          assertEquals(edge.getLinkDir().charAt(0), 'r');
          // The first characters in the string should be the reverse complement
          // of the start node.
          assertEquals(rc_key.toString(), uncompressed.substring(0, K));
        } else if (edge.getState() == ReadState.END3) {
          // The canonical version of the sequence should match the canonical
          // version of the last Kmer in the read.
          // Get the canonical version of the last K + 1 based in the read.
          Sequence last_kmer = 
              new Sequence(uncompressed.substring(uncompressed.length()- K), 
                           alphabet);
          last_kmer = DNAUtil.canonicalseq(last_kmer);
          assertEquals(last_kmer, canonical_key);
        }      
      }

      // Generate a list of all edges, (substrings of length K + 1), that should
      // be generated by the read.
      HashMap<String, java.lang.Integer> true_edges = 
          allEdgesForRead(test_data.getUncompressed(), K);

      // Check that the list of edges from the read matches the set of edges
      // created from the KMerEdges outputted by the mapper.
      assertEquals(true_edges.entrySet(), edges.entrySet());  
    }
  }

  /**
   * Used by the testReduce to find a KMerEdge which would have produced
   * an edge between the given source and destination.
   * 
   * @param canonical_src: The canonical representation of the source.
   * @param dest_node: Represents the destination node. 
   * @param link_dir: A particular link direction for this source and 
   *   destination. 
   * @param Edges: The list of KMerEdges to search to see if it contains one 
   *   that could have produced the edge defined by the tuple 
   *   (canonical_src, dest_node, link_dir).
   */
  private static boolean foundKMerEdge(
      Sequence canonical_src, EdgeDestNode dest_node, DestForLinkDir link_info, 
      ArrayList<KMerEdge> edges) {
    Alphabet alphabet = DNAAlphabetFactory.create();
    // Get the destination canonical sequence.
    Sequence canonical_dest = new Sequence(alphabet);
    canonical_dest.readPackedBytes(
        dest_node.getCanonicalSequence().getDna().array(), 
        dest_node.getCanonicalSequence().getLength());

    // Check that the lengths of the src and dest are the same.
    assertEquals(canonical_src.size(), canonical_dest.size());

    int K = canonical_src.size();

    // Convert the canonical representations of the source and destination
    // to the direction for this edge.
    Sequence src = new Sequence(canonical_src);
    src = DNAUtil.canonicalToDir(canonical_src, link_info.getLinkDir().charAt(0));

    Sequence dest = new Sequence(canonical_dest);
    dest = DNAUtil.canonicalToDir(dest, link_info.getLinkDir().charAt(1));

    // Check that the src and dest overlap by K -1 bases.
    assertEquals(src.subSequence(1, K), dest.subSequence(0, K-1));


    // The edge given by (source kmer, dest kmer, edge direction) could
    // appear multiple times with different tags for the destination KMer.
    // We want to find all such edges. So we construct a list of all
    // the tags that we need to match.
    HashSet<String> tags_to_find = new HashSet<String>();
    for (Iterator<CharSequence> it = link_info.getReadTags().iterator();
        it.hasNext();) {        
      tags_to_find.add(it.next().toString());
    }

    Sequence dest_last_base = dest.subSequence(K-1, K);
    
    // Set of edges that we found.
    HashSet<CharSequence> found_tags = new HashSet<CharSequence>();

    // Keep track of the positions in edges of the edges that we matched.
    // We will delete these edges so that edges will only contain unmatched
    // eges.
    List<Integer> pos_to_delete = new ArrayList<Integer>();

    for (int index = 0; index < edges.size(); index++) {
      KMerEdge edge = edges.get(index);            
      Sequence edge_last_base = new Sequence(alphabet);
      edge_last_base.readPackedBytes(
          edge.getLastBase().array(), 1);
      if (dest_last_base.equals(edge_last_base)) {
        String x = link_info.getLinkDir().toString();
        String y = edge.getLinkDir().toString();
        if (x.compareTo(y) == 0) {
          if (tags_to_find.contains(edge.getTag().toString())) {
            found_tags.add(edge.getTag());
            pos_to_delete.add(index);
          }
        }
      }
    }

    // Check we found edges that matched.
    assertEquals(found_tags, tags_to_find);

    Iterator<Integer> it_pos_to_delete = pos_to_delete.iterator();
    while (it_pos_to_delete.hasNext()) {     
      edges.remove(it_pos_to_delete.next().intValue());
    }
    return true;
  }

  /**
   * Class to contain the data for testing the reduce phase.
   */
  public static class ReduceTest {
    public String uncompressed;
    public int K;
    private List<KMerEdge> input_edges;

    private static Alphabet alphabet = DNAAlphabetFactory.create();

    /**
     * Construct a specific test case.
     * @param uncompressed: The uncompressed K-mer for the source sequence.
     * @param src_dir: 'f' or 'r' representing the direction of the source KMer.
     * @param last_base: The base we need to add to the soruce KMer to 
     *   get the destination KMer.
     */
    public ReduceTest(String uncompressed, String src_dir, String last_base) {
      this.uncompressed = uncompressed;
      this.K = uncompressed.length();
      input_edges = new ArrayList<KMerEdge>();

      KMerEdge node = new KMerEdge();      

      Sequence seq_uncompressed = new Sequence(uncompressed, alphabet);
      Sequence src_canonical = DNAUtil.canonicalseq(seq_uncompressed);         
      String link_dir = src_dir;
      Sequence seq_last_base = new Sequence(last_base, alphabet);

      // The destination direction depends on the source direction
      // and the K-1 overlap
      Sequence dest_kmer = 
          DNAUtil.canonicalToDir(src_canonical, link_dir.charAt(0));
      dest_kmer = dest_kmer.subSequence(1,  dest_kmer.size());
      dest_kmer.add(seq_last_base);

      link_dir += DNAUtil.canonicaldir(dest_kmer);

      node.setLinkDir(link_dir);

      node.setLastBase(ByteBuffer.wrap(
          seq_last_base.toPackedBytes(), 0, seq_last_base.numPackedBytes()));                

      node.setTag("read_0");
      node.setState(ReadState.MIDDLE);
      node.setChunk(0);        
      input_edges.add(node);      
    }

    /**
     * Construct a reduce test.
     * @param uncompressed: The uncompressed KMer for the source sequence.
     * @param input_edges: A list of KMerEdges describing edges coming from
     *   this KMer.
     * @param K: The length of KMers.
     */
    public ReduceTest(String uncompressed, List<KMerEdge> input_edges, int K) {
      this.uncompressed = uncompressed;
      this.K = K;
      this.input_edges = input_edges;
    }

    public Sequence getSrcSequence() {
      return new Sequence(uncompressed, alphabet);      
    }

    public List<KMerEdge> getInputEdges() {
      List<KMerEdge> new_list = new ArrayList<KMerEdge>();
      new_list.addAll(input_edges);
      return new_list;
    }

    /**
     * Generate a random test case.
     */
    public static ReduceTest RandomTest(int MIN_K, int MAX_K, int MAX_EDGES) {
      int K;
      K = (int)Math.ceil(Math.random()*MAX_K) + 1;
      K = K > MIN_K ? K : MIN_K;

      Alphabet alphabet = DNAAlphabetFactory.create();
      // Generate the source KMer.           
      List<KMerEdge> input_edges = new ArrayList<KMerEdge> ();  
      int num_edges = (int) Math.ceil(Math.random() * MAX_EDGES) + 1;
  
      String uncompressed = randomString(K, alphabet);
      Sequence seq_uncompressed = new Sequence(uncompressed, alphabet);
      Sequence src_canonical = DNAUtil.canonicalseq(seq_uncompressed);
      
      // Randomly generate the edges.
      for (int eindex = 0; eindex < num_edges; eindex++) {
        KMerEdge node = new KMerEdge();
  
        // Randomly determine the direction for the source.         
        String link_dir = randomDir();
  
        // Generate the base.
        Sequence last_base = new Sequence(randomString(1, alphabet), alphabet);
  
        // The destination direction depends on the source direction
        // and the K-1 overlap
        Sequence dest_kmer = 
            DNAUtil.canonicalToDir(src_canonical, link_dir.charAt(0));
        dest_kmer = dest_kmer.subSequence(1,  dest_kmer.size());
        dest_kmer.add(last_base);
  
        link_dir += DNAUtil.canonicaldir(dest_kmer);
  
        node.setLinkDir(link_dir);
  
        node.setLastBase(ByteBuffer.wrap(
            last_base.toPackedBytes(), 0, last_base.numPackedBytes()));                
        node.setTag("read_" + eindex);
        // TODO(jlewi): We should pick values for the state and chunk
        // that would make it easy to verify that the reducer properly 
        // uses the state and chunk.
        node.setState(ReadState.MIDDLE);
        node.setChunk(0);        
        input_edges.add(node);      
      }
      return new ReduceTest(uncompressed, input_edges, K);
    } 
  }
  
  // TODO(jlewi): The testing for the reduce phase is quite limited.
  // currently all we check is that the list of edges added is correct.
  // We generate a bunch of edges all with the same source KMer.
  // We then check that the outcome of the reduce phase is a GraphNode
  // with the edges set correctly.
  @Test
  public void TestReduce() {
    final int NTRIALS = 10;
    final int MAX_K = 30;    
    final int MIN_K = 2;
    final int MAX_EDGES = 10;

    ReduceTest reduce_data;
    Alphabet alphabet = DNAAlphabetFactory.create();
    for (int trial = 0; trial < NTRIALS; trial++) {
      reduce_data = ReduceTest.RandomTest(MIN_K, MAX_K, MAX_EDGES);
            
      JobConf job = new JobConf(BuildGraphAvro.BuildGraphReducer.class);
      ContrailConfig.PREPROCESS_SUFFIX = 0;
      ContrailConfig.TEST_MODE = true;
      ContrailConfig.K = reduce_data.K;
      ContrailConfig.initializeConfiguration(job);

      BuildGraphAvro.BuildGraphReducer reducer = 
          new BuildGraphAvro.BuildGraphReducer();
      reducer.configure(job);

      AvroCollectorMock<Pair<ByteBuffer, GraphNodeData>> collector_mock = 
          new AvroCollectorMock<Pair<ByteBuffer, GraphNodeData>>();

      ReporterMock reporter_mock = new ReporterMock();
      Reporter reporter = (Reporter) reporter_mock;

      Sequence src_canonical = 
          DNAUtil.canonicalseq(reduce_data.getSrcSequence());
      try {        
        reducer.reduce(ByteBuffer.wrap(src_canonical.toPackedBytes()),
            reduce_data.getInputEdges(), collector_mock, reporter);
      }
      catch (IOException exception){
        fail("IOException occured in reduce: " + exception.getMessage());
      }

      // Check the output of the reducer.
      {
        List<KMerEdge> input_edges = reduce_data.getInputEdges();
        assertEquals(collector_mock.data.size(), 1);

        Pair<ByteBuffer, GraphNodeData> out_pair = collector_mock.data.get(0);

        // Check the output key is the sequence specified in reduce_data.
        Sequence out_key = new Sequence(alphabet);
        out_key.readPackedBytes(out_pair.key().array(), (int)ContrailConfig.K);
        assertEquals(out_key, src_canonical);

        GraphNodeData graph_data = out_pair.value();

        // edges_to_find keeps track of all input KMerEdge's which we haven't
        // matched to the output yet. We use this to verify that all KMerEdges
        // are accounted for in the reducer output.
        ArrayList<KMerEdge> edges_to_find = new ArrayList<KMerEdge>();
        edges_to_find.addAll(input_edges);

        // Check the edges in the output node are correct. For each edge in the
        // output we make sure there is a KMerEdge in the input that would have
        // generated that edge. We also count the number of edges to make sure 
        // we don't have extra edges. As we go we delete KMerEdges so we don't 
        // count any twice.
        for (Iterator<EdgeDestNode> it_dest = graph_data.getDestNodes().iterator();
            it_dest.hasNext();) {

          EdgeDestNode dest_node = it_dest.next();

          for (Iterator<DestForLinkDir> it_instances = 
               dest_node.getLinkDirs().iterator(); it_instances.hasNext();) {
            DestForLinkDir link_info = it_instances.next();

            // FoundKMerEdge will search for the edge in edges_to_find and 
            // remove it if its found.
            assertTrue(foundKMerEdge(
                src_canonical, dest_node, link_info,  edges_to_find));
          } // for it_instances
        } // for edge_dir 
        // Check there were no edges that didn't match.
        assertEquals(edges_to_find.size(), 0);
      }
    } // for trial
  } // TestReduce
}

