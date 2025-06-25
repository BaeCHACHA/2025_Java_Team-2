package teamproject;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupManager {

    private final ConnectionManager connectionManager;

    private static final int JOIN_KEY_LENGTH = 10;
    private static final int MAX_KEY_GENERATION_RETRIES = 10;

    private final User user;

    // 반드시 로그인 한 뒤 user를 할당하고 사용
    GroupManager(ConnectionManager connectionManager, User user) {
        if (connectionManager == null) {
            throw new IllegalArgumentException("ConnectionManager cannot be null.");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for GroupManager");
        }
        this.user = user;
        this.connectionManager = connectionManager;
    }

    /**
     * 해당 user에 대한 모든 membership을 SEARCH
     * user_id, group_id, user_role을 매핑
     * @return 모든 Membership 목록
     */
    public List<Membership> searchGroup() {
        String sql = "SELECT group_id, user_role " +
                "FROM group_membership " +
                "WHERE user_id = ?";

        ResultSet rs = null;
        List<Membership> membershipList = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, user.userId());
            rs = pstmt.executeQuery();

            while (rs.next()) {
                long groupId = rs.getLong("group_id");
                String userRole = rs.getString("user_role");

                membershipList.add(new Membership(groupId, user.userId(), userRole));
            }
        }
        catch (SQLException e) {
            System.err.println("Group Manager: Error searching group membership: " + e.getMessage());
            e.printStackTrace();
        }

        return membershipList;
    }

    /**
     * 현재 user가 group을 생성
     * 생성한 group에 대한 membership 만들기
     * @param groupName group 이름
     * @return INSERT 성공 시 group_id, 실패 시 -1을 반환
     */
    public long makeGroup(String groupName) {
        String sql = "INSERT INTO group_data " +
                "(group_name, join_key, created_by_user_id) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, groupName);
            pstmt.setString(2, generateUniqueJoinKey());
            pstmt.setLong(3, user.userId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        System.out.println("Group '" + groupName + "' inserted successfully with ID: " + generatedId);
                        makeGroupMembership(generatedId, user.userId(), "admin");

                        return generatedId;
                    } else {
                        throw new SQLException("Inserting group failed for " + groupName + ", no ID obtained.");
                    }
                }
            } else {
                throw new SQLException("Inserting group failed for " + groupName + ", no rows affected.");
            }
        }
        catch (SQLException e) {
            System.err.println("Group Manager: Error inserting group: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 특정 그룹에 대한 멤버십을 생성
     * @param groupId 멤버십을 생성할 그룹 ID
     * @param userId 멤버십을 생성할 사용자 ID
     * @param role 해당 사용자의 역할 ("admin" 또는 "user")
     */
    private boolean makeGroupMembership(long groupId, long userId, String role) {
        // 이미 해당 group_id와 user_id에 대한 membership이 존재하면 insert하지 않고 true 반환

        // 1. 해당 그룹에 해당 사용자의 멤버십이 이미 존재하는지 확인
        String checkSql = "SELECT COUNT(*) " +
                "FROM group_membership " +
                "WHERE group_id = ? AND user_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {

            checkPstmt.setLong(1, groupId);
            checkPstmt.setLong(2, userId);

            try (ResultSet rs = checkPstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Membership already exists for User ID " + userId + " in Group ID " + groupId);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Group Manager: Error checking existing membership: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // 2. 멤버십이 존재하지 않으면 새로 삽입
        String insertSql = "INSERT INTO group_membership " +
                "(group_id, user_id, user_role) " +
                "VALUES (?, ?, ?)";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) { // 생성 키 반환 설정

            insertPstmt.setLong(1, groupId);
            insertPstmt.setLong(2, userId);
            insertPstmt.setString(3, role);

            int affectedRows = insertPstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        System.out.println("Membership inserted successfully. Membership ID: " + generatedKeys.getLong(1));
                        return true;
                    } else {
                        throw new SQLException("Inserting membership succeeded but no ID obtained.");
                    }
                }
            }
            else {
                throw new SQLException("Inserting membership failed, no rows affected.");
            }
        }
        catch (SQLException e) {
            System.err.println("Group Manager: Error inserting group membership: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 해당 joinKey에 대한 group이 존재하는지 검색하고, 존재하면 현재 user를 해당 그룹에 참여
     * @param joinKey 참여할 그룹의 join_key
     * 수정)GUI에서 케이스에 따른 그룹 가입 가능여부를 표현하기 위해 리턴값을 int로 변경했습니다
     */
    public int joinGroup(Connection conn, String joinKey, long userId) {
        // 1. join_key로 그룹 ID 찾기
        String findGroupSql = "SELECT group_id " +
                "FROM group_data " +
                "WHERE join_key = ?";

        long targetGroupId = -1;
        try (PreparedStatement pstmt = conn.prepareStatement(findGroupSql)) {

            pstmt.setString(1, joinKey);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    targetGroupId = rs.getLong("group_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Group Manager: Error searching group by join key: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
        // 해당 join_key를 가진 그룹이 없는 경우
        if (targetGroupId == -1) {
            System.err.println("Joining group failed: Invalid join key '" + joinKey + "'.");
            return -1;
        }

        // 2. 찾은 그룹에 현재 사용자를 일반 멤버로 추가 (makeGroupMembership 재사용)
        if(!makeGroupMembership(targetGroupId, user.userId(), "user")) {
        	System.out.println("이미 참여중인 그룹입니다.");
        	return -2;
        }
        
        return 1;
    }

    /**
     * 해당 group_id에 대한 group이 존재하는지 검색하고
     * 존재하면 해당 group과 membership에 대한 데이터를 삭제하는 메소드를 호출
     * @param groupId 삭제할 group_id
     */
    public boolean deleteGroup(long groupId) {
        String sql = "SELECT created_by_user_id " +
                "FROM group_data " +
                "WHERE group_id = ?";

        ResultSet rs = null;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
            	// 현재 로그인한 user_id가 해당 그룹의 created_by_user_id인지 확인
                if (user.userId() == rs.getLong("created_by_user_id")) {
                    deleteData(groupId);
                    return true;
                }
                else {
                    System.err.println("User is not group admin");
                    return false;
                }
            }
            else {
                throw new SQLException("Group is not found");
            }
        }
        catch (SQLException e) {
            System.err.println("Group Manager: Error deleting group: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * group_membership과 group_data 테이블에서 해당 group_id값을 가진 데이터를 모두 삭제
     * @param groupId 삭제할 그룹의 group_id
     */
    private void deleteData(long groupId) throws SQLException {
        String sql = "DELETE " +
                "FROM group_membership " +
                "WHERE group_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);

            int affectedRows = pstmt.executeUpdate();
            System.out.println(affectedRows + " row(s) deleted from group membership");

        } catch (SQLException e) {
            System.err.println("Group Manager: Error deleting group membership: " + e.getMessage());
            throw e;
        }

        sql = "DELETE " +
                "FROM group_data " +
                "WHERE group_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);

            int affectedRows = pstmt.executeUpdate();
            System.out.println(affectedRows + " row(s) deleted from group data");

        } catch (SQLException e) {
            System.err.println("Group Manager: Error deleting group data: " + e.getMessage());
            throw e;
        }

        System.out.println("Group with ID: " + groupId + " deleted successfully.");
    }

    /**
     * group_id에 대한 그룹 데이터를 조회하여 Group 객체로 반환
     * @param groupId 조회할 그룹의 group_id
     * @return 해당 groupId에 해당하는 Group 객체, 없으면 null
     */
    public Group selectGroup(long groupId) {
        String sql = "SELECT group_id, group_name, join_key, created_by_user_id, created_at " +
                "FROM group_data " +
                "WHERE group_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long retrievedGroupId = rs.getLong("group_id");
                    String groupName = rs.getString("group_name");
                    String joinKey = rs.getString("join_key");
                    long createdByUserId = rs.getLong("created_by_user_id");
                    Timestamp createdAt = rs.getTimestamp("created_at");

                    return new Group(retrievedGroupId, groupName, joinKey, createdByUserId, createdAt);
                }
                else {
                    System.out.println("Group with ID " + groupId + " not found.");
                }
            }
        }
        catch (SQLException e) {
            System.err.println("Group Manager: Error selecting group with ID " + groupId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * group_id에 대한 join key를 반환
     * @param groupId 조회할 그룹의 group_id
     * @return 해당 그룹의 join key, 존재하지 않으면 null
     */
    public String getJoinKey(long groupId) {

        String sql = "SELECT join_key " +
                "FROM group_data " +
                "WHERE group_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, groupId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String joinKey = rs.getString("join_key");
                    System.out.println("Join key for Group ID " + groupId + ": " + joinKey);
                    return joinKey;
                }
                else {
                    throw new SQLException("Group with ID " + groupId + " not found for join key search.");
                }
            }
        }
        catch (SQLException e) {
            System.err.println("Error searching join key for group ID " + groupId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * JOIN_KEY_LENGTH 길이(10)의 랜덤한 문자열을 생성
     * group_data에 join_key가 존재하는지 확인 후, 존재하지 않는 키이면 생성하여 반환
     * 최대 시도 횟수 내에 고유한 키를 찾지 못하면 예외 발생
     * @return 생성된 고유한 랜덤 문자열 (join_key)
     */
    private String generateUniqueJoinKey() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom secureRandom = new SecureRandom();
        String key = null;
        boolean isUnique = false;
        int attempts = 0;

        while (!isUnique && attempts < MAX_KEY_GENERATION_RETRIES) {
            StringBuilder sb = new StringBuilder(JOIN_KEY_LENGTH);
            for (int i = 0; i < JOIN_KEY_LENGTH; i++) {
                int randomIndex = secureRandom.nextInt(characters.length());
                sb.append(characters.charAt(randomIndex));
            }
            key = sb.toString();
            attempts++;

            String checkSql = "SELECT COUNT(*) " +
                    "FROM group_data " +
                    "WHERE join_key = ?";
            try (Connection conn = connectionManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(checkSql)) {

                pstmt.setString(1, key);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        // 해당 키가 데이터베이스에 존재하지 않음 (고유함)
                        isUnique = true;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error checking join_key uniqueness for key " + key + ": " + e.getMessage());
                throw new RuntimeException("Database error during unique join key generation check.", e);
            }
            // 루프 조건을 통해 isUnique가 true가 되면 반복 종료
        }

        if (!isUnique) {
            // 최대 시도 횟수 내에 고유한 키를 찾지 못한 경우
            throw new RuntimeException("Failed to generate a unique join key after " + MAX_KEY_GENERATION_RETRIES + " retries.");
        }
        return key; // 고유함이 확인된 키 반환
    }
    
    /**
     * 주어진 joinKey로 group_id를 조회
     * @param joinKey 그룹 참여 키
     * @return group_id (존재하지 않으면 -1 반환)
     */
    public long getGroupIdByJoinKey(String joinKey) {  // joinKey로 그룹 ID를 얻는 메서드
        String sql = "SELECT group_id FROM group_data WHERE join_key = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, joinKey);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("group_id");
                } else {
                    System.err.println("No group found with join key: " + joinKey);
                    return -1;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting group ID by join key: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    public String getGroupNameByJoinKey(String joinKey) {  // joinKey로 그룹 이름을 얻는 메서드
        String groupName = null;
        
        String sql = "SELECT group_name FROM group_data WHERE join_key = ?";
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, joinKey);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                groupName = rs.getString("group_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return groupName;
    }
}
