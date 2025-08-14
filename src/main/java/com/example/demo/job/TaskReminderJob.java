package com.example.demo.job;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.entity.Task;
import com.example.demo.server.TaskService;
import com.example.demo.server.EmailService;

@Component
public class TaskReminderJob implements Job {

    @Autowired
    private TaskService taskService;

    @Autowired
    private EmailService emailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            List<Task> tasksDueSoon = taskService.getTasksDueSoon(72);
            // define remind the user that they have due tasks within 3 days

            for (Task task : tasksDueSoon) {
                emailService.sendTaskReminder(task);
            }
        } catch (Exception e) {
            throw new JobExecutionException("Error sending task reminders", e);
        }
    }
}