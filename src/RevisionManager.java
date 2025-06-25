package teamproject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RevisionManager {

    RevisionManager() {}

    /**
     * file_revisions 테이블에 데이터를 삽입
     * @param pageId 이 revision이 속한 page_id
     * @param committedByUserId 이 revision을 생성한 user_id
     * @param parentRevisionId 부모 revision (첫 revision의 경우 null)
     * @param commitMessage commit message
     * @param actualDataId 실제 파일 데이터가 저장된 file_data 테이블의 actual_data_id
     * @return 삽입된 file_revisions 테이블의 revision_id
     * @throws SQLException 오류 발생 시 롤백을 유도할 Exception
     */
    public long insertRevision(Connection conn,
                               long pageId,
                               long committedByUserId,
                               Long parentRevisionId,   // 처음엔 null이어야 하므로
                               String commitMessage,
                               long actualDataId)
            throws SQLException {

        String sql = "INSERT INTO file_revisions " +
                "(page_id, committed_by_user_id, parent_revision_id, commit_message, actual_data_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, pageId);
            pstmt.setLong(2, committedByUserId);

            // parentRevisionId가 null인 경우 null로 처리
            if (parentRevisionId == null) {
                pstmt.setNull(3, Types.BIGINT);
            }
            else {
                pstmt.setLong(3, parentRevisionId);
            }

            pstmt.setString(4, commitMessage);
            pstmt.setLong(5, actualDataId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                    else {
                        throw new SQLException("Inserting revision failed, no ID obtained.");
                    }
                }
            }
            else {
                throw new SQLException("Inserting revision failed, no rows affected.");
            }
        }
    }

    /**
     * file_revisions 테이블에서 revision_id에 해당하는 데이터를 삭제
     * 부모 revision도 삭제할 수 있으므로, 이 메소드를 호출하는 메소드에서 판단하여 처리할 것
     * @param revisionId 삭제할 revision_id
     * @return 삭제된 행의 수
     * @throws SQLException 오류 발생 시 롤백을 유도할 Exception
     */
    public int deleteRevision(Connection conn, long revisionId) throws SQLException {

        String sql = "DELETE FROM file_revisions " +
                "WHERE revision_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, revisionId);

            return pstmt.executeUpdate();
        }
    }

    /**
     * revision_id에 해당하는 revision을 반환
     * @param revisionId 찾을 revision_id
     * @return 찾은 Revision 객체, 검색 실패 시 null
     */
    public Revision getRevision(Connection conn, long revisionId) throws SQLException {

        String sql = "SELECT * " +
                "FROM file_revisions " +
                "WHERE revision_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, revisionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                long pageId = rs.getLong("page_id");
                long actualDataId = rs.getLong("actual_data_id");
                long committedByUserId = rs.getLong("committed_by_user_id");
                Long parentRevisionId = rs.getLong("parent_revision_id");
                String commitMessage = rs.getString("commit_message");
                Timestamp createdAt = rs.getTimestamp("created_at");

                return new Revision(
                        revisionId,
                        pageId,
                        actualDataId,
                        committedByUserId,
                        parentRevisionId,
                        commitMessage,
                        createdAt);

            }
        }
        catch (SQLException e) {
            System.err.println("Revision Manager: Error searching revision: " + e.getMessage());
            throw e;
        }
        return null;
    }

    /**
     * page_id에 해당하는 페이지에 속하는 모든 revision을 반환
     * PageManager에서 이 메소드를 호출
     * @param pageId 검색할 페이지의 ID
     * @return List 객체로 Revision들을 반환
     * @throws SQLException 예외 발생 시, 호출한 PageManager에서 처리
     */
    public List<Revision> getRevisionsByPageId(Connection conn, long pageId) throws SQLException {

        String sql = "SELECT * " +
                "FROM file_revisions " +
                "WHERE page_id = ?";
        List<Revision> revisionList = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, pageId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                long revisionId = rs.getLong("revision_id");
                long actualDataId = rs.getLong("actual_data_id");
                long committedByUserId = rs.getLong("committed_by_user_id");
                Long parentRevisionId = rs.getLong("parent_revision_id");
                String commitMessage = rs.getString("commit_message");
                Timestamp createdAt = rs.getTimestamp("created_at");

                revisionList.add(new Revision(
                        revisionId,
                        pageId,
                        actualDataId,
                        committedByUserId,
                        parentRevisionId,
                        commitMessage,
                        createdAt)
                );
            }
        }
        return revisionList;
    }
}