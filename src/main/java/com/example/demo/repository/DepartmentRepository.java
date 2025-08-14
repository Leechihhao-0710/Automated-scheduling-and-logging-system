package com.example.demo.repository;

import com.example.demo.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    // after declare the JpaRepository , already have methods(CRUD) in-build ->
    // findById(),findAll(),save(),deleteById()
    Optional<Department> findByName(String name);// SQL -> select * from departments where name = String name;

    boolean existsByName(String name);// to avoid duplicate name

    List<Department> findByNameContainingIgnoreCase(String name);// fuzzy search or ignore the character case

    List<Department> findAllByOrderByName();

    @Query("SELECT COUNT(d) FROM Department d") // SQL -> select count(*) from department
    long countAllDepartments();
}
