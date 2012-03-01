
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import plugins.TabularDataReader;


public final class TabularDataConverter
{
    TabularDataReader source;
    String dest;
    String tablename;
    
    public TabularDataConverter(TabularDataReader source) throws ClassNotFoundException {
        this(source, "");
    }
    
    public TabularDataConverter(TabularDataReader source, String dest) throws ClassNotFoundException {
        // load the Sqlite JDBC driver
        Class.forName("org.sqlite.JDBC");
        
        setSource(source);
        setDestination(dest);
    }
    
    /**
     * Set the source data for this TabularDataConverter.  The source
     * TabularDataReader must have a data source open and ready to access.
     * 
     * @param source The data source from which to read.
     */
    public final void setSource(TabularDataReader source) {
        this.source = source;
        tablename = "";
    }
    
    /**
     * The SQLite JDBC connection string to use for the destination.
     * 
     * @param dest A valid JDBC SQLite connection string.
     */
    public final void setDestination(String dest) {
        this.dest = dest;
    }
    
    public String getDestination() {
        return dest;
    }
    
    /**
     * Specify a table name to use for storing the converted data in the
     * destination database.  This will only apply to the first table in a data
     * source, and is intended for data sources that don't explicitly provide a
     * table name, such as CSV files.
     * 
     * @param tablename A valid SQLite table name.
     */
    public void setTableName(String tablename) {
        this.tablename = tablename;
    }
    
    public String getTableName() {
        return tablename;
    }
    
    /**
     * Reads the source data and converts it to tables in a Sqlite database.
     * Uses the database connection string provided in the constructor or in a
     * call to setDestination().  The name to use for the FIRST table in the
     * database can be specified by calling setTableName().  Otherwise, and for
     * all remaining tables, the table names are taken from the source data
     * reader.  Any tables that already exist in the destination database will
     * be DROPPED.  Destination tables will have columns matching the names and
     * number of elements in the first row of each table in the source data.
     * All rows from the source are copied to the new table.
     * 
     * @throws SQLException 
     */
    public void convert() throws SQLException {
        int tablecnt = 0;
        String tname;
        
        Connection conn = DriverManager.getConnection(dest);

        while (source.hasNextTable()) {
            source.moveToNextTable();
            tablecnt++;

            // If the user supplied a name for the first table in the data
            // source, use it.  Otherwise, take the table name from the data
            // source.
            if ((tablecnt == 1) && (tablename != ""))
                tname = tablename;
            else
                tname = source.getCurrentTableName();

            buildTable(conn, tname);
        }
        
        conn.close();
    }

    /**
     * Creates a single table in the destination database using the current
     * table in the data source.  If the specified table name already exists in
     * the database, IT IS DROPPED.  A new table with columns matching the names
     * and number of elements in the first row of the source data is created,
     * and all rows from the source are copied to the new table.
     *
     * @param conn A valid connection to a destinatino database.
     * @param tname The name to use for the table in the destination database.
     *
     * @throws SQLException
     */
    private void buildTable(Connection conn, String tname) throws SQLException {
        int colcnt, cnt;
        Statement stmt = conn.createStatement();

        // if this table exists, drop it
        stmt.executeUpdate("DROP TABLE IF EXISTS " + tname);

        // set up the table definition query
        String query = "CREATE TABLE \"" + tname + "\" (";
        colcnt = 0;
        for (String colname : source.tableGetNextRow()) {
            if (colcnt++ > 0)
                query += ", ";
            query += "\"" + colname + "\"";
        }
        query += ")";
        //System.out.println(query);

        // create the table
        stmt.executeUpdate(query);
        stmt.close();

        // create a prepared statement for insert queries
        query = "INSERT INTO " + tname + " VALUES (";
        for (cnt = 0; cnt < colcnt; cnt++) {
            if (cnt > 0)
                query += ", ";
            query += "?";
        }
        query += ")";
        //System.out.println(query);
        PreparedStatement insstmt = conn.prepareStatement(query);

        // populate the table with the source data
        while (source.tableHasNextRow()) {
            cnt = 0;
            for (String dataval : source.tableGetNextRow()) {
                insstmt.setString(++cnt, dataval);
            }

            // Supply blank strings for any missing columns.  This does not appear
            // to be strictly necessary, at least with the Sqlite driver we're
            // using, but it is included as insurance against future changes.
            while (cnt < colcnt) {
                insstmt.setString(++cnt, "");
            }

            // add the row to the database
            insstmt.executeUpdate();
        }

        insstmt.close();
    }
}
