package com.example.demo.security;

import com.example.demo.entity.Employee;
import com.example.demo.entity.Department;
import com.example.demo.enums.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil
 * Testing JWT token generation, validation, and claims extraction
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private Employee testEmployee;
    private Employee testAdmin;
    private String testSecret;
    private long testExpirationMs;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        testSecret = "mySecretKeyForTestingPurposesOnly123456789012345678901234567890";
        testExpirationMs = 3600000; // 1 hour in milliseconds

        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", testExpirationMs);

        jwtUtil.init();

        setupTestData();
    }

    private void setupTestData() {
        // Test Department
        Department testDepartment = new Department();
        testDepartment.setId(1);
        testDepartment.setName("Engineering");

        // Test User Employee
        testEmployee = new Employee();
        testEmployee.setId("0002");
        testEmployee.setName("John Doe");
        testEmployee.setEmail("john.doe@company.com");
        testEmployee.setEmployeeNumber(2);
        testEmployee.setRole(Role.USER);
        testEmployee.setPassword("$2a$10$encodedPassword");
        testEmployee.setDateOfBirth(LocalDate.of(1990, 5, 15));
        testEmployee.setDepartment(testDepartment);

        // Test Admin Employee
        testAdmin = new Employee();
        testAdmin.setId("0001");
        testAdmin.setName("Admin User");
        testAdmin.setEmail("admin@company.com");
        testAdmin.setEmployeeNumber(1);
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setPassword("$2a$10$encodedAdminPassword");
        testAdmin.setDateOfBirth(LocalDate.of(1985, 3, 10));
    }

    // Test Case 1: Generate Token - User Role

    @Test
    @DisplayName("Should generate valid JWT token for user employee")
    void generateToken_forUserEmployee_shouldCreateValidToken() {
        // When
        String token = jwtUtil.generateToken(testEmployee);

        // Then
        assertNotNull(token, "Generated token should not be null");
        assertTrue(token.length() > 0, "Generated token should not be empty");
        assertTrue(token.startsWith("eyJ"), "JWT token should start with 'eyJ'");

        // Verify token structure (should have 3 parts separated by dots)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT token should have 3 parts (header.payload.signature)");

        // Verify token is valid
        assertTrue(jwtUtil.isTokenValid(token), "Generated token should be valid");
    }

    // Test Case 2: Generate Token - Admin Role

    @Test
    @DisplayName("Should generate valid JWT token for admin employee")
    void generateToken_forAdminEmployee_shouldCreateValidToken() {
        // When
        String token = jwtUtil.generateToken(testAdmin);

        // Then
        assertNotNull(token, "Generated token should not be null");
        assertTrue(token.length() > 0, "Generated token should not be empty");
        assertTrue(jwtUtil.isTokenValid(token), "Generated token should be valid");

        // Verify admin-specific claims
        String extractedRole = jwtUtil.extractRole(token);
        assertEquals("ADMIN", extractedRole, "Extracted role should be ADMIN");

        String extractedEmployeeId = jwtUtil.extractEmployeeId(token);
        assertEquals("0001", extractedEmployeeId, "Extracted employee ID should match");

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("0001", extractedUsername, "Extracted username should be formatted employee number");
    }

    // Test Case 3: Extract Claims - User Employee

    @Test
    @DisplayName("Should extract correct claims from user token")
    void extractClaims_fromUserToken_shouldReturnCorrectValues() {
        // Given
        String token = jwtUtil.generateToken(testEmployee);

        // When
        String extractedUsername = jwtUtil.extractUsername(token);
        String extractedEmployeeId = jwtUtil.extractEmployeeId(token);
        String extractedRole = jwtUtil.extractRole(token);

        // Then
        assertEquals("0002", extractedUsername, "Username should be formatted employee number");
        assertEquals("0002", extractedEmployeeId, "Employee ID should match");
        assertEquals("USER", extractedRole, "Role should be USER");
    }

    // Test Case 4: Token Validation - Valid Token

    @Test
    @DisplayName("Should validate correct token as valid")
    void isTokenValid_withValidToken_shouldReturnTrue() {
        // Given
        String token = jwtUtil.generateToken(testEmployee);

        // When
        boolean isValid = jwtUtil.isTokenValid(token);

        // Then
        assertTrue(isValid, "Valid token should return true");
    }

    // Test Case 5: Token Validation - Invalid Token

    @Test
    @DisplayName("Should validate incorrect token as invalid")
    void isTokenValid_withInvalidToken_shouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtUtil.isTokenValid(invalidToken);

        // Then
        assertFalse(isValid, "Invalid token should return false");
    }

    // Test Case 6: Token Validation - Malformed Token

    @Test
    @DisplayName("Should handle malformed token gracefully")
    void isTokenValid_withMalformedToken_shouldReturnFalse() {
        // Given
        String malformedToken = "this-is-not-a-jwt-token";

        // When
        boolean isValid = jwtUtil.isTokenValid(malformedToken);

        // Then
        assertFalse(isValid, "Malformed token should return false");
    }

    // Test Case 7: Token Validation - Empty Token

    @Test
    @DisplayName("Should handle empty token gracefully")
    void isTokenValid_withEmptyToken_shouldReturnFalse() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtUtil.isTokenValid(emptyToken);

        // Then
        assertFalse(isValid, "Empty token should return false");
    }

    // Test Case 8: Token Validation - Null Token

    @Test
    @DisplayName("Should handle null token gracefully")
    void isTokenValid_withNullToken_shouldReturnFalse() {
        // Given
        String nullToken = null;

        // When
        boolean isValid = jwtUtil.isTokenValid(nullToken);

        // Then
        assertFalse(isValid, "Null token should return false");
    }

    // Test Case 9: Extract Claims from Invalid Token

    @Test
    @DisplayName("Should throw exception when extracting claims from invalid token")
    void extractClaims_fromInvalidToken_shouldThrowException() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(JwtException.class, () -> {
            jwtUtil.extractUsername(invalidToken);
        }, "Should throw JwtException for invalid token");

        assertThrows(JwtException.class, () -> {
            jwtUtil.extractEmployeeId(invalidToken);
        }, "Should throw JwtException for invalid token");

        assertThrows(JwtException.class, () -> {
            jwtUtil.extractRole(invalidToken);
        }, "Should throw JwtException for invalid token");
    }

    // Test Case 10: Token with Wrong Signature

    @Test
    @DisplayName("Should invalidate token signed with different key")
    void isTokenValid_withWrongSignature_shouldReturnFalse() {
        // Given - create a token with a different key
        Key differentKey = Keys.hmacShaKeyFor("differentSecretKey12345678901234567890123456789012345".getBytes());

        String tokenWithWrongSignature = Jwts.builder()
                .setSubject("0002")
                .claim("employeeId", "0002")
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + testExpirationMs))
                .signWith(differentKey)
                .compact();

        // When
        boolean isValid = jwtUtil.isTokenValid(tokenWithWrongSignature);

        // Then
        assertFalse(isValid, "Token with wrong signature should be invalid");
    }

    // Test Case 11: Token Expiration Test (Simulated)

    @Test
    @DisplayName("Should invalidate expired token")
    void isTokenValid_withExpiredToken_shouldReturnFalse() {
        // Given - create a token that expired 1 hour ago
        Key key = Keys.hmacShaKeyFor(testSecret.getBytes());

        String expiredToken = Jwts.builder()
                .setSubject("0002")
                .claim("employeeId", "0002")
                .claim("role", "USER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(key)
                .compact();

        // When
        boolean isValid = jwtUtil.isTokenValid(expiredToken);

        // Then
        assertFalse(isValid, "Expired token should be invalid");
    }

    // Test Case 12: Token Format Verification

    @Test
    @DisplayName("Should generate token with correct subject format")
    void generateToken_shouldFormatSubjectCorrectly() {
        // Given
        Employee employeeWithSmallNumber = new Employee();
        employeeWithSmallNumber.setId("0005");
        employeeWithSmallNumber.setEmployeeNumber(5);
        employeeWithSmallNumber.setRole(Role.USER);
        employeeWithSmallNumber.setName("Test User");

        // When
        String token = jwtUtil.generateToken(employeeWithSmallNumber);
        String extractedSubject = jwtUtil.extractUsername(token);

        // Then
        assertEquals("0005", extractedSubject, "Subject should be formatted as 4-digit employee number");
    }

    // Test Case 13: Multiple Token Generation Consistency

    @Test
    @DisplayName("Should generate tokens with same claims for same employee")
    void generateToken_multipleTimes_shouldHaveSameClaims() throws InterruptedException {
        // Given
        String token1 = jwtUtil.generateToken(testEmployee);

        // Wait 1+ seconds to ensure different issued time (JWT iat is in seconds)
        Thread.sleep(1100);

        String token2 = jwtUtil.generateToken(testEmployee);

        // Then - tokens might be different due to different iat timestamps
        // But both should be valid and contain same claims
        assertTrue(jwtUtil.isTokenValid(token1), "First token should be valid");
        assertTrue(jwtUtil.isTokenValid(token2), "Second token should be valid");

        assertEquals(jwtUtil.extractEmployeeId(token1), jwtUtil.extractEmployeeId(token2),
                "Both tokens should have same employee ID");
        assertEquals(jwtUtil.extractRole(token1), jwtUtil.extractRole(token2),
                "Both tokens should have same role");
        assertEquals(jwtUtil.extractUsername(token1), jwtUtil.extractUsername(token2),
                "Both tokens should have same username");

        // Note: tokens generated at different times (different iat) will be different
        // This is expected behavior for JWT tokens
    }
}