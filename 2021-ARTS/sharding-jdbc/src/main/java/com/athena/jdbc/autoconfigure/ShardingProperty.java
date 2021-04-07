package com.athena.jdbc.autoconfigure;

import java.util.List;

/**
 * @author yusheng
 */
public class ShardingProperty {

    private String databaseAlias;

    private Integer databaseNum;

    private Integer tableNum;



    private List<DataSourceConfig> dataSourceConfigs;



    class DataSourceConfig{

        private String username;

        private String password;

        private String driverClassName;

        private String url;
    }
}
