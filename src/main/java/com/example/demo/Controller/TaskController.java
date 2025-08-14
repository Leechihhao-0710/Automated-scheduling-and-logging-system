package com.example.demo.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Task;
import com.example.demo.entity.TaskAssignment;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.enums.TaskType;
import com.example.demo.repository.TaskAssignmentRepository;
import com.example.demo.enums.TaskStatus;
import com.example.demo.enums.RecurrenceType;
import com.example.demo.enums.Role;
import com.example.demo.server.TaskService;
import com.example.demo.server.EmployeeService;
import com.example.demo.server.DepartmentService;
import java.time.LocalDate;
import java.util.ArrayList;
import com.example.demo.enums.Role;
import java.time.LocalDate;
import java.time.Duration;

@Controller
@RequestMapping("/tasks")
public class TaskController {
    @Autowired
    private TaskService taskService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String adminDashboard(@AuthenticationPrincipal Employee employee, Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "tasks");
        return "admin/task_management";
    }

    // @PreAuthorize("hasRole('USER')")
    // @GetMapping("/user")
    // public String showUserTasks(@AuthenticationPrincipal Employee employee, Model
    // model) {
    // model.addAttribute("user", employee);
    // model.addAttribute("activePage", "tasks");
    // return "user/user_tasks";
    // }

    // @PreAuthorize("hasRole('USER')")
    // @GetMapping("/user/userTaskManagement")
    // public String userTaskManagement(@AuthenticationPrincipal Employee employee,
    // Model model) {
    // model.addAttribute("user", employee);
    // model.addAttribute("activePage", "userTaskManagement");
    // return "user/user_task_management";
    // }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTaskByFilter(
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String search) {
        try {
            List<Task> tasks;
            TaskType type = taskType != null && !taskType.isEmpty() ? TaskType.valueOf(taskType) : null;
            TaskStatus taskStatus = status != null && !status.isEmpty() ? TaskStatus.valueOf(status) : null;
            if (employeeId != null && !employeeId.isEmpty()) {
                tasks = taskService.getTasksByEmployeeId(search, type, taskStatus);
            } else if (departmentId != null) {
                tasks = taskService.getTasksByDepartmentId(departmentId, type, taskStatus);
            } else if (search != null && !search.trim().isEmpty()) {
                tasks = taskService.searchTasks(search.trim());
                if (type != null) {
                    tasks = tasks.stream().filter(t -> t.getTaskType() == type).collect(Collectors.toList());
                }
                if (taskStatus != null) {
                    tasks = tasks.stream().filter(t -> t.getStatus() == taskStatus).collect(Collectors.toList());
                }
            } else {
                tasks = taskService.getTasksByFilter(type, taskStatus);
            }
            List<Map<String, Object>> taskList = tasks.stream().map(this::convertTaskToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(taskList);
        } catch (Exception e) {
            System.err.println("error in getTaskByFilter: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTaskById(@PathVariable Long id) {
        try {
            Task task = taskService.getTaskByIdOrThrow(id);
            return ResponseEntity.ok(convertTaskToMap(task));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> taskData) {
        try {
            Task task = new Task();

            task.setTitle((String) taskData.get("title"));
            task.setDescription((String) taskData.get("description"));
            task.setTaskType(TaskType.valueOf((String) taskData.get("taskType")));
            task.setDueDateTime(LocalDateTime.parse((String) taskData.get("dueDateTime")));

            if (taskData.get("location") != null) {
                task.setLocation((String) taskData.get("location"));
            }

            Boolean isRecurring = (Boolean) taskData.get("recurring");
            if (isRecurring != null && isRecurring) {
                task.setRecurring(true);
                if (taskData.get("recurrenceType") != null) {
                    task.setRecurrenceType(RecurrenceType.valueOf((String) taskData.get("recurrenceType")));
                }
                if (taskData.get("recurrenceInterval") != null) {
                    task.setRecurrenceInterval((Integer) taskData.get("recurrenceInterval"));
                }
                if (taskData.get("recurrenceEndDate") != null) {
                    task.setRecurrenceEndDate(LocalDateTime.parse((String) taskData.get("recurrenceEndDate")));
                }
                if (taskData.get("recurringDayOfWeek") != null) {
                    task.setRecurringDayOfWeek((Integer) taskData.get("recurringDayOfWeek"));
                }
                if (taskData.get("recurringDayOfMonth") != null) {
                    task.setRecurringDayOfMonth((Integer) taskData.get("recurringDayOfMonth"));
                }
                if (taskData.get("skipWeekends") != null) {
                    task.setSkipWeekends((Boolean) taskData.get("skipWeekends"));
                }
            }

            // if (taskData.get("emailReminder") != null) {
            // task.setEmailReminder((Boolean) taskData.get("emailReminder"));
            // }
            // if (taskData.get("reminderDaysBefore") != null) {
            // task.setReminderDaysBefore((Integer) taskData.get("reminderDaysBefore"));
            // }

            Integer departmentId = (Integer) taskData.get("departmentId");
            @SuppressWarnings("unchecked")
            List<String> employeeIds = (List<String>) taskData.get("employeeIds");

            Task savedTask = taskService.createTaskWithAssignments(task, departmentId, employeeIds);

            return ResponseEntity.ok(convertTaskToMap(savedTask));

        } catch (Exception e) {
            System.err.println("Error creating task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createErrorResponse("Error creating task: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/user/create")
    @ResponseBody
    public ResponseEntity<?> createUserTask(@AuthenticationPrincipal Employee employee,
            @RequestBody Map<String, Object> taskData) {
        try {
            Task task = new Task();
            task.setTitle((String) taskData.get("title"));
            task.setDescription((String) taskData.get("description"));
            task.setTaskType(TaskType.valueOf((String) taskData.get("taskType")));
            task.setDueDateTime(LocalDateTime.parse((String) taskData.get("dueDateTime")));

            if (taskData.get("location") != null) {
                task.setLocation((String) taskData.get("location"));
            }

            task.setCreator(employee);

            List<String> employeeIds = List.of(employee.getId());
            Task savedTask = taskService.createTaskWithAssignments(task, null, employeeIds);

            return ResponseEntity.ok(convertTaskToMap(savedTask));

        } catch (Exception e) {
            System.err.println("Error creating user task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createErrorResponse("Error creating task: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> taskData) {
        try {
            Task taskDetails = new Task();

            if (taskData.get("title") != null) {
                taskDetails.setTitle((String) taskData.get("title"));
            }
            if (taskData.get("description") != null) {
                taskDetails.setDescription((String) taskData.get("description"));
            }
            if (taskData.get("taskType") != null) {
                taskDetails.setTaskType(TaskType.valueOf((String) taskData.get("taskType")));
            }
            if (taskData.get("dueDateTime") != null) {
                taskDetails.setDueDateTime(LocalDateTime.parse((String) taskData.get("dueDateTime")));
            }
            if (taskData.get("location") != null) {
                taskDetails.setLocation((String) taskData.get("location"));
            }

            if (taskData.get("recurring") != null) {
                Boolean isRecurring = (Boolean) taskData.get("recurring");
                taskDetails.setRecurring(isRecurring);
                if (isRecurring) {
                    if (taskData.get("recurrenceType") != null) {
                        taskDetails.setRecurrenceType(RecurrenceType.valueOf((String) taskData.get("recurrenceType")));
                    }
                    if (taskData.get("recurrenceInterval") != null) {
                        taskDetails.setRecurrenceInterval((Integer) taskData.get("recurrenceInterval"));
                    }
                    if (taskData.get("recurrenceEndDate") != null) {
                        taskDetails
                                .setRecurrenceEndDate(LocalDateTime.parse((String) taskData.get("recurrenceEndDate")));
                    }
                    if (taskData.get("recurringDayOfWeek") != null) {
                        taskDetails.setRecurringDayOfWeek((Integer) taskData.get("recurringDayOfWeek"));
                    }
                    if (taskData.get("recurringDayOfMonth") != null) {
                        taskDetails.setRecurringDayOfMonth((Integer) taskData.get("recurringDayOfMonth"));
                    }
                    if (taskData.get("skipWeekends") != null) {
                        taskDetails.setSkipWeekends((Boolean) taskData.get("skipWeekends"));
                    }
                }
            }

            // if (taskData.get("emailReminder") != null) {
            // taskDetails.setEmailReminder((Boolean) taskData.get("emailReminder"));
            // }
            // if (taskData.get("reminderDayssBefore") != null) {
            // taskDetails.setReminderDaysBefore((Integer)
            // taskData.get("reminderDaysBefore"));
            // }

            Integer departmentId = (Integer) taskData.get("departmentId");
            @SuppressWarnings("unchecked")
            List<String> employeeIds = (List<String>) taskData.get("employeeIds");

            Task updatedTask = taskService.updateTaskWithAssignments(id, taskDetails, departmentId, employeeIds);

            return ResponseEntity.ok(convertTaskToMap(updatedTask));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Error updating task: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error updating task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.ok(createSuccessResponse("Task deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Error deleting task: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error deleting task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/api/user/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUserTask(@AuthenticationPrincipal Employee employee,
            @PathVariable Long id) {
        try {
            Task task = taskService.getTaskByIdOrThrow(id);

            if (!task.getCreator().getId().equals(employee.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("You can only delete tasks created by yourself"));
            }

            taskService.deleteTask(id);
            return ResponseEntity.ok(createSuccessResponse("Task deleted successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Error deleting task: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error deleting user task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/{id}/status") // partial update
    @ResponseBody
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long id, @RequestBody Map<String, Object> statusData) {
        try {
            String statusStr = (String) statusData.get("status");
            TaskStatus newStatus = TaskStatus.valueOf(statusStr);

            Task updatedTask = taskService.updateTaskStatus(id, newStatus);
            return ResponseEntity.ok(convertTaskToMap(updatedTask));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error updating task status: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/departments")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllDepartments() {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            List<Map<String, Object>> departmentList = departments.stream()
                    .map(dept -> {
                        Map<String, Object> deptMap = new HashMap<>();
                        deptMap.put("id", dept.getId());
                        deptMap.put("name", dept.getName());
                        deptMap.put("description", dept.getDescription());
                        return deptMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(departmentList);
        } catch (Exception e) {
            System.err.println("Error getting departments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/departments/{departmentId}/employees")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEmployeesByDepartment(@PathVariable Integer departmentId) {
        try {
            List<Employee> employees = employeeService.getEmployeesByDepartment(departmentId);
            List<Map<String, Object>> employeeList = employees.stream()
                    .map(emp -> {
                        Map<String, Object> empMap = new HashMap<>();
                        empMap.put("id", emp.getId());
                        empMap.put("name", emp.getName());
                        empMap.put("employeeNumber", emp.getEmployeeNumber());
                        empMap.put("email", emp.getEmail());
                        return empMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(employeeList);
        } catch (Exception e) {
            System.err.println("Error getting employees by department: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/employees")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllEmployees() {
        try {
            List<Employee> employees = employeeService.getAllUsers();
            List<Map<String, Object>> employeeList = employees.stream()
                    .map(emp -> {
                        Map<String, Object> empMap = new HashMap<>();
                        empMap.put("id", emp.getId());
                        empMap.put("name", emp.getName());
                        empMap.put("employeeNumber", emp.getEmployeeNumber());
                        empMap.put("email", emp.getEmail());
                        empMap.put("department", emp.getDepartmentName());
                        return empMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(employeeList);
        } catch (Exception e) {
            System.err.println("Error getting all employees: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTaskStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", taskService.getTotalTaskCount());
            stats.put("pendingTasks", taskService.getTaskCountByStatus(TaskStatus.PENDING));
            stats.put("inProgressTasks", taskService.getTaskCountByStatus(TaskStatus.IN_PROGRESS));
            stats.put("completedTasks", taskService.getTaskCountByStatus(TaskStatus.COMPLETED));
            stats.put("overdueTasks", taskService.getOverdueTasks().size());
            stats.put("todayTasks", taskService.getTasksDueToday().size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting task stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> convertTaskToMap(Task task) {// transfer the structure
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("title", task.getTitle());
        taskMap.put("description", task.getDescription());
        taskMap.put("taskType", task.getTaskType().toString());

        TaskStatus displayStatus = calculateTaskDisplayStatus(task);
        taskMap.put("status", displayStatus.toString());

        taskMap.put("dueDateTime", task.getDueDateTime().toString());
        taskMap.put("completedDateTime",
                task.getCompletedDateTime() != null ? task.getCompletedDateTime().toString() : null);
        taskMap.put("location", task.getLocation());
        taskMap.put("recurring", task.isRecurring());
        taskMap.put("recurrenceType", task.getRecurrenceType() != null ? task.getRecurrenceType().toString() : null);
        taskMap.put("recurrenceInterval", task.getRecurrenceInterval());
        taskMap.put("recurrenceEndDate",
                task.getRecurrenceEndDate() != null ? task.getRecurrenceEndDate().toString() : null);
        // taskMap.put("emailReminder", task.isEmailReminder());
        // taskMap.put("reminderDaysBefore", task.getReminderDaysBefore());
        // taskMap.put("reminderSent", task.isReminderSent());
        taskMap.put("createdAt", task.getCreatedAt().toString());
        taskMap.put("updatedAt", task.getUpdatedAt().toString());

        if (task.getCreator() != null) {
            Map<String, Object> creatorMap = new HashMap<>();
            creatorMap.put("id", task.getCreator().getId());
            creatorMap.put("name", task.getCreator().getName());
            creatorMap.put("employeeNumber", task.getCreator().getEmployeeNumber());
            creatorMap.put("role", task.getCreator().getRole().toString());
            taskMap.put("creator", creatorMap);
        }

        if (task.getDepartment() != null) {
            Map<String, Object> deptMap = new HashMap<>();
            deptMap.put("id", task.getDepartment().getId());
            deptMap.put("name", task.getDepartment().getName());
            taskMap.put("department", deptMap);
        }

        List<Map<String, Object>> assignedEmployees = task.getTaskAssignments().stream()
                .map(assignment -> {
                    Map<String, Object> empMap = new HashMap<>();
                    Employee emp = assignment.getEmployee();
                    empMap.put("id", emp.getId());
                    empMap.put("name", emp.getName());
                    empMap.put("employeeNumber", emp.getEmployeeNumber());
                    empMap.put("email", emp.getEmail());
                    empMap.put("individualStatus", assignment.getIndividualStatus().toString());
                    return empMap;
                })
                .collect(Collectors.toList());
        taskMap.put("assignedEmployees", assignedEmployees);

        return taskMap;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    private TaskStatus calculateTaskDisplayStatus(Task task) {
        Set<TaskAssignment> assignments = task.getTaskAssignments();

        if (assignments.isEmpty()) {
            return task.getStatus();
        }

        long completedCount = assignments.stream()
                .filter(assignment -> assignment.getIndividualStatus() == TaskStatus.COMPLETED)
                .count();

        long inProgressCount = assignments.stream()
                .filter(assignment -> assignment.getIndividualStatus() == TaskStatus.IN_PROGRESS)
                .count();

        if (completedCount == assignments.size()) {
            return TaskStatus.COMPLETED;
        }

        else if (inProgressCount > 0) {
            return TaskStatus.IN_PROGRESS;
        }

        else {
            return TaskStatus.PENDING;
        }
    }

    // get user's all tasks
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/user/{employeeId}/tasks")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUserTasks(@PathVariable String employeeId,
            @AuthenticationPrincipal Employee employee) {
        try {
            if (!employee.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<TaskAssignment> assignments = taskAssignmentRepository.findByEmployeeId(employeeId);
            List<Map<String, Object>> taskList = assignments.stream()
                    .map(assignment -> {
                        Map<String, Object> taskMap = convertTaskToMap(assignment.getTask());
                        taskMap.put("individualStatus", assignment.getIndividualStatus().toString());
                        taskMap.put("assignedAt", assignment.getAssignedAt().toString());
                        taskMap.put("report", assignment.getReport());
                        return taskMap;
                    }).collect(Collectors.toList());
            return ResponseEntity.ok(taskList);
        } catch (Exception e) {
            System.err.println("error getting user tasks: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // update the user's task status
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/api/user/tasks/{taskId}/status")
    @ResponseBody
    public ResponseEntity<?> updateUserTaskStatus(
            @PathVariable Long taskId,
            @RequestParam String employeeId,
            @RequestBody Map<String, Object> statusData) {
        try {
            String statusStr = (String) statusData.get("status");
            TaskStatus newStatus = TaskStatus.valueOf(statusStr);

            TaskAssignment assignment = taskService.updateUserTaskStatus(taskId, employeeId, newStatus);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("indivudualStatus", assignment.getIndividualStatus().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("error updating task status: " + e.getMessage()));
        }
    }

    // report the tasks
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/api/user/tasks/{taskId}/report")
    @ResponseBody
    public ResponseEntity<?> submitTaskReport(
            @PathVariable Long taskId,
            @RequestParam String employeeId,
            @RequestBody Map<String, Object> reportData) {
        try {
            String report = (String) reportData.get("report");
            TaskStatus status = TaskStatus.valueOf((String) reportData.get("status"));

            TaskAssignment assignment = taskService.submitUserTaskReport(taskId, employeeId, report, status);

            return ResponseEntity.ok(createSuccessResponse(
                    "task report with assignmentId: " + assignment.getId() + "submitted succedssfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("error submitting report: " + e.getMessage()));
        }
    }

    // get the user tasks count
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/user/{employeeId}/task-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserTaskStats(@PathVariable String employeeId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", taskService.getTaskCountByEmployee(employeeId));
            stats.put("pendingTasks", taskService.getTaskCountByEmployeeAndStatus(employeeId, TaskStatus.PENDING));
            stats.put("inProgressTasks",
                    taskService.getTaskCountByEmployeeAndStatus(employeeId, TaskStatus.IN_PROGRESS));
            stats.put("completedTasks", taskService.getTaskCountByEmployeeAndStatus(employeeId, TaskStatus.COMPLETED));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting user task stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/overview")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTasksForAdminOverview(
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) List<String> employeeIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String creatorType) {
        try {
            List<Task> tasks;
            // if select specific department but not employee , get all employees in the
            // department
            List<String> targetEmployeeIds = new ArrayList<>();
            if (employeeIds != null && !employeeIds.isEmpty()) {
                targetEmployeeIds.addAll(employeeIds);
            } else if (employeeId != null && !employeeId.trim().isEmpty()) {
                targetEmployeeIds.add(employeeId);
            } else if (departmentId != null) {
                // get all employees ID
                List<Employee> departmentEmployees = employeeService.getEmployeesByDepartment(departmentId);
                targetEmployeeIds = departmentEmployees.stream().map(Employee::getId).collect(Collectors.toList());
            }
            // select with conditions
            TaskType type = taskType != null && !taskType.isEmpty() ? TaskType.valueOf(taskType) : null;
            TaskStatus taskStatus = status != null && !status.isEmpty() ? TaskStatus.valueOf(status) : null;

            if (!targetEmployeeIds.isEmpty()) {
                tasks = new ArrayList<>();
                for (String empId : targetEmployeeIds) {
                    List<Task> empTasks = taskService.getTasksByEmployeeId(empId, type, taskStatus);
                    tasks.addAll(empTasks);
                }
                tasks = tasks.stream().distinct().collect(Collectors.toList());
            } else {
                tasks = taskService.getTasksByFilter(type, taskStatus);
            }

            if (creatorType != null && !creatorType.isEmpty()) {
                if ("admin".equals(creatorType)) {
                    tasks = tasks.stream()
                            .filter(task -> task.getCreator() != null && task.getCreator().getRole() == Role.ADMIN)
                            .collect(Collectors.toList());
                } else if ("employee".equals(creatorType)) {
                    tasks = tasks.stream()
                            .filter(task -> task.getCreator() != null && task.getCreator().getRole() == Role.USER)
                            .collect(Collectors.toList());
                }
            }
            // search with keywords
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = search.trim().toLowerCase();
                tasks = tasks.stream()
                        .filter(task -> task.getTitle().toLowerCase().contains(searchTerm) ||
                                (task.getDescription() != null
                                        && task.getDescription().toLowerCase().contains(searchTerm)))
                        .collect(Collectors.toList());
            }
            // search with date
            if (startDate != null && !startDate.isEmpty()) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                tasks = tasks.stream()
                        .filter(task -> task.getCreatedAt().isAfter(start) || task.getCreatedAt().isEqual(start))
                        .collect(Collectors.toList());
            }

            if (endDate != null && !endDate.isEmpty()) {
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                tasks = tasks.stream()
                        .filter(task -> task.getCreatedAt().isBefore(end) || task.getCreatedAt().isEqual(end))
                        .collect(Collectors.toList());
            }
            List<Map<String, Object>> taskList = tasks.stream().map(this::convertTaskToMap)
                    .collect(Collectors.toList());

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalTasks", taskList.size());
            statistics.put("pendingTasks",
                    tasks.stream().filter(t -> calculateTaskDisplayStatus(t) == TaskStatus.PENDING).count());
            statistics.put("inProgressTasks",
                    tasks.stream().filter(t -> calculateTaskDisplayStatus(t) == TaskStatus.IN_PROGRESS).count());
            statistics.put("completedTasks",
                    tasks.stream().filter(t -> calculateTaskDisplayStatus(t) == TaskStatus.COMPLETED).count());
            statistics.put("overdueTasks",
                    tasks.stream().filter(t -> t.getDueDateTime().isBefore(LocalDateTime.now())
                            && t.getStatus() != TaskStatus.COMPLETED).count());
            Map<String, Object> response = new HashMap<>();
            response.put("tasks", taskList);
            response.put("statistics", statistics);
            response.put("totalCount", taskList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in getTasksForAdminOverview: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving tasks: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/department-stats/{departmentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDepartmentTaskStats(@PathVariable Integer departmentId) {
        try {
            List<Employee> employees = employeeService.getEmployeesByDepartment(departmentId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("departmentId", departmentId);
            stats.put("employeeCount", employees.size());

            long totalTasks = 0;
            long pendingTasks = 0;
            long inProgressTasks = 0;
            long completedTasks = 0;

            for (Employee emp : employees) {
                totalTasks += taskService.getTaskCountByEmployee(emp.getId());
                pendingTasks += taskService.getTaskCountByEmployeeAndStatus(emp.getId(), TaskStatus.PENDING);
                inProgressTasks += taskService.getTaskCountByEmployeeAndStatus(emp.getId(), TaskStatus.IN_PROGRESS);
                completedTasks += taskService.getTaskCountByEmployeeAndStatus(emp.getId(), TaskStatus.COMPLETED);
            }

            stats.put("totalTasks", totalTasks);
            stats.put("pendingTasks", pendingTasks);
            stats.put("inProgressTasks", inProgressTasks);
            stats.put("completedTasks", completedTasks);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("Error getting department stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting department statistics"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/task-details/{taskId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTaskDetails(@PathVariable Long taskId) {
        try {
            Task task = taskService.getTaskByIdOrThrow(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("task", convertTaskToMap(task));

            List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(taskId);
            List<Map<String, Object>> assignmentDetails = assignments.stream()
                    .map(assignment -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("employee", assignment.getEmployee().getName());
                        detail.put("employeeNumber", assignment.getEmployee().getEmployeeNumber());
                        detail.put("individualStatus", assignment.getIndividualStatus().toString());
                        detail.put("assignedAt", assignment.getAssignedAt().toString());
                        detail.put("startedAt",
                                assignment.getStartedAt() != null ? assignment.getStartedAt().toString() : null);
                        detail.put("completedAt",
                                assignment.getCompletedAt() != null ? assignment.getCompletedAt().toString() : null);
                        detail.put("report", assignment.getReport());
                        return detail;
                    })
                    .collect(Collectors.toList());

            response.put("assignments", assignmentDetails);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error getting task details"));
        }
    }

    // dashboard statistics for user - for user dashboard
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/user/{employeeId}/dashboard-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserDashboardStats(@PathVariable String employeeId,
            @AuthenticationPrincipal Employee employee) {
        try {
            if (!employee.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Map<String, Object> stats = new HashMap<>();

            // Active tasks (not completed)
            long activeTasks = taskAssignmentRepository.countByEmployeeIdAndIndividualStatus(employeeId,
                    TaskStatus.PENDING) +
                    taskAssignmentRepository.countByEmployeeIdAndIndividualStatus(employeeId, TaskStatus.IN_PROGRESS);

            // Tasks due today
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
            long dueToday = taskAssignmentRepository.findByEmployeeId(employeeId).stream()
                    .filter(ta -> ta.getIndividualStatus() != TaskStatus.COMPLETED)
                    .filter(ta -> {
                        LocalDateTime dueDateTime = ta.getTask().getDueDateTime();
                        return dueDateTime.isAfter(startOfDay) && dueDateTime.isBefore(endOfDay);
                    })
                    .count();

            // Overdue tasks
            long overdue = taskAssignmentRepository.findByEmployeeId(employeeId).stream()
                    .filter(ta -> ta.getIndividualStatus() != TaskStatus.COMPLETED)
                    .filter(ta -> ta.getTask().getDueDateTime().isBefore(LocalDateTime.now()))
                    .count();

            // Completed this month
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                    .withNano(0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
            long completedThisMonth = taskAssignmentRepository.findByEmployeeId(employeeId).stream()
                    .filter(ta -> ta.getIndividualStatus() == TaskStatus.COMPLETED)
                    .filter(ta -> ta.getCompletedAt() != null)
                    .filter(ta -> ta.getCompletedAt().isAfter(startOfMonth) && ta.getCompletedAt().isBefore(endOfMonth))
                    .count();

            stats.put("activeTasks", activeTasks);
            stats.put("dueToday", dueToday);
            stats.put("overdue", overdue);
            stats.put("completedThisMonth", completedThisMonth);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting user dashboard stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // upcoming tasks(due in next 3 days) - for user dashboard
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/user/{employeeId}/upcoming-tasks")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUserUpcomingTasks(@PathVariable String employeeId,
            @AuthenticationPrincipal Employee employee) {
        try {
            if (!employee.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeDaysLater = now.plusDays(3);

            List<TaskAssignment> assignments = taskAssignmentRepository.findByEmployeeId(employeeId).stream()
                    .filter(ta -> ta.getIndividualStatus() != TaskStatus.COMPLETED)
                    .filter(ta -> ta.getTask().getDueDateTime().isAfter(now) &&
                            ta.getTask().getDueDateTime().isBefore(threeDaysLater))
                    .sorted((ta1, ta2) -> ta1.getTask().getDueDateTime().compareTo(ta2.getTask().getDueDateTime()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> taskList = assignments.stream()
                    .map(assignment -> {
                        Map<String, Object> taskMap = new HashMap<>();
                        Task task = assignment.getTask();

                        taskMap.put("id", task.getId());
                        taskMap.put("title", task.getTitle());
                        taskMap.put("description", task.getDescription());
                        taskMap.put("dueDateTime", task.getDueDateTime().toString());
                        taskMap.put("individualStatus", assignment.getIndividualStatus().toString());
                        taskMap.put("taskType", task.getTaskType().toString());

                        return taskMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(taskList);
        } catch (Exception e) {
            System.err.println("Error getting user upcoming tasks: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // recent tasks assigned by admin(in last 3 days) - for user dashboard
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/user/{employeeId}/recent-assignments")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUserRecentAssignments(@PathVariable String employeeId,
            @AuthenticationPrincipal Employee employee) {
        try {
            if (!employee.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

            List<TaskAssignment> recentAssignments = taskAssignmentRepository.findByEmployeeId(employeeId).stream()
                    .filter(ta -> ta.getAssignedAt().isAfter(threeDaysAgo))
                    .sorted((ta1, ta2) -> ta2.getAssignedAt().compareTo(ta1.getAssignedAt())) // Most recent first
                    .limit(10) // Limit to 10 most recent
                    .collect(Collectors.toList());

            List<Map<String, Object>> notificationList = recentAssignments.stream()
                    .map(assignment -> {
                        Map<String, Object> notificationMap = new HashMap<>();
                        Task task = assignment.getTask();

                        notificationMap.put("assignedAt", assignment.getAssignedAt().toString());

                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("id", task.getId());
                        taskMap.put("title", task.getTitle());
                        taskMap.put("description", task.getDescription());
                        taskMap.put("taskType", task.getTaskType().toString());

                        if (task.getCreator() != null) {
                            Map<String, Object> creatorMap = new HashMap<>();
                            creatorMap.put("id", task.getCreator().getId());
                            creatorMap.put("name", task.getCreator().getName());
                            taskMap.put("creator", creatorMap);
                        }

                        notificationMap.put("task", taskMap);

                        return notificationMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(notificationList);
        } catch (Exception e) {
            System.err.println("Error getting user recent assignments: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/user/{employeeId}/overview")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUserOverviewTasks(
            @PathVariable String employeeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String creatorRole,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal Employee employee) {

        try {
            if (!employee.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            TaskType type = taskType != null && !taskType.isEmpty() ? TaskType.valueOf(taskType) : null;
            TaskStatus taskStatus = status != null && !status.isEmpty() ? TaskStatus.valueOf(status) : null;
            Role creatorRoleEnum = creatorRole != null && !creatorRole.isEmpty() ? Role.valueOf(creatorRole) : null;

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            if (startDate != null && !startDate.isEmpty()) {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.isEmpty()) {
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            }

            List<TaskAssignment> assignments = taskAssignmentRepository.findUserTasksForOverview(
                    employeeId, search, type, taskStatus, creatorRoleEnum, startDateTime, endDateTime);

            List<Map<String, Object>> taskList = assignments.stream()
                    .map(assignment -> {
                        Map<String, Object> taskMap = convertTaskToMap(assignment.getTask());
                        taskMap.put("individualStatus", assignment.getIndividualStatus().toString());
                        taskMap.put("assignedAt", assignment.getAssignedAt().toString());
                        taskMap.put("report", assignment.getReport());
                        boolean isOverdue = assignment.getTask().getDueDateTime().isBefore(LocalDateTime.now())
                                && assignment.getIndividualStatus() != TaskStatus.COMPLETED;
                        taskMap.put("isOverdue", isOverdue);

                        long hoursUntilDue = java.time.Duration.between(LocalDateTime.now(),
                                assignment.getTask().getDueDateTime()).toHours();
                        taskMap.put("hoursUntilDue", hoursUntilDue);

                        return taskMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(taskList);

        } catch (Exception e) {
            System.err.println("Error getting user overview tasks: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/user/{employeeId}/overview-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserOverviewStats(@PathVariable String employeeId,
            @AuthenticationPrincipal Employee employee) {
        try {
            if (!employee.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Map<String, Object> stats = new HashMap<>();

            long totalTasks = taskAssignmentRepository.countByEmployeeId(employeeId);

            long pendingTasks = taskAssignmentRepository.countByEmployeeIdAndIndividualStatus(employeeId,
                    TaskStatus.PENDING);
            long inProgressTasks = taskAssignmentRepository.countByEmployeeIdAndIndividualStatus(employeeId,
                    TaskStatus.IN_PROGRESS);
            long completedTasks = taskAssignmentRepository.countByEmployeeIdAndIndividualStatus(employeeId,
                    TaskStatus.COMPLETED);

            long overdueTasks = taskAssignmentRepository.countOverdueTasksByEmployee(employeeId, LocalDateTime.now());

            LocalDateTime endOfWeek = LocalDateTime.now().plusDays(7);
            long dueThisWeek = taskAssignmentRepository.countTasksDueInPeriod(employeeId, LocalDateTime.now(),
                    endOfWeek);

            stats.put("totalTasks", totalTasks);
            stats.put("pendingTasks", pendingTasks);
            stats.put("inProgressTasks", inProgressTasks);
            stats.put("completedTasks", completedTasks);
            stats.put("overdueTasks", overdueTasks);
            stats.put("dueThisWeek", dueThisWeek);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("Error getting user overview stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
