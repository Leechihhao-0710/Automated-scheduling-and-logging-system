package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.demo.enums.RecurrenceType;
import com.example.demo.enums.TaskStatus;
import com.example.demo.enums.TaskType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime dueDateTime;

    @Column
    private LocalDateTime completedDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Employee creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({ "machines", "hibernateLazyInitializer", "handler" })
    private Department department;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<TaskAssignment> taskAssignments = new HashSet<>();

    @Column
    private Boolean recurring = false;

    @Enumerated(EnumType.STRING)
    @Column
    private RecurrenceType recurrenceType;

    @Column
    private Integer recurrenceInterval = 1;

    @Column
    private LocalDateTime recurrenceEndDate;

    @Column
    private Integer recurringDayOfWeek;

    @Column
    private Integer recurringDayOfMonth;

    @Column
    private Boolean skipWeekends = true;

    @Column
    private LocalDateTime nextExecutionDate;

    // @Column
    // private Boolean emailReminder = true;

    // @Column
    // private Integer reminderDaysBefore = 3;

    // @Column
    // private Boolean reminderSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column
    private String location;

    public Task() {
    }

    public Task(String title, String description, TaskType taskType, LocalDateTime dueDateTime, Employee creator) {
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.dueDateTime = dueDateTime;
        this.creator = creator;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }

    public void setDueDateTime(LocalDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    public LocalDateTime getCompletedDateTime() {
        return completedDateTime;
    }

    public void setCompletedDateTime(LocalDateTime completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public Employee getCreator() {
        return creator;
    }

    public void setCreator(Employee creator) {
        this.creator = creator;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Set<TaskAssignment> getTaskAssignments() {
        return taskAssignments;
    }

    public void setTaskAssignments(Set<TaskAssignment> taskAssignments) {
        this.taskAssignments = taskAssignments;
    }

    public Set<Employee> getAssignedEmployees() {
        return taskAssignments.stream()
                .map(TaskAssignment::getEmployee)
                .collect(Collectors.toSet());
    }

    public void addTaskAssignment(TaskAssignment taskAssignment) {
        taskAssignments.add(taskAssignment);
        taskAssignment.setTask(this);
    }

    public void removeTaskAssignment(TaskAssignment taskAssignment) {
        taskAssignments.remove(taskAssignment);
        taskAssignment.setTask(null);
    }

    public Boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(Boolean recurring) {
        this.recurring = recurring;
    }

    public RecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(RecurrenceType recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(Integer recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    public LocalDateTime getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    public void setRecurrenceEndDate(LocalDateTime recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }

    // public Boolean isEmailReminder() {
    // return emailReminder;
    // }

    // public void setEmailReminder(Boolean emailReminder) {
    // this.emailReminder = emailReminder;
    // }

    // public Integer getReminderDaysBefore() {
    // return reminderDaysBefore;
    // }

    // public void setReminderDaysBefore(Integer reminderDaysBefore) {
    // this.reminderDaysBefore = reminderDaysBefore;
    // }

    // public Boolean isReminderSent() {
    // return reminderSent;
    // }

    // public void setReminderSent(Boolean reminderSent) {
    // this.reminderSent = reminderSent;
    // }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getRecurringDayOfWeek() {
        return recurringDayOfWeek;
    }

    public void setRecurringDayOfWeek(Integer recurringDayOfWeek) {
        this.recurringDayOfWeek = recurringDayOfWeek;
    }

    public Integer getRecurringDayOfMonth() {
        return recurringDayOfMonth;
    }

    public void setRecurringDayOfMonth(Integer recurringDayOfMonth) {
        this.recurringDayOfMonth = recurringDayOfMonth;
    }

    public Boolean getSkipWeekends() {
        return skipWeekends;
    }

    public void setSkipWeekends(Boolean skipWeekends) {
        this.skipWeekends = skipWeekends;
    }

    public LocalDateTime getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(LocalDateTime nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }

}