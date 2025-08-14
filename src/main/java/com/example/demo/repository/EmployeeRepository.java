package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.enums.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Employee;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmployeeNumber(Integer employeeNumber);

    List<Employee> findByNameContainingIgnoreCase(String name);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Employee> findByRole(Role role);

    @Query("select e from Employee e where e.department.id = :departmentId")
    List<Employee> findByDepartmentId(@Param("departmentId") Integer departmentId);

    @Query("select e from Employee e where e.department.name = :departmentName")
    List<Employee> findByDepartmentName(@Param("departmentName") String departmentName);

    @Query("select e from Employee e where e.department.id = :departmentId and e.role = :role")
    List<Employee> findByDepartmentIdAndRole(@Param("departmentId") Integer departmentId, @Param("role") Role role);

    @Query("select count(e) from Employee e where e.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Integer departmentId);

    @Query("select e from Employee e where e.role='ADMIN'")
    List<Employee> findAllAdmins();

    @Query("select e from Employee e where e.role='USER'")
    List<Employee> findAllUsers();

    boolean existsByEmployeeNumber(Integer employeeNumber);

    @Query("select e.name from Employee e where e.id=:id")
    Optional<String> findNameById(@Param("id") String id);

    List<Employee> findAllByOrderByEmployeeNumber();

    @Query("select max(e.employeeNumber) from Employee e")
    Optional<Integer> findMaxEmployeeNumber();

    @Query("select e from Employee e where e.department is null")
    List<Employee> findEmployeesWithoutDepartment();

    // @Query("select distinct e from Employee e left join fetch e.employeeMachines
    // em left join fetch em.machine order by e,employeeNumber")
    // List<Employee> findAllWithMachines();
}
