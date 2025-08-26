package com.example.demo.integration;

import com.example.demo.entity.Task;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.entity.TaskAssignment;
import com.example.demo.enums.TaskType;
import com.example.demo.enums.TaskStatus;
import com.example.demo.enums.Role;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.TaskAssignmentRepository;
import com.example.demo.server.TaskService;
import com.example.demo.server.EmployeeService;
import com.example.demo.server.DepartmentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Task Management System
 * Tests the complete business flow across multiple services and repositories
 */
@SpringBootTest
@Transactional
class TaskManagementIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private EntityManager entityManager;

    private Employee adminEmployee;
    private Employee userEmployee1;
    private Employee userEmployee2;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        taskAssignmentRepository.deleteAll();
        taskRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        setupTestData();
    }

    private void setupTestData() {

        testDepartment = new Department();
        testDepartment.setName("Engineering");
        testDepartment.setDescription("Engineering Department");
        testDepartment = departmentRepository.save(testDepartment);

        adminEmployee = new Employee();
        adminEmployee.setName("Admin User");
        adminEmployee.setEmail("admin@company.com");
        adminEmployee.setEmployeeNumber(1);
        adminEmployee.setRole(Role.ADMIN);
        adminEmployee.setPassword("encodedPassword");
        adminEmployee.setDateOfBirth(LocalDate.of(1985, 3, 10));
        adminEmployee = employeeRepository.save(adminEmployee);

        userEmployee1 = new Employee();
        userEmployee1.setName("John Doe");
        userEmployee1.setEmail("john.doe@company.com");
        userEmployee1.setEmployeeNumber(2);
        userEmployee1.setRole(Role.USER);
        userEmployee1.setPassword("encodedPassword");
        userEmployee1.setDateOfBirth(LocalDate.of(1990, 5, 15));
        userEmployee1.setDepartment(testDepartment);
        userEmployee1 = employeeRepository.save(userEmployee1);

        userEmployee2 = new Employee();
        userEmployee2.setName("Jane Smith");
        userEmployee2.setEmail("jane.smith@company.com");
        userEmployee2.setEmployeeNumber(3);
        userEmployee2.setRole(Role.USER);
        userEmployee2.setPassword("encodedPassword");
        userEmployee2.setDateOfBirth(LocalDate.of(1992, 8, 20));
        userEmployee2.setDepartment(testDepartment);
        userEmployee2 = employeeRepository.save(userEmployee2);
    }

    // Test Case 1: Complete Task Creation with Department Assignment

    @Test
    @DisplayName("Integration: Should create task and assign to all department employees")
    void createTaskWithDepartmentAssignment_shouldAssignToAllDepartmentEmployees() {
        // Given
        Task task = new Task();
        task.setTitle("Department Meeting");
        task.setDescription("Weekly team meeting");
        task.setTaskType(TaskType.MEETING);
        task.setDueDateTime(LocalDateTime.now().plusDays(3));
        task.setLocation("Conference Room A");

        Task createdTask = taskService.createTaskWithAssignments(task, testDepartment.getId(), null);

        assertNotNull(createdTask, "Task should be created");
        assertNotNull(createdTask.getId(), "Task should have an ID");
        assertEquals("Department Meeting", createdTask.getTitle());
        assertEquals(TaskType.MEETING, createdTask.getTaskType());
        assertEquals(TaskStatus.PENDING, createdTask.getStatus());

        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(createdTask.getId());
        assertEquals(2, assignments.size(), "Should assign to both department employees");

        List<String> assignedEmployeeIds = assignments.stream()
                .map(assignment -> assignment.getEmployee().getId())
                .toList();
        assertTrue(assignedEmployeeIds.contains(userEmployee1.getId()),
                "Employee 1 should be assigned");
        assertTrue(assignedEmployeeIds.contains(userEmployee2.getId()),
                "Employee 2 should be assigned");

        assignments.forEach(assignment -> {
            assertEquals(TaskStatus.PENDING, assignment.getIndividualStatus());
            assertNotNull(assignment.getAssignedAt());
        });
    }

    // Test Case 2: Task Creation with Specific Employee Assignment

    @Test
    @DisplayName("Integration: Should create task and assign to specific employees")
    void createTaskWithSpecificEmployeeAssignment_shouldAssignToSpecifiedEmployees() {
        // Given
        Task task = new Task();
        task.setTitle("Personal Task");
        task.setDescription("Individual assignment");
        task.setTaskType(TaskType.PERSONAL);
        task.setDueDateTime(LocalDateTime.now().plusDays(5));

        List<String> employeeIds = Arrays.asList(userEmployee1.getId());

        // When
        Task createdTask = taskService.createTaskWithAssignments(task, null, employeeIds);

        // Then
        assertNotNull(createdTask, "Task should be created");
        assertEquals("Personal Task", createdTask.getTitle());
        assertEquals(TaskType.PERSONAL, createdTask.getTaskType());

        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(createdTask.getId());
        assertEquals(1, assignments.size(), "Should assign to only one employee");
        assertEquals(userEmployee1.getId(), assignments.get(0).getEmployee().getId());
    }

    // Test Case 3: Task Assignment Status Update Flow

    @Test
    @DisplayName("Integration: Should update task assignment status and track timing")
    void updateTaskAssignmentStatus_shouldUpdateStatusAndTrackTiming() {

        Task task = createTestTaskWithAssignment();
        TaskAssignment assignment = taskAssignmentRepository.findByTaskId(task.getId()).get(0);

        assertEquals(TaskStatus.PENDING, assignment.getIndividualStatus());
        assertNull(assignment.getStartedAt());

        TaskAssignment updatedAssignment = taskService.updateUserTaskStatus(
                task.getId(), userEmployee1.getId(), TaskStatus.IN_PROGRESS);

        assertNotNull(updatedAssignment, "Updated assignment should not be null");
        assertEquals(TaskStatus.IN_PROGRESS, updatedAssignment.getIndividualStatus());
        assertNotNull(updatedAssignment.getStartedAt(), "Started time should be set");

        TaskAssignment completedAssignment = taskService.updateUserTaskStatus(
                task.getId(), userEmployee1.getId(), TaskStatus.COMPLETED);

        assertEquals(TaskStatus.COMPLETED, completedAssignment.getIndividualStatus());
        assertNotNull(completedAssignment.getCompletedAt(), "Completed time should be set");
    }

    // ==================== Test Case 4: Task Report Submission ====================

    @Test
    @DisplayName("Integration: Should submit task report and update status")
    void submitTaskReport_shouldUpdateReportAndStatus() {
        // Given
        Task task = createTestTaskWithAssignment();
        String reportContent = "Task completed successfully with all requirements met.";

        // When
        TaskAssignment reportedAssignment = taskService.submitUserTaskReport(
                task.getId(), userEmployee1.getId(), reportContent, TaskStatus.COMPLETED);

        // Then
        assertNotNull(reportedAssignment, "Reported assignment should not be null");
        assertEquals(TaskStatus.COMPLETED, reportedAssignment.getIndividualStatus());
        assertEquals(reportContent, reportedAssignment.getReport());
        assertNotNull(reportedAssignment.getCompletedAt(), "Completed time should be set");
    }

    // Test Case 5: Employee Task Retrieval

    @Test
    @DisplayName("Integration: Should retrieve all tasks assigned to employee")
    void getEmployeeTasks_shouldReturnAllAssignedTasks() {
        Task task1 = createTestTask("Task 1", TaskType.PERSONAL);
        Task task2 = createTestTask("Task 2", TaskType.MEETING);
        Task task3 = createTestTask("Task 3", TaskType.MAINTENANCE);

        taskService.createTaskWithAssignments(task1, null, Arrays.asList(userEmployee1.getId()));
        taskService.createTaskWithAssignments(task2, null, Arrays.asList(userEmployee1.getId()));
        taskService.createTaskWithAssignments(task3, null, Arrays.asList(userEmployee2.getId()));

        // When
        List<TaskAssignment> employeeTasks = taskAssignmentRepository
                .findByEmployeeIdOrderByAssignedAtDesc(userEmployee1.getId());

        // Then
        assertEquals(2, employeeTasks.size(), "Employee should have 2 assigned tasks");

        List<String> taskTitles = employeeTasks.stream()
                .map(assignment -> assignment.getTask().getTitle())
                .toList();
        assertTrue(taskTitles.contains("Task 1"), "Should include Task 1");
        assertTrue(taskTitles.contains("Task 2"), "Should include Task 2");
        assertFalse(taskTitles.contains("Task 3"), "Should not include Task 3");
    }

    // Test Case 6: Multi-Employee Task Completion

    @Test
    @DisplayName("Integration: Should handle task completion by multiple employees")
    void multiEmployeeTaskCompletion_shouldUpdateTaskStatusCorrectly() {

        Task task = new Task();
        task.setTitle("Team Task");
        task.setDescription("Task requiring multiple employees");
        task.setTaskType(TaskType.MEETING);
        task.setDueDateTime(LocalDateTime.now().plusDays(2));
        task.setCreator(adminEmployee);

        Task createdTask = taskService.createTaskWithAssignments(task, testDepartment.getId(), null);

        assertEquals(TaskStatus.PENDING, createdTask.getStatus());

        taskService.updateUserTaskStatus(createdTask.getId(), userEmployee1.getId(), TaskStatus.IN_PROGRESS);

        entityManager.flush();
        entityManager.clear();

        Task updatedTask1 = taskRepository.findById(createdTask.getId()).orElse(null);
        assertNotNull(updatedTask1);

        List<TaskAssignment> assignments1 = taskAssignmentRepository.findByTaskId(createdTask.getId());
        boolean hasInProgress = assignments1.stream()
                .anyMatch(assignment -> assignment.getIndividualStatus() == TaskStatus.IN_PROGRESS);
        assertTrue(hasInProgress, "Should have at least one assignment in IN_PROGRESS status");

        taskService.updateUserTaskStatus(createdTask.getId(), userEmployee1.getId(), TaskStatus.COMPLETED);

        entityManager.flush();
        entityManager.clear();

        Task updatedTask2 = taskRepository.findById(createdTask.getId()).orElse(null);
        assertNotNull(updatedTask2);

        List<TaskAssignment> assignments2 = taskAssignmentRepository.findByTaskId(createdTask.getId());
        long completedCount = assignments2.stream()
                .filter(assignment -> assignment.getIndividualStatus() == TaskStatus.COMPLETED)
                .count();
        assertEquals(1, completedCount, "Should have exactly one completed assignment");

        taskService.updateUserTaskStatus(createdTask.getId(), userEmployee2.getId(), TaskStatus.COMPLETED);

        entityManager.flush();
        entityManager.clear();

        Task finalTask = taskRepository.findById(createdTask.getId()).orElse(null);
        assertNotNull(finalTask);

        List<TaskAssignment> finalAssignments = taskAssignmentRepository.findByTaskId(createdTask.getId());
        long finalCompletedCount = finalAssignments.stream()
                .filter(assignment -> assignment.getIndividualStatus() == TaskStatus.COMPLETED)
                .count();
        assertEquals(2, finalCompletedCount, "Should have two completed assignments");

        assertEquals(TaskStatus.COMPLETED, finalTask.getStatus(),
                "Task should be COMPLETED when all employees complete");
        assertNotNull(finalTask.getCompletedDateTime(), "Task completion time should be set");
    }

    // ==================== Test Case 7: Error Handling - Non-existent Task
    // ====================

    @Test
    @DisplayName("Integration: Should handle non-existent task gracefully")
    void updateNonExistentTask_shouldThrowException() {
        // Given
        Long nonExistentTaskId = 9999L;
        String employeeId = userEmployee1.getId();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.updateUserTaskStatus(nonExistentTaskId, employeeId, TaskStatus.IN_PROGRESS);
        });

        assertTrue(exception.getMessage().contains("task assignemnt not found") ||
                exception.getMessage().contains("not found"),
                "Exception should mention task not found");
    }

    // ==================== Helper Methods ====================

    private Task createTestTaskWithAssignment() {
        Task task = new Task();
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setTaskType(TaskType.PERSONAL);
        task.setDueDateTime(LocalDateTime.now().plusDays(7));
        task.setCreator(adminEmployee);

        return taskService.createTaskWithAssignments(task, null, Arrays.asList(userEmployee1.getId()));
    }

    private Task createTestTask(String title, TaskType taskType) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription("Test task: " + title);
        task.setTaskType(taskType);
        task.setDueDateTime(LocalDateTime.now().plusDays(7));
        task.setCreator(adminEmployee);

        return taskRepository.save(task);
    }
}