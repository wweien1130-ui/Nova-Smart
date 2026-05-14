package com.hmall.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class, BaseMapper.class})   // 仅当 classpath 中存在这些类时才生效
public class MyBatisConfig {
    @Bean
    @ConditionalOnMissingBean                  // 如果已经有人定义过了，就不再重复创建
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 创建分页拦截器，指定数据库类型为 MySQL
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 2. 设置单次查询最大条数上限为 1000
        //    防止前端传入 pageSize=999999 导致全表扫描
        paginationInnerInterceptor.setMaxLimit(1000L);
        // 3. 将分页拦截器添加到 MyBatis-Plus 拦截器链中
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}