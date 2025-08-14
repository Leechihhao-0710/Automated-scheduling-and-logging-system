package com.example.demo.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.entity.Department;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Machine;
import com.example.demo.server.DepartmentService;
import com.example.demo.server.EmployeeService;
import com.example.demo.server.MachineService;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/departments")
public class departmentController {
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private MachineService machineService;

    @GetMapping // return the page(HTML not JSON)
    public String showDepartmentManagementPage(@AuthenticationPrincipal Employee employee, Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "department");
        return "admin/department_management";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/departments") // GET request
    @ResponseBody // return JSON directly instead of template
    public ResponseEntity<List<Map<String, Object>>> getAllDepartmentApi() {// Map<String,object> - build JSON object
        try {
            List<Department> departments = departmentService.getAllDepartments();
            List<Map<String, Object>> departmentList = departments.stream()
                    .map(dept -> {
                        Map<String, Object> deptMap = new HashMap<>();
                        deptMap.put("id", dept.getId());
                        deptMap.put("name", dept.getName());
                        deptMap.put("description", dept.getDescription());
                        deptMap.put("employeeCount", employeeService.getEmployeeCountByDepartment(dept.getId()));
                        deptMap.put("machineCount", machineService.getMachineCountByDepartment(dept.getId()));
                        deptMap.put("createdAt", dept.getCreatedAt());
                        return deptMap;// transfer Department object to Map and return deptMap , transfer the type
                    })
                    .collect(java.util.stream.Collectors.toList());// terminal operation , collect the map become
                                                                   // List<Map<...>>->JSON array
            return ResponseEntity.ok(departmentList);
        } catch (Exception e) {
            System.err.println("Error in getAllDepartmentApi : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/create") // post request,create in the frontend and send JSON to backend transfer to
                                // department object then store in database
    @ResponseBody
    public ResponseEntity<?> createDepartmentApi(@RequestBody Map<String, Object> departmentData) {
        try {
            Department department = new Department();
            department.setName((String) departmentData.get("name"));
            department.setDescription((String) departmentData.get("description"));

            Department savedDepartment = departmentService.createDepartment(department);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedDepartment.getId());
            response.put("name", savedDepartment.getName());
            response.put("description", savedDepartment.getDescription());
            response.put("message", "Department created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error creating department: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error creating department: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/update/{id}") // put request
    @ResponseBody
    public ResponseEntity<?> updateDepartmentApi(@PathVariable Integer id,
            @RequestBody Map<String, Object> departmentData) {
        try {
            Department departmentDetails = new Department();
            departmentDetails.setName((String) departmentData.get("name"));
            departmentDetails.setDescription((String) departmentData.get("description"));

            Department updatedDepartment = departmentService.updateDepartment(id, departmentDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedDepartment.getId());
            response.put("name", updatedDepartment.getName());
            response.put("description", updatedDepartment.getDescription());
            response.put("message", "Department updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error updating department: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error updating department: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteDepartmentApi(@PathVariable Integer id) {
        try {
            // Check if department has employees
            long employeeCount = employeeService.getEmployeeCountByDepartment(id);
            if (employeeCount > 0) {
                // Set all employees' department to null before deleting department
                employeeService.removeEmployeesFromDepartment(id);
            }

            // Check if department has machines
            long machineCount = machineService.getMachineCountByDepartment(id);
            if (machineCount > 0) {
                // Set all machines' department to null before deleting department
                machineService.removeMachinesFromDepartment(id);
            }

            departmentService.deleteDepartment(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Department deleted successfully");
            if (employeeCount > 0) {
                response.put("employeesAffected", employeeCount);
            }
            if (machineCount > 0) {
                response.put("machinesAffected", machineCount);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error deleting department: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting department: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/machines")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllMachinesApi() {
        try {
            List<Machine> machines = machineService.getAllMachines();

            List<Map<String, Object>> machineList = machines.stream()
                    .map(machine -> {
                        Map<String, Object> machineMap = new HashMap<>();
                        machineMap.put("id", machine.getId());
                        machineMap.put("name", machine.getName());
                        machineMap.put("createdAt", machine.getCreatedAt());

                        if (machine.getDepartment() != null) {
                            Map<String, Object> deptMap = new HashMap<>();
                            deptMap.put("id", machine.getDepartment().getId());
                            deptMap.put("name", machine.getDepartment().getName());
                            machineMap.put("department", deptMap);
                        } else {
                            machineMap.put("department", null);
                        }

                        return machineMap;
                    })
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(machineList);
        } catch (Exception e) {
            System.err.println("Error in getAllMachinesApi: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/machines/create")
    @ResponseBody
    public ResponseEntity<?> createMachineApi(@RequestBody Map<String, Object> machineData) {
        try {
            Machine machine = new Machine();
            machine.setId((String) machineData.get("id"));
            machine.setName((String) machineData.get("name"));

            Integer departmentId = (Integer) machineData.get("departmentId");

            Machine savedMachine = machineService.createMachine(machine, departmentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedMachine.getId());
            response.put("name", savedMachine.getName());
            response.put("message", "Machine created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error creating machine: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error creating machine: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/machines/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateMachineApi(@PathVariable String id, @RequestBody Map<String, Object> machineData) {
        try {
            Machine machineDetails = new Machine();
            machineDetails.setName((String) machineData.get("name"));

            Integer departmentId = (Integer) machineData.get("departmentId");

            Machine updatedMachine = machineService.updateMachine(id, machineDetails, departmentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedMachine.getId());
            response.put("name", updatedMachine.getName());
            response.put("message", "Machine updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error updating machine: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error updating machine: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/machines/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteMachineApi(@PathVariable String id) {
        try {
            machineService.deleteMachine(id);
            return ResponseEntity.ok("Machine deleted successfully");
        } catch (Exception e) {
            System.err.println("Error deleting machine: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error deleting machine: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/check-department-name")
    @ResponseBody
    public boolean checkDepartmentNameAvaiable(@RequestParam String name,
            @RequestParam(required = false) Integer currentId) {
        if (currentId != null) {
            return departmentService.isDepartmentNameAvaiable(name, currentId);
        }
        return departmentService.isDepartmentNameAvaiable(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/check-machine-id/{machineId}")
    @ResponseBody
    public boolean checkMachineIdAvailable(@PathVariable String machineId) {
        return machineService.isMachineIdAvaiable(machineId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/machines/next-id")
    public ResponseEntity<String> getNextMachineId() {
        String nextId = machineService.getNextMachineId();
        return ResponseEntity.ok(nextId);
    }

}
