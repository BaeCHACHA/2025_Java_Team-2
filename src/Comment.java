package teamproject;

import java.sql.Timestamp;

public record Comment(long commentId,
                      long pageId,
                      long commentedByUserId,
                      String userName,
                      String commentData,
                      Timestamp createdAt) {
}