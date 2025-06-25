package teamproject;

import java.awt.Container;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.JOptionPane;

public class RevisionController {
   private final Page page;
   private final PageManager pageManager;
   private final ConnectionManager connectionManager;
   private FileIo fileIo;
   
   public RevisionController(Page page, PageManager pageManager, ConnectionManager connectionManager) {
      this.page = page;
      this.pageManager = pageManager;
      this.connectionManager = connectionManager;
      fileIo = new FileIo(pageManager);
   }
   
   public void showAllRevisions(List<Revision> revisionList) {
      //UI에서 원하는 형식으로 출력
      
      //아래는 예시
      int index = 1;
       RevisionManager revision = new RevisionManager();
       UserManager userManager = new UserManager(connectionManager);
       
       try (Connection conn = connectionManager.getConnection()){
           revisionList = revision.getRevisionsByPageId(conn, page.pageId());
        } catch (SQLException e) {
           System.err.printf("%d 페이지 불러오기 실패\n", page.pageId());
           e.printStackTrace();
        } 
       int commitNum = revisionList.size() - 1;
       Timestamp timestamp;
       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
       
       //기능분리
       //revisionList를 통해 모든 리비젼 출력 (수정된 횟수 포함)
       //showAllRevision(List<Revision> revisionList)
       System.out.printf("\nCommit Log | ");
       System.out.printf("%s | (%d번 수정됨) |\n", page.pageName(), commitNum);
       System.out.printf("---------------------------------------------------\n\n");

       for (Revision rev : revisionList) {
          timestamp = rev.createdAt();
          System.out.printf("%d| (rev ID: %d) | (parent rev ID: %d)\n", index++, rev.revisionId(), rev.parentRevisionId());
          System.out.printf( "Commit Message: %s | 작성자: %s |",  rev.commitMessage(), 
                          userManager.findUserNameByUserId(rev.committedByUserId()).username());   
          System.out.printf("(%s)\n\n", sdf.format(timestamp)); //업로드 날짜 표시
       }
       System.out.println("---------------------------------------------------");
   }
   
   /**
    * 리비젼 DB에 넣는 메서드
    * 
    * @param revisionList : parentId에 해당하는 revision 찾기 위함.
    * @param parentId : 지정 해줄지 안해줄지 UI 정하기 나름
    * @return parentId에 해당하는 revision 이 존재하면 true 아니면 false
    */
   public boolean insertRevision_con(Container con, List<Revision> revisionList, long parentId) {
      int isExist = 0;
      
      if (parentId == 0) { // 0 입력시 자동지정 (UI 정하기 나름)
         isExist = 1;
         parentId = pageManager.getLatestRevisionId(page.pageId());
         fileIo.fileUpLoad();
           fileIo.insertFileToRevision(con, page.pageId(), parentId);
      }
      else {
         for (Revision rev : revisionList) {
            if (rev.revisionId() == parentId) {
               isExist = 1;
                  fileIo.fileUpLoad();
                  fileIo.insertFileToRevision(con, page.pageId(), parentId);
                  break;
            }   
         }
      }
      if (isExist == 1)
         return true;
      else
         return false;
   }
   
   /**
    * parent id 제외
    */
   public void insertRevNoparent(Container con) {
      Long parentId = pageManager.getLatestRevisionId(page.pageId());
      fileIo.fileUpLoad();
       if (fileIo.insertFileToRevision(con, page.pageId(), parentId)) {
          JOptionPane.showMessageDialog(con, "리비젼 업로드 성공");
       }
       else {
          return;
       }
   }
   
   /**
    * DB에서 revision id 에 해당하는 파일데이터 불러와서
     * 사용자가 지정한 파일제목으로 지정한 경로에 파일 저장 (이때 확장자는 page 확장자로 강제됨)
    * @param revisionId 
    */
   public void downLoadRevision_con(Container con, long revisionId) {
      try (Connection conn = connectionManager.getConnection()){
         fileIo.fileDownLoad(conn, revisionId); //DB에서 파일내용 불러옴
      } catch (SQLException e) {
         JOptionPane.showMessageDialog(con, "파일 불러오기 실패");
         return;
      }
      fileIo.selectDownloadPath(page.pageName());//파일탐색기에서 정한 경로에 파일 저장
   }
   
   /**
    * DB에서 선택한 revision 에 대한 파일 내용을 String 으로 변환
    * @param revisionId
    * @return 읽어온 파일내용을 저장한 String 반환
    */
   public String showRevisionContent_con(long revisionId) {
      String content = null;
      try(Connection conn = connectionManager.getConnection()){ //파일용량이 커서 다운로드에 시간이 오래걸릴 경우
         fileIo.fileDownLoad(conn, revisionId);           //connection 누수 경고 뜰수 있음.
         content = fileIo.showFileContent();
      } catch (SQLException e) {
         System.out.println("파일 읽기 실패.");
         e.printStackTrace();
         return null;
      }
      if (content == null) {
         System.out.println("파일이 비어있습니다.");
         return null;
      }
      else {
         return content;
      }
   }
   Page getPage() {
      return page;
   }
}