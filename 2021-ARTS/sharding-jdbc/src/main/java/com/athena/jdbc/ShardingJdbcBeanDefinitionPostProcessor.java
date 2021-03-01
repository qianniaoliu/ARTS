package com.athena.jdbc;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.util.JdbcConstants;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yusheng
 */
public class ShardingJdbcBeanDefinitionPostProcessor implements ImportBeanDefinitionRegistrar {

    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final List<String> readMethodNames = new ArrayList<>();
    private final List<String> writeMethodNames = new ArrayList<>();
    private final List<String> shardingTableNames = new ArrayList<>();

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
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        if(beanDefinitionNames == null || beanDefinitionNames.length == 0){
            return;
        }
        for(String beanDefinitionName : beanDefinitionNames){
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            if(beanDefinition.getBeanClassName().equals("org.mybatis.spring.SqlSessionTemplate")){
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(SqlSessionTemplate.class);
                enhancer.setCallback(new SqlSessionTemplateMethodInterceptor());
                Class clazz = enhancer.createClass();
                beanDefinition.setBeanClassName(clazz.getCanonicalName());
            }
        }
    }

    class SqlSessionTemplateMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            if(o instanceof SqlSessionTemplate){
                SqlSessionTemplate sqlSessionTemplate = (SqlSessionTemplate) o;
                if(!readMethodNames.contains(method.getName())){
                    return methodProxy.invokeSuper(o, objects);
                }
                String statement = String.valueOf(objects[0]);
                String sql = sqlSessionTemplate.getConfiguration()
                        .getMappedStatement(statement)
                        .getBoundSql(null)
                        .getSql();
                List<String> tableNames = parseTableName(sql);

            }
            methodProxy.invokeSuper(o, objects);
            method.invoke(new SqlSessionTemplate(null), objects);
            return null;
        }
    }

    private List<String> parseTableName(String sql) {
        List<String> tableNames = new ArrayList<>();
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        for (SQLStatement sqlStatement : sqlStatements){
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            sqlStatement.accept(visitor);
            tableNames.addAll(visitor.getTables().keySet().stream().map(TableStat.Name::getName).collect(Collectors.toSet()));
        }
        return tableNames;
    }

    public static void main(String[] args) {
        String sql = "SELECT a.* FROM ware_sku a, ware_ware b WHERE a.ware_id=b.ware_id";
        DbType dbType = JdbcConstants.MYSQL;
        String result = SQLUtils.format(sql, dbType);
        System.out.println(result);
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, dbType);
        for (SQLStatement sqlStatement : sqlStatements){
            MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
            sqlStatement.accept(visitor);
            System.out.println("Tables : " + visitor.getTables());
        }
    }
}
