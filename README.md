# 📋 Automated Scheduling and Logging System

An enterprise-grade web-based task management system designed for small and medium-sized enterprises, featuring automated scheduling, email notifications, and comprehensive reporting capabilities.

## 🌐 Live Demo
The system is deployed and accessible at:  
🔗 https://automated-scheduling-and-logging-system-production-3044.up.railway.app/login

## ✨ Features
- Role-based Access Control (Admin/User) with JWT authentication
- Automated recurring task scheduling (Quartz)
- Email reminders for due tasks
- Department and task management
- Real-time task tracking and completion status
- Responsive UI with Bootstrap 5

## 🛠️ Tech Stack
- **Backend**: Spring Boot 3.x, Spring Security, Spring Data JPA  
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript (AJAX)  
- **Database**: MySQL 8.0  
- **Scheduling**: Quartz Scheduler  
- **Authentication**: JWT (JSON Web Tokens)  
- **Email**: JavaMailSender  
- **Deployment**: Docker, Railway Platform  

## 🚀 Quick Start

### Prerequisites
- Java 17+  
- MySQL 8.0+  
- Maven 3.6+  
- Docker (optional)

### Clone the Repository
```bash
git clone https://github.com/Leechihhao-0710/Automated-scheduling-and-logging-system.git
cd automated-scheduling-system
```

### Configure the Database
```sql
-- Create MySQL database
CREATE DATABASE task_management_system;

-- Run SQL scripts:
-- 1. 01-database-schema.sql
-- 2. 02-quartz-tables.sql
```

### Configure Application Properties
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/task_management_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=******

# Email Configuration
spring.mail.username=chihhao1study@gmail.com
spring.mail.password=********
```

### Run the Application
```bash
mvn spring-boot:run
```

### Access the System
- URL: http://localhost:8080  
- Default Admin: Employee ID: 1  
- Password: admin_dob (format: YYYY-MM-DD)

## 🕒 Scheduling Configuration

### Default Cron Jobs (SchedulerInitializer.java)
| Job Type       | Cron Expression     | Description                |
|----------------|---------------------|----------------------------|
| Weekly Check   | `0 25 15 ? * FRI`   | Every Friday at 3:25 PM    |
| Monthly Check  | `0 25 15 8 * ?`     | 8th of each month at 3:25 PM |
| Daily Reminder | `0 25 15 * * ?`     | Every day at 3:25 PM       |

### Modify Reminder Time (example)
```java
Trigger reminderTrigger = TriggerBuilder.newTrigger()
    .withIdentity("REMINDER_TRIGGER", "NOTIFICATIONS")
    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?"))
    .build();
```

### Change Reminder Window (TaskReminderJob.java)
```java
// Change 72 to desired hours (e.g., 24 for 1 day, 168 for 1 week)
List<Task> tasksDueSoon = taskService.getTasksDueSoon(72);
```

### Cron Examples
```bash
# Every day at 8:00 AM
0 0 8 * * ?

# Every Monday at 9:30 AM
0 30 9 ? * MON

# Last day of month at noon
0 0 12 L * ?
```

## 🐳 Docker Deployment

### Build and Run
```bash
docker-compose up --build
```

### Environment Variables (.env)
```env
DB_USERNAME=root
DB_PASSWORD=********
JWT_SECRET=your_super_secret_jwt_key_must_be_long_enough
MAIL_USERNAME=chihhao1study@gmail.com
MAIL_PASSWORD=********
EMAIL_ENABLED=true
```

⚠️ **Warning**: Do NOT commit `.env` or sensitive credentials to public repositories.

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Categories
```bash
# Unit Tests
mvn test -Dtest=*Test

# Integration Tests
mvn test -Dtest=*IntegrationTest

# Specific Test Class
mvn test -Dtest=JwtUtilTest
```

> **Test Coverage**: 48 comprehensive tests including unit, integration, security, and error handling.

## 👥 User Roles & Default Access

| Role         | Login Info                       | Capabilities                                 |
|--------------|----------------------------------|----------------------------------------------|
| Administrator| Employee ID: 1, Password: admin_dob | Full access, user & task management        |
| User         | Employee ID: 2+, Password: employee_dob | View tasks, report progress, update status |

📝 Password format: **YYYY-MM-DD** (date of birth)

## 📊 System Architecture

- Three-tier architecture: Presentation → Logic → Data access  
- JWT-based stateless authentication (RBAC)  
- Quartz + DB persistence for scheduling  
- JavaMailSender for notifications  
- MySQL with JPA/Hibernate ORM

## 🔒 Security Features

- JWT authentication
- Role-based access control
- BCrypt password hashing
- CSRF protection
- Input validation

## 📈 Performance

- Lighthouse Performance: **100/100**  
- Accessibility: **91/100**  
- Responsive: Desktop, tablet, mobile  
- Browser Compatibility: Chrome, Firefox, Safari, Edge

## 🤝 Contributing

1. Fork the repository  
2. Create a feature branch  
```bash
git checkout -b feature/AmazingFeature
```
3. Commit your changes  
```bash
git commit -m "Add AmazingFeature"
```
4. Push and open a pull request

## 📄 License

This project is part of a Master's dissertation at the University of Glasgow.

## 📧 Contact

**Author**: Chih Hao Lee  
**Institution**: University of Glasgow, School of Computing Science  
**Email**: 2966830L@student.gla.ac.uk

> This system was developed as part of a Master of Science dissertation focused on operational efficiency solutions for SMEs.