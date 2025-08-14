package com.example.demo.service;

import com.example.demo.entity.Task;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.entity.TaskAssignment;
import com.example.demo.enums.TaskType;
import com.example.demo.enums.TaskStatus;
import com.example.demo.enums.Role;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.TaskAssignmentRepository;
import com.example.demo.server.TaskService;
import com.example.demo.server.EmployeeService;
import com.example.demo.server.DepartmentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService
 * Focus on core business logic of task creation and assignment
 */
@SpringBootTest
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private Employee testEmployee;
    private Employee testAdmin;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Test Admin (creator)
        testAdmin = new Employee();
        testAdmin.setId("0001");
        testAdmin.setName("Test Admin");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setEmployeeNumber(1);

        // Test Employee (assignee)
        testEmployee = new Employee();
        testEmployee.setId("0002");
        testEmployee.setName("Test Employee");
        testEmployee.setEmail("employee@test.com");
        testEmployee.setRole(Role.USER);
        testEmployee.setEmployeeNumber(2);

        // Test Department
        testDepartment = new Department();
        testDepartment.setId(1);
        testDepartment.setName("Engineering");
        testDepartment.setDescription("Engineering Department");

        // Test Task
        testTask = new Task();
        testTask.setTitle("Test Maintenance Task");
        testTask.setDescription("Test task description");
        testTask.setTaskType(TaskType.MAINTENANCE);
        testTask.setDueDateTime(LocalDateTime.now().plusDays(1));
        testTask.setCreator(testAdmin);
    }

    // ==================== Test Case 1: Normal Task Creation ====================

    @Test
    @DisplayName("Should create task and assign to specific employee successfully")
    void createTaskWithAssignments_withSpecificEmployee_shouldCreateSuccessfully() {
        // Given
        List<String> employeeIds = Arrays.asList("0002");

        // Mock repository behaviors
        when(employeeService.getEmployeeByIdOrThrow("0002")).thenReturn(testEmployee);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(new TaskAssignment());

        // When
        Task result = taskService.createTaskWithAssignments(testTask, null, employeeIds);

        // Then
        assertNotNull(result);
        assertEquals("Test Maintenance Task", result.getTitle());
        assertEquals(TaskType.MAINTENANCE, result.getTaskType());

        // Verify interactions
        verify(employeeService, times(1)).getEmployeeByIdOrThrow("0002");
        verify(taskRepository, times(2)).save(any(Task.class)); // Once for initial save, once for final save
        verify(taskAssignmentRepository, times(1)).save(any(TaskAssignment.class));
    }

    // ==================== Test Case 2: Department Assignment ====================

    @Test
    @DisplayName("Should create task and assign to entire department")
    void createTaskWithAssignments_withDepartment_shouldAssignToAllDepartmentEmployees() {
        // Given
        Integer departmentId = 1;
        List<Employee> deptEmployees = Arrays.asList(testEmployee);

        // Mock repository behaviors
        when(departmentService.getDepartmentByIdOrThrow(departmentId)).thenReturn(testDepartment);
        when(employeeService.getEmployeesByDepartment(departmentId)).thenReturn(deptEmployees);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(new TaskAssignment());

        // When
        Task result = taskService.createTaskWithAssignments(testTask, departmentId, null);

        // Then
        assertNotNull(result);
        assertEquals(testDepartment, result.getDepartment());

        // Verify interactions
        verify(departmentService, times(1)).getDepartmentByIdOrThrow(departmentId);
        verify(employeeService, times(1)).getEmployeesByDepartment(departmentId);
        verify(taskAssignmentRepository, times(1)).save(any(TaskAssignment.class));
    }

    // ==================== Test Case 3: Assign to All Users ====================

    @Test
    @DisplayName("Should assign task to all users when no specific assignment provided")
    void createTaskWithAssignments_withNullParameters_shouldAssignToAllUsers() {
        // Given
        List<Employee> allUsers = Arrays.asList(testEmployee, testAdmin);

        // Mock repository behaviors
        when(employeeService.getAllUsers()).thenReturn(allUsers);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(new TaskAssignment());

        // When
        Task result = taskService.createTaskWithAssignments(testTask, null, null);

        // Then
        assertNotNull(result);

        // Verify interactions
        verify(employeeService, times(1)).getAllUsers();
        verify(taskAssignmentRepository, times(2)).save(any(TaskAssignment.class)); // Two employees
    }

    // ==================== Test Case 4: Exception Handling ====================

    @Test
    @DisplayName("Should throw exception when employee ID does not exist")
    void createTaskWithAssignments_withInvalidEmployeeId_shouldThrowException() {
        // Given
        List<String> employeeIds = Arrays.asList("9999");

        // Mock repository behaviors - task will be saved first
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(employeeService.getEmployeeByIdOrThrow("9999"))
                .thenThrow(new RuntimeException("employee not found with id: 9999"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTaskWithAssignments(testTask, null, employeeIds);
        });

        assertEquals("employee not found with id: 9999", exception.getMessage());

        // Verify that task was saved but assignment was not created due to exception
        verify(taskRepository, times(1)).save(any(Task.class)); // Task is saved first
        verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class)); // But assignment fails
    }

    // ==================== Test Case 5: Creator Assignment ====================

    @Test
    @DisplayName("Should assign first admin as creator when no creator provided")
    void createTaskWithAssignments_withNoCreator_shouldAssignFirstAdmin() {
        // Given
        Task taskWithoutCreator = new Task();
        taskWithoutCreator.setTitle("Task Without Creator");
        taskWithoutCreator.setTaskType(TaskType.MAINTENANCE);
        taskWithoutCreator.setDueDateTime(LocalDateTime.now().plusDays(1));
        // No creator set

        List<Employee> admins = Arrays.asList(testAdmin);
        List<String> employeeIds = Arrays.asList("0002");

        // Mock repository behaviors
        when(employeeService.getAllAdmins()).thenReturn(admins);
        when(employeeService.getEmployeeByIdOrThrow("0002")).thenReturn(testEmployee);
        when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutCreator);
        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenReturn(new TaskAssignment());

        // When
        Task result = taskService.createTaskWithAssignments(taskWithoutCreator, null, employeeIds);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCreator());
        assertEquals(testAdmin, result.getCreator());

        // Verify interactions
        verify(employeeService, times(1)).getAllAdmins();
    }

    // ==================== Test Case 6: No Admin Available ====================

    @Test
    @DisplayName("Should throw exception when no admin available as creator")
    void createTaskWithAssignments_withNoAdminAvailable_shouldThrowException() {
        // Given
        Task taskWithoutCreator = new Task();
        taskWithoutCreator.setTitle("Task Without Creator");
        taskWithoutCreator.setTaskType(TaskType.MAINTENANCE);
        taskWithoutCreator.setDueDateTime(LocalDateTime.now().plusDays(1));

        List<Employee> emptyAdminList = Arrays.asList(); // No admins
        List<String> employeeIds = Arrays.asList("0002");

        // Mock repository behaviors
        when(employeeService.getAllAdmins()).thenReturn(emptyAdminList);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTaskWithAssignments(taskWithoutCreator, null, employeeIds);
        });

        assertEquals("no admin found to set as task creator", exception.getMessage());

        // Verify that no task was saved
        verify(taskRepository, never()).save(any(Task.class));
    }
}