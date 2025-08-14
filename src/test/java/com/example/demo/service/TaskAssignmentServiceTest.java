package com.example.demo.service;

import com.example.demo.entity.Task;
import com.example.demo.entity.TaskAssignment;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.enums.TaskStatus;
import com.example.demo.enums.Role;
import com.example.demo.repository.TaskAssignmentRepository;
import com.example.demo.server.TaskAssignmentService;
import com.example.demo.server.TaskService;
import com.example.demo.server.EmployeeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskAssignmentService
 * Testing core assignment management and status update logic
 */
@ExtendWith(MockitoExtension.class)
class TaskAssignmentServiceTest {

        @Mock
        private TaskAssignmentRepository taskAssignmentRepository;

        @Mock
        private TaskService taskService;

        @Mock
        private EmployeeService employeeService;

        private TaskAssignmentService taskAssignmentService;

        private Task testTask;
        private Employee testEmployee;
        private TaskAssignment testAssignment;
        private Department testDepartment;

        @BeforeEach
        void setUp() {
                taskAssignmentService = new TaskAssignmentService();

                ReflectionTestUtils.setField(taskAssignmentService, "taskAssignmentRepository",
                                taskAssignmentRepository);
                ReflectionTestUtils.setField(taskAssignmentService, "taskService", taskService);
                ReflectionTestUtils.setField(taskAssignmentService, "employeeService", employeeService);

                setupTestData();
        }

        private void setupTestData() {
                // Test Department
                testDepartment = new Department();
                testDepartment.setId(1);
                testDepartment.setName("Engineering");

                // Test Task
                testTask = new Task();
                testTask.setId(1L);
                testTask.setTitle("Test Task");
                testTask.setDescription("Test Description");
                testTask.setStatus(TaskStatus.PENDING);
                testTask.setDueDateTime(LocalDateTime.now().plusDays(7));

                // Test Employee
                testEmployee = new Employee();
                testEmployee.setId("0001");
                testEmployee.setName("John Doe");
                testEmployee.setEmail("john.doe@company.com");
                testEmployee.setEmployeeNumber(1);
                testEmployee.setRole(Role.USER);
                testEmployee.setDepartment(testDepartment);

                // Test Assignment
                testAssignment = new TaskAssignment();
                testAssignment.setId(1L);
                testAssignment.setTask(testTask);
                testAssignment.setEmployee(testEmployee);
                testAssignment.setIndividualStatus(TaskStatus.PENDING);
                testAssignment.setAssignedAt(LocalDateTime.now());
        }

        // Test Case 1: Update Assignment Status - PENDING to IN_PROGRESS

        @Test
        @DisplayName("Should update assignment status from PENDING to IN_PROGRESS and set started time")
        void updateAssignmentStatus_fromPendingToInProgress_shouldSetStartedTime() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";
                TaskStatus newStatus = TaskStatus.IN_PROGRESS;
                String report = null;

                testAssignment.setIndividualStatus(TaskStatus.PENDING);
                testAssignment.setStartedAt(null);

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.of(testAssignment));
                when(taskAssignmentRepository.save(any(TaskAssignment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // When
                TaskAssignment result = taskAssignmentService.updateAssignmentStatus(taskId, employeeId, newStatus,
                                report);

                // Then
                assertNotNull(result, "Updated assignment should not be null");
                assertEquals(TaskStatus.IN_PROGRESS, result.getIndividualStatus());
                assertNotNull(result.getStartedAt(), "Started time should be set when status changes to IN_PROGRESS");
                assertNull(result.getReport(), "Report should remain null");

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
                verify(taskAssignmentRepository, times(1)).save(testAssignment);
        }

        // Test Case 2: Update Assignment Status - IN_PROGRESS to COMPLETED

        @Test
        @DisplayName("Should update assignment status to COMPLETED and set assigned time if null")
        void updateAssignmentStatus_toCompleted_shouldSetAssignedTimeIfNull() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";
                TaskStatus newStatus = TaskStatus.COMPLETED;
                String report = "Task completed successfully";

                testAssignment.setIndividualStatus(TaskStatus.IN_PROGRESS);
                testAssignment.setStartedAt(LocalDateTime.now().minusHours(2));
                testAssignment.setAssignedAt(null);

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.of(testAssignment));
                when(taskAssignmentRepository.save(any(TaskAssignment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // When
                TaskAssignment result = taskAssignmentService.updateAssignmentStatus(taskId, employeeId, newStatus,
                                report);

                // Then
                assertNotNull(result, "Updated assignment should not be null");
                assertEquals(TaskStatus.COMPLETED, result.getIndividualStatus());
                assertEquals(report, result.getReport());
                assertNotNull(result.getAssignedAt(),
                                "Assigned time should be set when status changes to COMPLETED and assignedAt is null");

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
                verify(taskAssignmentRepository, times(1)).save(testAssignment);
        }

        // Test Case 3: Update Assignment Status - Assignment Not Found

        @Test
        @DisplayName("Should throw exception when assignment not found")
        void updateAssignmentStatus_assignmentNotFound_shouldThrowException() {
                // Given
                Long taskId = 999L;
                String employeeId = "0001";
                TaskStatus newStatus = TaskStatus.IN_PROGRESS;
                String report = null;

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.empty());

                // When & Then
                RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                        taskAssignmentService.updateAssignmentStatus(taskId, employeeId, newStatus, report);
                });

                assertTrue(exception.getMessage().contains("task assignment not found"),
                                "Exception message should mention assignment not found");
                assertTrue(exception.getMessage().contains("task " + taskId),
                                "Exception message should include task ID");
                assertTrue(exception.getMessage().contains("employee " + employeeId),
                                "Exception message should include employee ID");

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
                verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        }

        // Test Case 4: Create Assignment - Success

        @Test
        @DisplayName("Should create assignment successfully when task and employee exist")
        void createAssignment_withValidData_shouldCreateSuccessfully() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";

                when(taskService.getTaskByIdOrThrow(taskId)).thenReturn(testTask);
                when(employeeService.getEmployeeByIdOrThrow(employeeId)).thenReturn(testEmployee);
                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.empty());
                when(taskAssignmentRepository.save(any(TaskAssignment.class)))
                                .thenAnswer(invocation -> {
                                        TaskAssignment savedAssignment = invocation.getArgument(0);
                                        savedAssignment.setId(1L);
                                        return savedAssignment;
                                });

                // When
                TaskAssignment result = taskAssignmentService.createAssignment(taskId, employeeId);

                // Then
                assertNotNull(result, "Created assignment should not be null");
                assertEquals(testTask, result.getTask());
                assertEquals(testEmployee, result.getEmployee());
                assertNotNull(result.getAssignedAt(), "Assigned time should be set");

                // Verify interactions
                verify(taskService, times(1)).getTaskByIdOrThrow(taskId);
                verify(employeeService, times(1)).getEmployeeByIdOrThrow(employeeId);
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
                verify(taskAssignmentRepository, times(1)).save(any(TaskAssignment.class));
        }

        // Test Case 5: Create Assignment - Duplicate Assignment

        @Test
        @DisplayName("Should throw exception when assignment already exists")
        void createAssignment_withDuplicateAssignment_shouldThrowException() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";

                when(taskService.getTaskByIdOrThrow(taskId)).thenReturn(testTask);
                when(employeeService.getEmployeeByIdOrThrow(employeeId)).thenReturn(testEmployee);
                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.of(testAssignment));

                // When & Then
                RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                        taskAssignmentService.createAssignment(taskId, employeeId);
                });

                assertTrue(exception.getMessage().contains("already assigned"),
                                "Exception message should mention already assigned");

                // Verify interactions
                verify(taskService, times(1)).getTaskByIdOrThrow(taskId);
                verify(employeeService, times(1)).getEmployeeByIdOrThrow(employeeId);
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
                verify(taskAssignmentRepository, never()).save(any(TaskAssignment.class));
        }

        // Test Case 6: Start Task Convenience Method

        @Test
        @DisplayName("Should start task using convenience method")
        void startTask_shouldCallUpdateAssignmentStatusWithInProgress() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";

                testAssignment.setIndividualStatus(TaskStatus.PENDING);

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.of(testAssignment));
                when(taskAssignmentRepository.save(any(TaskAssignment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // When
                TaskAssignment result = taskAssignmentService.startTask(taskId, employeeId);

                // Then
                assertNotNull(result, "Started task assignment should not be null");
                assertEquals(TaskStatus.IN_PROGRESS, result.getIndividualStatus());

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
                verify(taskAssignmentRepository, times(1)).save(testAssignment);
        }

        // Test Case 7: Complete Task Convenience Method

        @Test
        @DisplayName("Should complete task with report using convenience method")
        void completeTask_shouldCallUpdateAssignmentStatusWithCompleted() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";
                String report = "Task completed with report";

                testAssignment.setIndividualStatus(TaskStatus.IN_PROGRESS);

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.of(testAssignment));
                when(taskAssignmentRepository.save(any(TaskAssignment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // When
                TaskAssignment result = taskAssignmentService.completeTask(taskId, employeeId, report);

                // Then
                assertNotNull(result, "Completed task assignment should not be null");
                assertEquals(TaskStatus.COMPLETED, result.getIndividualStatus());
                assertEquals(report, result.getReport());

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
                verify(taskAssignmentRepository, times(1)).save(testAssignment);
        }

        // Test Case 8: Get Assignment by Task and Employee
        @Test
        @DisplayName("Should return assignment when found by task and employee")
        void getAssignment_whenExists_shouldReturnAssignment() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.of(testAssignment));

                // When
                Optional<TaskAssignment> result = taskAssignmentService.getAssignment(taskId, employeeId);

                // Then
                assertTrue(result.isPresent(), "Assignment should be found");
                assertEquals(testAssignment.getId(), result.get().getId());
                assertEquals(testTask, result.get().getTask());
                assertEquals(testEmployee, result.get().getEmployee());

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
        }

        // Test Case 9: Get Assignment - Not Found

        @Test
        @DisplayName("Should return empty when assignment not found")
        void getAssignment_whenNotExists_shouldReturnEmpty() {
                // Given
                Long taskId = 999L;
                String employeeId = "0001";

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.empty());

                // When
                Optional<TaskAssignment> result = taskAssignmentService.getAssignment(taskId, employeeId);

                // Then
                assertFalse(result.isPresent(), "Assignment should not be found");

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
        }

        // Test Case 10: Get Assignments by Employee

        @Test
        @DisplayName("Should return all assignments for employee")
        void getAssignmentsByEmployee_shouldReturnEmployeeAssignments() {
                // Given
                String employeeId = "0001";

                TaskAssignment assignment2 = new TaskAssignment();
                assignment2.setId(2L);
                assignment2.setEmployee(testEmployee);

                List<TaskAssignment> assignments = Arrays.asList(testAssignment, assignment2);

                when(taskAssignmentRepository.findByEmployeeIdOrderByAssignedAtDesc(employeeId))
                                .thenReturn(assignments);

                // When
                List<TaskAssignment> result = taskAssignmentService.getAssignmentsByEmployee(employeeId);

                // Then
                assertNotNull(result, "Assignment list should not be null");
                assertEquals(2, result.size());
                assertEquals(testAssignment.getId(), result.get(0).getId());
                assertEquals(assignment2.getId(), result.get(1).getId());

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByEmployeeIdOrderByAssignedAtDesc(employeeId);
        }

        // Test Case 11: Is Task Assigned to Employee

        @Test
        @DisplayName("Should return true when task is assigned to employee")
        void isTaskAssignedToEmployee_whenAssigned_shouldReturnTrue() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.of(testAssignment));

                // When
                boolean result = taskAssignmentService.isTaskAssignedToEmployee(taskId, employeeId);

                // Then
                assertTrue(result, "Should return true when task is assigned to employee");

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
        }

        // Test Case 12: Is Task Not Assigned to Employee

        @Test
        @DisplayName("Should return false when task is not assigned to employee")
        void isTaskAssignedToEmployee_whenNotAssigned_shouldReturnFalse() {
                // Given
                Long taskId = 1L;
                String employeeId = "0001";

                when(taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId))
                                .thenReturn(Optional.empty());

                // When
                boolean result = taskAssignmentService.isTaskAssignedToEmployee(taskId, employeeId);

                // Then
                assertFalse(result, "Should return false when task is not assigned to employee");

                // Verify interactions
                verify(taskAssignmentRepository, times(1)).findByTaskIdAndEmployeeId(taskId, employeeId);
        }
}