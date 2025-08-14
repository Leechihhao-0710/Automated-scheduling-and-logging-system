package com.example.demo.server;

import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.enums.Role;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.DepartmentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private EmployeeMachineService employeeMachineService;

    private final PasswordEncoder passwordEncoder;

    public EmployeeService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAllByOrderByEmployeeNumber();
    }// find all employees in order

    public Optional<Employee> getEmployeeById(String id) {
        return employeeRepository.findById(id);
    }// find employee by id

    public Employee getEmployeeByIdOrThrow(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }

    public Optional<Employee> getEmployeeByNumber(Integer employeeNumber) {
        return employeeRepository.findByEmployeeNumber(employeeNumber);
    }// find the employee by employee number

    public Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }// find employee by email

    public Employee createEmployeeWithMachines(Employee employee, Integer departmentId, List<String> machineIds) {
        if (employee.getEmployeeNumber() != null
                && employeeRepository.existsByEmployeeNumber(employee.getEmployeeNumber())) {
            throw new RuntimeException("Employee number exists" + employee.getEmployeeNumber());
        } // check if the employee number already exists
        if (employee.getEmployeeNumber() == null) {
            employee.setEmployeeNumber(getNextEmployeeNumber());
        }
        employee.setId(String.format("%04d", employee.getEmployeeNumber()));

        if (employee.getEmail() != null && employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Email exists" + employee.getEmail());
        } // check if the employee email alreadty exists
        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("department not found with id" + departmentId));
            employee.setDepartment(department);
        } // set the employee department and check if the department exists
        String rawPassword = employee.getPassword();
        if (rawPassword == null || rawPassword.isEmpty()) {
            rawPassword = Employee.formatPasswordFromDate(employee.getDateOfBirth());
        } // set the employee password , set as the date of birth
        employee.setPassword(passwordEncoder.encode(rawPassword));// encrypt the password
        if (employee.getRole() == null) {
            employee.setRole(Role.USER);
        }

        Employee savedEmployee = employeeRepository.saveAndFlush(employee);// save or update the employee

        if (machineIds != null && !machineIds.isEmpty()) {// assigned the machine to employee
            for (String machineId : machineIds) {
                try {
                    employeeMachineService.assignMachineToEmployee(savedEmployee.getId(), machineId);
                } catch (RuntimeException e) {
                    System.err.println("Failed to assign machine " + machineId + ": " + e.getMessage());
                }
            }
        }
        return savedEmployee;

    }

    public Employee createEmployee(Employee employee, Integer departmentId) {
        return createEmployeeWithMachines(employee, departmentId, null);
    }

    // update employee information
    public Employee updateEmployeeWithMachines(String id, Employee employeeDetails, Integer departmentId,
            List<String> machineIds) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("employee not found with id " + id));

        if (employeeDetails.getEmployeeNumber() != null &&
                !employee.getEmployeeNumber().equals(employeeDetails.getEmployeeNumber())) {
            if (employeeRepository.existsByEmployeeNumber(employeeDetails.getEmployeeNumber())) {
                throw new RuntimeException("Employee number already exists: " + employeeDetails.getEmployeeNumber());
            }
            employee.setEmployeeNumber(employeeDetails.getEmployeeNumber());
        }

        if (employeeDetails.getEmail() != null &&
                !employeeDetails.getEmail().equals(employee.getEmail()) &&
                employeeRepository.existsByEmail(employeeDetails.getEmail())) {
            throw new RuntimeException("Email already exists: " + employeeDetails.getEmail());
        }

        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("department not found with id " + departmentId));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        if (employeeDetails.getName() != null) {
            employee.setName(employeeDetails.getName());
        }
        if (employeeDetails.getEmail() != null) {
            employee.setEmail(employeeDetails.getEmail());
        }
        if (employeeDetails.getDateOfBirth() != null) {
            employee.setDateOfBirth(employeeDetails.getDateOfBirth());

            String newPassword = Employee.formatPasswordFromDate(employeeDetails.getDateOfBirth());
            employee.setPassword(passwordEncoder.encode(newPassword));
            System.out.println("Password updated for employee " + id + " based on new birth date");
        }
        if (employeeDetails.getRole() != null) {
            employee.setRole(employeeDetails.getRole());
        }

        Employee updatedEmployee = employeeRepository.save(employee);

        if (machineIds != null) {
            employeeMachineService.unassignAllMachinesFromEmployee(id);
            if (!machineIds.isEmpty()) {
                for (String machineId : machineIds) {
                    try {
                        employeeMachineService.assignMachineToEmployee(id, machineId);
                    } catch (RuntimeException e) {
                        System.err.println("Failed to assign machine " + machineId + ": " + e.getMessage());
                    }
                }
            }
        }
        return updatedEmployee;
    }

    public Employee updateEmployee(String id, Employee employeeDetails, Integer departmentId) {
        return updateEmployeeWithMachines(id, employeeDetails, departmentId, null);
    }// without changing assigned machine

    public void deleteEmployee(String id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }

        employeeMachineService.unassignAllMachinesFromEmployee(id);// clean the relation between machine and employee

        employeeRepository.deleteById(id);// delete the employee
    }

    public List<Employee> getEmployeesByDepartment(Integer departmentId) {
        return employeeRepository.findByDepartmentId(departmentId);
    }// find the employee by the department

    public List<Employee> getEmployeesByDepartmentName(String departmentName) {
        return employeeRepository.findByDepartmentName(departmentName);
    }// find the employee by department name

    public List<Employee> getEmployeesByRole(Role role) {
        return employeeRepository.findByRole(role);
    }// find employee bt role

    public List<Employee> getAllAdmins() {
        return employeeRepository.findAllAdmins();
    }

    public List<Employee> getAllUsers() {
        return employeeRepository.findAllUsers();
    }// find the admins and users

    public List<Employee> searchEmployeesByName(String name) {
        return employeeRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Employee> getEmployeesByDepartmentAndRole(Integer departmentId, Role role) {
        return employeeRepository.findByDepartmentIdAndRole(departmentId, role);
    }

    public long getEmployeeCountByDepartment(Integer departmentId) {
        return employeeRepository.countByDepartmentId(departmentId);
    }// count how much employees in the department

    public List<String> getEmployeeMachines(String employeeId) {
        return employeeMachineService.getMachineIdsByEmployee(employeeId);
    }// get the list with connection between employees and machines

    public Integer getNextEmployeeNumber() {
        Optional<Integer> maxNumber = employeeRepository.findMaxEmployeeNumber();
        return maxNumber.orElse(0) + 1;
    }// get next employee number

    public boolean isEmployeeNumberAvailable(Integer employeeNumber) {
        return !employeeRepository.existsByEmployeeNumber(employeeNumber);
    }

    public boolean isEmailAvailable(String email) {
        return !employeeRepository.existsByEmail(email);
    }

    public long getTotalEmployeeCount() {
        return employeeRepository.count();
    }

    public List<Employee> getEmployeesWithoutDepartment() {
        return employeeRepository.findEmployeesWithoutDepartment();
    }

    public Optional<Employee> validateLogin(Integer employeeNumber, String password) {
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeNumber(employeeNumber);

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            if (passwordEncoder.matches(password, employee.getPassword())) {
                return Optional.of(employee);
            }
        }
        return Optional.empty();
    }// basic login logic

    public void removeEmployeesFromDepartment(Integer departmentId) {
        List<Employee> employees = employeeRepository.findByDepartmentId(departmentId);
        for (Employee employee : employees) {
            employee.setDepartment(null);
            employeeRepository.save(employee);
        }
    }

}
