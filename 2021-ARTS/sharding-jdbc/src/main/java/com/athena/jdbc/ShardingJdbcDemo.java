package com.athena.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author yusheng
 */
public class ShardingJdbcDemo {

    public static void main(String[] args) throws SQLException {
        // 配置真实数据源
        Map<String, DataSource> dataSourceMap = new HashMap<>();

        // 配置数据源
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("1234567890");
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://11.91.139.199:3306/ept_ware");
        dataSourceMap.put("ept_ware",druidDataSource);

        // 配置表规则
        ShardingTableRuleConfiguration wareTableRuleConfig
                = new ShardingTableRuleConfiguration("ware_ware", "ept_ware.ware_ware_${0..1}");
        // 配置分库策略
        wareTableRuleConfig.setDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("ware_id", "dbShardingAlgorithm"));
        // 配置分表策略
        wareTableRuleConfig.setTableShardingStrategy(new StandardShardingStrategyConfiguration("ware_id", "tableShardingAlgorithm"));

        // 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(wareTableRuleConfig);

        // 配置分库算法
        Properties dbShardingAlgorithmrProps = new Properties();
        dbShardingAlgorithmrProps.setProperty("algorithm-expression", "ept_ware");
        shardingRuleConfig.getShardingAlgorithms().put("dbShardingAlgorithm", new ShardingSphereAlgorithmConfiguration("INLINE", dbShardingAlgorithmrProps));

        // 配置分表算法
        Properties tableShardingAlgorithmrProps = new Properties();
        tableShardingAlgorithmrProps.setProperty("algorithm-expression", "ware_ware_${ware_id % 2}");
        shardingRuleConfig.getShardingAlgorithms().put("tableShardingAlgorithm", new ShardingSphereAlgorithmConfiguration("INLINE", tableShardingAlgorithmrProps));

        // 创建 ShardingSphereDataSource
        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(dataSourceMap, Collections.singleton(shardingRuleConfig), new Properties());

        String sql = "SELECT * FROM ware_ware WHERE title=?";
//        String sql = "INSERT INTO ware_ware (ware_id, title) VALUES (?, ?) ";
        Connection connection = dataSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql);
//        ps.setLong(1, 5);
        ps.setString(1, "test566");
        ResultSet rs = ps.executeQuery();
        while (rs.next()){
            System.out.println();
        }
    }
}
