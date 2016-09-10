package org.apache.sling.dm.jcr.jackrabbit.server.extensions.db;

import javax.sql.DataSource;

import org.apache.jackrabbit.core.persistence.pool.PostgreSQLPersistenceManager;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPostgreSQLPersistenceManager extends PostgreSQLPersistenceManager {
	private static Logger log = LoggerFactory.getLogger(CustomPostgreSQLPersistenceManager.class);

	protected String tableSchema = "public";

	public String getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}
	public CustomPostgreSQLPersistenceManager() {
		super();
	}
	
	@Override
	protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
		return new PostgreSQLConnectionHelper(dataSrc, this.tableSchema, true, false, 1000);
	}
}
