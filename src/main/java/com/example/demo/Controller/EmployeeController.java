package com.example.demo.Controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.entity.Department;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Machine;
import com.example.demo.enums.Role;
import com.example.demo.server.DepartmentService;
import com.example.demo.server.EmployeeMachineService;
import com.example.demo.server.EmployeeService;
import com.example.demo.server.MachineService;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Controller
@RequestMapping("/employees")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private MachineService machineService;
    @Autowired
    private EmployeeMachineService employeeMachineService;

    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String showDepartmentManagementPage(@AuthenticationPrincipal Employee employee, Model model) {
        model.addAttribute("user", employee);
        model.addAttribute("activePage", "employees");
        return "admin/employee_management";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public String ListEmployees(@RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "department", required = false) Integer departmentId,
            @RequestParam(value = "role", required = false) Role role, Model model) {
        List<Employee> employees;
        if (search != null && !search.trim().isEmpty()) {
            employees = employeeService.searchEmployeesByName(search.trim());
        } else if (departmentId != null && role != null) {
            employees = employeeService.getEmployeesByDepartmentAndRole(departmentId, role);
        } else if (departmentId != null) {
            employees = employeeService.getEmployeesByDepartment(departmentId);
        } else if (role != null) {
            employees = employeeService.getEmployeesByRole(role);
        } else {
            employees = employeeService.getAllEmployees();
        }

        List<Department> departments = departmentService.getAllDepartments();
        model.addAttribute("employees", employees);
        model.addAttribute("departments", departments);
        model.addAttribute("roles", Role.values());
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectDepartment", departmentId);
        model.addAttribute("selectRole", role);

        return "employees/list";

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public String viewEmployee(@PathVariable String id, Model model) {
        try {
            Employee employee = employeeService.getEmployeeByIdOrThrow(id);
            List<String> assignedMachineIds = employeeService.getEmployeeMachines(id);
            model.addAttribute("employee", employee);
            model.addAttribute("assignedMachineIds", assignedMachineIds);
            return "employees/view";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/employees";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        Employee employee = new Employee();
        employee.setEmployeeNumber(employeeService.getNextEmployeeNumber());
        List<Department> departments = departmentService.getAllDepartments();
        List<Machine> machines = machineService.getAllMachines();
        model.addAttribute("employee", employee);
        model.addAttribute("departments", departments);
        model.addAttribute("machines", machines);
        model.addAttribute("assignedMachineIds", List.of());// empty list
        model.addAttribute("roles", Role.values());
        return "redirect:/employees";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String createEmployee(@ModelAttribute Employee employee,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) List<String> machineIds,
            RedirectAttributes redirectAttributes) {
        // TODO: process POST request
        try {
            Employee savedEmployee = employeeService.createEmployeeWithMachines(employee, departmentId, machineIds);
            redirectAttributes.addFlashAttribute("success",
                    "employee created successfully id= " + savedEmployee.getId());
            return "redirect:/employees";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "error creating employee " + e.getMessage());
            return "redirect:/employees/create";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("{id}/edit")
    public String showEditForm(@PathVariable String id, Model model) {
        try {
            Employee employee = employeeService.getEmployeeByIdOrThrow(id);
            List<Department> departments = departmentService.getAllDepartments();
            List<Machine> machines = machineService.getAllMachines();
            List<String> assignedMachineIds = employeeService.getEmployeeMachines(id);

            model.addAttribute("employee", employee);
            model.addAttribute("departments", departments);
            model.addAttribute("machines", machines);
            model.addAttribute("assignedMachineIds", assignedMachineIds);
            model.addAttribute("roles", Role.values());
            return "employees/edit";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/employees";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    public String updateEmployee(@PathVariable String id,
            @ModelAttribute Employee employeeDetails,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) List<String> machineIds,
            RedirectAttributes redirectAttributes) {
        // TODO: process POST request
        try {
            Employee updateEmployee = employeeService.updateEmployeeWithMachines(id, employeeDetails, departmentId,
                    machineIds);
            redirectAttributes.addFlashAttribute("success", "employee update successfully " + updateEmployee.getId());
            return "redirect:/employees";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "error updating employee " + e.getMessage());
            return "redirect:/employee/" + id + "/edit";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/delete")
    public String deleteEmployee(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            Employee employee = employeeService.getEmployeeByIdOrThrow(id);
            String employeeName = employee.getName();
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "employee deleted successfully " + employeeName);

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "error deleting employee " + e.getMessage());
        }
        return "redirect:/employees";
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
                        return machineMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(machineList);
        } catch (Exception e) {
            System.err.println("Error in getAllMachinesApi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/machines-by-department/{departmentId}")
    @ResponseBody
    public List<Machine> getMachinesByDepartment(@PathVariable Integer departmentId) {
        return machineService.getMachinesByDepartment(departmentId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/check-employee-number/{employeeNumber}")
    @ResponseBody
    public boolean checkEmployeeNumberAvaiable(@PathVariable Integer employeeNumber) {
        return employeeService.isEmployeeNumberAvailable(employeeNumber);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/check-email")
    @ResponseBody
    public boolean checkEmailAvaiable(@RequestParam String email, @RequestParam(required = false) String currentId) {
        if (currentId != null) {
            Optional<Employee> currentEmployee = employeeService.getEmployeeById(currentId);
            if (currentEmployee.isPresent() && email.equals(currentEmployee.get().getEmail())) {
                return true;
            }
        }
        return employeeService.isEmailAvailable(email);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public String showStatistics(Model model) {
        List<Department> departments = departmentService.getAllDepartments();
        for (Department dept : departments) {
            long employeeCount = employeeService.getEmployeeCountByDepartment(dept.getId());
            dept.setDescription(dept.getDescription() + "(employees : " + employeeCount + ")");
        }

        model.addAttribute("departments", departments);
        model.addAttribute("totalEmployees", employeeService.getTotalEmployeeCount());
        model.addAttribute("totalAdmins", employeeService.getAllAdmins().size());
        model.addAttribute("totalUsers", employeeService.getAllUsers().size());
        model.addAttribute("employeeWithoutDepartment", employeeService.getEmployeesWithoutDepartment());
        return "employees/stats";
    }

    // @GetMapping("/api/all") // get all employees JSON API
    // @ResponseBody
    // public List<Employee> getAllEmployeesApi() {
    // return employeeService.getAllEmployees();
    // }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllEmployeesApi() {
        try {
            List<Employee> employees = employeeService.getAllEmployees();

            List<Map<String, Object>> employeeList = employees.stream()
                    .map(emp -> {
                        Map<String, Object> employeeMap = new HashMap<>();
                        employeeMap.put("id", emp.getId());
                        employeeMap.put("employeeNumber", emp.getEmployeeNumber());
                        employeeMap.put("name", emp.getName());
                        employeeMap.put("email", emp.getEmail());
                        employeeMap.put("role", emp.getRole().toString());
                        employeeMap.put("dateOfBirth", emp.getDateOfBirth());
                        employeeMap.put("createdAt", emp.getCreatedAt());
                        employeeMap.put("updatedAt", emp.getUpdatedAt());

                        if (emp.getDepartment() != null) {
                            Map<String, Object> deptMap = new HashMap<>();
                            deptMap.put("id", emp.getDepartment().getId());
                            deptMap.put("name", emp.getDepartment().getName());
                            deptMap.put("description", emp.getDepartment().getDescription());
                            employeeMap.put("department", deptMap);
                        } else {
                            employeeMap.put("department", null);
                        }
                        List<Map<String, Object>> assignedMachines = emp.getMachines().stream()
                                .map(machine -> {
                                    Map<String, Object> machineMap = new HashMap<>();
                                    machineMap.put("id", machine.getId());
                                    machineMap.put("name", machine.getName());
                                    return machineMap;
                                })
                                .collect(Collectors.toList());
                        employeeMap.put("assignedMachines", assignedMachines);

                        return employeeMap;
                    })
                    .collect(Collectors.toList());

            System.out.println("API called - returning " + employeeList.size() + " employees");
            return ResponseEntity.ok(employeeList);

        } catch (Exception e) {
            System.err.println("Error in getAllEmployeesApi: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/stats") // retrive the calculate data
    @ResponseBody
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmployees", employeeService.getTotalEmployeeCount());
        stats.put("totalDepartments", departmentService.getTotalDepartmentCount());
        stats.put("totalAdmins", employeeService.getAllAdmins().size());
        return stats;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/departments") // get all departments
    @ResponseBody
    public List<Map<String, Object>> getAllDepartmentsApi() {
        try {
            List<Department> departments = departmentService.getAllDepartments();

            List<Map<String, Object>> departmentList = departments.stream()
                    .map(dept -> {
                        Map<String, Object> deptMap = new HashMap<>();
                        deptMap.put("id", dept.getId());
                        deptMap.put("name", dept.getName());
                        deptMap.put("description", dept.getDescription());
                        return deptMap;
                    })
                    .collect(Collectors.toList());

            System.out.println("Departments API called - returning " + departmentList.size() + " departments");
            return departmentList;

        } catch (Exception e) {
            System.err.println("Error in getAllDepartmentsApi: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load departments");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/create") // the API of creating employee
    @ResponseBody
    public ResponseEntity<?> createEmployeeApi(@RequestBody Map<String, Object> employeeData) {
        try {
            Employee employee = new Employee();
            employee.setName((String) employeeData.get("name"));
            employee.setEmail((String) employeeData.get("email"));
            employee.setDateOfBirth(LocalDate.parse((String) employeeData.get("dateOfBirth")));
            employee.setRole(Role.valueOf((String) employeeData.get("role")));

            Integer departmentId = (Integer) employeeData.get("departmentId");
            Employee savedEmployee = employeeService.createEmployee(employee, departmentId);

            // System.out.println("=== DEBUG INFO ===");
            // System.out.println("Saved employee ID: '" + savedEmployee.getId() + "'");
            // System.out.println("Employee ID length: " + savedEmployee.getId().length());
            Employee verifyEmployee = employeeService.getEmployeeByIdOrThrow(savedEmployee.getId());

            @SuppressWarnings("unchecked")
            List<String> machineIds = (List<String>) employeeData.get("machineIds");

            if (machineIds != null && !machineIds.isEmpty()) {
                for (String machineId : machineIds) {
                    try {
                        employeeMachineService.assignMachineToEmployee(savedEmployee.getId(), machineId);
                        System.out.println("Successfully assigned machine: " + machineId);
                    } catch (Exception e) {
                        System.err.println("Failed to assign machine " + machineId + ": " + e.getMessage());
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedEmployee.getId());
            response.put("name", savedEmployee.getName());
            response.put("email", savedEmployee.getEmail());
            response.put("employeeNumber", savedEmployee.getEmployeeNumber());
            response.put("message", "Employee created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error creating employee: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating employee: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/update/{id}") // update employee API
    @ResponseBody
    public ResponseEntity<?> updateEmployeeApi(@PathVariable String id, @RequestBody Map<String, Object> employeeData) {
        try {
            Employee employeeDetails = new Employee();
            employeeDetails.setName((String) employeeData.get("name"));
            employeeDetails.setEmail((String) employeeData.get("email"));
            String dateOfBirthStr = (String) employeeData.get("dateOfBirth");
            if (dateOfBirthStr != null && !dateOfBirthStr.trim().isEmpty()) {
                employeeDetails.setDateOfBirth(LocalDate.parse(dateOfBirthStr));
            }
            employeeDetails.setRole(Role.valueOf((String) employeeData.get("role")));

            Integer departmentId = (Integer) employeeData.get("departmentId");

            @SuppressWarnings("unchecked")
            List<String> machineIds = (List<String>) employeeData.get("machineIds");

            Employee updatedEmployee = employeeService.updateEmployeeWithMachines(id, employeeDetails, departmentId,
                    machineIds);

            return ResponseEntity.ok(updatedEmployee);
        } catch (Exception e) {
            System.err.println("Error updating employee: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error updating employee: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/delete/{id}") // delete employee API
    @ResponseBody
    public ResponseEntity<?> deleteEmployeeApi(@PathVariable String id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok("Employee deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/js/employeeManagement.js")
    @ResponseBody
    public ResponseEntity<String> getEmployeeManagementJs() {
        try {
            ClassPathResource resource = new ClassPathResource("static/js/employeeManagement.js");
            String content = new String(resource.getInputStream().readAllBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/javascript"));
            headers.setCacheControl("no-cache");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/next-number")
    public ResponseEntity<Integer> getNextEmployeeNumber() {
        return ResponseEntity.ok(employeeService.getNextEmployeeNumber());
    }

}
