package teamproject;

import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileIo {
	private String pageName;
	private String filePath;
	private PageManager pageManager;
	private String commitMessage;
	private byte[] fileData;
	
	public FileIo(PageManager pagemanager) {
		this.pageManager = pagemanager;
		this.pageName = null;
		this.filePath = null;
		this.commitMessage = null;
		this.fileData = null;
	}
	/**
	 * JFileChooser로 컴퓨터에서 파일을 불러와 DB에 저장.
	 * 확장자는 소스파일 및 텍스트파일로 강제됨. (그 밖의 확장자는 표시되지 않음)
	 */
	public void fileUpLoad() {
		JFrame frame = new JFrame();
    	frame.setAlwaysOnTop(true);
    	JFileChooser chooser = new JFileChooser();
    	chooser.setDialogTitle("파일탐색기");
    	// filter에 java c 외 추가 소스코드 확장자 강제 가능
    	FileNameExtensionFilter filter = new FileNameExtensionFilter("소스코드 및 텍스트 파일 (*.txt, *.java, *.c)", "txt", "java", "c");
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false); // 필터 확장자 말고 선택할 수 없도록
        
        int result = chooser.showOpenDialog(frame);
    	
    	if (result == JFileChooser.APPROVE_OPTION) {
    		File file = chooser.getSelectedFile();
    		JOptionPane.showMessageDialog(null, "선택한 파일: " + file.getAbsolutePath());
    		
    		pageName = file.getName();
    		filePath = file.getPath();
    		
    	}
	}
	/**
	 * 리비젼 아이디에 해당하는 파일을 불러오는 메소드.
	 * @param conn
	 * @param revisionId
	 */
	public void fileDownLoad(Connection conn, long revisionId) {
		String sql = "SELECT content FROM file_data WHERE actual_data_id = ?";
		long actualDataId = revisionId; //DB 구조상 항상 actual_data_id랑 revision_id의 값은 같음
		
		try (PreparedStatement pstmt = conn.prepareStatement(sql)){
			
			pstmt.setLong(1, actualDataId);
			try (ResultSet rs = pstmt.executeQuery()){
				if (rs.next()) {
					fileData = rs.getBytes("content"); // !!!파일용량 큰 경우 connection 시간 지체되어 누수 경고메시지 뜰 수 있음
				}
			}
		}  catch (SQLException e) {
            System.err.println("파일을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
            return;
        }
		finally {
			if (fileData == null) {
				System.out.println("파일을 찾을 수 없습니다");
				return;
			}
		}
	}
	/**
	    * 컴퓨터에 파일을 저장할 위치를 골라 해당 위치에 파일을 저장
	    * 똑같은 JFileChooser 창을 띄우지만 저장할때 확장자가 페이지 확장자로 강제됨.
	    * @param pageName 해당 페이지의 확장자 알기위함
	    */
	   public void selectDownloadPath(String pageName) {
	      try {
	         String extenstion = getFileExtension(pageName);
	         if (extenstion == null) 
	            return;
	         
	         JFrame frame = new JFrame();
	         frame.setAlwaysOnTop(true);
	         JFileChooser chooser = new JFileChooser();
	         chooser.setDialogTitle("저장할 파일 위치 선택");
	         FileNameExtensionFilter filter = new FileNameExtensionFilter("*." + extenstion + " 파일", extenstion);
	         chooser.setFileFilter(filter);
	         chooser.setAcceptAllFileFilterUsed(false); // 필터 확장자 말고 선택할 수 없도록
	         
	         int result = chooser.showOpenDialog(frame);
	          
	          if (result == JFileChooser.APPROVE_OPTION) {
	             File savedFile = chooser.getSelectedFile();
	             String savedFilePath = savedFile.getAbsolutePath();
	             if (savedFilePath == null)
	                return;
	             JOptionPane.showMessageDialog(null, savedFilePath + "위치에 파일이 저장됩니다.");
	             if(!savedFilePath.endsWith("." + extenstion)) { // 확장자 없는경우 자동 추가
	                savedFilePath += "." + extenstion;
	             }
	             Files.write(Paths.get(savedFilePath), fileData);
	             System.out.println("파일을 성공적으로 저장했습니다!");
	          }
	      } catch(IOException e) {
	         e.printStackTrace();
	         System.out.println("파일 저장중 오류가 발생했습니다.");
	         return;
	      }
	      
	   }
	/**
	 * 반드시 fileUpLoad() 사용 후 사용.
	 * fileUpLoad에서 불러온 파일 내용을 file_data DB에 저장하고 pages DB 업데이트
	 */
	public boolean insertFileToPage(Container pagePanel, String commitMessage) {  // void -> boolean으로 바꿈.
    	if (filePath != null) {
    		System.out.println("파일 불러오기 성공!");
    		System.out.print("초기 커밋 메시지를 입력하세요: ");
    		commitMessage = JOptionPane.showInputDialog(pagePanel, "commitMessege를 입력하시오 : ");
              
            try {
                  // 파일 내용을 byte[]로 읽어오는 헬퍼 메소드 호출
                byte[] fileContent = readFileContent(filePath);

                if (fileContent != null) {
                      // makePage 메소드 호출 (groupManager가 아닌 pageManager 사용)
                      // makePage는 pageManager의 메소드입니다.
                    long createdPageId = pageManager.makePage(pageName, fileContent, commitMessage);

                    if (createdPageId != -1) { // makePage는 성공 시 page_id 반환
                        System.out.println("페이지 생성 성공! 페이지 ID: " + createdPageId);
                        return true;
                    } else {
                        System.out.println("페이지 생성 실패."); // makePage 내부에서 오류 메시지 출력됨
                        return false;
                    }
                } else {
                    System.out.println("파일을 읽어오는데 실패했습니다. 경로를 확인하세요.");
                    return false;
                }

            } catch (IOException e) {
                  // readFileContent에서 발생한 예외 처리
                System.err.println("파일 읽기 오류: " + e.getMessage());
                e.printStackTrace();
                System.out.println("페이지 생성 실패 (파일 읽기 오류).");
            }
    	}
    	else {
    		System.out.println("파일을 읽어오는데 실패했습니다.");
    		return false;
    	}
		return false;
	}
	
	 /**
     * 반드시 fileUpLoad() 사용 후 사용.
     * fileUpLoad에서 불러온 파일 내용을 file_data DB에 저장하고 file_revisions DB 업데이트
     * @param pageId 리비젼이 속한 페이지
     * @param parentId 수정 히스토리 표시
     */
    public boolean insertFileToRevision(Container file, long pageId, long parentId) {
       if (filePath != null) {
           /*System.out.println("파일 불러오기 성공!");
           System.out.print("커밋 메시지를 입력하세요: ");
           commitMessage = scanner.nextLine();
           */
          System.out.println("파일 불러오기 성공!");
            // Swing 팝업창으로 커밋 메시지 입력받기
            commitMessage = JOptionPane.showInputDialog(null, "커밋 메시지를 입력하세요:", "커밋 메시지 입력", JOptionPane.PLAIN_MESSAGE);

            // 사용자가 취소하거나 아무것도 입력하지 않으면 처리하지 않음
            if (commitMessage == null || commitMessage.trim().isEmpty()) {
                System.out.println("커밋 메시지가 비어 있어 Revision을 생성하지 않습니다.");
                return false;
            }
           
           try {
                // 파일 내용을 byte[]로 읽어오는 헬퍼 메소드 호출
                 byte[] fileContent = readFileContent(filePath);
                 
                 if (fileContent != null) {
                   pageManager.insertRevision(pageId, parentId, fileContent, commitMessage);
                   return true;
                 }
           } catch (IOException e) {
                // readFileContent에서 발생한 예외 처리
                 JOptionPane.showMessageDialog(file, "파일 읽기 오류: " + e.getMessage());
                 e.printStackTrace();
                // System.out.println("Revision 생성 실패 (파일 읽기 오류).");
                 return false;
           }
       }
       else {
           System.out.println("파일을 읽어오는데 실패했습니다.");
           return false;
       }
       return false;
    }
	
	
	/**
	 * 확장자를 추출하는 메서드
	 * pageName이 잘못되어 확장자 추출 실패할 경우
	 * @param pageName 페이지 이름에서 확장자를 추출
	 * @return 확장자 반환 ex) java, c, py, cpp ...
	 */
	private String getFileExtension(String pageName) {
		String extenstion = "";
		int tokens = pageName.lastIndexOf(".");
		if (tokens != -1 && tokens < pageName.length() - 1) {
			extenstion = pageName.substring(tokens + 1);
			return extenstion;
		}
		else {	
			System.out.println("확장자를 찾을 수 없습니다.");
			System.out.println("페이지 확장자를 확인해주세요.");
			return null;
		}
	}
	/**
	 * 반드시 fileDownload 실행 후 실행
	 * 소스코드 및 텍스트 파일 내용을 String 으로 변환 후 반환
	 */
	public String showFileContent() {
		if (fileData == null) {
			System.out.println("파일내용을 불러올 수 없습니다.");
			return null;
		}
		String showContent = new String(fileData, StandardCharsets.UTF_8);
		return showContent;
	}
	
	private static byte[] readFileContent(String filePath) throws IOException {
       // java.nio.file.Files를 사용하여 파일 내용을 쉽게 읽습니다.
       // Paths.get() 메소드로 문자열 경로를 Path 객체로 변환합니다.
       try {
           return Files.readAllBytes(Paths.get(filePath));
       } catch (IOException e) {
           // 파일이 없거나, 권한이 없거나, 읽기 오류 등 발생 시
           System.err.println("Error reading file: " + e.getMessage());
           e.printStackTrace();
           throw e; // 예외를 잡아서 메시지 출력 후 다시 던져서 호출부에서 처리하도록 함
           // 또는 단순히 null을 반환하고 호출부에서 null 체크
           // return null; // 이 경우 catch 블록에서 throw e; 대신 이 줄 사용
       }
    }
	
	public String getPageName() {
		return pageName;
	}
	public String getFilePath() {
		return filePath;
	}
	public String getCommitMessage() {
		return commitMessage;
	}
}