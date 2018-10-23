// https://searchcode.com/api/result/55077078/

package votorola.a.trust; // Copyright 2008, 2010, Michael Allan.  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Votorola Software"), to deal in the Votorola Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicence, and/or sell copies of the Votorola Software, and to permit persons to whom the Votorola Software is furnished to do so, subject to the following conditions: The preceding copyright notice and this permission notice shall be included in all copies or substantial portions of the Votorola Software. THE VOTOROLA SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE VOTOROLA SOFTWARE OR THE USE OR OTHER DEALINGS IN THE VOTOROLA SOFTWARE.

import java.io.*;
import java.sql.*;
import java.util.*;
import votorola._.*;
import votorola.a.*;
import votorola.g.lang.*;
import votorola.g.sql.*;


/** A committable node in a network trace.
  */
public @ThreadRestricted( "single writer, readers touch" ) class ListNodeC implements ListNode
{

    // cf. a/poll/CountNode

    private static final long serialVersionUID = 0L;



    /** Creates a ListNodeC, with all variable items at default values.
      *
      *     @see #table()
      *     @see #userName()
      */
    ListNodeC( Table table, String userName )
    {
        if( table == null || userName == null ) throw new NullPointerException(); // fail fast

        this.table = table;
        this.userName = userName;
        trustEdgeCountArray = new int[0];
        isChanged = true;
    }



    /** Constructs a ListNodeC.
      *
      *     @see #table()
      *     @see #userName()
      *     @see #getRegistration()
      *     @see #getGeohandle()
      *     @see #attachTrustEdge(int)
      */
    public ListNodeC( Table table, String userName,
      String registration, String geohandle, int[] trustEdgeCountArray )
    {
        if( table == null || userName == null ) throw new NullPointerException(); // fail fast

        this.table = table;
        this.userName = userName;

        // Adding fields here?  Increment serialVersionUID and NetworkTrace.serialVersionUID.
        this.registration = registration;
        this.geohandle = geohandle;
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


        /** Detaches a trust edge from this node.  The change may affect the {@linkplain
          * #trustLevel() trust level} of this node, but is not automatically propagated
          * to other nodes.
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



    /** The table in which this node is stored.
      *
      *     @return table, or null if this is a deserialized, read-only node
      */
    public final Table table() { return table; }


        private final transient Table table; // FIX get rid of this field | or call it tableOrNull() so client is forewarned



   // - L i s t - N o d e ----------------------------------------------------------------


    /** @see #setRegistration(String)
      */
    public final String getRegistration() { return registration; }


        private String registration = "";


        /** @see #getRegistration()
          */
        public final void setRegistration( String newRegistration )
        {
            if( newRegistration == null ) throw new NullPointerException(); // fail fast

            if( newRegistration.equals( registration )) return;

            registration = newRegistration;
            isChanged = true;
        }



    /** @see #setGeohandle(String)
      */
    public final String getGeohandle() { return geohandle; }


        private String geohandle;


        /** @see #getGeohandle()
          */
        public final void setGeohandle( String newGeohandle )
        {
            if( ObjectX.nullEquals( newGeohandle, geohandle )) return;

            geohandle = newGeohandle;
            isChanged = true;
        }



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



    public final String userName() { return userName; }


        private final String userName;



   // - O b j e c t ----------------------------------------------------------------------


    /** Returns the {@linkplain #userName() user name}.
      */
    public @Override final String toString() { return userName(); }



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
      * a {@linkplain NetworkTrace network trace}.
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
                      + " (userName character varying PRIMARY KEY,"
                      +  " registration character varying NOT NULL,"
                      +  " geohandle character varying,"
                      +  " trustEdgeCountArray character varying)" );

                    // Changing table structure?  Then also increment NetworkTrace.serialVersionUID.

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
        public ListNodeC get( final String userName ) throws SQLException
        {
            if( userName == null ) throw new NullPointerException(); // fail fast

            final String key = statementKeyBase + "get";
            synchronized( database )
            {
                PreparedStatement s = database.statementCache().get( key );
                if( s == null )
                {
                    s = database.connection().prepareStatement(
                      "SELECT registration,geohandle,trustEdgeCountArray"
                      + " FROM \"" + SCHEMA_NAME + "\".\"" + tableName + "\""
                      + " WHERE userName = ?" );
                    database.statementCache().put( key, s );
                }
                s.setString( 1, userName );
                final ResultSet r = s.executeQuery();
                try
                {
                    if( !r.next() ) return null;

                    return new ListNodeC( Table.this, userName,
                      r.getString(1), r.getString(2), ArrayX.stringToInts(r.getString(3)) );
                }
                finally{ r.close(); }
            }
        }



        /** Retrieves a node from this table; or, if none is stored, a default node.
          *
          *     @return ListNodeC as stored in the table; or, if none is stored,
          *         a {@linkplain ListNodeIC ListNodeIC} with default values
          */
        public ListNodeC getOrCreate( final String userName ) throws SQLException
        {
            ListNodeC listNode = get( userName );
            if( listNode == null ) listNode = new ListNodeIC( Table.this, userName );

            return listNode;
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
                          + " SET registration = ?, geohandle = ?, trustEdgeCountArray = ?"
                          + " WHERE userName = ?" );
                        database.statementCache().put( key, s );
                    }
                    s.setString( 1, node.getRegistration() );
                    s.setString( 2, node.getGeohandle() );
                    s.setString( 3, ArrayX.intsToString( trustEdgeCountArray ));
                    s.setString( 4, node.userName() );
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
                          + " (userName, registration, geohandle, trustEdgeCountArray)"
                          + " VALUES (?, ?, ?, ?)" );
                        database.statementCache().put( key, s );
                    }
                    s.setString( 1, node.userName() );
                    s.setString( 2, node.getRegistration() );
                    s.setString( 3, node.getGeohandle() );
                    s.setString( 4, ArrayX.intsToString( trustEdgeCountArray ));
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
                         "SELECT userName, registration, geohandle, trustEdgeCountArray"
                      + " FROM \"" + SCHEMA_NAME + "\".\"" + tableName + "\"" );
                    database.statementCache().put( key, s );
                }
                final ResultSet r = s.executeQuery();
                try
                {
                    while( r.next() )
                    {
                        final ListNodeC node = new ListNodeC( Table.this, r.getString(1),
                          r.getString(2), r.getString(3), ArrayX.stringToInts(r.getString(4)) );
                        runner.run( node );
                    }
                }
                finally{ r.close(); }
            }
        }



        /** The name of the table's schema.
          */
        public static final String SCHEMA_NAME = "out_trace";



        /** The name of this table.
          */
        public String tableName() { return tableName; }


            private final String tableName;



    }



//// P r i v a t e ///////////////////////////////////////////////////////////////////////


    private boolean isChanged;



}

