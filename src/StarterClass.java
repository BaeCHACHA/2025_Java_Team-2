package teamproject;


import java.sql.SQLException;

public class StarterClass {
	public static void main(String[] args) {
		ConnectionManager connectionManager = new ConnectionManager();
		try {
			connectionManager.connect();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("fail to connection.");
		}
		UserManager userManager = new UserManager(connectionManager);
        new LogIn(userManager, connectionManager);
        }
}