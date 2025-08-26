package com.example.demo.security;

import com.example.demo.entity.Employee;
import com.example.demo.server.EmployeeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // oncePerRequestFilter -> to avoid duplicate authentication(every request can
    // only request one time)
    private final JwtUtil jwtUtil;
    private final EmployeeService employeeService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Lazy EmployeeService employeeService) {
        // @Lazy -> to avoid the circular dependency between @Bean
        this.jwtUtil = jwtUtil; // JWT generate , authenticate , analyse
        this.employeeService = employeeService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.equals("/api/login");
        // when enter the login page, do not need filter
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        // Token validation and user authentication logic
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                if (jwtUtil.isTokenValid(jwt)) {
                    String employeeNumber = jwtUtil.extractUsername(jwt);

                    if (employeeNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        Employee employee = employeeService
                                .getEmployeeByNumber(Integer.parseInt(employeeNumber))
                                .orElse(null);

                        if (employee != null) {
                            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));

                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    employee, null, authorities);

                            authToken.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            // the flow: find the employee -> establish spring security authentication ->
                            // store into securityContextHolder
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("JWT auth failed：" + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
