package com.backendcr.residentialcomplex.config;

import org.hibernate.cfg.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import com.backendcr.residentialcomplex.config.multitenant.SchemaMultiTenantConnectionProvider;
import com.backendcr.residentialcomplex.config.multitenant.TenantIdentifierResolver;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class HibernateConfig {

	@Bean
	JpaVendorAdapter jpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Bean
	LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
			SchemaMultiTenantConnectionProvider connectionProvider, TenantIdentifierResolver tenantResolver) {

		Map<String, Object> properties = new HashMap<>();
		properties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
		properties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
		properties.put("hibernate.multiTenancy", "SCHEMA");
		properties.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
		properties.put(Environment.HBM2DDL_AUTO, "update");
		
		properties.put(Environment.SHOW_SQL, true);
		properties.put(Environment.FORMAT_SQL, true);

		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan("com.backendcr.residentialcomplex");
		em.setJpaVendorAdapter(jpaVendorAdapter());
		em.setJpaPropertyMap(properties);
		return em;
	}
}
