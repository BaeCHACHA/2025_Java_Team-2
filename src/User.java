package teamproject;

import java.sql.Timestamp;

public record User(long userId,
                   String username,
                   Timestamp createdAt) {
}