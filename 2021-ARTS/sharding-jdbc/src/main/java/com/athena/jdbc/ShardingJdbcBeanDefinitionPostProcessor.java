package com.athena.jdbc;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Method;

/**
 * @author yusheng
 */
public class ShardingJdbcBeanDefinitionPostProcessor implements ImportBeanDefinitionRegistrar {

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
            methodProxy.invokeSuper(o, objects);
            method.invoke(new SqlSessionTemplate(null), objects);
            return null;
        }
    }
}
