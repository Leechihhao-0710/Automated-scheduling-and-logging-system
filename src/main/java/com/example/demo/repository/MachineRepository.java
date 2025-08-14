package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Department;
import com.example.demo.entity.Machine;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, String> {

    List<Machine> findByDepartment(Department department);

    List<Machine> findByNameContainingIgnoreCase(String name);

    @Query("select m from Machine m where m.department.id=:departmentId")
    List<Machine> findByDepartmentId(@Param("departmentId") Integer departmentId);

    @Query("select m from Machine m where m.department.name = :departmentName")
    List<Machine> findByDepartmentName(@Param("departmentName") String departmentName);

    List<Machine> findAllByOrderByCreatedAtDesc();

    @Query("select count(m) from Machine m where m.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Integer departmentId);

    List<Machine> findByDepartmentIsNull();
}
