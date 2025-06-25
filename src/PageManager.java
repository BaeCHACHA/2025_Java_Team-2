package teamproject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PageManager {

    private final ConnectionManager connectionManager;

    private final User user;
    private final Group group;
    private final RevisionManager revisionManager;
    private final FileDataManager fileDataManager;
    private final CommentManager commentManager;

    PageManager(ConnectionManager connectionManager, User user, Group group) {
        if (connectionManager == null) {
            throw new IllegalArgumentException("ConnectionManager cannot be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for PageManager");
        }
        if (group == null) {
            throw new IllegalArgumentException("Group cannot be null for PageManager");
        }
        this.connectionManager = connectionManager;
        this.user = user;
        this.group = group;

        this.revisionManager = new RevisionManager();
        this.fileDataManager = new FileDataManager();
        this.commentManager = new CommentManager();
    }

    /**
     * 파일 내용의 체크섬 (SHA-256) 계산
     * @param content 파일 내용 (byte 배열)
     * @return 계산된 체크섬의 16진수 문자열 형태, 오류 시 null
     */
    private String calculateChecksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Error calculating checksum: SHA-256 algorithm not found.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * group_id에 대한 그룹에 속한 모든 페이지를 객체로 리턴
     * @return List 객체로 Page를 반환
     */
    public List<Page> searchPage(Connection conn) {
        String sql = "SELECT page_id, page_name, created_at, latest_revision_id " +
                "FROM pages " +
                "WHERE group_id = ?";
        ResultSet rs = null;
        List<Page> pageList = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, group.groupId());
            rs = pstmt.executeQuery();

            while (rs.next()) {
                long pageId = rs.getLong("page_id");
                String pageName = rs.getString("page_name");
                Timestamp createdAt = rs.getTimestamp("created_at");
                long latestRevisionId = rs.getLong("latest_revision_id");

                pageList.add(new Page(pageId, group.groupId(), pageName, createdAt, latestRevisionId));
            }
        }
        catch (SQLException e) {
            System.err.println("Page Manager: Error searching pages: " + e.getMessage());
        }

        return pageList;
    }

    /**
     * 현재 그룹에 페이지를 생성하고, 생성된 페이지의 ID를 반환
     * 1. pages 테이블에 데이터를 삽입하고 latest_revision_id 자리를 비워 둔다. page_id를 기억해 둔다.
     * 2. file_data 테이블에 실제 데이터를 삽입하고 actual_data_id를 받아온다.
     * 3. file_revision 테이블에 데이터를 삽입하고 revision_id를 받아 온다.
     *     3-1. actual_data_id, committed_by_user_id, page_id를 현재 ID값에 대해 할당한다.
     *     3-2. parent_revision_id = null, commit_message는 null 또는 입력한 값으로 한다.
     * 4. page_id 데이터의 latest_revision_id를 받아온 revision_id로 한다.
     * 5. page_id를 리턴한다.
     * 전체 과정을 하나의 트랜잭션으로 묶고, 예외 발생 시 롤백
     * @param pageName 페이지 이름
     * @param fileContent 실제 파일 데이터
     * @param commitMessage commit message
     * @return 생성된 page_id, 실패 시 -1
     */
    public long makePage(String pageName, byte[] fileContent, String commitMessage) {
        // 전체 과정을 하나의 트랜잭션으로 두고, 예외 발생 시 롤백 처리를 위해 try-with-resources 밖에서 선언 후 수행
        long generatedPageId = -1;

        Connection conn = null;

        try {
            conn = connectionManager.getConnection();

            // 트랜잭션 시작 부분 (자동 커밋 비활성화, finally에서 다시 활성화)
            conn.setAutoCommit(false);

            // 1. pages 테이블에 데이터를 삽입 (latest_revision_id = null)
            generatedPageId = insertPage(conn, group.groupId(), pageName);
            if (generatedPageId == -1) {
                throw new SQLException("Failed to insert page.");
            }

            // 2. file_data 테이블에 실제 파일 데이터를 삽입
            String checksum = calculateChecksum(fileContent);
            long actualDataId = fileDataManager.insertFileData(conn, fileContent, checksum);

            // 3. file_revisions 테이블에 데이터를 삽입, parent_revision_id = null
            long revisionId = revisionManager.insertRevision(
                    conn,
                    generatedPageId,    // 1에서 얻은 page_id
                    user.userId(),   // 현재 로그인한 user
                    null,               // 첫 revision이므로 parent_revision_id = null
                    commitMessage,
                    actualDataId        // 2에서 얻은 actual_data_id
            );

            // 4. pages 테이블의 latest_revision_id 업데이트
            updatePageLatestRevision(conn, generatedPageId, revisionId);

            // 5. 트랜잭션 커밋 후 리턴
            conn.commit();
            System.out.println("Page generated with ID: " + generatedPageId);
            return generatedPageId;
        }
        catch (SQLException e) {
            // 단계 중 하나라도 실패 시 롤백
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Page Manager: Transaction rolled back due to SQLException.");
                }
                catch (SQLException rollbackEx) {
                    System.err.println("Page Manager: Error during transaction rollback: " + rollbackEx);
                    return -1;
                    // TODO : 추가적인 로깅이 필요하다 (심각)
                }
            }
            System.err.println("Page Manager: Error creating page: " + e.getMessage());
            return -1;
            // TODO : 예외를 다시 던지거나 특정 오류 코드를 반환
        }
        finally {
            // 연결 닫기, 자동 커밋 다시 활성화
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                }
                catch (SQLException autoCommitEx) {
                    System.err.println("Error resetting auto-commit to true during cleanup: " + autoCommitEx.getMessage());
                    autoCommitEx.printStackTrace();
                    // TODO : 로깅
                    // 예외 발생 시 catch하되 연결 닫기를 막지 말아야 한다
                }

                try {
                    conn.close();
                }
                catch (SQLException closeEx) {
                    System.err.println("Error closing database connection in finally block: " + closeEx.getMessage());
                    closeEx.printStackTrace();
                    // TODO : 로깅, 연결 자원 누수 가능성 있음
                }
            }
        }
    }

    /**
     * pages 테이블에 새 레코드를 삽입
     * makePage 메소드에서만 호출됨
     * @param conn 데이터베이스 연결
     * @param groupId 페이지가 속할 group_id
     * @param pageName 페이지 이름
     * @return 삽입된 페이지의 page_id, 실패 시 -1
     * @throws SQLException 오류 발생 시 롤백을 유도할 Exception
     */
    private long insertPage(Connection conn, long groupId, String pageName) throws SQLException {
        // makePage 메소드에서 호출되는 메소드, latest_revision_id = null
        String sql = "INSERT INTO pages " +
                "(group_id, page_name) " +
                "VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, groupId);
            pstmt.setString(2, pageName);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                    else {
                        throw new SQLException("Inserting page failed, no ID obtained.");
                    }
                }
            }
            else {
                throw new SQLException("Inserting page failed, no rows affected.");
            }
        }
    }

    /**
     * pages 테이블의 latest_revision_id 값을 업데이트
     * makePage, insertRevision 메소드에서 호출되는 메소드
     * @param conn 데이터베이스 연결
     * @param pageId 업데이트할 페이지의 ID
     * @param latestRevisionId 가장 최신의 revision_id
     * @throws SQLException 오류 발생 시 롤백을 유도할 Exception
     */
    private void updatePageLatestRevision(Connection conn, long pageId, long latestRevisionId) throws SQLException {

        String sql = "UPDATE pages " +
                "SET latest_revision_id = ? " +
                "WHERE page_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, latestRevisionId);
            pstmt.setLong(2, pageId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating page latest_revision_id failed, page ID " + pageId + " not found.");
            }
        }
    }

    /**
     * 메소드 호출 시, 해당 user가 이 페이지를 삭제할 권한이 있는지 검사해야 한다.
     * page_id에 대한 페이지를 삭제한다.
     * 1. page_id에 대한 모든 revision을 삭제
     *     1-1. page_id에 대한 모든 revision을 List로 검색
     *     1-2. 리스트의 모든 항목들에 대해 삭제 처리
     * 2. 해당 페이지를 삭제
     * 3. 성공 시 커밋
     * 전체 과정을 하나의 트랜잭션으로 묶고, 예외 발생 시 롤백
     * @param pageId 삭제할 페이지의 ID
     */
    public void deletePage(long pageId) {
        // 전체 과정을 하나의 트랜잭션으로 두고, 예외 발생 시 롤백 처리를 위해 try-with-resources 밖에서 선언 후 수행
        Connection conn = null;

        try {
            conn = connectionManager.getConnection();

            // 트랜잭션 시작 부분 (자동 커밋 비활성화, finally에서 다시 활성화)
            conn.setAutoCommit(false);

            // 1. page_id에 대한 모든 revision을 삭제

            // 1-1. page_id에 대한 모든 revision을 검색
            List<Revision> revisionsToDelete = revisionManager.getRevisionsByPageId(conn, pageId);

            // 1-2. 리스트의 모든 revision과 연결된 file_data를 삭제한 후, revision 자체를 삭제
            for (Revision revision : revisionsToDelete) {
                int deletedRevisionsCount = revisionManager.deleteRevision(conn, revision.revisionId());
                if (deletedRevisionsCount == 0) {
                    System.err.println("Warning: Revision ID " + revision.revisionId() + " not found for deletion.");
                    // 트랜잭션 롤백 가능
                    // throw new SQLException("Revision " + revision.getRevisionId() + " not found during deletion process.");
                }

                int deletedFileDataCount = fileDataManager.deleteFileData(conn, revision.actualDataId());
                if (deletedFileDataCount == 0) {
                    System.err.println("Warning: FileData ID " + revision.actualDataId() + " not found for deletion.");
                    // 트랜잭션 롤백 가능
                    // throw new SQLException("File " + revision.getActualDataId() + " not found during deletion process.");
                }
            }

            // 2. 해당 페이지 삭제
            int deletedPagesCount = deletePageRecord(pageId, conn);
            if (deletedPagesCount == 0) {
                throw new SQLException("Page with ID " + pageId + " not found for deletion.");
            }

            // 3. 트랜잭션 커밋
            conn.commit();
            System.out.println("Page deleted for ID: " + pageId);
        }
        catch (SQLException e) {
            // 단계 중 하나라도 실패 시 다시 롤백
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Page Manager: Transaction rolled back due to SQLException during page deletion.");
                }
                catch (SQLException rollbackEx) {
                    System.err.println("Page Manager: Error during transaction rollback for page deletion: " + rollbackEx.getMessage());
                    rollbackEx.printStackTrace();
                    // TODO : 추가적인 로깅이 필요하다 (심각)
                }
            }
            System.err.println("Page Manager: Error deleting page with ID " + pageId + ": " + e.getMessage());
            e.printStackTrace();
            // TODO : 예외를 다시 던질 수 있음
        }
        finally {
            // 연결 닫기, 자동 커밋 다시 활성화
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                }
                catch (SQLException autoCommitEx) {
                    System.err.println("Error resetting auto-commit to true during cleanup: " + autoCommitEx.getMessage());
                    autoCommitEx.printStackTrace();
                    // TODO : 로깅
                    // 예외 발생 시 catch하되 연결 닫기를 막지 말아야 한다
                }

                try {
                    conn.close();
                }
                catch (SQLException closeEx) {
                    System.err.println("Error closing database connection in finally block: " + closeEx.getMessage());
                    closeEx.printStackTrace();
                    // TODO : 로깅, 연결 자원 누수 가능성 있음
                }
            }
        }
    }

    /**
     * deletePage 메소드에서 호출한 페이지를 삭제하는 메소드
     * 예외 발생 시 throw하여 deletePage 메소드에서 처리
     * @param pageId 삭제할 page_id
     * @return 삭제된 행의 수
     * @throws SQLException 롤백 또는 예외 처리를 유도할 Exception
     */
    private int deletePageRecord(long pageId, Connection conn) throws SQLException {

        String sql = "DELETE FROM pages " +
                "WHERE page_id = ?";

       
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setLong(1, pageId);
        return pstmt.executeUpdate();
        
    }

    /**
     * page_id에 해당하는 페이지에 속하는 모든 revision을 반환
     * @param pageId 검색할 페이지의 ID
     * @return List 객체로 Revision들을 반환, 실패 시 빈 ArrayList
     */
    public List<Revision> getRevisionsByPageId(long pageId) {
        try (Connection conn = connectionManager.getConnection()) {
            return revisionManager.getRevisionsByPageId(conn, pageId);
        }
        catch (SQLException e) {
            System.err.println("Page Manager: Error searching revisions by page id: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * page_id에 해당하는 페이지에 revision을 생성
     * @param pageId revision을 생성할 페이지의 ID
     * @param content 새로 개정된 데이터
     * @param commitMessage commit message
     * @return 생성된 revision_id, 실패 시 -1
     */
    public long insertRevision(long pageId, long parentRevisionId, byte[] content, String commitMessage) {
        try (Connection conn = connectionManager.getConnection()) {
            long actualDataId = fileDataManager.insertFileData(conn, content, calculateChecksum(content));
            long revisionId = revisionManager.insertRevision(
                    conn,
                    pageId,
                    user.userId(),
                    parentRevisionId,
                    commitMessage,
                    actualDataId
            );
            updatePageLatestRevision(conn, pageId, revisionId);// 생성된 리비젼이 최신 리비젼
            return revisionId;
        }
        catch (SQLException e) {
            System.err.println("Page Manager: Error inserting revision: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 새 댓글 추가
     * @param pageId 댓글을 추가할 페이지의 ID
     * @param commentContent 추가할 댓글 내용
     */
    public void insertComment(long pageId, String commentContent) throws SQLException {
        try (Connection conn = connectionManager.getConnection()) {
            commentManager.insertComment(conn, pageId, user.userId(), commentContent);
        }
        catch (SQLException e) {
            System.err.println("Page Manager: Error inserting comment for page ID " + pageId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 특정 댓글을 삭제
     * @param commentId 삭제할 댓글의 ID
     * @return DB에서 삭제된 행의 수
     */
    public int deleteComment(long commentId) {

        try (Connection conn = connectionManager.getConnection()) {
            int affectedRows = commentManager.deleteComment(conn, commentId, user.userId());

            if (affectedRows > 0) {
                System.out.println("Page Manager: Comment ID " + commentId + " deleted successfully by user " + user.userId());
            }
            else {
                System.out.println("Page Manager: User " + user.userId() + " does not have permission to delete.");
            }
            return affectedRows;
        }
        catch (SQLException e) {
            System.err.println("Page Manager: Error deleting comment ID " + commentId + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 특정 페이지의 모든 댓글 목록을 조회
     * @param pageId 댓글을 조회할 페이지의 ID
     * @return 해당 페이지의 Comment 객체
     */
    public List<Comment> getComments(long pageId) {

        List<Comment> comments = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection()) {
            comments = commentManager.getComments(conn, pageId);
        }
        catch (SQLException e) {
            System.err.println("Page Manager: Error getting comments for page ID " + pageId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return comments;
    }

    /**
     * page_id에 대한 페이지에서 latest_revision_id를 반환
     * @param pageId 검색할 페이지의 ID
     * @return latest_revision_id, 검색 실패 시 -1
     */
    long getLatestRevisionId(long pageId) {

        String sql = "SELECT latest_revision_id " +
                "FROM pages " +
                "WHERE page_id = ?";
        ResultSet rs = null;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, pageId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getLong("latest_revision_id");
            }
            else {
                throw new SQLException("Page with ID " + pageId + " not found");
            }
        }
        catch (SQLException e) {
            System.err.println("Error searching latest revision id: " + e.getMessage());
            return -1;
        }
    }
}