

INSERT INTO departments (name, description) VALUES
('Production', 'Production Department - Manufacturing and assembly operations'),
('Maintenance', 'Maintenance Department - Equipment maintenance and repair'),
('IT', 'Information Technology Department - IT support and development');

INSERT INTO machines (id, name, department_id) VALUES
('M001', 'Assembly Robot 1', 1),  -- Production department
('M002', 'Packaging Machine', 1),  -- Production department
('M003', 'Diagnostic Equipment', 2);  -- Maintenance department

INSERT INTO employees (name, email, date_of_birth, department_id, role, password) VALUES
('System Administrator', 'admin@company.com', '1980-01-01', 3, 'ADMIN', '$2a$10$B6ZGJN/XOiVv3WAQoQQq9e0OWwECfzUZCtKn5Rq4bve.gonYwS78q'),
('Lee Chih Hao', 'sodagreenb@gmail.com', '1991-07-10', 1, 'USER', '$2a$10$JEVo4Ra4EYfwT0xCKwDrgeRrf/MzesPprOkbxWCOeoIfqj2fu4r.K'),
('Ai Chia Yi', 'ai.chia.yi@company.com', '1996-08-29', 2, 'USER', '$2a$10$58G3cPJi/JCpbBOLj8b5XuXaB6Tw5wPFDCmZsveFbzUMzRf9U51RS');


INSERT INTO employee_machines (employee_id, machine_id) VALUES
('0001', 'M001'),  -- System Administrator -> Assembly Robot 1
('0002', 'M001'),  -- Lee Chih Hao -> Assembly Robot 1
('0002', 'M002'),  -- Lee Chih Hao -> Packaging Machine  
('0003', 'M003');  -- Ai Chia Yi -> Diagnostic Equipment