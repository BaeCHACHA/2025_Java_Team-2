package teamproject;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import java.awt.Container;

public class PageController {
	private final PageManager pageManager;
	private final ConnectionManager connectionManager;
	private CommentManager commentManager;
	private User currentUser;
	private Group currentGroup;
	
	// getter()
	public User getCurrentUser() {
	    return this.currentUser;
	}
	 
	// getter()
	public Group getCurrentGroup() {
	    return currentGroup;
	}
	
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public PageController(PageManager pageManager, ConnectionManager connectionManager, 
			User currentUser, Group currentGroup) {
		if (connectionManager == null) {
            throw new IllegalArgumentException("ConnectionManager cannot be null");
        }
		this.pageManager = pageManager;
		this.connectionManager = connectionManager;
		this.currentUser = currentUser;
		this.currentGroup = currentGroup;
		commentManager = new CommentManager();
	}
	
	public List<Page> getAllPages_con() {
		List<Page> pageList = new ArrayList<>();
		try (Connection conn = connectionManager.getConnection()){
    		pageList = pageManager.searchPage(conn);
    	} catch(SQLException e) {
    		e.getStackTrace();
    		System.out.println("Can not borrow connection from connection pool.");
    		return null;
    	}
		return pageList;
	}
	
	public void showAllPages_con(List<Page> pageList) {
		int index = 1;
		
		//예시
		//UI에서 출력하는건 정하기 나름
		System.out.printf("\n                    페이지 목록\n");
    	System.out.println("---------------------------------------------------");
        for (Page page : pageList) {
            System.out.printf("%d |'%s'| latest revision id: %d\n\n",index++, page.pageName(), page.latestRevisionId());
        }
        System.out.println("---------------------------------------------------");
	}
	
	public boolean insertPage_con(Container pagePanel, String commitMessage) {  // void -> boolean으로 바꿈.
		FileIo fileIo = new FileIo(pageManager);
    	fileIo.fileUpLoad(); //파일탐색기에서 파일 가져옴
    	return fileIo.insertFileToPage(pagePanel, commitMessage); //가져온 파일 DB에 저장 (fileUpLoad 무조건 실행 후 실행)
	}
	
	public boolean deletePage_con(long pageId) {
		List<Comment> commentList = new ArrayList<>();
		commentList = getAllComments_con(pageId);
		
		if (currentUser.userId() != currentGroup.createdByUserId()) {
    		System.out.println("admin이 아닙니다.");
    		return false;
    	}
		else {
			pageManager.deletePage(pageId);
			for (Comment comment : commentList) { //페이지에 속한 comment 삭제
				deleteComment_con(comment.commentId());
			}
			return true;
		}
	}
	
	public List<Revision> getAllRevisions_con(long pageId){
		List<Revision> revisionList = new ArrayList<>();
		RevisionManager revision = new RevisionManager();
		
     	try (Connection conn = connectionManager.getConnection()){
     		revisionList = revision.getRevisionsByPageId(conn, pageId);
     	} catch (SQLException e) {
     		System.err.printf("%d 페이지 불러오기 실패\n", pageId);
     		e.printStackTrace();
     		return null;
     	}
     	return revisionList;
	}
	
	public List<Comment> getAllComments_con(long pageId){
		List<Comment> commentList = new ArrayList<>();
		
		try (Connection conn = connectionManager.getConnection()){
    		commentList = commentManager.getComments(conn, pageId);
    	} catch (SQLException e) {
    		e.getStackTrace();
    		System.out.println("Can not borrow connection from connection pool.");
    		return null;
    	}
		return commentList;
	}
	
	/**
	 * 현재 페이지에 댓글 적기
	 * @param pageId
	 * @param comment
	 */
	public void insertComment_con(long pageId, String comment) {
		try (Connection conn = connectionManager.getConnection()){
    		commentManager.insertComment(conn, pageId, currentUser.userId(), comment);
    	} catch (SQLException e) {
    		e.getStackTrace();
    		System.out.println("댓글을 올리지 못했습니다.");
    		System.out.println("Can not borrow connection from connection pool.");
    	}
	}
	
	/**
	 * 작성한 유저만 삭제할 수 있는 comment
	 * @param commentId
	 */
	public void deleteComment_con(long commentId) {
		try(Connection conn = connectionManager.getConnection()){
			commentManager.deleteComment(conn, commentId, currentUser.userId());
		}catch(SQLException e) {
			e.getStackTrace();
			System.out.println("댓글 삭제 실패.");
		}
	}
	
	/**
	 * 해당 페이지에 해당하는 모든 comment 삭제 (페이지나 그룹 삭제시 호출)
	 * @param pageId
	 */
	public void deleteAllComment_con(long pageId) {
		try(Connection conn = connectionManager.getConnection()){
			commentManager.deleteAllComments(conn, pageId);
		}catch(SQLException e) {
			e.getStackTrace();
			System.out.println("댓글 삭제 실패.");
		}
	}
	
	public void showAllComments_con(List<Comment> commentList) {
		Timestamp timestamp;
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	
    	//예시
    	//UI에서 출력하는건 정하기 나름
    	for (Comment comment : commentList) {
    		timestamp = comment.createdAt();
    		System.out.println("---------------------------------------------------");
    		System.out.printf("%s: %s |(%s)\n",comment.userName(), comment.commentData(), sdf.format(timestamp));
    		System.out.println("---------------------------------------------------");
    	}
	}
}
