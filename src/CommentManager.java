package teamproject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CommentManager {

    public CommentManager() { }

    /**
     * 새 댓글 추가
     * @param conn 데이터베이스 연결 객체
     * @param pageId 댓글이 달릴 페이지 ID
     * @param userId 댓글 작성자 ID
     * @param commentContent 댓글 내용
     * @throws SQLException SQL 오류 발생 시
     */
    public void insertComment(Connection conn, long pageId, long userId, String commentContent) throws SQLException {
        String sql = "SELECT user_name " +
                "FROM users " +
                "WHERE user_id = ?";

        ResultSet rs = null;
        String userName = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                userName = rs.getString("user_name");
            }
            else {
                throw new SQLException("Error searching user inserting comment.");
            }
        }

        sql = "INSERT INTO comments " +
                "(page_id, commented_by_user_id, commented_by_user_name, comment_data) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, pageId);
            pstmt.setLong(2, userId);
            pstmt.setString(3, userName);
            pstmt.setString(4, commentContent);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("CommentManager: Creating comment failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    generatedKeys.getLong(1);
                } else {
                    throw new SQLException("CommentManager: Creating comment failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * 특정 댓글을 데이터베이스에서 삭제
     * @param conn 데이터베이스 연결 객체
     * @param commentId 삭제할 댓글 ID
     * @param userId 작업을 요청한 사용자 ID (권한 확인용)
     * @return 삭제된 행의 수
     * @throws SQLException SQL 오류 발생 시
     */
    public int deleteComment(Connection conn, long commentId, long userId) throws SQLException {
        String sql = "SELECT commented_by_user_id " +
                "FROM comments " +
                "WHERE comment_id = ?";

        ResultSet rs = null;
        long commentedByUserId;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                commentedByUserId = rs.getLong("commented_by_user_id");
            }
            else {
                throw new SQLException("Error searching comment to delete");
            }
        }

        if (userId == commentedByUserId) {
            sql = "DELETE FROM comments " +
                    "WHERE comment_id = ? AND commented_by_user_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, commentId);
                pstmt.setLong(2, userId);
                return pstmt.executeUpdate();
            }
        }
        else {
            System.out.println("User " + userId + " does not have permission to delete.");
            return 0;
        }
    }

    /**
     * 특정 페이지의 모든 댓글을 가져옴
     * 사용자 이름도 함께 가져오기 위해 users 테이블과 조인
     * @param conn 데이터베이스 연결 객체
     * @param pageId 댓글을 조회할 페이지 ID
     * @return 해당 페이지의 Comment 객체 리스트
     * @throws SQLException SQL 오류 발생 시
     */
    public List<Comment> getComments(Connection conn, long pageId) throws SQLException {
        List<Comment> comments = new ArrayList<>();

        // 댓글 정보와 함께 사용자 이름(username)을 가져오기 위해 COALESCE와 LEFT JOIN 사용
        String sql = "SELECT c.comment_id, c.page_id, c.commented_by_user_id, COALESCE(u.user_name, '탈퇴한 사용자') as commented_by_user_name, c.comment_data, c.created_at " +
                "FROM comments c " +
                "LEFT JOIN users u ON c.commented_by_user_id = u.user_id " +
                "WHERE c.page_id = ? ORDER BY c.created_at ASC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(new Comment(
                            rs.getLong("comment_id"),
                            rs.getLong("page_id"),
                            rs.getLong("commented_by_user_id"),
                            rs.getString("commented_by_user_name"),
                            rs.getString("comment_data"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return comments;
    }
    
    /**
     * 페이지 삭제시 해당 페이지에 속한 댓글 모두 삭제
     * @param conn
     * @param pageId
     */
    public void deleteAllComments(Connection conn, long pageId) {
    	String sql = "DELETE FROM comments WHERE page_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pageId);
            pstmt.executeUpdate();
        } catch(SQLException e) {
        	e.getStackTrace();
        	System.out.println("댓글 삭제 실패");
        }
    }
}