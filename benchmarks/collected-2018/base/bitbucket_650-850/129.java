// https://searchcode.com/api/result/55077110/

package votorola.a.register; // Copyright 2008, 2010 Michael Allan.  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Votorola Software"), to deal in the Votorola Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicence, and/or sell copies of the Votorola Software, and to permit persons to whom the Votorola Software is furnished to do so, subject to the following conditions: The preceding copyright notice and this permission notice shall be included in all copies or substantial portions of the Votorola Software. THE VOTOROLA SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE VOTOROLA SOFTWARE OR THE USE OR OTHER DEALINGS IN THE VOTOROLA SOFTWARE.

import java.io.*;
import java.sql.*;
import java.util.*;
import votorola._.*;
import votorola.a.*;
import votorola.g.lang.*;
import votorola.g.sql.*;


/** A committable node in a compiled voter list.
  */
public @ThreadRestricted( "single writer, readers touch" ) class ListNodeC implements ListNode
{

    // cf. a/poll/CountNode

    private static final long serialVersionUID = 2L;



    /** Creates a ListNodeC, with all variable items at default values.
      *
      *     @param table per {@linkplain #table() table}()
      *     @param voterEmail per {@linkplain #voterEmail() voterEmail}()
      */
    ListNodeC( Table table, String voterEmail )
    {
        if( table == null || voterEmail == null ) throw new NullPointerException(); // fail fast

        this.table = table;
        this.voterEmail = voterEmail;
        trustEdgeCountArray = new int[0];
        isChanged = true;
    }



    /** Constructs a ListNodeC.
      *
      *     @param table per {@linkplain #table() table}()
      *     @param voterEmail per {@linkplain #voterEmail() voterEmail}()
      *     @param bar per {@linkplain #getBar() getBar}()
      *     @param doubterCount per {@linkplain #doubterCount() doubterCount}()
      *     @param neighbourhoodPath per {@linkplain #getNeighbourhoodPath() getNeighbourhoodPath}()
      *     @param residence per {@linkplain #getResidence() getResidence}()
      *     @param trustEdgeCountArray defining
      *       {@linkplain #attachTrustEdge(int) attached trust edges}()
      */
    public ListNodeC( Table table, String voterEmail, String bar, int doubterCount,
      String neighbourhoodPath, String residence, int[] trustEdgeCountArray )
    {
        if( table == null || voterEmail == null ) throw new NullPointerException(); // fail fast

        this.table = table;
        this.voterEmail = voterEmail;

        // Adding fields here?  Increment serialVersionUID and VoterList.serialVersionUID.
        this.bar = bar;
        this.doubterCount = doubterCount;
        this.neighbourhoodPath = neighbourhoodPath;
        this.residence = residence;
        this.trustEdgeCountArray = trustEdgeCountArray;
    }


   // ------------------------------------------------------------------------------------


    /** Attaches a trust edge to this node.  The change may affect the trust level of this
      * node, but is not automatically propagated to other nodes.
      *
      *     @param trustLevel0 trust level of source node,
      *         or Integer.MAX_VALUE for infinity
      *
      *     @see #detachTrustEdge(int)
      *     @see #trustLevel()
      */
    public final void attachTrustEdge( int trustLevel0 )
    {
        if( trustLevel0 == 0 ) { assert false: "attach only from traced source"; return; }

        if( trustLevel0 == Integer.MAX_VALUE ) trustLevel0 = 0;
        if( trustLevel0 + 1 > trustEdgeCountArray.length )
        {
            trustEdgeCountArray = Arrays.copyOf( trustEdgeCountArray,
              trustLevel0 + 1 + GROWTH_PADDING );
        }
        ++trustEdgeCountArray[trustLevel0];
        isChanged = true;
    }


        /** Count of attached edges, by source trust level.  Index 0 is reserved
          * for the level of infinity (from the root node).
          */
        private int[] trustEdgeCountArray;


        private static final int GROWTH_PADDING = 0; // none, because it can grow once only per trace (because cycles are impossible), and is never subject to more than one trace (because nodes are not cached)


        /** Detaches a trust edge from this node.  The change may affect the
          * {@linkplain #trustLevel() trust level} of this node, but is not automatically
          * propagated to other nodes.
          *
          *     @param trustLevel0 trust level of source node
          *
          *     @see #attachTrustEdge(int)
          */
        public final void detachTrustEdge( int trustLevel0 )
        {
            if( trustLevel0 == 0 ) { assert false: "detach only when attached"; return; }

            if( trustLevel0 == Integer.MAX_VALUE ) trustLevel0 = 0;
            if( trustEdgeCountArray[trustLevel0] <= 0 ) { assert false: "detach never causes negative trust"; return; }

            --trustEdgeCountArray[trustLevel0]; // no need to shrink array, it's done in commit()
            isChanged = true;
        }



    /** Writes this node to the table, if it has uncommitted changes.
      */
    public final void commit() throws SQLException
    {
        if( !isChanged ) return;

        final int[] trustEdgeCountArrayTrimmed;
        {
            int length = trustEdgeCountArray.length; // same as before, till proven otherwise
            for(; length > 0; --length )
            {
                if( trustEdgeCountArray[length-1] > 0 ) break; // found a positive count
            }
            if( length == trustEdgeCountArray.length )
            {
                trustEdgeCountArrayTrimmed = trustEdgeCountArray;
            }
            else trustEdgeCountArrayTrimmed = Arrays.copyOf( trustEdgeCountArray, length );
        }

        table.put( ListNodeC.this, trustEdgeCountArrayTrimmed );
        isChanged = false;
    }



    /** Returns an un-localized bar, complaining that the voter's residential address
      * cannot be parsed to a local neighbourhood.
      */
    static String defaultRootNeighbourhoodBar( String voterEmail )
    {
        return "Neighbourhood unknown.  The residential address of voter " // cf. default register-run.js. FIX, localize this, somehow, or make it locally configurable - at least when it is used in ListNode0
          + voterEmail + " cannot be parsed to a local neighbourhood.";

    }



    /** The table in which this node is stored.
      *
      *     @return table, or null if this is a deserialized, read-only node
      */
    public final Table table() { return table; }


        private final transient Table table; // FIX get rid of this field | or call it tableOrNull() so client is forewarned



   // - L i s t - N o d e ----------------------------------------------------------------


    /** @see #incrementDoubterCount()
      */
    public int doubterCount() { return doubterCount; }


        private int doubterCount;


        /** Increments the doubter count.
          *
          *     @see #doubterCount()
          */
        final void incrementDoubterCount()
        {
            ++doubterCount;
            isChanged = true;
        }



    /** @see #setBar(String)
      */
    public final String getBar()
    {
        return bar == null && getNeighbourhoodPath().length() == 0?
          defaultRootNeighbourhoodBar( voterEmail ): bar;
    }


        private String bar;


        /** Sets a list bar against the voter.
          *
          *     @see #getBar()
          */
        public final void setBar( String newBar )
        {
            if( ObjectX.nullEquals( newBar, bar )) return;

            bar = newBar;
            isChanged = true;
        }



    /** @see #setNeighbourhoodPath(String)
      */
    public final String getNeighbourhoodPath() { return neighbourhoodPath; }


        private String neighbourhoodPath = "";


        /** @see #getNeighbourhoodPath()
          */
        public final void setNeighbourhoodPath( String newNeighbourhoodPath )
        {
            if( newNeighbourhoodPath == null ) throw new NullPointerException(); // fail fast

            if( newNeighbourhoodPath.equals( neighbourhoodPath )) return;

            neighbourhoodPath = newNeighbourhoodPath;
            isChanged = true;
        }



    /** @see #setResidence(String)
      */
    public final String getResidence() { return residence; }


        private String residence;


        /** @see #getResidence()
          */
        public final void setResidence( String newResidence ) // FIX, by adding setFromRegistration(), and having *that* call setResidence() - so easier to add new attributes without breaking code
        {
            if( ObjectX.nullEquals( newResidence, residence )) return;

            residence = newResidence;
            isChanged = true;
        }



    public final String leafRegisterPath() { return "."; } // super-register traces not yet supported, only leaves



    public final int primaryTrustEdgeCount()
    {
        int count = 0;
        if( trustEdgeCountArray.length > 0 ) count = trustEdgeCountArray[0];
        return count;
    }



    /** @see #attachTrustEdge(int)
      */
    public final int trustLevel()
    {
        int level = primaryTrustEdgeCount(); // sources of infinite trust
        for( int l = trustEdgeCountArray.length - 1; l > level; --l ) // only sources at levels greater than the current level can contribute an increase
        {
            level = Math.min( level + trustEdgeCountArray[l], l ); // cannot reduce level, because l > level (per loop guard)
        }
        return level;
    }



    public final String voterEmail() { return voterEmail; }


        private final String voterEmail;



   // - O b j e c t ----------------------------------------------------------------------


    /** Returns the {@linkplain #voterEmail() voter email address}.
      */
    public @Override final String toString() { return voterEmail(); }



   // ====================================================================================


    /** A routine that runs in the context of a list node.
      */
    public interface Runner
    {

       // - L i s t - N o d e - C . R u n n e r ------------------------------------------


        /** Runs this routine in the context of the specified list node.
          */
        public void run( final ListNodeC node );

    }



   // ====================================================================================


    /** The relational store of voters, that (in part) backs
      * a {@linkplain VoterList compiled voter list}.
      *
      *     @see votorola.a.register.trust.TrustEdge.Table
      */
    public static @ThreadSafe final class Table
    {

        // cf. a/poll/CountTable


        /** Constructs a Table.
          *
          *     @param readyDirectory per {@linkplain #readyDirectory() readyDirectory}()
          *     @param database per {@linkplain #database() database}()
          */
        public Table( final ReadyDirectory readyDirectory, final Database database )
          throws IOException, SQLException
        {
            this.readyDirectory = readyDirectory;
            this.database = database;

            synchronized( database ) { database.ensureSchema( SCHEMA_NAME ); }
            final String snapSuffix = OutputStore.suffix(
              readyDirectory.snapDirectory().getName() );
            if( !OutputStore.isY4MDS( snapSuffix )) throw new VotorolaRuntimeException( "improperly suffixed snap directory parent of ready directory: " + readyDirectory );

            tableName = snapSuffix.substring(1) + OutputStore.SUFFIX_DELIMITER
              + "list_node" + OutputStore.suffix(readyDirectory.getName());
            statementKeyBase = getClass().getName() + ":" + SCHEMA_NAME + "/" + tableName + ".";
        }



        private final String statementKeyBase;



       // --------------------------------------------------------------------------------


        /** Creates this table in the database.
          */
        void create() throws SQLException
        {
            final String key = statementKeyBase + "create";
            synchronized( database )
            {
                PreparedStatement s = database.statementCache().get( key );
                if( s == null )
                {
                    s = database.connection().prepareStatement(
                     "CREATE TABLE \"" + SCHEMA_NAME + "\".\"" + tableName + "\""
                      + " (voterEmail character varying PRIMARY KEY,"
                      +  " bar character varying,"
                      +  " doubterCount integer NOT NULL,"
                      +  " neighbourhoodPath character varying NOT NULL,"
                      +  " residence character varying,"
                      +  " trustEdgeCountArray character varying)" );

                    // Changing table structure?  Then also increment VoterList.serialVersionUID.

                    database.statementCache().put( key, s );
                }
                s.execute();
            }
        }



        /** Returns the database in which this table is stored.
          */
        Database database() { return database; }


            private final Database database;



        /** Drops this table from the database.
          */
        void drop() throws SQLException
        {
            final String key = statementKeyBase + "drop";
            synchronized( database )
            {
                PreparedStatement s = database.statementCache().get( key );
                if( s == null )
                {
                    s = database.connection().prepareStatement(
                     "DROP TABLE \"" + SCHEMA_NAME + "\".\"" + tableName + "\"" );
                    database.statementCache().put( key, s );
                }
                s.execute();
            }
        }



        /** Returns true if this table exists in the database; false otherwise.
          */
        boolean exists() throws SQLException
        {
            final String key = statementKeyBase + "exists";
            synchronized( database )
            {
                PreparedStatement s = database.statementCache().get( key );
                if( s == null )
                {
                    s = database.connection().prepareStatement(
                     "SELECT * FROM \"" + SCHEMA_NAME + "\".\"" + tableName + "\"" );
                    s.setMaxRows( 1 );
                    database.statementCache().put( key, s );
                }
                try
                {
                    s.execute();
                }
                catch( SQLException x )
                {
                    final String sqlState = x.getSQLState();
                    if( "3F000".equals( sqlState )) return false; // 3F000 = [missing schema]

                    if( "42P01".equals( sqlState )) return false; // 42P01 = UNDEFINED TABLE

                    throw x;
                }
            }
            return true;
        }



        /** Retrieves a node from this table.
          *
          *     @return node as stored in the table; or null, if none is stored
          */
        public ListNodeC get( final String voterEmail ) throws SQLException
        {
            if( voterEmail == null ) throw new NullPointerException(); // fail fast

            final String key = statementKeyBase + "get";
            synchronized( database )
            {
                PreparedStatement s = database.statementCache().get( key );
                if( s == null )
                {
                    s = database.connection().prepareStatement(
                      "SELECT bar,doubterCount,neighbourhoodPath,residence,trustEdgeCountArray"
                      + " FROM \"" + SCHEMA_NAME + "\".\"" + tableName + "\""
                      + " WHERE voterEmail = ?" );
                    database.statementCache().put( key, s );
                }
                s.setString( 1, voterEmail );
                final ResultSet r = s.executeQuery();
                try
                {
                    if( !r.next() ) return null;

                    return new ListNodeC( Table.this, voterEmail, r.getString(1), r.getInt(2),
                      r.getString(3), r.getString(4), ArrayX.stringToInts(r.getString(5)) );
                }
                finally{ r.close(); }
            }
        }



        /** Retrieves a node from this table; or, if none is stored, a default node.
          *
          *     @return ListNodeC as stored in the table; or, if none is stored,
          *         a {@linkplain ListNodeIC ListNodeIC} with default values
          */
        public ListNodeC getOrCreate( final String voterEmail ) throws SQLException
        {
            ListNodeC listNode = get( voterEmail );
            if( listNode == null ) listNode = new ListNodeIC( Table.this, voterEmail );

            return listNode;
        }



        /** Retrieves a list of all unbarred nodes in the same leaf neighbourhood.  The
          * list is pre-sorted by residential and email addresses.
          */
        final java.util.List<ListNode> listUnbarredNeighbours( final String neighbourhoodPath )
          throws SQLException
        {
            final String key = statementKeyBase + "getNeighbours";
            synchronized( database )
            {
                PreparedStatement s = database.statementCache().get( key );
                if( s == null )
                {
                    s = database.connection().prepareStatement(
                      "SELECT voterEmail,bar,doubterCount,residence,trustEdgeCountArray"
                      + " FROM \"" + SCHEMA_NAME + "\".\"" + tableName + "\""
                      + " WHERE bar IS NULL AND neighbourhoodPath = ?"
                      + " ORDER BY residence, voterEmail" );
                    database.statementCache().put( key, s );
                }
                s.setString( 1, neighbourhoodPath ); // child pattern, exactly one character longer
                final ResultSet r = s.executeQuery();
                final ArrayList<ListNode> nodeList = new ArrayList<ListNode>();
                try
                {
                    while( r.next() )
                    {
                        nodeList.add( new ListNodeC( Table.this, r.getString(1), r.getString(2),
                          r.getInt(3), neighbourhoodPath, r.getString(4),
                          ArrayX.stringToInts(r.getString(5)) ));
                    }
                }
                finally{ r.close(); }
                return nodeList;
            }
        }



        /** Stores a node.
          */
        void put( final ListNodeC node, final int[] trustEdgeCountArray ) throws SQLException
        {
            synchronized( database )
            {
                {
                    final String key = statementKeyBase + "putU";
                    PreparedStatement s = database.statementCache().get( key );
                    if( s == null )
                    {
                        s = database.connection().prepareStatement(
                             "UPDATE \"" + SCHEMA_NAME + "\".\"" + tableName + "\""
                          + " SET bar = ?, doubterCount = ?, neighbourhoodPath = ?,"
                          +   " residence = ?, trustEdgeCountArray = ?"
                          + " WHERE voterEmail = ?" );
                        database.statementCache().put( key, s );
                    }
                    s.setString( 1, node.getBar() );
                    s.setInt( 2, node.doubterCount() );
                    s.setString( 3, node.getNeighbourhoodPath() );
                    s.setString( 4, node.getResidence() );
                    s.setString( 5, ArrayX.intsToString( trustEdgeCountArray ));
                    s.setString( 6, node.voterEmail() );
                    final int updatedRows = s.executeUpdate();
                    if( updatedRows > 0 ) { assert updatedRows == 1; return; }
                }
                {
                    final String key = statementKeyBase + "putI";
                    PreparedStatement s = database.statementCache().get( key );
                    if( s == null )
                    {
                        s = database.connection().prepareStatement(
                             "INSERT INTO \"" + SCHEMA_NAME + "\".\"" + tableName + "\""
                          + " (voterEmail, bar, doubterCount, neighbourhoodPath, residence,"
                          +  " trustEdgeCountArray)"
                          + " VALUES (?, ?, ?, ?, ?, ?)" );
                        database.statementCache().put( key, s );
                    }
                    s.setString( 1, node.voterEmail() );
                    s.setString( 2, node.getBar() );
                    s.setInt( 3, node.doubterCount() );
                    s.setString( 4, node.getNeighbourhoodPath() );
                    s.setString( 5, node.getResidence() );
                    s.setString( 6, ArrayX.intsToString( trustEdgeCountArray ));
                    s.executeUpdate();
                }
            }
        }



        /** The file-based counterpart to this table.
          */
        ReadyDirectory readyDirectory() { return readyDirectory; }


            private final ReadyDirectory readyDirectory; // final after init



        /** Passes all nodes of this table through the specified runner.
          */
        void run( final ListNodeC.Runner runner ) throws SQLException
        {
            final String key = statementKeyBase + "run";
            synchronized( database )
            {
                PreparedStatement s = database.statementCache().get( key );
                if( s == null )
                {
                    s = database.connection().prepareStatement(
                         "SELECT voterEmail, bar, doubterCount, neighbourhoodPath, residence,"
                      +   " trustEdgeCountArray"
                      + " FROM \"" + SCHEMA_NAME + "\".\"" + tableName + "\"" );
                    database.statementCache().put( key, s );
                }
                final ResultSet r = s.executeQuery();
                try
                {
                    while( r.next() )
                    {
                        final ListNodeC node = new ListNodeC( Table.this, r.getString(1),
                          r.getString(2), r.getInt(3), r.getString(4), r.getString(5),
                          ArrayX.stringToInts(r.getString(6)) );
                        runner.run( node );
                    }
                }
                finally{ r.close(); }
            }
        }



        /** The name of the table's schema.
          */
        public static final String SCHEMA_NAME = "out_list";



        /** The name of this table.
          */
        public String tableName() { return tableName; }


            private final String tableName;



    }



//// P r i v a t e ///////////////////////////////////////////////////////////////////////


    private boolean isChanged;



}

