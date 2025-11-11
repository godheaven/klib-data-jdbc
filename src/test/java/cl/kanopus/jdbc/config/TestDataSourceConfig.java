/*-
 * !--
 * For support and inquiries regarding this library, please contact:
 *   soporte@kanopus.cl
 *
 * Project website:
 *   https://www.kanopus.cl
 * %%
 * Copyright (C) 2025 Pablo Díaz Saavedra
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * --!
 */
package cl.kanopus.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "cl.kanopus")
@Import({cl.kanopus.jdbc.impl.DAOTestImpl.class, cl.kanopus.jdbc.example.ExampleDAO.class})
public class TestDataSourceConfig {

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("org.postgresql.Driver");
        cfg.setJdbcUrl("jdbc:postgresql://localhost:5432/test?useSSL=false");
        cfg.setUsername("user_test");
        cfg.setPassword("pass_test");
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(3);
        return new HikariDataSource(cfg);
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jt = new JdbcTemplate(dataSource);
        jt.setFetchSize(100);
        return jt;
    }

    @Bean
    public NamedParameterJdbcTemplate jdbcTemplateTest(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }
}
