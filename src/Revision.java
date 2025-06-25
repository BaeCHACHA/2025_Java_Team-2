package teamproject;

import java.sql.Timestamp;

public record Revision(long revisionId,
                       long pageId,
                       long actualDataId,
                       long committedByUserId,
                       long parentRevisionId,
                       String commitMessage,
                       Timestamp createdAt) {

}