

DELIMITER //
CREATE TRIGGER before_employee_insert
BEFORE INSERT ON employees
FOR EACH ROW
BEGIN
    DECLARE next_number INT;
    IF NEW.id IS NULL OR NEW.id = '' THEN
        SELECT IFNULL(MAX(employee_number), 0) + 1 INTO next_number FROM employees;
        SET NEW.id = LPAD(next_number, 4, '0');
        SET NEW.employee_number = next_number;
    ELSE
        SET NEW.employee_number = CAST(NEW.id AS UNSIGNED);
    END IF;
END//
DELIMITER ;

DELIMITER //
CREATE FUNCTION get_next_employee_id() RETURNS VARCHAR(4)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE next_id VARCHAR(4);
    DECLARE next_number INT;
    
    SELECT IFNULL(MAX(employee_number), 0) + 1 INTO next_number FROM employees;
    SET next_id = LPAD(next_number, 4, '0');
    RETURN next_id;
END//
DELIMITER ;