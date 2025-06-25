package teamproject;

import java.sql.Timestamp;

public record FileData(long actualDataId,
                       long revisionId,
                       byte[] content,
                       String checkSum,
                       Timestamp createdAt) {
}