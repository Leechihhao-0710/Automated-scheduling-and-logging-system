package com.example.demo.server;

import com.example.demo.entity.EmployeeMachine;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Machine;
import com.example.demo.repository.EmployeeMachineRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.MachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeMachineService {
    @Autowired // import the operate object / declare the variable
    private EmployeeMachineRepository employeeMachineRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private MachineRepository machineRepository;

    public List<EmployeeMachine> getAllAssignments() {
        return employeeMachineRepository.findAllByOrderByAssignedAtDesc();
    }// check the assignd relations

    public EmployeeMachine assignMachineToEmployee(String employeeId, String machineId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        // check emplyee
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new RuntimeException("Machine not found with id: " + machineId));
        // check machine
        if (employeeMachineRepository.existsByEmployeeIdAndMachineId(employeeId, machineId)) {
            throw new RuntimeException("Machine " + machineId + " is already assigned to employee " + employeeId);
        } // check if the machine is already assigned to the employee

        EmployeeMachine assignment = new EmployeeMachine();
        assignment.setEmployee(employee);
        assignment.setMachine(machine);

        return employeeMachineRepository.save(assignment);
    }

    public void unassignMachineFromEmployee(String employeeId, String machineId) {
        if (!employeeMachineRepository.existsByEmployeeIdAndMachineId(employeeId, machineId)) {
            throw new RuntimeException("Assignment not found for employee " + employeeId + " and machine " + machineId);
        }
        employeeMachineRepository.deleteByEmployeeIdAndMachineId(employeeId, machineId);
    }// cancel the assignd machine

    public List<EmployeeMachine> getMachinesByEmployee(String employeeId) {
        return employeeMachineRepository.findByEmployeeId(employeeId);
    }// get the machines that assigned to the employee

    public List<String> getMachineIdsByEmployee(String employeeId) {
        return employeeMachineRepository.findByEmployeeId(employeeId)
                .stream()
                .map(em -> em.getMachine().getId())
                .collect(Collectors.toList());
    }// get the table with the specific employee and assigned machines

    public List<EmployeeMachine> getEmployeesByMachine(String machineId) {
        return employeeMachineRepository.findByMachineId(machineId);
    }// get the employees who assigned to the machine

    public Optional<EmployeeMachine> getAssignment(String employeeId, String machineId) {
        return employeeMachineRepository.findByEmployeeIdAndMachineId(employeeId, machineId);
    }

    public List<EmployeeMachine> getAssignmentsByDepartment(Integer departmentId) {
        return employeeMachineRepository.findByEmployeeDepartmentId(departmentId);
    }// search the machine assignment in the department

    public long getMachineCountByEmployee(String employeeId) {
        return employeeMachineRepository.countByEmployeeId(employeeId);
    }// count the number of machines assigned to employee

    public long getEmployeeCountByMachine(String machineId) {
        return employeeMachineRepository.countByMachineId(machineId);
    }

    public void assignMultipleMachinesToEmployee(String employeeId, List<String> machineIds) {
        for (String machineId : machineIds) {
            try {
                assignMachineToEmployee(employeeId, machineId);
            } catch (RuntimeException e) {
                System.err.println(
                        "Failed to assign machine " + machineId + " to employee " + employeeId + ": " + e.getMessage());
            }
        }
    }

    public void unassignAllMachinesFromEmployee(String employeeId) {
        List<EmployeeMachine> assignments = employeeMachineRepository.findByEmployeeId(employeeId);
        employeeMachineRepository.deleteAll(assignments);
    }// cancel all machines which assigned to the employee -> for delete the employee

    public void unassignAllEmployeesFromMachine(String machineId) {
        List<EmployeeMachine> assignments = employeeMachineRepository.findByMachineId(machineId);
        employeeMachineRepository.deleteAll(assignments);
    }// cancel all employees who assigned to the machine -> for delete the machine

    public boolean isEmployeeAssignedToMachine(String employeeId, String machineId) {
        return employeeMachineRepository.existsByEmployeeIdAndMachineId(employeeId, machineId);
    }

    public void updateEmployeeMachineAssignments(String employeeId, List<String> newMachineIds) {
        unassignAllMachinesFromEmployee(employeeId);
        if (newMachineIds != null && !newMachineIds.isEmpty()) {
            assignMultipleMachinesToEmployee(employeeId, newMachineIds);
        }
    }// update the assigned machine
}
