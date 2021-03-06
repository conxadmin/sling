package org.apache.sling.dm.jcr.jackrabbit.server.extensions.db;

import javax.sql.DataSource;

import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgreSQLDbDataStore extends DbDataStore {
	private static Logger log = LoggerFactory.getLogger(PostgreSQLDbDataStore.class);

	protected String tableSchema = "public";

	public String getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}

	public PostgreSQLDbDataStore() {
		super();
	}

	@Override
	protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
		return new PostgreSQLConnectionHelper(dataSrc, this.tableSchema, true, false, 1000);
	}	
}
