package com.example.demo.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.server.RecurringTaskService;

@Component
public class RecurringTaskCheckJob implements Job {// Quartz job interface

    @Autowired // import the recurringTaskService to handle the recurring task logics
    private RecurringTaskService recurringTaskService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {// Job's method
        JobKey jobKey = context.getJobDetail().getKey();
        // in JobExecutionContext has Quartz job's information , trigger , schedule
        // status

        try {
            if ("WEEKLY_CHECK".equals(jobKey.getName())) {
                // weekly recurring tasks checked on friday every week
                recurringTaskService.processWeeklyRecurringTasks();
            } else if ("MONTHLY_CHECK".equals(jobKey.getName())) {
                // monthly recurring tasks checkedon 15th every month
                recurringTaskService.processMonthlyRecurringTasks();
            }
        } catch (Exception e) {
            throw new JobExecutionException("Error processing recurring tasks", e);
        }
    }
}
