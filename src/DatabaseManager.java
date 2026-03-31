import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/project";
    private static final String USER = "root";
    private static final String PASSWORD = "0546141972Aa";

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    //Creating new version
    public static void createVersion(int documentId, String content, String status) throws Exception {
        String sql = "INSERT INTO versions (document_id, content, status, created_at) VALUES (?, ?, ?, NOW())";

        try(Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, documentId);
            statement.setString(2, content);
            statement.setString(3, status);

            statement.executeUpdate();
        }catch(SQLException e)
        {
            throw new Exception("Coundln't write in the datebase " + e.getMessage());
        }


    }


}
