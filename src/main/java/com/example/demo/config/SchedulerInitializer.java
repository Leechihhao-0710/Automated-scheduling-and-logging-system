package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

import org.quartz.*;

import com.example.demo.job.RecurringTaskCheckJob;
import com.example.demo.job.TaskReminderJob;

@Component
public class SchedulerInitializer implements ApplicationRunner {

    @Autowired
    private Scheduler scheduler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeScheduledJobs();// initialize when start hte system
    }

    private void initializeScheduledJobs() throws SchedulerException {

        JobDetail weeklyJob = JobBuilder.newJob(RecurringTaskCheckJob.class)
                .withIdentity("WEEKLY_CHECK", "RECURRING_TASKS")
                .build();// create a weekly_check job , the job logic is in RecurringTaskCheckJob class

        Trigger weeklyTrigger = TriggerBuilder.newTrigger()
                .withIdentity("WEEKLY_TRIGGER", "RECURRING_TASKS")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 17 ? * FRI"))
                .build();// use Cron expression to tell quartz when to trigger the job

        JobDetail monthlyJob = JobBuilder.newJob(RecurringTaskCheckJob.class)
                .withIdentity("MONTHLY_CHECK", "RECURRING_TASKS")
                .build();

        Trigger monthlyTrigger = TriggerBuilder.newTrigger()
                .withIdentity("MONTHLY_TRIGGER", "RECURRING_TASKS")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 10 15 * ?"))
                .build();

        JobDetail reminderJob = JobBuilder.newJob(TaskReminderJob.class)
                .withIdentity("REMINDER_CHECK", "NOTIFICATIONS")
                .build();

        Trigger reminderTrigger = TriggerBuilder.newTrigger()
                .withIdentity("REMINDER_TRIGGER", "NOTIFICATIONS")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 24 14 * * ?")
                        .inTimeZone(TimeZone.getTimeZone("Asia/Bangkok"))
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        if (scheduler.checkExists(weeklyJob.getKey())) {
            scheduler.deleteJob(weeklyJob.getKey());
        }
        if (scheduler.checkExists(monthlyJob.getKey())) {
            scheduler.deleteJob(monthlyJob.getKey());
        }
        if (scheduler.checkExists(reminderJob.getKey())) {
            scheduler.deleteJob(reminderJob.getKey());
        } // avoid duplicate registration

        scheduler.scheduleJob(weeklyJob, weeklyTrigger);
        scheduler.scheduleJob(monthlyJob, monthlyTrigger);
        scheduler.scheduleJob(reminderJob, reminderTrigger);

        System.out.println("Quartz Jobs initialized successfully!");
        System.out.println("Weekly check: Every Friday 5PM");
        System.out.println("Monthly check: 15th of each month 10AM");
        System.out.println("Daily reminder check: Every day 9AM");
    }
}