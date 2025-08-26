package com.example.demo.server;

import com.example.demo.entity.Department;
import com.example.demo.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DepartmentService {
    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAllByOrderByName();
    }

    public Optional<Department> getDepartmentById(Integer id) {
        return departmentRepository.findById(id);
    }

    public Department getDepartmentByIdOrThrow(Integer id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("department not found with id " + id));
    }

    public Optional<Department> getDepartmentByName(String name) {
        return departmentRepository.findByName(name);
    }

    public Department createDepartment(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new RuntimeException("department name exists " + department.getName());
        }
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Integer id, Department departmentDetails) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("department not found with id " + id));
        if (!department.getName().equals(departmentDetails.getName())) {
            if (departmentRepository.existsByName(departmentDetails.getName())) {
                throw new RuntimeException("department name exists " + departmentDetails.getName());
            }
        }
        department.setName(departmentDetails.getName());
        department.setDescription(departmentDetails.getDescription());
        return departmentRepository.save(department);
    }

    public void deleteDepartment(Integer id) {
        if (!departmentRepository.existsById(id)) {
            throw new RuntimeException("department not found with id " + id);
        }
        departmentRepository.deleteById(id);
    }

    public List<Department> searchDepartmentsByName(String name) {
        return departmentRepository.findByNameContainingIgnoreCase(name);
    }

    public long getTotalDepartmentCount() {
        return departmentRepository.countAllDepartments();
    }

    public boolean isDepartmentNameAvaiable(String name) {
        return !departmentRepository.existsByName(name);
    }

    public boolean isDepartmentNameAvaiable(String name, Integer currentId) {
        Optional<Department> existDep = departmentRepository.findByName(name);
        if (existDep.isPresent()) {
            return existDep.get().getId().equals(currentId);
        }
        return true;
    }
}
