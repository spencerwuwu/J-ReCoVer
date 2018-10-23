// https://searchcode.com/api/result/118158631/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.screenscraper.datamanager.sql.util;

import com.screenscraper.datamanager.*;
import com.screenscraper.datamanager.sql.SqlDataManager;
import com.screenscraper.datamanager.sql.SqlDatabaseSchema;
import com.screenscraper.datamanager.sql.SqlTableSchema;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class that is simplifies looking up primary keys based on a number of constraints.
 * It is a wrapper that simplifies the SqlLookup class to return a List of matching primary keys
 * for the specified table.  This class is very similar to the SqlDuplicateFilter class, however, it is
 * designed so the developer can easily retrieve data that may be required in the scraping process.
 * For example if the developer is trying to determine if he needs to scrape a certain page from a
 * list of search results he could build an SqlPkLookup based on the information on the search results
 * page to determine if the data has already been scraped.  The developer can use this information
 * to reduce the number of requests on websites with redundant data.  The SqlDuplicateFilter, on the other hand,
 * would require scraping all the data then when DataManager.flush is called it would perform the lookup
 * and filter/supplement duplicate records.
 *
 * Example usage:
 * //look up a real estate listing based its brokers name and the listing address
 * SqlPkLookup lookup = new SqlPkLookup(dm, "listings");
 * lookup.addConstraint("brokers", "lastname", dataRecord.get("LASTNAME"));
 * lookup.addConstraint("brokers", "firstname", dataRecord.get("FIRSTNAME"));
 * lookup.addConstraint("listings", "address", dataRecord.get("ADDRESS"));
 * lookup.addConstraint("listings", "address2", dataRecord.get("ADDRESS2"));
 * lookup.addConstraint("listings", "zipcode", dataRecord.get("ZIPCODE"));
 * List matches = lookup.execute();
 * if(matches.size()==0)
 *  session.scrapeFile("Details");
 *
 * @author ryan
 */
public class SqlPkLookup{

    private DatabaseSchema schemas;
    private SqlDataManager dm;
    private Log log = LogFactory.getLog(SqlPkLookup.class);
    private SqlLookup lookup;

    /**
     * @param dm
     * @param table
     */
    public SqlPkLookup(SqlDataManager dm,String table)
    {
        schemas = new DatabaseSchema();
        for(RelationalSchema s: dm.getDatabaseSchema().getRelationalSchemas()){
            schemas.addRelationalSchema(s);
        }
        this.dm=dm;
        if(dm.getDatabaseSchema().getAttr(SqlDatabaseSchema.DatabaseSchemaAttr.Vendor).contains("Microsoft")){
            lookup = new MsSqlLookup(schemas);
        }else{
            lookup = new SqlLookup(schemas);
        }
        lookup.addSelectColumns(table, ((SqlTableSchema)schemas.getRelationalSchema(table)).getPrimaryKeyColumns());
    }

    private boolean isValidColumn(String table, String column)
    {
        RelationalSchema s = schemas.getRelationalSchema(table);
        if(s==null)
            return false;
        if(!s.getColumns().contains(column))
            return false;
        return true;
    }

    /**
     * Limit the number of results to return.  Setting will greatly improve the efficiency
     * of the lookup.
     * @param limit
     */
    public void setLimit(int limit)
    {
        lookup.setLimit(limit);
    }

    /**
     * add a constraint to the lookup.  This translates into a "WHERE" in your query.
     * @param table
     * @param column
     */
    public void addConstraint(String table, String column, Object value) throws UnsupportedEncodingException
    {
        lookup.addConstraint(table, column, value);
    }

    /**
     * exete the lookup
     * @return a List of results that matches.  Each individual result is a Map
     * of the pk column name and its corresponding value.
     * @throws UnsupportedEncodingException
     */
    public List<Map<String,DataObject>> execute() throws UnsupportedEncodingException {
        //log.debug("Running filter "+ filterName);
        Connection con = null;

        List<Map<String,DataObject>> returnData = new ArrayList<Map<String,DataObject>>();
        try {
            con = dm.getConnection();
            if (con == null || con.isClosed()) {
                throw new IllegalStateException("Sql connection invalid");
            }



            DmPreparedStatement ps = lookup.getPreparedStatement(con);
            //log.debug("running query " + ps.getSql() + "\n" + ps.getData());
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                Map values = QueryUtils.saveRowAsMap(rs);
                returnData.add(values);
            }
            ps.close();
         } catch (SQLException ex) {
            log.error("SqlFilterError", ex);
        }
        finally
        {
            try{con.close();}
            catch(Exception e){}
        }
        return returnData;
    }
}
