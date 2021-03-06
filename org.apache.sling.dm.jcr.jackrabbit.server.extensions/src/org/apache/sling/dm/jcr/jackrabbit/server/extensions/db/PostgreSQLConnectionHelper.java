package org.apache.sling.dm.jcr.jackrabbit.server.extensions.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.DbUtility;



/**
 * This class provides convenience methods to execute SQL statements. They can be either executed in isolation
 * or within the context of a JDBC transaction; the so-called <i>batch mode</i> (use the {@link #startBatch()}
 * and {@link #endBatch(boolean)} methods for this).
 *
 * <p>
 *
 * This class contains logic to retry execution of SQL statements. If this helper is <i>not</i> in batch mode
 * and if a statement fails due to an {@code SQLException}, then it is retried. If the {@code block} argument
 * of the constructor call was {@code false} then it is retried only once. Otherwise the statement is retried
 * until either it succeeds or the thread is interrupted. This clearly assumes that the only cause of {@code
 * SQLExceptions} is faulty {@code Connections} which are restored eventually. <br/> <strong>Note</strong>:
 * This retry logic only applies to the following methods:
 * <ul>
 * <li>{@link #exec(String, Object...)}</li>
 * <li>{@link #update(String, Object[])}</li>
 * <li>{@link #exec(String, Object[], boolean, int)}</li>
 * </ul>
 *
 * <p>
 *
 * This class is not thread-safe and if it is to be used by multiple threads then the clients must make sure
 * that access to this class is properly synchronized.
 *
 * <p>
 *
 * <strong>Implementation note</strong>: The {@code Connection} that is retrieved from the {@code DataSource}
 * in {@link #getConnection()} may be broken. This is so because if an internal {@code DataSource} is used,
 * then this is a commons-dbcp {@code DataSource} with a <code>testWhileIdle</code> validation strategy (see
 * the {@code ConnectionFactory} class). Furthermore, if it is a {@code DataSource} obtained through JNDI then we
 * can make no assumptions about the validation strategy. This means that our retry logic must either assume that
 * the SQL it tries to execute can do so without errors (i.e., the statement is valid), or it must implement its
 * own validation strategy to apply. Currently, the former is in place.
 */
public class PostgreSQLConnectionHelper extends ConnectionHelper {
	
	

	private String tableSchema;
	
	public PostgreSQLConnectionHelper() {
		super(null,false);
	}
	

	public PostgreSQLConnectionHelper(DataSource dataSrc, Boolean checkWithUserName, Boolean block, Integer fetchSize) {
		super(dataSrc, checkWithUserName, block, fetchSize);
	}

	public PostgreSQLConnectionHelper(DataSource dataSrc, Boolean checkWithUserName, Boolean block) {
		super(dataSrc, checkWithUserName, block);
	}

	public PostgreSQLConnectionHelper(DataSource dataSrc, boolean block) {
		super(dataSrc, block);
	}

	public PostgreSQLConnectionHelper(DataSource dataSrc, String tableSchema,  Boolean checkWithUserName, Boolean block, Integer fetchSize) {
		super(dataSrc,checkWithUserName,block,fetchSize);
		this.tableSchema = tableSchema;
	}
	
    /**
     * Checks whether the given table exists in the database.
     *
     * @param tableName the name of the table
     * @return whether the given table exists
     * @throws SQLException on error
     */
	@Override
    public boolean tableExists(String tableName) throws SQLException {
		System.out.println(String.format("tableExists (%s.%s)",this.tableSchema,tableName));
        Connection con = dataSource.getConnection();
        ResultSet rs = null;
        boolean schemaExists = false;
        String name = tableName;
        try {
            DatabaseMetaData metaData = con.getMetaData();
            if (metaData.storesLowerCaseIdentifiers()) {
                name = tableName.toLowerCase();
            } else if (metaData.storesUpperCaseIdentifiers()) {
                name = tableName.toUpperCase();
            }
            rs = metaData.getTables(null, this.tableSchema, name, null);
            schemaExists = rs.next();
    		System.out.println(String.format("tableExists (%s.%s)=%s",this.tableSchema,tableName,Boolean.valueOf(schemaExists)));
        } finally {
            DbUtility.close(con, null, rs);
        }
        return schemaExists;
    }
}
