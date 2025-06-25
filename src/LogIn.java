package teamproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LogIn extends JFrame{
	private static final long serialVersionUID = 1L;
	User currentUser = null;
	public LogIn(UserManager userManager, ConnectionManager connectionManager) {
		
		Color background = new Color(220, 220, 220);  // 배경 : 회색
		
		//initial panel
		JPanel initialPanel = new JPanel();
	    initialPanel.setBounds(80, 90, 400, 250);
	    initialPanel.setBackground(background);
	    getContentPane().add(initialPanel, BorderLayout.CENTER);
	    initialPanel.setLayout(null);

		JLabel idJLabel = new JLabel("ID : ");
		JTextField txtID = new JTextField();
		JLabel pwJLabel = new JLabel("PW : ");
		JPasswordField txtPass = new JPasswordField();
		
		idJLabel.setBounds(10, 73, 30, 35);
		initialPanel.add(idJLabel);
		
		txtID.setBounds(50, 73, 350, 35);
	    initialPanel.add(txtID);
	    
	    pwJLabel.setBounds(10, 118, 30, 35);
	    initialPanel.add(pwJLabel);
		
		txtPass.setBounds(50, 118, 350, 35);
	    initialPanel.add(txtPass);

		JButton logButton = new JButton("로그인");
	    changebtnColor(logButton);
	    logButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	    logButton.setFocusPainted(false);
	    
	    logButton.setBounds(50, 163, 350, 35);
	    initialPanel.add(logButton);

		//register
		JButton btnRegister = new JButton("회원가입");
	    changebtnColor(btnRegister);
	    btnRegister.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	    btnRegister.setBounds(50, 208, 350, 35);
	    initialPanel.add(btnRegister);
	    
	    JLabel lblTitle = new JLabel("Sharing Cloud");
	    lblTitle.setFont(new Font("D2Coding", Font.PLAIN, 24));
	    lblTitle.setBounds(50, 0, 235, 63);
	    initialPanel.add(lblTitle);

		// '로그인' 버튼
		logButton.addActionListener(e -> {
			String id = txtID.getText();  // String으로 바꿔서 넣기
			String pw = new String(txtPass.getPassword());  // 비밀번호는 char[] 이므로 String으로 변환
					
			currentUser = userManager.login(id, pw);
					
			if (id.isEmpty() || pw.isEmpty()) {
				JOptionPane.showMessageDialog(this, "아이디와 비밀번호를 모두 입력해주세요.");
			}
			else if(currentUser != null) {
				JOptionPane.showMessageDialog(this, "로그인 성공! 사용자: " + id);
				dispose();
				new GroupEditor(currentUser, userManager, connectionManager); 
			}
			else {
				JOptionPane.showMessageDialog(this, "아이디 또는 비밀번호가 잘못되었습니다.");
				txtID.setText("");
                txtPass.setText("");
			}
		});
		
		btnRegister.addActionListener(e -> {
			new Register(currentUser, userManager);
		});
		
		
		setTitle("Log In / Register");
		getContentPane().setBackground(background);
	    setBounds(100, 100, 600, 490);
	    setLayout(new BorderLayout());

		setLocationRelativeTo(null);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	// 버튼 디자인 바꾸는 메서드
    public void changebtnColor(JButton button)
    {
    	button.setBackground(new Color(190, 180, 220));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("D2Coding", Font.PLAIN, 14));
        button.setBorder(BorderFactory.createLineBorder(Color.BLACK));
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