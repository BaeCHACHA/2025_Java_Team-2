package teamproject;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FileEditor extends JFrame {
    
	private static final long serialVersionUID = 1L;
	private JPanel revisionListPanel;
    private JTextArea commentArea;
    private JTextField commentInput;
    private JButton sendButton;
    private JTextArea filePreviewArea;
    private PageController pageCon;
    private RevisionController revCon;
    private Page page;

    public FileEditor(List<Revision> revisions, PageController pageCon, RevisionController revCon,
          ConnectionManager connectionManager, Page page, User user, Group group) {
       this.pageCon = pageCon;
       this.revCon = revCon;
       this.page = page;
      
       	Color background = new Color(220, 220, 220);  // 배경 : 회색
       	
        setTitle("Page Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel(page.pageName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setOpaque(true);  // 배경 보이게 설정
		titleLabel.setBackground(background);
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        centerPanel.setBackground(background);
        add(centerPanel, BorderLayout.CENTER);

        // Revision + Comment Panel (좌측)
        JPanel midPanel = new JPanel(new BorderLayout());
        midPanel.setBackground(background);

        revisionListPanel = new JPanel();
        revisionListPanel.setLayout(new BoxLayout(revisionListPanel, BoxLayout.Y_AXIS));
        revisionListPanel.setBackground(background);

        updateRevDisplay(revisions);

        JScrollPane revScroll = new JScrollPane(revisionListPanel);
        revScroll.setBorder(BorderFactory.createTitledBorder("Revision List"));
        revScroll.setPreferredSize(new Dimension(500, 300));
        // revScroll.setBackground(background);
        revScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        midPanel.add(revScroll, BorderLayout.CENTER);

        List<Comment> commentList = pageCon.getAllComments_con(page.pageId());
        JPanel commentPanel = new JPanel(new BorderLayout());
        commentPanel.setBackground(background);
        commentArea = new JTextArea(5, 20);
        commentArea.setEditable(false);
        commentArea.setBorder(BorderFactory.createTitledBorder("댓글 Panel"));
        commentPanel.add(new JScrollPane(commentArea), BorderLayout.CENTER);
        updateCommentDisplay(commentList);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(background);
        commentInput = new JTextField();
        sendButton = new JButton("send");
        AtomicReference<List<Comment>> commentListRef = new AtomicReference<>(commentList);

        changebtnColor(sendButton);
        sendButton.addActionListener(e -> {
            String newComment = commentInput.getText().trim();
            if (!newComment.isEmpty()) {
                pageCon.insertComment_con(page.pageId(), newComment);
                commentListRef.set(pageCon.getAllComments_con(page.pageId()));
                updateCommentDisplay(commentListRef.get());
                commentInput.setText("");
            }
        });
        inputPanel.add(commentInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        commentPanel.add(inputPanel, BorderLayout.SOUTH);

        midPanel.add(commentPanel, BorderLayout.SOUTH);
        centerPanel.add(midPanel);

        // 파일 미리보기 Panel (우측)
        filePreviewArea = new JTextArea();
        filePreviewArea.setEditable(false);
        JScrollPane previewScroll = new JScrollPane(filePreviewArea);
        // previewScroll.setBackground(background);
        previewScroll.setBorder(BorderFactory.createTitledBorder("파일 미리보기"));
        centerPanel.add(previewScroll);

        //뒤로가기 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setPreferredSize(new Dimension(1000, 40));
        bottomPanel.setBackground(background);
        JButton backButton = new JButton("← 뒤로가기");
        changebtnColor(backButton);
        backButton.addActionListener(e -> {
           this.dispose();
           new PageEditor(user, group, connectionManager, null);
        });
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }
    private void updateRevDisplay(List<Revision> revisions) {
        revisionListPanel.removeAll();
        for (Revision rev : revisions) {
             JPanel revItemPanel = new JPanel(new BorderLayout());
             revItemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
             revItemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

             JLabel revLabel = new JLabel("rev ID: " + rev.revisionId() +" | " + rev.commitMessage());
             revLabel.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
             
             JPanel buttonPanel = new JPanel();
             buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

             JButton previewButton = new JButton();
             previewButton.setPreferredSize(new Dimension(30, 25));
             previewButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
             changebtnColor(previewButton);
             String viewContainer = revCon.showRevisionContent_con(rev.revisionId());
             previewButton.addActionListener(e -> filePreviewArea.setText("미리보기 내용: \n" + viewContainer ));

             JButton downloadButton = new JButton();
             downloadButton.setPreferredSize(new Dimension(30, 25));
             downloadButton.setIcon(UIManager.getIcon("FileChooser.upFolderIcon"));
             changebtnColor(downloadButton);
             downloadButton.addActionListener(e -> {
                revCon.downLoadRevision_con(this, rev.revisionId());
             });

             buttonPanel.add(previewButton);
             buttonPanel.add(downloadButton);

             revItemPanel.add(revLabel, BorderLayout.CENTER);
             revItemPanel.add(buttonPanel, BorderLayout.EAST);
             revisionListPanel.add(Box.createVerticalStrut(5));
             revisionListPanel.add(revItemPanel);
             
            revisionListPanel.revalidate();
            revisionListPanel.repaint();
         }
        //+ 버튼
        AtomicReference<List<Revision>> revListRef = new AtomicReference<>(revisions);
         
        JPanel plusButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        plusButtonPanel.setBackground(new Color(220, 220, 220));
        JButton addRevisionButton = new JButton("+");
        addRevisionButton.setPreferredSize(new Dimension(50, 30));
        changebtnColor(addRevisionButton);
        addRevisionButton.addActionListener(e -> {
           revCon.insertRevNoparent(this);
           revListRef.set(pageCon.getAllRevisions_con(page.pageId()));
           updateRevDisplay(revListRef.get());
        });
        plusButtonPanel.add(addRevisionButton);
        revisionListPanel.add(Box.createVerticalStrut(10));
        revisionListPanel.add(plusButtonPanel);
    }
    
    private void updateCommentDisplay(List<Comment> comments) {
       Timestamp timestamp;
       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
       
        commentArea.setText("");
        for (Comment comment : comments) {
           timestamp = comment.createdAt();
            commentArea.append( comment.userName() +  "  |  " + "(" + sdf.format(timestamp) + ")"+ "\n" + comment.commentData() + "\n\n");
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