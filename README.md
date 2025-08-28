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
# For local development
mvn spring-boot:run

# For Docker deployment  
docker-compose up --build
```

### Access the System
- Use the live demo URL provided above
- For local development: http://localhost:8080  
- Default Admin: Employee ID: 0001  
- Password: admin_dob (format: YYYY-MM-DD)

## 📖 User Guide & System Operations

### 🔐 Getting Started - Login Process

#### Step 1: Admin Login
1. Navigate to the login page using the live demo URL above
2. Use the default administrator account:
   - **Employee ID**: `0001`
   - **Password**: `1980-01-01` (admin's date of birth in YYYY-MM-DD format)
3. Click "Login" to access the admin dashboard

#### Step 2: System Overview
After login, you'll see the main dashboard with navigation options:
- **Dashboard**: System overview and statistics
- **Employees**: Manage employee records
- **Departments**: Manage departments and machines
- **Tasks**: View and manage tasks
- **Profile**: View your account information

### 👥 Managing Employees (Admin Only)

#### Adding New Employees
1. Go to **Employees** → **Management**
2. Click **"Add New Employee"** button
3. Fill in the required information:
   - Employee Number (auto-generated or manual)
   - Name
   - Email address
   - Date of Birth (this becomes their password in YYYY-MM-DD format)
   - Department
   - Role (ADMIN or USER)
   - Assigned Machines
4. Click **"Save Employee"**

#### Example: Creating a Test Employee
```
Name: John Smith
Email: john.smith@company.com
Date of Birth: 1990-05-15
Department: Production
Role: USER
Machines: Assembly Robot 1
```
👉 **Login credentials will be**: Employee ID: `[auto-generated]`, Password: `1990-05-15`

### 🔄 Testing User Login Flow

#### Step 3: Test New Employee Account
1. **Logout** from admin account (top-right menu)
2. **Login** with the newly created employee:
   - Employee ID: Use the ID shown in the employee list
   - Password: Use the date of birth (YYYY-MM-DD format)
3. Explore the **user interface** (limited permissions compared to admin)

#### Step 4: User Capabilities
As a regular user, you can:
- ✅ View assigned tasks
- ✅ Update task progress and status
- ✅ Submit task completion reports
- ✅ View your profile information
- ❌ Cannot create/delete employees
- ❌ Cannot manage departments or machines
- ❌ Cannot access admin-only features

### 📋 Task Management Workflow

#### For Administrators
1. **Create Tasks**: Navigate to Tasks → Create New Task
2. **Assign Tasks**: Select employee and set due dates
3. **Monitor Progress**: View task status and completion rates
4. **Receive Email Notifications**: Automatic reminders for due tasks

#### For Regular Users
1. **View Tasks**: See your assigned tasks on the dashboard
2. **Update Status**: Mark tasks as In Progress, Completed, etc.
3. **Add Comments**: Provide progress updates and notes
4. **Upload Files**: Attach relevant documents (if enabled)

### ⚙️ System Administration

#### Department & Machine Management
1. Navigate to **Departments**
2. Add new departments or modify existing ones
3. Assign machines to departments
4. Link employees to specific machines

#### Email Configuration Testing
- Check email settings in application.properties
- Test email functionality by creating overdue tasks
- Verify reminder emails are sent according to the scheduled cron jobs

### 🚨 Troubleshooting Common Issues

#### Login Problems
- **Wrong Password**: Remember password format is YYYY-MM-DD (date of birth)
- **Account Not Found**: Check if employee ID is correct
- **Access Denied**: Verify user has appropriate role permissions

#### Email Issues
- Check SMTP configuration in application.properties
- Verify Gmail app password is correct
- Ensure EMAIL_ENABLED=true in environment variables

#### Database Connection
- Verify MySQL is running
- Check database connection settings
- Ensure database schema is properly initialized

### 💡 Quick Demo Workflow

**5-Minute System Demo:**
1. Login as admin (`0001` / `1980-01-01`)
2. Create a new employee with tomorrow's date as DOB
3. Assign the employee to a department and machine
4. Create a task for this employee
5. Logout and login as the new employee
6. Update the task status
7. Switch back to admin to see the changes

This workflow demonstrates the complete user lifecycle and role-based access control in action.

### 📊 Monitoring & Analytics

#### Admin Dashboard Features
- **Employee Statistics**: Total employees by department and role
- **Task Analytics**: Completion rates and overdue items
- **System Health**: Database status and scheduled job monitoring
- **Email Logs**: Track notification delivery status

## 💥 User Roles & Default Access

| Role         | Login Info                       | Capabilities                                 |
|--------------|----------------------------------|----------------------------------------------|
| Administrator| Employee ID: 0001, Password: 1980-01-01 | Full access, user & task management        |
| User         | Employee ID: 0002+, Password: employee_dob | View tasks, report progress, update status |

📝 Password format: **YYYY-MM-DD** (date of birth)

## 🕐 Scheduling Configuration

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