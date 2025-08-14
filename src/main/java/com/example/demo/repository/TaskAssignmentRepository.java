package com.example.demo.repository;

import com.example.demo.entity.TaskAssignment;
import com.example.demo.entity.Task;
import com.example.demo.entity.Employee;
import com.example.demo.enums.TaskStatus;
import com.example.demo.enums.TaskType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.example.demo.enums.Role;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    List<TaskAssignment> findByTask(Task task);

    List<TaskAssignment> findByTaskId(Long taskId);

    List<TaskAssignment> findByEmployee(Employee employee);

    List<TaskAssignment> findByEmployeeId(String employeeId);

    Optional<TaskAssignment> findByTaskAndEmployee(Task task, Employee employee);

    Optional<TaskAssignment> findByTaskIdAndEmployeeId(Long taskId, String employeeId);

    List<TaskAssignment> findByEmployeeAndIndividualStatus(Employee employee, TaskStatus status);

    List<TaskAssignment> findByEmployeeIdAndIndividualStatus(String employeeId, TaskStatus status);

    List<TaskAssignment> findByTaskAndIndividualStatus(Task task, TaskStatus status);

    List<TaskAssignment> findByTaskIdAndIndividualStatus(Long taskId, TaskStatus status);

    List<TaskAssignment> findByAssignedAtBetween(LocalDateTime start, LocalDateTime end);

    List<TaskAssignment> findByCompletedAtBetween(LocalDateTime start, LocalDateTime end);

    List<TaskAssignment> findByEmployeeOrderByAssignedAtDesc(Employee employee);

    List<TaskAssignment> findByEmployeeIdOrderByAssignedAtDesc(String employeeId);

    List<TaskAssignment> findByTaskOrderByAssignedAtDesc(Task task);

    long countByEmployee(Employee employee);

    long countByEmployeeId(String employeeId);

    long countByEmployeeAndIndividualStatus(Employee employee, TaskStatus status);

    long countByEmployeeIdAndIndividualStatus(String employeeId, TaskStatus status);

    long countByTask(Task task);

    long countByTaskId(Long taskId);

    long countByTaskAndIndividualStatus(Task task, TaskStatus status);

    long countByTaskIdAndIndividualStatus(Long taskId, TaskStatus status);

    // find the task that specific employee not completed
    @Query("select ta from TaskAssignment ta where ta.employee.id = :employeeId and ta.individualStatus != 'COMPLETED'")
    List<TaskAssignment> findUncompletedTasksByEmployee(@Param("employeeId") String employeeId);

    // search employee tasks by department
    @Query("select ta from TaskAssignment ta where ta.employee.department.id = :departmentId")
    List<TaskAssignment> findByEmployeeDepartmentId(@Param("departmentId") Integer departmentId);

    // search the employee whose task deadline is today
    @Query("select ta from TaskAssignment ta where date(ta.task.dueDateTime) = date(:today) and ta.individualStatus != 'COMPLETED'")
    List<TaskAssignment> findEmployeeTasksDueToday(@Param("today") LocalDateTime today);

    // search employee's ovr due task
    @Query("select ta from TaskAssignment ta where ta.task.dueDateTime < :now and ta.individualStatus != 'COMPLETED' and ta.employee.id = :employeeId")
    List<TaskAssignment> findOverdueTasksByEmployee(@Param("employeeId") String employeeId,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from TaskAssignment ta where ta.task = :task")
    void deleteByTask(@Param("task") Task task);

    @Modifying
    @Query("delete from TaskAssignment ta where ta.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("delete from TaskAssignment ta where ta.employee = :employee")
    void deleteByEmployee(@Param("employee") Employee employee);

    @Modifying
    @Query("delete from TaskAssignment ta where ta.employee.id = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") String employeeId);

    @Modifying
    @Query("DELETE FROM TaskAssignment ta where ta.task.id = :taskId and ta.employee.id = :employeeId")
    void deleteByTaskIdAndEmployeeId(@Param("taskId") Long taskId, @Param("employeeId") String employeeId);

    @Query("select ta from TaskAssignment ta where ta.report is not null and ta.report != ''")
    List<TaskAssignment> findAssignmentsWithReports();

    @Query("select ta from TaskAssignment ta where ta.employee.id = :employeeId and ta.report is not null and ta.report != ''")
    List<TaskAssignment> findAssignmentsWithReportsByEmployee(@Param("employeeId") String employeeId);

    // for user overview filter / search
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.employee.id = :employeeId " +
            "AND (:search IS NULL OR LOWER(ta.task.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(ta.task.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:taskType IS NULL OR ta.task.taskType = :taskType) " +
            "AND (:status IS NULL OR ta.individualStatus = :status) " +
            "AND (:creatorRole IS NULL OR ta.task.creator.role = :creatorRole) " +
            "AND (:startDate IS NULL OR ta.task.dueDateTime >= :startDate) " +
            "AND (:endDate IS NULL OR ta.task.dueDateTime <= :endDate) " +
            "ORDER BY CASE WHEN ta.task.dueDateTime < CURRENT_TIMESTAMP AND ta.individualStatus != 'COMPLETED' THEN 0 ELSE 1 END, "
            +
            "ta.task.dueDateTime ASC")
    List<TaskAssignment> findUserTasksForOverview(
            @Param("employeeId") String employeeId,
            @Param("search") String search,
            @Param("taskType") TaskType taskType,
            @Param("status") TaskStatus status,
            @Param("creatorRole") Role creatorRole,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(ta) FROM TaskAssignment ta WHERE ta.employee.id = :employeeId " +
            "AND ta.task.dueDateTime < :now AND ta.individualStatus != 'COMPLETED'")
    long countOverdueTasksByEmployee(@Param("employeeId") String employeeId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(ta) FROM TaskAssignment ta WHERE ta.employee.id = :employeeId " +
            "AND ta.task.dueDateTime BETWEEN :startDate AND :endDate " +
            "AND ta.individualStatus != 'COMPLETED'")
    long countTasksDueInPeriod(@Param("employeeId") String employeeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.employee.id = :employeeId " +
            "AND ta.task.creator.role = :creatorRole " +
            "ORDER BY ta.task.dueDateTime ASC")
    List<TaskAssignment> findByEmployeeIdAndCreatorRole(@Param("employeeId") String employeeId,
            @Param("creatorRole") Role creatorRole);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.employee.id = :employeeId " +
            "AND ta.task.dueDateTime BETWEEN :now AND :futureTime " +
            "AND ta.individualStatus != 'COMPLETED' " +
            "ORDER BY ta.task.dueDateTime ASC")
    List<TaskAssignment> findTasksDueSoon(@Param("employeeId") String employeeId,
            @Param("now") LocalDateTime now,
            @Param("futureTime") LocalDateTime futureTime);
}
