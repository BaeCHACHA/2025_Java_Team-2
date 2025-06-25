package teamproject;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserManager {

    private final ConnectionManager connectionManager;
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserManager(ConnectionManager connectionManager) {
        if (connectionManager == null) {
            throw new IllegalArgumentException("ConnectionManager cannot be null.");
        }
        this.connectionManager = connectionManager;
    }

    /**
     * 평문 비밀번호를 안전하게 해싱
     * @param plainPassword 평문 비밀번호
     * @return 해싱된 비밀번호 문자열
     */
    private String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * 평문 비밀번호와 저장된 해시된 비밀번호를 비교
     * @param plainPassword 비교할 평문 비밀번호
     * @param hashedPassword 저장된 해시된 비밀번호
     * @return 일치하면 true, 그렇지 않으면 false
     */
    private boolean checkPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    // --- CRUD 작업 메소드 ---
    /**
     * 새 사용자를 추가하고 생성된 user_id를 반환
     * 중복 사용자 이름인 경우 특정 오류 메시지를 출력하고 -2를 반환
     * 다른 데이터베이스 오류인 경우 오류 메시지를 출력하고 -1을 반환
     * @param username 사용자 이름
     * @param password 일반 비밀번호
     * @return 생성된 사용자의 ID (long), 실패 시 -1
     */
    public long addUser(String username, String password) {
        // 해싱된 비밀번호로 사용
        String passwordHash = hashPassword(password);

        String sql = "INSERT INTO users " +
                "(user_name, password_hash, created_at) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        System.out.println("User inserted successfully: " + username + " (ID: " + generatedId + ")");
                        return generatedId;
                    } else {
                        throw new SQLException("Inserting user failed for " + username + ", no ID obtained.");
                    }
                }
            } else {
                throw new SQLException("Inserting user failed for " + username + ", no rows affected.");
            }

        }
        catch (SQLException e) {
            System.err.println("User Manager: Error adding user: " + e.getMessage());
            if (e.getMessage().contains("Duplicate entry")) return -2;
            else return -1;
        }
    }

    /**
     * 사용자 이름으로 사용자를 검색
     * @param username 검색할 사용자 이름
     * @return 찾은 사용자 정보 객체 (User), 없으면 null
     */
    public User findUserByUsername(String username) {
        String sql = "SELECT user_id, user_name, created_at " +
                "FROM users " +
                "WHERE user_name = ?";
        ResultSet rs = null;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                long userId = rs.getLong("user_id");
                String foundUsername = rs.getString("user_name");
                Timestamp createdAt = rs.getTimestamp("created_at");

                return new User(userId, foundUsername, createdAt);
            } else {
                System.out.println("No user found with username: " + username);
            }

        } catch (SQLException e) {
            System.err.println("User Manager: Error searching user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 사용자 아이디로 사용자를 검색
     * @param userId 검색할 사용자 아이디
     * @return 찾은 사용자 정보 객체 (User), 없으면 null
     */
    public User findUserNameByUserId(long userId) {
    	 String sql = "SELECT user_id, user_name, created_at " +
                 "FROM users " +
                 "WHERE user_id = ?";
    	 ResultSet rs = null;
    	 
    	 try (Connection conn = connectionManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

             pstmt.setLong(1, userId);
             rs = pstmt.executeQuery();
             
             if (rs.next()) {
                 long foundUserId = rs.getLong("user_id");
                 String username = rs.getString("user_name");
                 Timestamp createdAt = rs.getTimestamp("created_at");

                 return new User(foundUserId, username, createdAt);
             } else {
                 System.out.println("No user found with userId: " + userId);
                 return null;
             }
    	 } catch (SQLException e) {
             System.err.println("Error searching user: " + e.getMessage());
             e.printStackTrace();
             return null;
         }
    }

    /**
     * 사용자 ID로 사용자를 삭제
     * @param userId 삭제할 사용자의 ID
     * @return 삭제된 행의 수
     */
    public int deleteUser(long userId) {
        String sql = "DELETE " +
                "FROM users " +
                "WHERE user_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);

            int affectedRows = pstmt.executeUpdate();
            System.out.println(affectedRows + " row(s) deleted.");
            return affectedRows;

        } catch (SQLException e) {
            System.err.println("User Manager: Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * 모든 사용자를 검색
     * @return 모든 사용자 목록
     */
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, user_name, created_at FROM users";
        List<User> userList = new ArrayList<>();
        ResultSet rs = null;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            rs = pstmt.executeQuery();

            while (rs.next()) {
                long userId = rs.getLong("user_id");
                String username = rs.getString("user_name");
                Timestamp createdAt = rs.getTimestamp("created_at");

                userList.add(new User(userId, username, createdAt));
            }

        } catch (SQLException e) {
            System.err.println("User Manager: Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }

        return userList;
    }

    /**
     * 사용자 이름으로 사용자의 인증 정보를 포함하여 검색
     * 인증 목적으로만 사용하며, 비밀번호 해시 값을 포함하여 반환
     * @param username 검색할 사용자 이름
     * @return 인증에 필요한 사용자 정보 (user_id, user_name, password_hash), 없으면 null
     */
    private AuthenticationUser getUserForAuthentication(String username) {
        String sql = "SELECT user_id, user_name, password_hash " +
                "FROM users " +
                "WHERE user_name = ?";
        ResultSet rs = null;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                long userId = rs.getLong("user_id");
                String foundUsername = rs.getString("user_name");
                String passwordHash = rs.getString("password_hash");

                return new AuthenticationUser(userId, foundUsername, passwordHash);
            }
        }
        catch (SQLException e) {
            System.err.println("User Manager: Error retrieving user for authentication: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 사용자 이름과 비밀번호 평문을 사용자의 인증 정보와 비교
     * @param username 검색할 사용자 이름
     * @param plainPassword 해당 사용자에 대한 비밀번호 평문
     * @return 인증에 성공하면 해당 User를 반환, 실패하면 null 반환
     */
    public User login(String username, String plainPassword) {
        AuthenticationUser userForAuth = getUserForAuthentication(username);

        if (userForAuth == null) {
            System.out.println("Authentication failed: User '" + username + "' not found");
            return null;
        }

        // 비밀번호 평문과, 인증 정보에 있는 해싱된 정보를 비교
        if (checkPassword(plainPassword, userForAuth.passwordHash())) {
            return findUserByUsername(userForAuth.username());
        }
        else {
            System.out.println("Authentication failed: Invalid password for User '" + username + "'");
            return null;
        }
    }

    // 인증 목적으로만 사용될 내부 클래스
        private record AuthenticationUser(long userId, String username, String passwordHash) {}
}