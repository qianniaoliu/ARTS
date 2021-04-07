package com.athena.jdbc;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yusheng
 */
public class ShardingJdbcBeanDefinitionPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final static String SQL_PARAMETER_NAME = "statement";

    private final List<String> readMethodNames = new ArrayList<>();

    private final List<String> writeMethodNames = new ArrayList<>();

    private final List<String> shardingTableNames = new ArrayList<>();

    private ApplicationContext applicationContext;

    public ShardingJdbcBeanDefinitionPostProcessor() {
        readMethodNames.add("selectOne");
        readMethodNames.add("selectMap");
        readMethodNames.add("selectCursor");
        readMethodNames.add("selectList");
        readMethodNames.add("select");

        writeMethodNames.add("insert");
        writeMethodNames.add("update");
        writeMethodNames.add("delete");

        shardingTableNames.add("ware_sku");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionTemplate) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(SqlSessionTemplate.class);
            enhancer.setCallback(new SqlSessionTemplateMethodInterceptor());
            SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
            return enhancer.create(new Class[]{SqlSessionFactory.class},new Object[]{sqlSessionFactory});
        }
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    class SqlSessionTemplateMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            if (o instanceof SqlSessionTemplate) {
                SqlSessionTemplate sqlSessionTemplate = (SqlSessionTemplate) o;
                if (!readMethodNames.contains(method.getName())) {
                    return methodProxy.invokeSuper(o, objects);
                }
                String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
                if(parameterNames.length > 0 && Objects.equals(parameterNames[0], SQL_PARAMETER_NAME)){
                    String statement = String.valueOf(objects[0]);
                    String sql = sqlSessionTemplate.getConfiguration()
                            .getMappedStatement(statement)
                            .getBoundSql(null)
                            .getSql();
                    List<String> tableNames = parseTableName(sql);
                }
            }

            return methodProxy.invokeSuper(o, objects);
        }
    }

    private List<String> parseTableName(String sql) {
        List<String> tableNames = new ArrayList<>();
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        for (SQLStatement sqlStatement : sqlStatements) {
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            sqlStatement.accept(visitor);
            tableNames.addAll(visitor.getTables().keySet().stream().map(TableStat.Name::getName).collect(Collectors.toSet()));
        }
        return tableNames;
    }
}
