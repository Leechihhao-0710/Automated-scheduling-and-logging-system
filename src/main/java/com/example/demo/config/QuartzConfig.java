package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;

//Quartz configuration file , initialize Quartz schedular
@Configuration
public class QuartzConfig {

    @Autowired
    private DataSource dataSource;// connect to database to let Quartz can store the Job , trigger

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        return new SpringBeanJobFactory();// return a SpringBeanJobFactory , and it wil generate Job entities
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setJobFactory(springBeanJobFactory());// use spring-aware JobFactory
        schedulerFactory.setDataSource(dataSource);// use defined database
        schedulerFactory.setOverwriteExistingJobs(true);// overwrite the exist task when start
        schedulerFactory.setStartupDelay(30);// delay 30 second to let the system start first
        return schedulerFactory;
    }
}
