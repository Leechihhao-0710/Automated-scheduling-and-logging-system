package com.example.demo.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Task;
import com.example.demo.entity.Employee;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;// get the offical email from application properties

    @Value("${app.email.enabled:true}") // open the send email functionality
    private boolean emailEnabled;

    public void sendTaskReminder(Task task) {
        if (!emailEnabled) {
            System.out.println("Email disabled, skipping reminder for task: " + task.getTitle());
            return;
        }

        try {
            for (Employee employee : task.getAssignedEmployees()) {
                if (employee.getEmail() != null && !employee.getEmail().isEmpty()) {
                    sendReminderToEmployee(task, employee);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send task reminder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendReminderToEmployee(Task task, Employee employee) {// set the information about(send from/send to /
                                                                       // title)
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(employee.getEmail());
        message.setSubject("Task Reminder: " + task.getTitle());

        String emailBody = buildReminderEmailBody(task, employee);
        message.setText(emailBody);

        try {
            mailSender.send(message);
            System.out.println("Reminder sent to: " + employee.getEmail() + " for task: " + task.getTitle());
        } catch (Exception e) {
            System.err.println("Failed to send email to " + employee.getEmail() + ": " + e.getMessage());
        }
    }

    private String buildReminderEmailBody(Task task, Employee employee) {// the context of the email reminder
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(employee.getName()).append(",\n\n");
        body.append("This is a reminder for your upcoming task:\n\n");
        body.append("Task: ").append(task.getTitle()).append("\n");
        body.append("Description: ").append(task.getDescription() != null ? task.getDescription() : "No description")
                .append("\n");
        body.append("Due Date: ").append(task.getDueDateTime().format(formatter)).append("\n");
        body.append("Type: ").append(task.getTaskType().toString()).append("\n");

        if (task.getLocation() != null && !task.getLocation().isEmpty()) {
            body.append("Location: ").append(task.getLocation()).append("\n");
        }

        body.append("\n");
        body.append("Please complete this task before the due date.\n\n");
        body.append("Best regards,\n");
        body.append("Task Management System");

        return body.toString();
    }

}