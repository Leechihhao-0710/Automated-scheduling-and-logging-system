package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "machines")
public class Machine {
    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @CreationTimestamp
    @Column(name = "create_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "machine", "employee", "hibernateLazyInitializer", "handler" })
    private Set<EmployeeMachine> employeeMachines = new HashSet<>();

    public Machine() {
    }

    public Machine(String id, String name, Department department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<EmployeeMachine> getEmployeeMachines() {
        return employeeMachines;
    }

    public void setEmployeeMachines(Set<EmployeeMachine> employeeMachines) {
        this.employeeMachines = employeeMachines;
    }

    public Set<Employee> getEmployees() {
        return employeeMachines.stream()
                .map(EmployeeMachine::getEmployee)
                .collect(Collectors.toSet());
    }

    public void setEmployees(Set<Employee> employees) {
        this.employeeMachines.clear();

        for (Employee employee : employees) {
            EmployeeMachine employeeMachine = new EmployeeMachine(employee, this);
            this.employeeMachines.add(employeeMachine);
        }
    }

    public void addEmployee(Employee employee) {
        EmployeeMachine employeeMachine = new EmployeeMachine(employee, this);
        this.employeeMachines.add(employeeMachine);
        employee.getEmployeeMachines().add(employeeMachine);
    }

    public void removeEmployee(Employee employee) {
        EmployeeMachine employeeMachine = this.employeeMachines.stream()
                .filter(em -> em.getEmployee().equals(employee))
                .findFirst()
                .orElse(null);

        if (employeeMachine != null) {
            this.employeeMachines.remove(employeeMachine);
            employee.getEmployeeMachines().remove(employeeMachine);
        }
    }

}
