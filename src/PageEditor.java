package teamproject;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PageEditor extends JFrame {
	private static final long serialVersionUID = 1L;

	public PageEditor(User currentUser, Group currentGroup, ConnectionManager connectionManager, UserManager userManager) {
		try {
			PageManager pageManager = new PageManager(connectionManager, currentUser, currentGroup);
			PageController pageController = new PageController(pageManager, connectionManager, currentUser, currentGroup);
			RevisionManager revisionManager = new RevisionManager();
			
			Color background = new Color(220, 220, 220);  // 배경 : 회색
			
			// 1. 전체 프레임 (전체)
			setTitle(currentGroup.groupName());
			setSize(400, 500);
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setLayout(new BorderLayout());  // 동, 서, 남, 북, 중앙 다섯으로 나눈 배치 방법

			// 2. 제목 및 안내 라벨 (상단)
			JLabel pageLabel = new JLabel("'" + currentGroup.groupName() + "'" + " 그룹의 페이지↓");
			pageLabel.setFont(new Font("SansSerif", Font.BOLD, 16));  // 크고 굵게
			pageLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0));  // 여백 추가
			pageLabel.setOpaque(true);  // 배경 보이게 설정
			pageLabel.setBackground(background);
			add(pageLabel, BorderLayout.NORTH);
			
			// 3. 페이지 목록 패널 (중앙)
			JPanel pagePanel = new JPanel();  // 페이지들을 나열할 패널
			pagePanel.setLayout(new BoxLayout(pagePanel, BoxLayout.Y_AXIS));  // 세로 정렬
			pagePanel.setBackground(background);
			
			// 내부 패널이 너무 작으면 스크롤이 생기지 않음
			// 스크롤이 제대로 작동하도록 preferredSize 제한 X
			pagePanel.setAlignmentX(Component.TOP_ALIGNMENT);
			pagePanel.setAlignmentY(Component.LEFT_ALIGNMENT);

			JScrollPane scrollPane = new JScrollPane(pagePanel);  // 스크롤 가능하게 설정
			scrollPane.getVerticalScrollBar().setUnitIncrement(10);  // 스크롤 속도 향상
			scrollPane.setPreferredSize(new Dimension(400, 400));  // 크기 제한
			add(scrollPane, BorderLayout.CENTER);

			// 3-1. 기존의 페이지들 추가
			List<Page> pageList = new ArrayList<>();
			try {
				pageList = pageController.getAllPages_con();  // 모든 페이지 DB로부터 읽어오기
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "페이지 목록을 불러오는 중 오류가 발생했습니다:\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
			}

			for (int i = 0; i < pageList.size(); i++) {
				Page page = pageList.get(i);  // 해당 페이지 불러오기
				List<Revision> revisions = pageController.getAllRevisions_con(page.pageId());  // 해당 페이지의 모든 Revision 불러오기
				Revision firstRevision = revisions.get(0);  // 해당 페이지의 첫 번째 Revision 불러오기
				
				String commitMessage = firstRevision.commitMessage();  // 커밋 메시지 읽어오기
				
				addPage(pagePanel, pageController, currentUser, currentGroup, i + 1, page, pageManager, connectionManager, commitMessage);
			}

			// 4. 버튼 패널 (하단)
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			add(buttonPanel, BorderLayout.SOUTH);

			// 4-1. '페이지 추가' 버튼
			JButton btnAddPage = new JButton("페이지 추가");
			btnAddPage.addActionListener(e -> {  // 기능
				
				try {
					String commitContainer = null;  // 커밋 메시지 (처음에 설명 넣는 주석 같은 것)
					if (pageController.insertPage_con(pagePanel, commitContainer)) {  // 페이지 추가하기
						List<Page> pageList_2 = pageController.getAllPages_con();  // 모든 페이지 읽어오기
						Page lastPage = pageList_2.get(pageList_2.size() - 1);  // 추가한 페이지 읽어오기
						String commitMessage = null;
						
						// 커넥션을 안전하게 닫기 위한 try-with-resources 사용
						try (Connection conn = connectionManager.getConnection()) {
							Revision lastRev = revisionManager.getRevision(conn, lastPage.latestRevisionId());  // 추가한 페이지의 Revision 읽어오기
							commitMessage = lastRev.commitMessage();  // 커밋 메시지 읽어오기
						}
						catch (SQLException ex) {
							JOptionPane.showMessageDialog(this, "connection error.");
							ex.printStackTrace();
						}
						int newIndex = pageList_2.size();
						addPage(pagePanel, pageController, currentUser, currentGroup, newIndex, lastPage, pageManager, connectionManager, commitMessage);					
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "페이지 추가 중 오류 발생:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			});
			
			changebtnColor(btnAddPage);
			buttonPanel.add(btnAddPage);

			// 4-2. '뒤로가기' 버튼
			JButton btnBack = new JButton("뒤로가기");
			btnBack.addActionListener(e -> {  // 기능
				try {
					dispose();
					new GroupEditor(currentUser, userManager, connectionManager);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "뒤로가기 처리 중 오류 발생:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
				}
			});
			
			changebtnColor(btnBack);
			buttonPanel.add(btnBack);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "초기화 중 오류 발생:\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		setLocationRelativeTo(null);
		setVisible(true);
	}

	// 한 개의 페이지 패널을 만들어 추가하는 메서드
	public void addPage(Container pagePanel, PageController pageController, User currentUser, Group currentGroup,
						int index, Page page, PageManager pageManager, ConnectionManager connectionManager, String commitMessage) {
		try {
			// (pagePanel > innerPage > topPanel(btnOpenPage, btnDeletePage), commitMessageLabel)
			JPanel innerPage = new JPanel();
			innerPage.setBorder(BorderFactory.createLineBorder(Color.gray));  // 외곽선으로 구분
			innerPage.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));  // 크기(높이) 고정
			innerPage.setLayout(new BoxLayout(innerPage, BoxLayout.Y_AXIS));  // 수직 정렬
			innerPage.setPreferredSize(new Dimension(380, 70));
			
			JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));  // innerPage 안의 페이지 참여, 삭제 버튼을 포함한 패널
			
			JButton btnOpenPage = new JButton(index + ". " + page.pageName());  // '페이지 참여' 버튼

			JButton btnDeletePage = new JButton("X");  // '페이지 삭제' 버튼
			
			JLabel commitMessageLabel = new JLabel("commitMessage : " + commitMessage);  // 'commitMessage' 라벨
			commitMessageLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
			commitMessageLabel.setHorizontalAlignment(SwingConstants.CENTER);  // commitMessage를 중앙에 위치하게 함
			commitMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);  // BoxLayout 수직
			
			RevisionController revisionController = new RevisionController(page, pageManager, connectionManager);
			List<Revision> revisions = pageController.getAllRevisions_con(page.pageId());  // 해당 페이지의 모든 Revision 불러오기
			
			// '페이지 참여' 버튼
			btnOpenPage.addActionListener(e -> {
				try {
					dispose();
					new FileEditor(revisions, pageController, revisionController, connectionManager, page, currentUser, currentGroup);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "페이지 열기 중 오류 발생:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
				}
			});

			// '페이지 삭제' 버튼
			btnDeletePage.addActionListener(e -> {
				try {
					if (currentUser.userId() != currentGroup.createdByUserId()) {  // 그룹 어드민 Id와 User Id가 일치하지 않으면 삭제 못하게 함.
						JOptionPane.showMessageDialog(this, "삭제 권한이 없습니다.");
					} else {
						pageController.deletePage_con(page.pageId());  // 페이지 삭제
						JOptionPane.showMessageDialog(this, page.pageName() + " 삭제되었습니다.");
						repaintPageList(pagePanel, page, pageController, currentUser, currentGroup, pageManager, connectionManager);  // 페이지 순서를 정렬하기 위해 repaint메서드 사용
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, "페이지 삭제 중 오류 발생:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
				}
			});
			
			changebtnColor(btnOpenPage);
			changebtnColor(btnDeletePage);
			
			topPanel.add(btnOpenPage);
			topPanel.add(btnDeletePage);
			
			// pagePanel.add(Box.createVerticalStrut(5));  // 항목 간격 주기
			innerPage.add(topPanel);  // innerPage <- topPanel
			innerPage.add(commitMessageLabel);  // innerPage <- commitMessageLabel
			
			pagePanel.add(innerPage);  // 마지막에 pagePanel에 innerPage 넣어주기
			pagePanel.revalidate();
			pagePanel.repaint();
			
		} catch (Exception e) {
			JOptionPane.showMessageDialog(pagePanel, "페이지 생성 중 오류 발생:\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
		}
	}

	// 각 페이지의 순서를 정렬하기 위해 다 지우고 다시 페이지 패널들을 추가하는 메서드
	public void repaintPageList(Container pagePanel, Page page, PageController pageController, User currentUser, Group currentGroup, PageManager pageManager, ConnectionManager connectionManager) {
		try {
			pagePanel.removeAll();  // 전의 패널들 다 삭제
			pagePanel.revalidate();  // 마지막 한 페이지가 남았을 때는 addPage가 한번도 실행되지 않으므로 revalidate, repaint를 한번 해줌.
			pagePanel.repaint();
			List<Page> pages = pageController.getAllPages_con();
			
			for (int i = 0; i < pages.size(); i++) {
				Page rePage = pages.get(i);  // 해당 페이지 불러오기
				List<Revision> revisions = pageController.getAllRevisions_con(rePage.pageId());  // 해당 페이지의 모든 Revision 불러오기
				Revision firstRevision = revisions.get(0);  // 해당 페이지의 첫 번째 Revision 불러오기
				
				String commitMessage = firstRevision.commitMessage();  // 커밋 메시지 읽어오기
				
				addPage(pagePanel, pageController, currentUser, currentGroup, i + 1, rePage, pageManager, connectionManager, commitMessage);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "페이지 목록 갱신 중 오류 발생:\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	// 버튼 디자인 바꾸는 메서드
    public void changebtnColor(JButton button)
    {
    	button.setBackground(new Color(190, 180, 220));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("D2Coding", Font.PLAIN, 14));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
       	 public void mouseEntered(java.awt.event.MouseEvent evt) {  // 마우스 갖다 댈 시
       		 button.setBackground(new Color(124, 58, 237));  // 더 진한 보라
       	 }
       	 public void mouseExited(java.awt.event.MouseEvent evt) {  // 마우스 뗄 시
       		 button.setBackground(new Color(190, 180, 220));  // 원래 색으로
       	 }
        });
    }	
}