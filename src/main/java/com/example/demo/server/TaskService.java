package com.example.demo.server;

import com.example.demo.entity.Task;
import com.example.demo.entity.TaskAssignment;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.enums.TaskType;
import com.example.demo.enums.RecurrenceType;
import com.example.demo.enums.TaskStatus;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.TaskAssignmentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.HashSet;

@Service
@Transactional
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private DepartmentService departmentService;

    public List<Task> getAllTasks() {
        return taskRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public Task getTaskByIdOrThrow(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new RuntimeException("task not found with id: " + id));
    }

    public Task createTaskWithAssignments(Task task, Integer departmentId, List<String> employeeIds) {// create and
                                                                                                      // assign tasks

        if (task.getCreator() == null) {
            List<Employee> admins = employeeService.getAllAdmins();
            if (!admins.isEmpty()) {
                task.setCreator(admins.get(0));// set first admin as a creator
            } else {
                throw new RuntimeException("no admin found to set as task creator");
            }
        }
        Task savedTask = taskRepository.save(task);

        Set<Employee> assignedEmployees = new HashSet<>();
        if (employeeIds != null && !employeeIds.isEmpty()) {// assigned to specific employee
            for (String employeeId : employeeIds) {
                Employee employee = employeeService.getEmployeeByIdOrThrow(employeeId);
                assignedEmployees.add(employee);
            }
        } else if (departmentId != null) {// assigned to specific department
            Department department = departmentService.getDepartmentByIdOrThrow(departmentId);
            List<Employee> deptEmployees = employeeService.getEmployeesByDepartment(departmentId);
            assignedEmployees.addAll(deptEmployees);
            savedTask.setDepartment(department);
        } else {
            List<Employee> allUsers = employeeService.getAllUsers();// assigned to all users
            assignedEmployees.addAll(allUsers);
        }
        for (Employee employee : assignedEmployees) {
            TaskAssignment assignment = new TaskAssignment();
            assignment.setTask(savedTask);
            assignment.setEmployee(employee);
            assignment.setAssignedAt(LocalDateTime.now());
            taskAssignmentRepository.save(assignment);
        }
        // savedTask.setAssignedEmployees(assignedEmployees);
        return taskRepository.save(savedTask);
    }

    public Task createTask(Task task) {// create task without assigned
        return createTaskWithAssignments(task, null, null);
    }

    public Task updateTaskWithAssignments(Long id, Task taskDetails, Integer departmentId, List<String> employeeIds) {
        // if there are any update/modify or reassign , it will delete the old record
        // and build a new one
        Task task = getTaskByIdOrThrow(id);
        if (taskDetails.getTitle() != null) {
            task.setTitle(taskDetails.getTitle());
        }
        if (taskDetails.getDescription() != null) {
            task.setDescription(taskDetails.getDescription());
        }
        if (taskDetails.getTaskType() != null) {
            task.setTaskType(taskDetails.getTaskType());
        }
        if (taskDetails.getDueDateTime() != null) {
            task.setDueDateTime(taskDetails.getDueDateTime());
        }
        if (taskDetails.getLocation() != null) {
            task.setLocation(taskDetails.getLocation());
        }
        if (taskDetails.isRecurring() != null) {
            task.setRecurring(taskDetails.isRecurring());
            if (taskDetails.isRecurring()) {
                task.setRecurrenceType(taskDetails.getRecurrenceType());
                task.setRecurrenceInterval(taskDetails.getRecurrenceInterval());
                task.setRecurrenceEndDate(taskDetails.getRecurrenceEndDate());
            }
        }

        if (employeeIds != null || departmentId != null) {
            taskAssignmentRepository.deleteByTaskId(id);
            return createTaskWithAssignments(task, departmentId, employeeIds);
        }
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task taskDetails) {// update without reassign
        return updateTaskWithAssignments(id, taskDetails, null, null);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task not found with id : " + id);
        }
        taskAssignmentRepository.deleteByTaskId(id);// delete the assigned records
        taskRepository.deleteById(id);// delete the task
    }

    public List<Task> getTasksByFilter(TaskType taskType, TaskStatus status) {
        // filter the task list according the condition
        if (taskType != null && status != null) {
            return taskRepository.findByTaskTypeAndStatus(taskType, status);
        } else if (taskType != null) {
            return taskRepository.findByTaskType(taskType);
        } else if (status != null) {
            return taskRepository.findByStatus(status);
        } else {
            return taskRepository.findAllByOrderByCreatedAtDesc();
        }
    }

    public List<Task> getTasksByEmployee(Employee employee, TaskType taskType, TaskStatus status) {
        // search tasks according to the employee/taskType/status
        if (taskType != null && status != null) {
            return taskRepository.findByAssignedEmployeeAndTaskTypeAndStatus(employee, taskType, status);
        } else if (taskType != null) {
            return taskRepository.findByAssignedEmployeeAndTaskType(employee, taskType);
        } else if (status != null) {
            return taskRepository.findByAssignedEmployeeAndStatus(employee, status);
        } else {
            return taskRepository.findByAssignedEmployee(employee);
        }
    }

    public List<Task> getTasksByEmployeeId(String emplyeeId, TaskType taskType, TaskStatus status) {
        // filter the tasks by employeeId
        Employee employee = employeeService.getEmployeeByIdOrThrow(emplyeeId);
        return getTasksByEmployee(employee, taskType, status);
    }

    public List<Task> getTasksByDepartment(Department department, TaskType taskType, TaskStatus status) {
        // filter task by department
        if (taskType != null && status != null) {
            return taskRepository.findByDepartmentAndTaskTypeAndStatus(department, taskType, status);
        } else if (taskType != null) {
            return taskRepository.findByDepartmentAndTaskType(department, taskType);
        } else if (status != null) {
            return taskRepository.findByDepartmentAndStatus(department, status);
        } else {
            return taskRepository.findByDepartment(department);
        }
    }

    public List<Task> getTasksByDepartmentId(Integer departmentId, TaskType taskType, TaskStatus status) {
        // filter task by departmentId
        Department department = departmentService.getDepartmentByIdOrThrow(departmentId);
        return getTasksByDepartment(department, taskType, status);
    }

    public List<Task> getTasksByCreator(Employee creator, TaskType taskType, TaskStatus status) {
        // filter task by role(admin/employee)
        if (taskType != null && status != null) {
            return taskRepository.findByCreatorAndTaskTypeAndStatus(creator, taskType, status);
        } else if (taskType != null) {
            return taskRepository.findByCreatorAndTaskType(creator, taskType);
        } else if (status != null) {
            return taskRepository.findByCreatorAndStatus(creator, status);
        } else {
            return taskRepository.findByCreator(creator);
        }
    }

    public List<Task> searchTasks(String keywoord) {// search for the tasks(keywords)
        if (keywoord == null || keywoord.trim().isEmpty()) {
            return getAllTasks();
        }
        return taskRepository.findByTitleOrDescriptionContaining(keywoord.trim());
    }

    public List<Task> getTasksDueToday() {
        // get the task that deadline is today
        return taskRepository.findTasksDueToday(LocalDateTime.now());
    }

    public List<Task> getTasksDueSoon(int hoursAhead) {
        // get the tasks that due within three days
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime later = now.plusHours(hoursAhead);
        return taskRepository.findTasksDueSoon(now, later);
    }

    public List<Task> getOverdueTasks() {
        // get the overdue tasks
        return taskRepository.findOverdueTasks(LocalDateTime.now());
    }

    public List<Task> getRecurringTasks() {// all recurring tasks
        return taskRepository.findByRecurringTrue();
    }

    public List<Task> getActiveRecuringTasks() {// all active recurring tasks
        return taskRepository.findActiveRecurringTasks(LocalDateTime.now());
    }

    public List<Task> getActiveRecurringTasksByType(RecurrenceType type) {
        // get the recurring tasks by type(weekly or monthly)
        return taskRepository.findActiveRecurringTasksByType(type, LocalDateTime.now());
    }

    public long getTotalTaskCount() {
        return taskRepository.count();
    }

    public long getTaskCountByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    public long getTaskCountByType(TaskType taskType) {
        return taskRepository.countByTaskType(taskType);
    }

    public long getTaskCountByEmployee(String employeeId) {
        return taskRepository.countByAssignedEmployee(employeeId);
    }

    public long getTaskCountByEmployeeAndStatus(String employeeId, TaskStatus status) {
        return taskRepository.countByAssignedEmployeeAndStatus(employeeId, status);
    }

    public long getTaskCountByDepartment(Integer departmentId) {
        Department department = departmentService.getDepartmentByIdOrThrow(departmentId);
        return taskRepository.countByDepartment(department);
    }

    public Task updateTaskStatus(long taskId, TaskStatus newStatus) {
        Task task = getTaskByIdOrThrow(taskId);
        task.setStatus(newStatus);
        if (newStatus == TaskStatus.COMPLETED) {
            task.setCompletedDateTime(LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    // public Task markReminderSent(Long taskId) {
    // Task task = getTaskByIdOrThrow(taskId);
    // task.setReminderSent(true);
    // return taskRepository.save(task);
    // }

    public boolean taskExists(Long id) {// check task id if it exists
        return taskRepository.existsById(id);
    }

    public List<Task> getRecentTasks(int limit) {
        List<Task> allTasks = taskRepository.findRecentTasks();
        return allTasks.size() > limit ? allTasks.subList(0, limit) : allTasks;
    }

    public List<Task> getUpcomingPendingTasks(int daysAhead) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(daysAhead);
        return taskRepository.findUpcomingPendingTasks(start, end);
    }

    // update the user status
    public TaskAssignment updateUserTaskStatus(Long taskId, String employeeId, TaskStatus newStatus) {
        Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("task assignemnt not found for task " + taskId + " and employee " + employeeId);
        }
        TaskAssignment assignment = assignmentOpt.get();
        assignment.setIndividualStatus(newStatus);

        if (newStatus == TaskStatus.IN_PROGRESS && assignment.getStartedAt() == null) {
            assignment.setStartedAt(LocalDateTime.now());
        }
        if (newStatus == TaskStatus.COMPLETED) {
            assignment.setCompletedAt(LocalDateTime.now());
        }
        updateTaskStatusBasedOnAssignments(taskId);
        return taskAssignmentRepository.save(assignment);
    }

    private void updateTaskStatusBasedOnAssignments(Long taskId) {
        Task task = getTaskByIdOrThrow(taskId);
        Set<TaskAssignment> assignments = task.getTaskAssignments();

        if (!assignments.isEmpty()) {
            long completedCount = assignments.stream()
                    .filter(assignment -> assignment.getIndividualStatus() == TaskStatus.COMPLETED)
                    .count();

            long inProgressCount = assignments.stream()
                    .filter(assignment -> assignment.getIndividualStatus() == TaskStatus.IN_PROGRESS)
                    .count();

            // assign task to multiple employees in department and define the logic of the
            // status
            TaskStatus newStatus;
            if (completedCount == assignments.size()) {
                newStatus = TaskStatus.COMPLETED;
                task.setCompletedDateTime(LocalDateTime.now());
            } else if (inProgressCount > 0) {
                newStatus = TaskStatus.IN_PROGRESS;
            } else {
                newStatus = TaskStatus.PENDING;
            }

            task.setStatus(newStatus);
            taskRepository.save(task);
        }
    }

    // offer the user report
    public TaskAssignment submitUserTaskReport(Long taskId, String employeeId, String report, TaskStatus status) {
        Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository.findByTaskIdAndEmployeeId(taskId, employeeId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("task assignemnt not found for task " + taskId + " and employee " + employeeId);
        }
        TaskAssignment assignemnt = assignmentOpt.get();
        assignemnt.setReport(report);
        assignemnt.setIndividualStatus(status);
        if (status == TaskStatus.COMPLETED) {
            assignemnt.setCompletedAt(LocalDateTime.now());
        }
        return taskAssignmentRepository.save(assignemnt);
    }

    // get all users tasks
    public List<TaskAssignment> getUserAllTasks(String employeeId) {
        return taskAssignmentRepository.findByEmployeeId(employeeId);
    }

    public boolean isTaskCreatedByUser(Long taskId, String employeeId) {
        Task task = getTaskByIdOrThrow(taskId);
        return task.getCreator() != null && task.getCreator().getId().equals(employeeId);
    }

    public Task createUserTask(Task task, String employeeId) {
        Employee creator = employeeService.getEmployeeByIdOrThrow(employeeId);
        task.setCreator(creator);
        List<String> employeeIds = List.of(employeeId);
        return createTaskWithAssignments(task, null, employeeIds);
    }

    public void deleteUserTask(Long taskId, String employeeId) {
        if (!isTaskCreatedByUser(taskId, employeeId)) {// ensure the employee can only delete the task that created by
                                                       // themselves
            throw new RuntimeException("You can only delete tasks created by yourself");
        }
        deleteTask(taskId);
    }

    public List<Task> getTasksCreatedByUser(String employeeId) {
        Employee creator = employeeService.getEmployeeByIdOrThrow(employeeId);
        return taskRepository.findByCreator(creator);
    }
}
