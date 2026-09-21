package com.campus.controller;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ApiController {

    private static final String DB_URL = "jdbc:sqlite:smart_campus.db";

    public ApiController() {
        initDb();
    }

    private void initDb() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, role TEXT, name TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE, title TEXT, lecturer_id INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS enrollments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, course_id INTEGER, " +
                    "coursework_mark REAL DEFAULT 0, exam_mark REAL DEFAULT 0, attendance_count INTEGER DEFAULT 0)");

            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users WHERE role = 'ADMIN'");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, role, name) VALUES ('admin', 'admin123', 'ADMIN', 'System Admin')");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload) {
        Map<String, Object> res = new HashMap<>();
        String sql = "SELECT id, role, name FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, payload.get("username"));
            pstmt.setString(2, payload.get("password"));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                res.put("status", "success");
                res.put("userId", rs.getInt("id"));
                res.put("role", rs.getString("role"));
                res.put("name", rs.getString("name"));
            } else {
                res.put("status", "error");
                res.put("message", "Invalid credentials");
            }
        } catch (SQLException e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    // --- ADMIN ENDPOINTS ---
    @PostMapping("/admin/users")
    public Map<String, Object> addUser(@RequestBody Map<String, String> payload) {
        Map<String, Object> res = new HashMap<>();
        String sql = "INSERT INTO users (username, password, role, name) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, payload.get("username"));
            pstmt.setString(2, payload.get("password"));
            pstmt.setString(3, payload.get("role"));
            pstmt.setString(4, payload.get("name"));
            pstmt.executeUpdate();
            res.put("status", "success");
        } catch (SQLException e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/admin/courses")
    public Map<String, Object> addCourse(@RequestBody Map<String, String> payload) {
        Map<String, Object> res = new HashMap<>();
        String sql = "INSERT INTO courses (code, title) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, payload.get("code"));
            pstmt.setString(2, payload.get("title"));
            pstmt.executeUpdate();
            res.put("status", "success");
        } catch (SQLException e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/admin/assign")
    public Map<String, Object> assignLecturer(@RequestBody Map<String, Integer> payload) {
        Map<String, Object> res = new HashMap<>();
        String sql = "UPDATE courses SET lecturer_id = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, payload.get("lecturerId"));
            pstmt.setInt(2, payload.get("courseId"));
            pstmt.executeUpdate();
            res.put("status", "success");
        } catch (SQLException e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/courses")
    public List<Map<String, Object>> getCourses() {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM courses")) {
            
            while (rs.next()) {
                Map<String, Object> c = new HashMap<>();
                c.put("id", rs.getInt("id"));
                c.put("code", rs.getString("code"));
                c.put("title", rs.getString("title"));
                c.put("lecturerId", rs.getInt("lecturer_id"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- LECTURER ENDPOINTS ---
    @GetMapping("/lecturer/courses/{lecturerId}")
    public List<Map<String, Object>> getLecturerCourses(@PathVariable int lecturerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE lecturer_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, lecturerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> c = new HashMap<>();
                c.put("id", rs.getInt("id"));
                c.put("code", rs.getString("code"));
                c.put("title", rs.getString("title"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @PostMapping("/lecturer/attendance")
    public Map<String, Object> updateAttendance(@RequestBody Map<String, Integer> payload) {
        Map<String, Object> res = new HashMap<>();
        String sql = "UPDATE enrollments SET attendance_count = attendance_count + 1 WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, payload.get("enrollmentId"));
            pstmt.executeUpdate();
            res.put("status", "success");
        } catch (SQLException e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/lecturer/marks")
    public Map<String, Object> updateMarks(@RequestBody Map<String, Object> payload) {
        Map<String, Object> res = new HashMap<>();
        String sql = "UPDATE enrollments SET coursework_mark = ?, exam_mark = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, Double.parseDouble(payload.get("cw").toString()));
            pstmt.setDouble(2, Double.parseDouble(payload.get("exam").toString()));
            pstmt.setInt(3, Integer.parseInt(payload.get("enrollmentId").toString()));
            pstmt.executeUpdate();
            res.put("status", "success");
        } catch (SQLException e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    // --- STUDENT ENDPOINTS ---
    @PostMapping("/student/enroll")
    public Map<String, Object> enrollCourse(@RequestBody Map<String, Integer> payload) {
        Map<String, Object> res = new HashMap<>();
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, payload.get("studentId"));
            pstmt.setInt(2, payload.get("courseId"));
            pstmt.executeUpdate();
            res.put("status", "success");
        } catch (SQLException e) {
            res.put("status", "error");
            res.put("message", e.getMessage());
        }
        return res;
    }

    @GetMapping("/student/records/{studentId}")
    public List<Map<String, Object>> getStudentRecords(@PathVariable int studentId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT e.id, c.code, c.title, e.attendance_count, e.coursework_mark, e.exam_mark " +
                     "FROM enrollments e JOIN courses c ON e.course_id = c.id WHERE e.student_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> r = new HashMap<>();
                r.put("enrollmentId", rs.getInt("id"));
                r.put("code", rs.getString("code"));
                r.put("title", rs.getString("title"));
                r.put("attendance", rs.getInt("attendance_count"));
                r.put("cw", rs.getDouble("coursework_mark"));
                r.put("exam", rs.getDouble("exam_mark"));
                
                double total = rs.getDouble("coursework_mark") + rs.getDouble("exam_mark");
                r.put("total", total);
                r.put("grade", calcGrade(total));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String calcGrade(double mark) {
        if (mark >= 80) return "A";
        if (mark >= 75) return "B+";
        if (mark >= 70) return "B";
        if (mark >= 65) return "C+";
        if (mark >= 60) return "C";
        if (mark >= 50) return "D";
        return "F";
    }
}
