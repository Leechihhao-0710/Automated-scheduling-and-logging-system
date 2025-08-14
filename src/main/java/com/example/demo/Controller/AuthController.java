package com.example.demo.Controller;

import com.example.demo.entity.Employee;
import com.example.demo.security.JwtUtil;
import com.example.demo.server.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            int employeeNumber = Integer.parseInt(loginRequest.getEmployeeNumber());
            String password = loginRequest.getPassword();

            Employee employee = employeeService.getEmployeeByNumber(employeeNumber).orElse(null);

            if (employee != null && passwordEncoder.matches(password, employee.getPassword())) {
                String token = jwtUtil.generateToken(employee);

                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("employeeNumber", String.format("%04d", employee.getEmployeeNumber()));
                response.put("role", employee.getRole().name());
                response.put("name", employee.getName());

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Login failed: " + e.getMessage());
        }
    }

    // DTO class for login request
    public static class LoginRequest {
        private String employeeNumber;
        private String password;

        public String getEmployeeNumber() {
            return employeeNumber;
        }

        public void setEmployeeNumber(String employeeNumber) {
            this.employeeNumber = employeeNumber;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}