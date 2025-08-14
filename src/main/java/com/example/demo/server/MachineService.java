package com.example.demo.server;

import com.example.demo.entity.Machine;
import com.example.demo.entity.Department;
import com.example.demo.repository.MachineRepository;
import com.example.demo.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MachineService {
    @Autowired
    private MachineRepository machineRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Machine> getAllMachines() {
        return machineRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Machine> getMachineById(String id) {
        return machineRepository.findById(id);
    }

    public Machine getMachineByIdOrThrow(String id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("machine is not found with id " + id));
    }

    public Machine createMachine(Machine machine, Integer departmntId) {
        if (machineRepository.existsById(machine.getId())) {
            throw new RuntimeException("machine ID exists " + machine.getId());
        }
        if (departmntId != null) {
            Department department = departmentRepository.findById(departmntId)
                    .orElseThrow(() -> new RuntimeException("department is not found with id " + departmntId));
            machine.setDepartment(department);
        }
        return machineRepository.save(machine);
    }

    public Machine updateMachine(String id, Machine machineDetails, Integer departmentId) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("machine is not found with id " + id));
        if (departmentId != null) {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("department is not found with id " + departmentId));
            machine.setDepartment(department);
        } else {
            machine.setDepartment(null);
        }
        machine.setName(machineDetails.getName());
        return machineRepository.save(machine);
    }

    public void deleteMachine(String id) {
        if (!machineRepository.existsById(id)) {
            throw new RuntimeException("machine is not found with id " + id);
        }
        machineRepository.deleteById(id);
    }

    public List<Machine> getMachinesByDepartment(Integer departmentId) {
        return machineRepository.findByDepartmentId(departmentId);
    }

    public List<Machine> getDepartmentByDepartmentName(String name) {
        return machineRepository.findByDepartmentName(name);
    }

    public List<Machine> getMachinewByName(String name) {
        return machineRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Machine> getUnassignedMachines() {
        return machineRepository.findByDepartmentIsNull();
    }

    public long getMachineCountByDepartment(Integer departmentId) {
        return machineRepository.countByDepartmentId(departmentId);
    }

    public Machine assignMachineToDepartment(String machineId, Integer departmentId) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new RuntimeException("machine is not found with id " + machineId));
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("department not fount with id " + departmentId));
        machine.setDepartment(department);
        return machineRepository.save(machine);
    }

    public long getTotalMachineCount() {
        return machineRepository.count();
    }

    public boolean isMachineIdAvaiable(String id) {
        return !machineRepository.existsById(id);
    }

    public void removeMachinesFromDepartment(Integer departmentId) {
        List<Machine> machines = machineRepository.findByDepartmentId(departmentId);
        for (Machine machine : machines) {
            machine.setDepartment(null);
            machineRepository.save(machine);
        }
    }

    public String getNextMachineId() {
        List<Machine> allMachines = machineRepository.findAll();
        if (allMachines.isEmpty()) {
            return "M001";
        }
        int maxNumber = allMachines.stream()
                .mapToInt(machine -> {
                    String id = machine.getId();
                    if (id.startsWith("M") && id.length() > 1) {
                        try {
                            return Integer.parseInt(id.substring(1));
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    }
                    return 0;
                })
                .max()
                .orElse(0);
        return String.format("M%03d", maxNumber + 1);
    }
}
