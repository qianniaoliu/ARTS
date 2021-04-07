package com.athena.jdbc;

import com.athena.jdbc.autoconfigure.ShardingJdbcConfiguration;
import org.springframework.context.annotation.PropertySource;

import java.lang.annotation.*;

/**
 * @author yusheng
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@PropertySource(name = ShardingJdbcConfiguration.SHARDING_PROPERTY_SOURCE_NAME, value = "classpath:sharding.properties")
public @interface EnableShardingJdbc {

}
