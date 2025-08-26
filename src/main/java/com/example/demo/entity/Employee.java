package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.demo.enums.Role;
import com.example.demo.enums.TaskStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.entity.TaskAssignment;
import com.example.demo.enums.TaskStatus;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {
    @Id // primary key
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(length = 4) // define the column feature
    private String id;

    @Column(name = "employee_number", nullable = false, unique = true) // SQL-employee_number INT not null unique
    private Integer employeeNumber;

    @PrePersist
    public void generateId() {
        if (this.id == null && this.employeeNumber != null) {
            this.id = String.format("%04d", this.employeeNumber);
        }
    }

    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    @Column(unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING) // the enum type transfer into carchar(string) and store
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "create_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("employee-machines")
    private Set<EmployeeMachine> employeeMachines = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({ "machines", "hibernateLazyInitializer", "handler" }) // prevent dead loop
    private Department department;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "task", "employee", "hibernateLazyInitializer", "handler" })
    private Set<TaskAssignment> taskAssignments = new HashSet<>();

    public Employee() {
    }

    public Employee(String name, String email, LocalDate dateOfBirth, Department department, Role role,
            String password) {// constructor
        this.name = name;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.department = department;
        this.role = role;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(Integer employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Integer getDepartmentId() {
        return department != null ? department.getId() : null;
    }

    public String getDepartmentName() {
        return department != null ? department.getName() : null;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {// JPA/Hibernate generate column(JPA reflection)->no need to declare in the
                                         // parameter
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

    public Set<EmployeeMachine> getEmployeeMachines() {// the EmployeeMachine is a middle table between employee(ID) and
                                                       // machine(ID)
        return employeeMachines;
    }

    public void setEmployeeMachines(Set<EmployeeMachine> employeeMachines) {
        this.employeeMachines = employeeMachines;
    }

    public Set<Machine> getMachines() {
        return employeeMachines.stream()// for-each to all employees EmployeeMachine
                .map(EmployeeMachine::getMachine)// reflect EmployeeMachine to machine
                .collect(Collectors.toSet());// collect the machines to Set(avoid duplicate)
    }

    public void addMachine(Machine machine) {
        EmployeeMachine employeeMachine = new EmployeeMachine(this, machine);
        this.employeeMachines.add(employeeMachine);
        machine.getEmployeeMachines().add(employeeMachine);
    }

    public void removeMachine(Machine machine) {
        EmployeeMachine employeeMachine = this.employeeMachines.stream()// find all employee's assigned machines
                .filter(em -> em.getMachine().equals(machine))// filter the target machine
                .findFirst()// find the target(should only have one)
                .orElse(null);

        if (employeeMachine != null) {
            this.employeeMachines.remove(employeeMachine);
            machine.getEmployeeMachines().remove(employeeMachine);
        }
    }

    public void clearMachines() {
        Set<EmployeeMachine> copy = new HashSet<>(this.employeeMachines);
        for (EmployeeMachine employeeMachine : copy) {
            removeMachine(employeeMachine.getMachine());
        }
    }

    public int getMachineCount() {
        return this.employeeMachines.size();
    }

    public static String formatPasswordFromDate(LocalDate dateOfBirth) {
        return dateOfBirth.toString().replace("-", "");
    }

    public Set<TaskAssignment> getTaskAssignments() {
        return taskAssignments;
    }

    public void setTaskAssignments(Set<TaskAssignment> taskAssignments) {
        this.taskAssignments = taskAssignments;
    }

    public Set<Task> getAssignedTasks() {
        return taskAssignments.stream().map(TaskAssignment::getTask).collect(Collectors.toSet());
    }

    public List<TaskAssignment> getActiveTasks() {
        return taskAssignments.stream().filter(ta -> ta.getIndividualStatus() != TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }
}
