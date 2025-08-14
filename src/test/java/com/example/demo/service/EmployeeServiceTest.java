package com.example.demo.service;

import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.enums.Role;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.server.EmployeeService;
import com.example.demo.server.EmployeeMachineService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmployeeService
 * Focus on authentication and employee management logic
 * Based on actual EmployeeService implementation
 */
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeMachineService employeeMachineService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmployeeService employeeService;

    private Employee testEmployee;
    private Employee testAdmin;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        employeeService = new EmployeeService(passwordEncoder);

        try {
            java.lang.reflect.Field employeeRepoField = EmployeeService.class.getDeclaredField("employeeRepository");
            employeeRepoField.setAccessible(true);
            employeeRepoField.set(employeeService, employeeRepository);

            java.lang.reflect.Field departmentRepoField = EmployeeService.class
                    .getDeclaredField("departmentRepository");
            departmentRepoField.setAccessible(true);
            departmentRepoField.set(employeeService, departmentRepository);

            java.lang.reflect.Field employeeMachineField = EmployeeService.class
                    .getDeclaredField("employeeMachineService");
            employeeMachineField.setAccessible(true);
            employeeMachineField.set(employeeService, employeeMachineService);
        } catch (Exception e) {
            fail("Failed to inject dependencies: " + e.getMessage());
        }

        setupTestData();
    }

    private void setupTestData() {
        // Test Department
        testDepartment = new Department();
        testDepartment.setId(1);
        testDepartment.setName("Engineering");
        testDepartment.setDescription("Engineering Department");

        // Test Employee (USER role)
        testEmployee = new Employee();
        testEmployee.setId("0002");
        testEmployee.setName("John Doe");
        testEmployee.setEmail("john.doe@company.com");
        testEmployee.setEmployeeNumber(2);
        testEmployee.setRole(Role.USER);
        testEmployee.setPassword("$2a$10$encodedPassword"); // BCrypt encoded
        testEmployee.setDateOfBirth(LocalDate.of(1990, 5, 15));
        testEmployee.setDepartment(testDepartment);

        // Test Admin
        testAdmin = new Employee();
        testAdmin.setId("0001");
        testAdmin.setName("Admin User");
        testAdmin.setEmail("admin@company.com");
        testAdmin.setEmployeeNumber(1);
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setPassword("$2a$10$encodedAdminPassword");
        testAdmin.setDateOfBirth(LocalDate.of(1985, 3, 10));
    }

    // ==================== Test Case 1: Successful Login ====================

    @Test
    @DisplayName("Should validate login successfully with correct credentials")
    void validateLogin_withCorrectCredentials_shouldReturnEmployee() {
        // Given
        Integer employeeNumber = 2;
        String rawPassword = "password123";

        // Mock repository and password encoder behavior
        when(employeeRepository.findByEmployeeNumber(employeeNumber))
                .thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches(rawPassword, testEmployee.getPassword()))
                .thenReturn(true);

        // When
        Optional<Employee> result = employeeService.validateLogin(employeeNumber, rawPassword);

        // Then
        assertTrue(result.isPresent(), "Login should be successful");
        assertEquals(testEmployee.getId(), result.get().getId());
        assertEquals(testEmployee.getName(), result.get().getName());
        assertEquals(testEmployee.getRole(), result.get().getRole());

        // Verify interactions
        verify(employeeRepository, times(1)).findByEmployeeNumber(employeeNumber);
        verify(passwordEncoder, times(1)).matches(rawPassword, testEmployee.getPassword());
    }

    // ==================== Test Case 2: Failed Login - Wrong Password
    // ====================

    @Test
    @DisplayName("Should fail login with incorrect password")
    void validateLogin_withIncorrectPassword_shouldReturnEmpty() {
        // Given
        Integer employeeNumber = 2;
        String wrongPassword = "wrongpassword";

        // Mock repository and password encoder behavior
        when(employeeRepository.findByEmployeeNumber(employeeNumber))
                .thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.matches(wrongPassword, testEmployee.getPassword()))
                .thenReturn(false);

        // When
        Optional<Employee> result = employeeService.validateLogin(employeeNumber, wrongPassword);

        // Then
        assertFalse(result.isPresent(), "Login should fail with wrong password");

        // Verify interactions
        verify(employeeRepository, times(1)).findByEmployeeNumber(employeeNumber);
        verify(passwordEncoder, times(1)).matches(wrongPassword, testEmployee.getPassword());
    }

    // ==================== Test Case 3: Failed Login - Employee Not Found
    // ====================

    @Test
    @DisplayName("Should fail login when employee does not exist")
    void validateLogin_withNonExistentEmployee_shouldReturnEmpty() {
        // Given
        Integer nonExistentEmployeeNumber = 9999;
        String anyPassword = "anypassword";

        // Mock repository behavior
        when(employeeRepository.findByEmployeeNumber(nonExistentEmployeeNumber))
                .thenReturn(Optional.empty());

        // When
        Optional<Employee> result = employeeService.validateLogin(nonExistentEmployeeNumber, anyPassword);

        // Then
        assertFalse(result.isPresent(), "Login should fail when employee not found");

        // Verify interactions
        verify(employeeRepository, times(1)).findByEmployeeNumber(nonExistentEmployeeNumber);
        // Password encoder should not be called if employee doesn't exist
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // Test Case 4: Employee Creation with Department

    @Test
    @DisplayName("Should create employee successfully with department")
    void createEmployeeWithMachines_withValidData_shouldCreateSuccessfully() {
        // Given
        Employee newEmployee = new Employee();
        newEmployee.setName("Jane Smith");
        newEmployee.setEmail("jane.smith@company.com");
        newEmployee.setDateOfBirth(LocalDate.of(1992, 8, 20));
        newEmployee.setRole(Role.USER);

        Integer departmentId = 1;

        // Mock repository behaviors
        when(employeeRepository.existsByEmployeeNumber(anyInt())).thenReturn(false);
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(testDepartment));
        when(employeeRepository.findMaxEmployeeNumber()).thenReturn(Optional.of(2));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedNewPassword");

        Employee savedEmployee = new Employee();
        savedEmployee.setId("0003");
        savedEmployee.setName("Jane Smith");
        savedEmployee.setEmail("jane.smith@company.com");
        savedEmployee.setEmployeeNumber(3);
        savedEmployee.setRole(Role.USER);
        savedEmployee.setPassword("$2a$10$encodedNewPassword");
        savedEmployee.setDateOfBirth(LocalDate.of(1992, 8, 20));
        savedEmployee.setDepartment(testDepartment);

        when(employeeRepository.saveAndFlush(any(Employee.class))).thenReturn(savedEmployee);

        // When
        Employee result = employeeService.createEmployeeWithMachines(newEmployee, departmentId, null);

        // Then
        assertNotNull(result, "Created employee should not be null");
        assertEquals("Jane Smith", result.getName());

        // Verify interactions
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(employeeRepository, times(1)).saveAndFlush(any(Employee.class));
    }

    // Test Case 5: Employee Creation - Email Already Exists

    @Test
    @DisplayName("Should throw exception when email already exists")
    void createEmployeeWithMachines_withDuplicateEmail_shouldThrowException() {
        // Given
        Employee newEmployee = new Employee();
        newEmployee.setName("Duplicate Email User");
        newEmployee.setEmail("john.doe@company.com"); // Same as existing employee
        newEmployee.setDateOfBirth(LocalDate.of(1995, 1, 1));

        // Mock repository behaviors
        when(employeeRepository.existsByEmail("john.doe@company.com")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            employeeService.createEmployeeWithMachines(newEmployee, null, null);
        });

        assertTrue(exception.getMessage().contains("Email exists"),
                "Exception message should mention email exists");

        // Verify that employee was not saved
        verify(employeeRepository, never()).saveAndFlush(any(Employee.class));
    }

    // Test Case 6: Employee Creation - Auto Employee Number

    @Test
    @DisplayName("Should auto-assign next employee number when not provided")
    void createEmployeeWithMachines_withoutEmployeeNumber_shouldAutoAssign() {
        // Given
        Employee newEmployee = new Employee();
        newEmployee.setName("Auto Number User");
        newEmployee.setEmail("auto@company.com");
        newEmployee.setDateOfBirth(LocalDate.of(1993, 12, 5));
        // No employee number set

        // Mock repository behaviors
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.findMaxEmployeeNumber()).thenReturn(Optional.of(5)); // Current max is 5
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(employeeRepository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
            Employee savedEmployee = invocation.getArgument(0);
            Employee returnEmployee = new Employee();
            returnEmployee.setId(savedEmployee.getId());
            returnEmployee.setName(savedEmployee.getName());
            returnEmployee.setEmail(savedEmployee.getEmail());
            returnEmployee.setEmployeeNumber(savedEmployee.getEmployeeNumber());
            returnEmployee.setRole(savedEmployee.getRole());
            returnEmployee.setPassword(savedEmployee.getPassword());
            returnEmployee.setDateOfBirth(savedEmployee.getDateOfBirth());

            // Verify that employee number was set to 6 (max + 1)
            assertEquals(Integer.valueOf(6), savedEmployee.getEmployeeNumber());
            assertEquals("0006", savedEmployee.getId());
            return returnEmployee;
        });

        // When
        Employee result = employeeService.createEmployeeWithMachines(newEmployee, null, null);

        // Then
        assertNotNull(result, "Created employee should not be null");

        // Verify the next employee number was calculated
        verify(employeeRepository, times(1)).findMaxEmployeeNumber();
    }

    // Test Case 7: Get All Admins

    @Test
    @DisplayName("Should return all admin employees")
    void getAllAdmins_shouldReturnOnlyAdminEmployees() {
        // Given
        List<Employee> adminEmployees = Arrays.asList(testAdmin);

        // Mock repository behavior - using actual method findAllAdmins()
        when(employeeRepository.findAllAdmins()).thenReturn(adminEmployees);

        // When
        List<Employee> result = employeeService.getAllAdmins();

        // Then
        assertNotNull(result, "Admin list should not be null");
        assertEquals(1, result.size());
        assertEquals(Role.ADMIN, result.get(0).getRole());
        assertEquals(testAdmin.getId(), result.get(0).getId());

        // Verify interactions
        verify(employeeRepository, times(1)).findAllAdmins();
    }

    // Test Case 8: Get All Users

    @Test
    @DisplayName("Should return all user employees")
    void getAllUsers_shouldReturnOnlyUserEmployees() {
        // Given
        List<Employee> userEmployees = Arrays.asList(testEmployee);

        // Mock repository behavior - using actual method findAllUsers()
        when(employeeRepository.findAllUsers()).thenReturn(userEmployees);

        // When
        List<Employee> result = employeeService.getAllUsers();

        // Then
        assertNotNull(result, "User list should not be null");
        assertEquals(1, result.size());
        assertEquals(Role.USER, result.get(0).getRole());
        assertEquals(testEmployee.getId(), result.get(0).getId());

        // Verify interactions
        verify(employeeRepository, times(1)).findAllUsers();
    }

    // Test Case 9: Get Next Employee Number

    @Test
    @DisplayName("Should return next employee number correctly")
    void getNextEmployeeNumber_shouldReturnCorrectNextNumber() {
        // Given
        when(employeeRepository.findMaxEmployeeNumber()).thenReturn(Optional.of(10));

        // When
        Integer result = employeeService.getNextEmployeeNumber();

        // Then
        assertEquals(Integer.valueOf(11), result);
        verify(employeeRepository, times(1)).findMaxEmployeeNumber();
    }

    // Test Case 10: Get Next Employee Number - No Existing Employees

    @Test
    @DisplayName("Should return 1 when no employees exist")
    void getNextEmployeeNumber_withNoEmployees_shouldReturnOne() {
        // Given
        when(employeeRepository.findMaxEmployeeNumber()).thenReturn(Optional.empty());

        // When
        Integer result = employeeService.getNextEmployeeNumber();

        // Then
        assertEquals(Integer.valueOf(1), result);
        verify(employeeRepository, times(1)).findMaxEmployeeNumber();
    }
}