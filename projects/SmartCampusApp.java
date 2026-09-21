import java.sql.*;
import java.util.Scanner;

public class SmartCampusApp {
    private static final String DB_URL = "jdbc:sqlite:smart_campus.db";
    private static Connection conn;
    private static Scanner scanner = new Scanner(System.in);

    private static int currentUserId = -1;
    private static String currentUserRole = "";

    public static void main(String[] args) {
        initDatabase();
        
        System.out.println("=== SMART CAMPUS SYSTEM ===");
        while (true) {
            if (currentUserId == -1) {
                showLoginMenu();
            } else {
                switch (currentUserRole) {
                    case "ADMIN":
                        showAdminMenu();
                        break;
                    case "LECTURER":
                        showLecturerMenu();
                        break;
                    case "STUDENT":
                        showStudentMenu();
                        break;
                    default:
                        currentUserId = -1;
                        break;
                }
            }
        }
    }

    private static void initDatabase() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement();
            
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE, " +
                    "password TEXT, " +
                    "role TEXT, " +
                    "name TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "code TEXT UNIQUE, " +
                    "title TEXT, " +
                    "lecturer_id INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS enrollments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id INTEGER, " +
                    "course_id INTEGER, " +
                    "coursework_mark REAL DEFAULT 0, " +
                    "exam_mark REAL DEFAULT 0, " +
                    "attendance_count INTEGER DEFAULT 0)");

            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users WHERE role = 'ADMIN'");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, role, name) VALUES ('admin', 'admin123', 'ADMIN', 'System Admin')");
            }
        } catch (SQLException e) {
            System.err.println("DB Init error: " + e.getMessage());
        }
    }

    private static void showLoginMenu() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String uname = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();

        String sql = "SELECT id, role, name FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uname);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("id");
                currentUserRole = rs.getString("role");
                System.out.println("Welcome, " + rs.getString("name") + " (" + currentUserRole + ")");
            } else {
                System.out.println("Invalid credentials.");
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
    }

    private static void showAdminMenu() {
        System.out.println("\n--- ADMIN DASHBOARD ---");
        System.out.println("1. Register User (Student/Lecturer)");
        System.out.println("2. Add Course");
        System.out.println("3. Assign Lecturer to Course");
        System.out.println("4. Logout");
        System.out.print("Choice: ");
        
        String choice = scanner.nextLine();
        if (choice.equals("1")) {
            System.out.print("Username: ");
            String u = scanner.nextLine();
            System.out.print("Password: ");
            String p = scanner.nextLine();
            System.out.print("Role (STUDENT/LECTURER): ");
            String r = scanner.nextLine().toUpperCase();
            System.out.print("Full Name: ");
            String n = scanner.nextLine();

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password, role, name) VALUES (?, ?, ?, ?)")) {
                pstmt.setString(1, u);
                pstmt.setString(2, p);
                pstmt.setString(3, r);
                pstmt.setString(4, n);
                pstmt.executeUpdate();
                System.out.println("User registered successfully.");
            } catch (SQLException e) {
                System.out.println("Error saving user: " + e.getMessage());
            }
        } else if (choice.equals("2")) {
            System.out.print("Course Code: ");
            String code = scanner.nextLine();
            System.out.print("Course Title: ");
            String title = scanner.nextLine();

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO courses (code, title) VALUES (?, ?)")) {
                pstmt.setString(1, code);
                pstmt.setString(2, title);
                pstmt.executeUpdate();
                System.out.println("Course added.");
            } catch (SQLException e) {
                System.out.println("Error adding course: " + e.getMessage());
            }
        } else if (choice.equals("3")) {
            System.out.print("Course ID: ");
            int cId = Integer.parseInt(scanner.nextLine());
            System.out.print("Lecturer User ID: ");
            int lId = Integer.parseInt(scanner.nextLine());

            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE courses SET lecturer_id = ? WHERE id = ?")) {
                pstmt.setInt(1, lId);
                pstmt.setInt(2, cId);
                pstmt.executeUpdate();
                System.out.println("Lecturer assigned.");
            } catch (SQLException e) {
                System.out.println("Error assigning lecturer: " + e.getMessage());
            }
        } else if (choice.equals("4")) {
            currentUserId = -1;
            currentUserRole = "";
        }
    }

    private static void showLecturerMenu() {
        System.out.println("\n--- LECTURER DASHBOARD ---");
        System.out.println("1. View My Assigned Courses");
        System.out.println("2. Record Student Attendance");
        System.out.println("3. Enter Marks");
        System.out.println("4. Logout");
        System.out.print("Choice: ");

        String choice = scanner.nextLine();
        if (choice.equals("1")) {
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM courses WHERE lecturer_id = ?")) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    System.out.println("[" + rs.getInt("id") + "] " + rs.getString("code") + " - " + rs.getString("title"));
                }
            } catch (SQLException e) {
                System.out.println("Error fetching courses: " + e.getMessage());
            }
        } else if (choice.equals("2")) {
            System.out.print("Enrollment ID: ");
            int eId = Integer.parseInt(scanner.nextLine());
            
            // Todo: optimize this loop later
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE enrollments SET attendance_count = attendance_count + 1 WHERE id = ?")) {
                pstmt.setInt(1, eId);
                pstmt.executeUpdate();
                System.out.println("Attendance updated.");
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (choice.equals("3")) {
            System.out.print("Enrollment ID: ");
            int eId = Integer.parseInt(scanner.nextLine());
            System.out.print("Coursework Mark: ");
            double cw = Double.parseDouble(scanner.nextLine());
            System.out.print("Exam Mark: ");
            double ex = Double.parseDouble(scanner.nextLine());

            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE enrollments SET coursework_mark = ?, exam_mark = ? WHERE id = ?")) {
                pstmt.setDouble(1, cw);
                pstmt.setDouble(2, ex);
                pstmt.setInt(3, eId);
                pstmt.executeUpdate();
                System.out.println("Marks recorded.");
            } catch (SQLException e) {
                System.out.println("Error recording marks: " + e.getMessage());
            }
        } else if (choice.equals("4")) {
            currentUserId = -1;
            currentUserRole = "";
        }
    }

    private static void showStudentMenu() {
        System.out.println("\n--- STUDENT DASHBOARD ---");
        System.out.println("1. Register for Course");
        System.out.println("2. View Registered Courses & Attendance");
        System.out.println("3. View Results & Transcript");
        System.out.println("4. Logout");
        System.out.print("Choice: ");

        String choice = scanner.nextLine();
        if (choice.equals("1")) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM courses");
                System.out.println("Available Courses:");
                while (rs.next()) {
                    System.out.println("[" + rs.getInt("id") + "] " + rs.getString("code") + " - " + rs.getString("title"));
                }
                System.out.print("Enter Course ID to register: ");
                int cId = Integer.parseInt(scanner.nextLine());

                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)");
                pstmt.setInt(1, currentUserId);
                pstmt.setInt(2, cId);
                pstmt.executeUpdate();
                System.out.println("Enrolled successfully.");
            } catch (SQLException e) {
                System.out.println("Error enrolling: " + e.getMessage());
            }
        } else if (choice.equals("2") || choice.equals("3")) {
            // Combined handling for quick report view
            String sql = "SELECT e.id, c.code, c.title, e.attendance_count, e.coursework_mark, e.exam_mark " +
                         "FROM enrollments e JOIN courses c ON e.course_id = c.id WHERE e.student_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();
                
                double totalMarks = 0;
                int count = 0;

                System.out.println("\n--- ACADEMIC RECORDS ---");
                while (rs.next()) {
                    double cw = rs.getDouble("coursework_mark");
                    double ex = rs.getDouble("exam_mark");
                    double finalMark = cw + ex; 
                    totalMarks += finalMark;
                    count++;

                    String grade = calculateGrade(finalMark);
                    System.out.println(rs.getString("code") + " | Attended: " + rs.getInt("attendance_count") + 
                                       " | Final Mark: " + finalMark + " | Grade: " + grade);
                }

                if (choice.equals("3") && count > 0) {
                    double avg = totalMarks / count;
                    System.out.printf("Overall Average: %.2f%%\n", avg);
                    System.out.printf("Estimated GPA: %.2f\n", (avg / 100.0) * 4.0);
                }
            } catch (SQLException e) {
                System.out.println("Error generating report: " + e.getMessage());
            }
        } else if (choice.equals("4")) {
            currentUserId = -1;
            currentUserRole = "";
        }
    }

    private static String calculateGrade(double mark) {
        if (mark >= 80) return "A";
        if (mark >= 75) return "B+";
        if (mark >= 70) return "B";
        if (mark >= 65) return "C+";
        if (mark >= 60) return "C";
        if (mark >= 50) return "D";
        return "F";
    }
}