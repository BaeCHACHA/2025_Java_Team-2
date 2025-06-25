package teamproject;

import java.sql.Timestamp;

public record Group(long groupId,
                    String groupName,
                    String joinKey,
                    long createdByUserId,
                    Timestamp createdAt) {
}