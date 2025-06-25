package teamproject;

import java.sql.*;

public class FileDataManager {

    public FileDataManager() {}

    public long insertFileData(Connection conn, byte[] content, String checksum) throws SQLException {
        String sql = "INSERT INTO file_data " +
                "(content, checksum) " +
                "VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setBytes(1, content);
            pstmt.setString(2, checksum);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                    else {
                        throw new SQLException("Inserting file_data failed, no ID obtained.");
                    }
                }
            }
            else {
                throw new SQLException("Inserting file_data failed, no rows affected.");
            }
        }
    }

    public int deleteFileData(Connection conn, long fileDataId) throws SQLException {
        String sql = "DELETE FROM file_data " +
                "WHERE actual_data_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, fileDataId);
            return pstmt.executeUpdate();
        }
    }

    // TODO: 파일 조회 메소드
    public void getFileData(Connection conn, long fileDataId) throws SQLException {

    }
}