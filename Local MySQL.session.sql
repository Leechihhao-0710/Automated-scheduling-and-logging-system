
SELECT * FROM task_management_system.departments;


SELECT * FROM task_management_system.machines;


SELECT * FROM task_management_system.employees;


SELECT * FROM task_management_system.employee_machines;

SELECT * FROM task_management_system.task_assignments;
SELECT * FROM task_management_system.tasks;


SELECT 
    e.employee_number,
    e.name as employee_name,
    e.email,
    d.name as department_name,
    GROUP_CONCAT(m.name ORDER BY m.name SEPARATOR ', ') as assigned_machines,
    COUNT(m.id) as machine_count
FROM task_management_system.employees e
LEFT JOIN task_management_system.employee_machines em ON e.id = em.employee_id
LEFT JOIN task_management_system.machines m ON em.machine_id = m.id
LEFT JOIN task_management_system.departments d ON e.department_id = d.id
GROUP BY e.id, e.employee_number, e.name, e.email, d.name
ORDER BY e.employee_number;

SELECT id, employee_number, LENGTH(id) as id_length 
FROM employees 
ORDER BY create_at DESC;

SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('tasks', 'task_assignments')
ORDER BY TABLE_NAME, ORDINAL_POSITION;


SELECT * FROM task_assignments;

SELECT 
    ta.id AS assignment_id,
    ta.assigned_at,
    ta.individual_status,
    t.id AS task_id,
    t.title AS task_title,
    t.due_date_time,
    e.id AS employee_id,
    e.name AS employee_name,
    e.employee_number
FROM task_assignments ta
JOIN tasks t ON ta.task_id = t.id
JOIN employees e ON ta.employee_id = e.id
ORDER BY ta.assigned_at DESC;

SELECT id, title, recurring, recurrence_type, recurring_day_of_week, 
       recurring_day_of_month, reminder_days_before 
FROM tasks 
WHERE recurring = 1;

SELECT SCHED_NAME, JOB_NAME, JOB_GROUP, JOB_CLASS_NAME 
FROM QRTZ_JOB_DETAILS;

SELECT SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, TRIGGER_STATE
FROM QRTZ_TRIGGERS;

SELECT t.SCHED_NAME, t.TRIGGER_NAME, c.CRON_EXPRESSION, t.JOB_NAME
FROM QRTZ_TRIGGERS t
JOIN QRTZ_CRON_TRIGGERS c ON t.SCHED_NAME = c.SCHED_NAME 
  AND t.TRIGGER_NAME = c.TRIGGER_NAME 
  AND t.TRIGGER_GROUP = c.TRIGGER_GROUP;