package com.example.demo.repository;

import com.example.demo.entity.Task;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.enums.TaskType;
import com.example.demo.enums.RecurrenceType;
import com.example.demo.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
        List<Task> findByStatus(TaskStatus status);

        List<Task> findByTaskType(TaskType taskType);

        List<Task> findByTaskTypeAndStatus(TaskType taskType, TaskStatus status);

        List<Task> findAllByOrderByCreatedAtDesc();

        List<Task> findAllByOrderByDueDateTimeAsc();

        List<Task> findByCreator(Employee creator);

        List<Task> findByCreatorAndStatus(Employee creator, TaskStatus status);

        List<Task> findByCreatorAndTaskType(Employee creator, TaskType taskType);

        List<Task> findByCreatorAndTaskTypeAndStatus(Employee creator, TaskType taskType, TaskStatus status);

        @Query("select t from Task t where t.creator.id=:creatorId")
        List<Task> findByCreatorId(@Param("creatorId") String creatorId);

        @Query("SELECT DISTINCT t FROM Task t JOIN t.taskAssignments ta WHERE ta.employee = :employee")
        List<Task> findByAssignedEmployee(@Param("employee") Employee employee);

        @Query("SELECT DISTINCT t FROM Task t JOIN t.taskAssignments ta WHERE ta.employee = :employee AND t.status = :status")
        List<Task> findByAssignedEmployeeAndStatus(@Param("employee") Employee employee,
                        @Param("status") TaskStatus status);

        @Query("SELECT DISTINCT t FROM Task t JOIN t.taskAssignments ta WHERE ta.employee = :employee AND t.taskType = :taskType")
        List<Task> findByAssignedEmployeeAndTaskType(@Param("employee") Employee employee,
                        @Param("taskType") TaskType taskType);

        @Query("SELECT DISTINCT t FROM Task t JOIN t.taskAssignments ta WHERE ta.employee = :employee AND t.taskType = :taskType AND t.status = :status")
        List<Task> findByAssignedEmployeeAndTaskTypeAndStatus(@Param("employee") Employee employee,
                        @Param("taskType") TaskType taskType, @Param("status") TaskStatus status);

        @Query("SELECT DISTINCT t FROM Task t JOIN t.taskAssignments ta WHERE ta.employee.id = :employeeId")
        List<Task> findByAssignedEmployeeId(@Param("employeeId") String employeeId);

        // @Query("select t from Task t join t.assignedEmployees e where
        // e.id=:employeeId")
        // List<Task> findByAssignedEmployeeId(@Param("employeeId") String employeeId);

        List<Task> findByDepartment(Department department);

        List<Task> findByDepartmentAndStatus(Department department, TaskStatus status);

        List<Task> findByDepartmentAndTaskType(Department department, TaskType taskType);

        List<Task> findByDepartmentAndTaskTypeAndStatus(Department department, TaskType taskType, TaskStatus status);

        @Query("select t from Task t where t.department.id=:departmentId")
        List<Task> findByDepartmentId(@Param("departmentId") Integer departmentId);

        List<Task> findByDueDateTimeBetween(LocalDateTime start, LocalDateTime end);

        List<Task> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        @Query("select t from Task t where date(t.dueDateTime) = date(:today) and t.status != 'COMPLETED'")
        List<Task> findTasksDueToday(@Param("today") LocalDateTime today);

        @Query("select distinct t from Task t left join fetch t.taskAssignments ta left join fetch ta.employee where t.dueDateTime between :now and :later and t.status != 'COMPLETED'")
        List<Task> findTasksDueSoon(@Param("now") LocalDateTime now, @Param("later") LocalDateTime later);

        @Query("select t from Task t where t.dueDateTime < :now and t.status != 'COMPLETED'")
        List<Task> findOverdueTasks(@Param("now") LocalDateTime now);

        List<Task> findByRecurringTrue();

        @Query("select t from Task t where t.recurring = true and (t.recurrenceEndDate is null or t.recurrenceEndDate > :now)")
        List<Task> findActiveRecurringTasks(@Param("now") LocalDateTime now);// search active recurring tasks

        @Query("select distinct t from Task t left join fetch t.taskAssignments ta left join fetch ta.employee where t.recurring = true and t.recurrenceType = :type and (t.recurrenceEndDate is null or t.recurrenceEndDate > :now)")
        List<Task> findActiveRecurringTasksByType(@Param("type") RecurrenceType type, @Param("now") LocalDateTime now);// monthly
                                                                                                                       // or
                                                                                                                       // weekly

        List<Task> findByTitleContainingIgnoreCase(String title);

        @Query("select t from Task t where lower(t.title) like lower(concat('%', :keyword, '%')) or lower(t.description) like lower(concat('%', :keyword, '%'))")
        List<Task> findByTitleOrDescriptionContaining(@Param("keyword") String keyword);// search task by title or topic

        @Query("SELECT COUNT(DISTINCT t) FROM Task t JOIN t.taskAssignments ta WHERE ta.employee.id = :employeeId")
        long countByAssignedEmployee(@Param("employeeId") String employeeId);

        @Query("SELECT COUNT(DISTINCT t) FROM Task t JOIN t.taskAssignments ta WHERE ta.employee.id = :employeeId AND t.status = :status")
        long countByAssignedEmployeeAndStatus(@Param("employeeId") String employeeId,
                        @Param("status") TaskStatus status);

        long countByDepartment(Department department);

        long countByDepartmentAndStatus(Department department, TaskStatus status);

        long countByCreator(Employee creator);

        long countByCreatorAndStatus(Employee creator, TaskStatus status);

        long countByTaskType(TaskType taskType);

        long countByStatus(TaskStatus status);

        @Query("select distinct t from Task t left join t.taskAssignments ta left join ta.employee e where t.department.id = :departmentId or e.department.id = :departmentId")
        List<Task> findTasksByDepartmentIdIncludingEmployees(@Param("departmentId") Integer departmentId);

        @Query("select t from Task t order by t.createdAt desc")
        List<Task> findRecentTasks();

        @Query("select t from Task t where t.dueDateTime between :start and :end and t.status = 'PENDING' order by t.dueDateTime asc")
        List<Task> findUpcomingPendingTasks(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
