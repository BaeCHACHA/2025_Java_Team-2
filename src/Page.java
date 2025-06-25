package teamproject;

import java.sql.Timestamp;

public record Page(long pageId,
                   long groupId,
                   String pageName,
                   Timestamp createdAt,
                   long latestRevisionId) {
}