package teamproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class GroupEditor extends JFrame{
	private static final long serialVersionUID = 1L;

	public GroupEditor(User currentUser, UserManager userManager, ConnectionManager connectionManager) {
		
		UserManager userManager_2 = new UserManager(connectionManager);
		
		getContentPane().removeAll();
		revalidate();
		repaint();
		
		Color background = new Color(220, 220, 220);  // 배경 : 회색
		
		GroupManager groupManager = new GroupManager(connectionManager, currentUser);
		
		List<Membership> membershipList = groupManager.searchGroup();
		
		setTitle("Sharing Cloud");
		
		setSize(400, 500);
		setLayout(new BorderLayout());  // 동, 서, 남, 북, 중앙 다섯으로 나눈 배치 방법
		
		JPanel jpGroup = new JPanel();  // 그룹들을 나열할 가장 큰 패널
		jpGroup.setBackground(background);
		jpGroup.setLayout(new BoxLayout(jpGroup, BoxLayout.Y_AXIS));  // 세로 정렬
		
		JScrollPane scrollPane = new JScrollPane(jpGroup);  // 스크롤 가능하게 설정
		add(scrollPane, BorderLayout.CENTER);
				
		//String groupName, joinKey;
		
		// 그룹 목록이라는 것을 알려주는 Label
		JLabel groupLabel = new JLabel("그룹↓");
		groupLabel.setFont(new Font("SansSerif", Font.BOLD, 16));  // 크고 굵게
		groupLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0));  // 여백 추가
		groupLabel.setOpaque(true);  // 배경 보이게 설정
		groupLabel.setBackground(background);
		add(groupLabel, BorderLayout.NORTH);
		
		// 기존의 가입한 그룹을 보여주는 패널
		for (Membership membership : membershipList) { 
			String groupName = groupManager.selectGroup(membership.groupId()).groupName();
			String joinKey = groupManager.getJoinKey(membership.groupId());
					
			addGroup(jpGroup, groupName, joinKey, connectionManager, currentUser, userManager);
		}
		
		jpGroup.revalidate();
		jpGroup.repaint();
		
		JPanel jpButton = new JPanel();  // 하단 버튼을 담을 패널 (+, 뒤로가기 등)
		jpButton.setLayout(new FlowLayout(FlowLayout.CENTER));  // 버튼들을 수평 중앙 정렬
		
		JButton addButton = new JButton("그룹 생성");
		JButton joinButton = new JButton("그룹 참여");
		JButton btnCancel = new JButton("로그아웃");
		
		changebtnColor(addButton);
		changebtnColor(joinButton);
		changebtnColor(btnCancel);
	
		jpButton.add(addButton);
		jpButton.add(joinButton);
		jpButton.add(btnCancel);
		
		add(jpButton, BorderLayout.SOUTH);
		
		// '그룹 생성' 버튼
		addButton.addActionListener(e -> {
			String groupName = JOptionPane.showInputDialog(this, "그룹 이름 : ");
			if(groupName == null) return;
			long createGroupId = groupManager.makeGroup(groupName);  // 그룹 ID
			
			if (groupName != null && !groupName.trim().isEmpty()) {
				if (createGroupId != -1) {
					String createJoinKey = groupManager.getJoinKey(createGroupId);  // 그룹 Key
					JOptionPane.showMessageDialog(this, "그룹 생성 성공!\n그룹 이름: " + groupName + "\nJoin Key: " + createJoinKey);
					
					addGroup(jpGroup, groupName, createJoinKey, connectionManager, currentUser, userManager);
					jpGroup.revalidate();
					jpGroup.repaint();
				}
			}
			else
			{
				JOptionPane.showMessageDialog(this, "그룹 이름을 다시 입력해주세요.");
			}
			
		});
		// '그룹 참여' 버튼
		joinButton.addActionListener(e -> {
			String joinKey = JOptionPane.showInputDialog(this, "참여할 그룹 키 : ");
			if(joinKey == null) return;
			
			try(Connection conn = connectionManager.getConnection()) { 
				int groupCheck = groupManager.joinGroup(conn, joinKey, currentUser.userId());
				if (groupCheck == -1 || joinKey.isEmpty()) {  // DB에서 그룹 참여
					JOptionPane.showMessageDialog(this, "잘못된 키입니다");
				}			
				else if (groupCheck == -2) {
					JOptionPane.showMessageDialog(this, "이미 참여 중인 그룹입니다");
				}
				else {
					String openGroupName = groupManager.getGroupNameByJoinKey(joinKey);	
					addGroup(jpGroup, openGroupName, joinKey, connectionManager, currentUser, userManager);
					jpGroup.revalidate();
					jpGroup.repaint();
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		});
		// '뒤로가기' 버튼 -> 로그아웃으로 변경
		btnCancel.addActionListener(e -> {
			int confirm = JOptionPane.showConfirmDialog(
			        this,
			        "로그아웃 하시겠습니까?",
			        "로그아웃 확인",
			        JOptionPane.YES_NO_OPTION
			    );

			    if (confirm == JOptionPane.YES_OPTION) {
			        dispose();
			        new LogIn(userManager_2, connectionManager);
			    }
		});
		
		setLocationRelativeTo(null);
		setVisible(true);
		
	}
	
	// 한 개의 그룹 패널을 만들어 추가하는 메서드
	public void addGroup(Container jpGroup, String groupName, String joinKey, ConnectionManager connectionManager,
			User currentUser, UserManager userManager)
	{
		GroupManager groupManager = new GroupManager(connectionManager, currentUser);
		String deleteGroupName = groupName;
		
		// (jpGroup > innerGroup > topPanel, joinKeyLabel)
		JPanel innerGroup = new JPanel();  // 새로 추가될 그룹 패널
		innerGroup.setLayout(new BoxLayout(innerGroup, BoxLayout.Y_AXIS));  // 수직 정렬
		innerGroup.setBorder(BorderFactory.createLineBorder(Color.GRAY));  // 외곽선으로 구분
		
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));  // innerGroup패널의 첫번째 행 패널 (그룹 열기, 삭제 버튼)
		JButton btnGroup = new JButton(groupName);  // '그룹 열기' 버튼
		JButton btnDelete = new JButton("X");  // '그룹 삭제' 버튼
		
		changebtnColor(btnGroup);
		changebtnColor(btnDelete);
		
		topPanel.add(btnGroup);
		topPanel.add(btnDelete);
		
		JLabel joinKeyLabel = new JLabel("join key: " + joinKey);
		joinKeyLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
		joinKeyLabel.setHorizontalAlignment(SwingConstants.CENTER);
		joinKeyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		innerGroup.add(topPanel);
		innerGroup.add(joinKeyLabel);
		
		innerGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));  // 크기(높이) 고정
		
		jpGroup.add(innerGroup);
		
		// '그룹 열기' 버튼
		btnGroup.addActionListener(e -> {
			long targetGroupId = groupManager.getGroupIdByJoinKey(joinKey);
			Group targetGroup = groupManager.selectGroup(targetGroupId);
			dispose();
			new PageEditor(currentUser, targetGroup, connectionManager, userManager); //PageEditor로 넘어감
		});
		// '그룹 삭제' 버튼
		
			long groupId = groupManager.getGroupIdByJoinKey(joinKey);
			Group delGroup = groupManager.selectGroup(groupId);
			PageManager pageManager = new PageManager(connectionManager, currentUser, delGroup);
			CommentManager delComment = new CommentManager();
			btnDelete.addActionListener(e -> {
			List<Page> delPageList = null; 
			if (groupManager.deleteGroup(groupId)) {  // 그룹을 DB에서 삭제
				try(Connection conn = connectionManager.getConnection()){
					delPageList = pageManager.searchPage(conn);
				} catch(SQLException ex1) {
					JOptionPane.showMessageDialog(this, "페이지 불러오기 실패");
				}
				if (delPageList != null) {
					for (Page delPage : delPageList) {
						pageManager.deletePage(delPage.pageId());
						try (Connection conn = connectionManager.getConnection()){
	        				delComment.deleteAllComments(conn, delPage.pageId());
	        			} catch(SQLException ex2) {
	        				JOptionPane.showMessageDialog(this, "댓글 삭제 실패");
	        			}
					}
				}
				JOptionPane.showMessageDialog(this, deleteGroupName + " 그룹이 삭제 되었습니다.");
				jpGroup.remove(innerGroup);  // 해당 그룹 패널 제거
				jpGroup.revalidate();
				jpGroup.repaint();
			}
			else {
				JOptionPane.showMessageDialog(this, "삭제 권한이 없습니다");
			}
		});
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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