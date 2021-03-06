package org.apache.sling.dm.jcr.jackrabbit.server.extensions.db;

import javax.sql.DataSource;

import java.lang.reflect.Method;
import org.apache.jackrabbit.core.fs.db.DbFileSystem;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class PostgreSQLDbFileSystem extends DbFileSystem {
	private static Logger log = LoggerFactory.getLogger(PostgreSQLDbFileSystem.class);

	protected String tableSchema = "public";

	public String getTableSchema() {
		return tableSchema;
	}

	public void setTableSchema(String tableSchema) {
		this.tableSchema = tableSchema;
	}

	public PostgreSQLDbFileSystem() {
		super();
	}

	public PostgreSQLDbFileSystem(String tableSchema) {
		super();
		this.tableSchema = tableSchema;
	}

	@Override
	protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
		return new PostgreSQLConnectionHelper(dataSrc, this.tableSchema, true, false, 1000);
	}

}
