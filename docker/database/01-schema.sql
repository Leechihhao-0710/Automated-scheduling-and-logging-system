

CREATE TABLE departments(
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'department ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'department name',
    description TEXT COMMENT 'department description',
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    INDEX idx_dep_name(name) COMMENT 'search index for department name'
) COMMENT='department information';

CREATE TABLE machines(
    id VARCHAR(10) PRIMARY KEY COMMENT 'machine id',
    name VARCHAR(100) NOT NULL COMMENT 'machine name',
    department_id INT COMMENT 'department foreign key',
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    
    FOREIGN KEY(department_id) REFERENCES departments(id) ON DELETE SET NULL,
    INDEX idx_machine_dept(department_id)
) COMMENT='machine information';

CREATE TABLE employees(
    id VARCHAR(4) PRIMARY KEY COMMENT 'employee ID 0000-9999',
    employee_number INT NOT NULL AUTO_INCREMENT UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    date_of_birth DATE NOT NULL,
    department_id INT COMMENT 'department foreign key',
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER' COMMENT 'employee role',
    password VARCHAR(255) NOT NULL COMMENT 'employee password',
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    
    FOREIGN KEY(department_id) REFERENCES departments(id) ON DELETE SET NULL,
    INDEX idx_employee_number(employee_number),
    INDEX idx_employee_dept_id(department_id),
    INDEX idx_employee_role(role),
    INDEX idx_employee_name(name),
    INDEX idx_employee_email(email)
) AUTO_INCREMENT=1 COMMENT='employee information';

CREATE TABLE employee_machines(
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(4) NOT NULL,
    machine_id VARCHAR(10) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY(machine_id) REFERENCES machines(id) ON DELETE CASCADE,
    UNIQUE KEY unique_assignment(employee_id, machine_id),
    INDEX idx_emp_machine_emp(employee_id),
    INDEX idx_emp_machine_machine(machine_id)
) COMMENT='employee machine assignment relationship';

CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'task ID',
    title VARCHAR(200) NOT NULL COMMENT 'task title',
    description TEXT COMMENT 'task description',
    task_type VARCHAR(255) NOT NULL COMMENT 'task type: PERSONAL, MEETING, MAINTENANCE',
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING' COMMENT 'task status: PENDING, IN_PROGRESS, COMPLETED',
    due_date_time DATETIME(6) NOT NULL COMMENT 'task due date and time',
    completed_date_time DATETIME(6) COMMENT 'task completion date and time',
    creator_id VARCHAR(4) NOT NULL COMMENT 'foreign key to employee who created the task',
    department_id INTEGER COMMENT 'foreign key to department',
    recurring BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'is this a recurring task',
    recurrence_type VARCHAR(255) COMMENT 'recurrence type: DAILY, WEEKLY, MONTHLY',
    recurrence_interval INTEGER DEFAULT 1 COMMENT 'recurrence interval',
    recurrence_end_date DATETIME(6) COMMENT 'when the recurrence ends',
    email_reminder BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'send email reminder',
    reminder_minutes_before INTEGER DEFAULT 60 COMMENT 'remind before X minutes',
    reminder_sent BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'reminder already sent',
    location VARCHAR(255) COMMENT 'task location',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'creation time',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'last update time',

    FOREIGN KEY (creator_id) REFERENCES employees(id) ON DELETE RESTRICT,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,

    INDEX idx_task_creator (creator_id),
    INDEX idx_task_department (department_id),
    INDEX idx_task_status (status),
    INDEX idx_task_type (task_type),
    INDEX idx_task_due_date (due_date_time),
    INDEX idx_task_recurring (recurring),
    INDEX idx_task_created_at (created_at)
) COMMENT='task definition table';

CREATE TABLE task_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'task assignment ID',
    task_id BIGINT NOT NULL COMMENT 'foreign key to task',
    employee_id VARCHAR(4) NOT NULL COMMENT 'foreign key to employee',
    individual_status VARCHAR(255) NOT NULL DEFAULT 'PENDING' COMMENT 'individual task status: PENDING, IN_PROGRESS, COMPLETED',
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'assignment time',
    started_at DATETIME(6) COMMENT 'when employee started the task',
    completed_at DATETIME(6) COMMENT 'when employee completed the task',
    report TEXT COMMENT 'employee report after completing task',
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    
    UNIQUE KEY unique_task_employee (task_id, employee_id),
    
    INDEX idx_task_assignment_task (task_id),
    INDEX idx_task_assignment_employee (employee_id),
    INDEX idx_task_assignment_status (individual_status),
    INDEX idx_task_assignment_assigned_at (assigned_at)
) COMMENT='task assigned to employees';