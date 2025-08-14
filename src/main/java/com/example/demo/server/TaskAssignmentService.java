package com.example.demo.server;

import com.example.demo.entity.TaskAssignment;
import com.example.demo.entity.Task;
import com.example.demo.entity.Employee;
import com.example.demo.enums.TaskStatus;
import com.example.demo.repository.TaskAssignmentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskAssignmentService {
    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private EmployeeService employeeService;

    public List<TaskAssignment> getAllAssignments() {
        return taskAssignmentRepository.findAll();
    }

    public Optional<TaskAssignment> getAssignmentById(Long id) {
        return taskAssignmentRepository.findById(id);
    }

    public TaskAssignment getAssignmentByIdOrThrow(Long id) {
        return taskAssignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task assignment not found with id: " + id));
    }

    public TaskAssignment createAssignment(Long taskId, String employeeId) {
        Task task = taskService.getTaskByIdOrThrow(taskId);
        Employee employee = employeeService.getEmployeeByIdOrThrow(employeeId);

        Optional<TaskAssignment> existingAssignment = taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId,
                employeeId);
        if (existingAssignment.isPresent()) {
            throw new RuntimeException("Task: " + taskId + " is already assigned to emplpyee");
        }
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setEmployee(employee);
        assignment.setAssignedAt(LocalDateTime.now());
        return taskAssignmentRepository.save(assignment);
    }

    public void deleteAssignment(Long id) {
        if (!taskAssignmentRepository.existsById(id)) {
            throw new RuntimeException("task assignment not found with id: " + id);
        }
        taskAssignmentRepository.deleteById(id);
    }

    public void deleteAssignment(Long id, String employeeId) {
        taskAssignmentRepository.deleteByTaskIdAndEmployeeId(id, employeeId);
    }

    public List<TaskAssignment> getAssignmentsByTask(Long taskId) {
        return taskAssignmentRepository.findByTaskId(taskId);
    }

    public List<TaskAssignment> getAssignmentsByEmployee(String employeeId) {
        return taskAssignmentRepository.findByEmployeeIdOrderByAssignedAtDesc(employeeId);
    }

    public List<TaskAssignment> getAssignmentsByEmployeeAndStatus(String employeeId, TaskStatus status) {
        return taskAssignmentRepository.findByEmployeeAndIndividualStatus(null, status);
    }

    public Optional<TaskAssignment> getAssignment(Long taskId, String employeId) {
        return taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeId);
    }

    public List<TaskAssignment> getUncompletedTaskSByEmployee(String employeeId) {
        return taskAssignmentRepository.findUncompletedTasksByEmployee(employeeId);
    }

    public List<TaskAssignment> getAssignmentsByDepartment(Integer departmentId) {
        return taskAssignmentRepository.findByEmployeeDepartmentId(departmentId);
    }

    public TaskAssignment updateAssignmentStatus(Long taskId, String employeId, TaskStatus newStatus, String report) {
        TaskAssignment assignment = taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeId)
                .orElseThrow(() -> new RuntimeException(
                        "task assignment not found for task " + taskId + " and employee " + employeId));

        assignment.setIndividualStatus(newStatus);
        assignment.setReport(report);
        if (newStatus == TaskStatus.IN_PROGRESS && assignment.getStartedAt() == null) {
            assignment.setStartedAt(LocalDateTime.now());
        } else if (newStatus == TaskStatus.COMPLETED && assignment.getAssignedAt() == null) {
            assignment.setAssignedAt(LocalDateTime.now());
        }
        return taskAssignmentRepository.save(assignment);
    }

    public TaskAssignment startTask(Long taskId, String employeeId) {
        return updateAssignmentStatus(taskId, employeeId, TaskStatus.IN_PROGRESS, null);
    }

    public TaskAssignment completeTask(Long taskId, String employeeId, String report) {
        return updateAssignmentStatus(taskId, employeeId, TaskStatus.COMPLETED, report);
    }

    public List<TaskAssignment> getEmployeeTaskDueToday() {
        return taskAssignmentRepository.findEmployeeTasksDueToday(LocalDateTime.now());
    }

    public List<TaskAssignment> getOverDueTasksByEmployee(String employeeId) {
        return taskAssignmentRepository.findOverdueTasksByEmployee(employeeId, LocalDateTime.now());
    }

    public long getAssignmentCountByEmployee(String employeeId) {
        return taskAssignmentRepository.countByEmployeeId(employeeId);
    }

    public long getAssignmentCountByEmployeeAndStatus(String employeeId, TaskStatus status) {
        return taskAssignmentRepository.countByEmployeeIdAndIndividualStatus(employeeId, status);
    }

    public long getAssignmentCountByTask(Long taskId) {
        return taskAssignmentRepository.countByTaskId(taskId);
    }

    public long getAssignmentCountByTaskAndStatus(Long taskId, TaskStatus status) {
        return taskAssignmentRepository.countByTaskIdAndIndividualStatus(taskId, status);
    }

    public List<TaskAssignment> getAssignmentsWithReports() {
        return taskAssignmentRepository.findAssignmentsWithReports();
    }

    public void assignTaskToMultipleEmployees(Long taskId, List<String> employeeIds) {
        for (String employeeId : employeeIds) {
            try {
                createAssignment(taskId, employeeId);
            } catch (RuntimeException e) {
                System.err.println(
                        "failed to assign task " + taskId + " to employee " + employeeId + " : " + e.getMessage());
            }
        }
    }

    public void unassignAllEmployeesFromTask(Long taskId) {
        taskAssignmentRepository.deleteById(taskId);
    }

    public void unassignAllTasksFromEmployee(String employeeID) {
        taskAssignmentRepository.deleteByEmployeeId(employeeID);
    }

    public boolean isTaskAssignedToEmployee(Long taskId, String employeeId) {
        return taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId).isPresent();
    }
}
