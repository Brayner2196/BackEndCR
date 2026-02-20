package com.backendcr.residentialcomplex.config.multitenant;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

@Component
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String>{
	
	private static final long serialVersionUID = 7573341091998777773L;
	
	private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        // PostgreSQL — para MySQL sería: "USE `" + tenantIdentifier + "`"
        connection.createStatement()
                  .execute("SET search_path TO " + tenantIdentifier);
        return connection;
    }
    
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.createStatement()
                  .execute("SET search_path TO public"); // resetea al schema base
        connection.close();
    }

    @Override public boolean supportsAggressiveRelease() { return false; }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) { return false; }

    @Override
    public <T> T unwrap(Class<T> unwrapType) { return null; }

	
}
