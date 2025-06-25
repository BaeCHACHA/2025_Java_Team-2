package teamproject;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Register extends JFrame{
	private static final long serialVersionUID = 1L;

	public Register(User currentUser, UserManager userManager) {
		
		Color background = new Color(220, 220, 220);  // 배경 : 회색
		
		setTitle("회원가입");
			
		JPanel panel = new JPanel(new GridLayout(3, 3, 10, 10));
		panel.setBorder(new EmptyBorder(20, 20, 20, 20));
		panel.setBackground(background);
		JLabel idJLabel = new JLabel("ID : ");
		JTextField txtID = new JTextField();
		JLabel pwJLabel = new JLabel("PW : ");
		JPasswordField txtPass = new JPasswordField();
		
		panel.add(idJLabel);
		panel.add(txtID);
		panel.add(pwJLabel);
		panel.add(txtPass);
		
		JButton btnRegister = new JButton("가입하기");
		changebtnColor(btnRegister);
		panel.add(btnRegister);
		
		JButton btnCancel = new JButton("취소");
		changebtnColor(btnCancel);
		panel.add(btnCancel);
		
		// '가입하기' 버튼
		btnRegister.addActionListener(e -> {
			String id = txtID.getText().trim();  // trim을 이용하여 앞뒤 공백을 제거하여 문자열을 가져옴
			String pw = new String(txtPass.getPassword());
			
			long stateUser = userManager.addUser(id, pw);
			
			if (id.isEmpty() || pw.isEmpty()) {
				JOptionPane.showMessageDialog(this, "모든 항목을 입력해주세요.");
			}
			else if (stateUser == -2) {  // 중복일 경우
				JOptionPane.showMessageDialog(this, "중복된 아이디입니다."); // 중복된 아이디 넣어봐도 기타 오류로 취급->UserManager 수정
				txtID.setText("");
                txtPass.setText("");
			}
			else if(stateUser == -1) {  // 다른 오류 발생 시
				JOptionPane.showMessageDialog(this, "오류 발생");
				txtID.setText("");
                txtPass.setText("");
			}
			else {
				JOptionPane.showMessageDialog(this, "회원가입 성공!");
				dispose();  // 로그인 창으로 돌아가기(창닫기)
			}
		});
		// '취소' 버튼
		btnCancel.addActionListener(e -> {
			dispose(); // 로그인 창으로 돌아가기(창닫기)
		});
		
		add(panel);
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
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
