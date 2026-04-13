import models.RoleRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RoleService {

    public void createRequest(int userId, String requestedRole) throws Exception
    {
        String sql = "INSERT INTO role_requests (user_id, requested_role) VALUES (?, ?)";
        try(Connection connection = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql))
        {
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, requestedRole);
            preparedStatement.execute();
        }

    }

    public List<RoleRequest> getPendingRequests() throws Exception
    {
        List<RoleRequest> requests = new ArrayList<>();
        String sql = "SELECT rr.id, u.username, u.role as current_role, rr.requested_role, rr.status, rr.created_at " +
                "FROM role_requests rr JOIN users u ON rr.user_id = u.id " +
                "WHERE rr.status = 'PENDING'";

        try(Connection connection = DatabaseManager.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery())
        {
            while (resultSet.next())
            {
                requests.add(new RoleRequest(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("current_role"),
                        resultSet.getString("requested_role"),
                        resultSet.getString("status"),
                        resultSet.getString("created_at")
                ));
            }
        }

        return requests;
    }

    public void handleRequest(int requestId, String newStatus) throws Exception
    {
        Connection connection = null;
        try{
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false); // започва транзакцията

            //Обновява статус на заявката
            String updateRequestSql = "UPDATE role_requests SET status = ? WHERE id = ?";
            try(PreparedStatement preparedStatement = connection.prepareStatement(updateRequestSql))
            {
                preparedStatement.setString(1, newStatus);
                preparedStatement.setInt(2, requestId);
                preparedStatement.execute();
            }

            if(newStatus.equals("APPROVED"))
            {
                String updateUserSql = "UPDATE users u JOIN role_requests rr ON u.id = rr.user_id " + "SET u.role = rr.requested_role WHERE rr.id = ?";
                try(PreparedStatement preparedStatement = connection.prepareStatement(updateUserSql))
                {
                    preparedStatement.setInt(1, requestId);
                    preparedStatement.execute();
                }
            }

            connection.commit();
        }catch(Exception e)
        {
            if (connection != null)
            {
                connection.rollback();
                throw e;
            }
        }finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }
}
