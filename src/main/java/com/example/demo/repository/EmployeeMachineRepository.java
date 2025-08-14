package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Employee;
import com.example.demo.entity.EmployeeMachine;
import com.example.demo.entity.Machine;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeMachineRepository extends JpaRepository<EmployeeMachine, Integer> {

        List<EmployeeMachine> findByEmployee(Employee employee);

        @Query("select em from EmployeeMachine em where em.employee.id = :employeeId") // :employeeId means variable not
                                                                                       // value -> with @Param(same
                                                                                       // name)
        List<EmployeeMachine> findByEmployeeId(@Param("employeeId") String employeeId);

        List<EmployeeMachine> findByMachine(Machine machine);

        @Query("select em from EmployeeMachine em where em.machine.id = :machineId")
        List<EmployeeMachine> findByMachineId(@Param("machineId") String machineId);

        @Query("select em from EmployeeMachine em where em.employee.id = :employeeId AND em.machine.id = :machineId")
        Optional<EmployeeMachine> findByEmployeeIdAndMachineId(@Param("employeeId") String employeeId,
                        @Param("machineId") String machineId);

        @Query("select case when count(em) > 0 then true else false end from EmployeeMachine em where em.employee.id = :employeeId and em.machine.id = :machineId")
        boolean existsByEmployeeIdAndMachineId(@Param("employeeId") String employeeId,
                        @Param("machineId") String machineId);

        @Query("select count(em) from EmployeeMachine em where em.employee.id = :employeeId")
        long countByEmployeeId(@Param("employeeId") String employeeId);

        @Query("select count(em) from EmployeeMachine em where em.machine.id = :machineId")
        long countByMachineId(@Param("machineId") String machineId);

        List<EmployeeMachine> findAllByOrderByAssignedAtDesc();

        @Query("select em from EmployeeMachine em where em.employee.department.id = :departmentId")
        List<EmployeeMachine> findByEmployeeDepartmentId(@Param("departmentId") Integer departmentId);

        @Query("select em from EmployeeMachine em where em.employee.department.name = :departmentName")
        List<EmployeeMachine> findByEmployeeDepartmentName(@Param("departmentName") String departmentName);

        @Query("select m from Machine m where m.id not in (select em.machine.id from EmployeeMachine em)")
        List<Machine> findUnassignedMachines();

        @Modifying
        @Query("DELETE from EmployeeMachine em where em.employee.id = :employeeId and em.machine.id = :machineId")
        void deleteByEmployeeIdAndMachineId(@Param("employeeId") String employeeId,
                        @Param("machineId") String machineId);
}
