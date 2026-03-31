import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    //Регистрацията
    public boolean register(String username, String TextPassword, String role) throws Exception
    {
        String hashedPassword = Security.hashPassword(TextPassword);
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        //Заявката е това

        try(Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            statement.setString(3, role);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        }
        catch(Exception e)
        {
            throw new Exception("Грешка при регистрацията: " + e.getMessage());
            //Ако се случи тази грешка потребителят съществува. Иначе няма как
        }

    }

    public User login(String username, String TextPassoword) throws Exception
    {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";

        try(Connection connection = DatabaseManager.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if(resultSet.next())
            {
                String storedHash = resultSet.getString("password_hash");

                //проверямваме паролата с хеша в бд
                if(Security.checkPassword(TextPassoword, storedHash))
                {
                    //Връщаме нов обект USer без парола
                    return new User(resultSet.getInt("id"),
                            resultSet.getString("username"),
                            resultSet.getString("role")
                    );
                }
            }
            throw new Exception("Невалидно потребителско име или парола");//Ако този ред се задейства имаме грешно име или парола
        }
        catch(Exception e)
        {
            throw new Exception("Грешка при вход: " +  e.getMessage());
        }

    }

    public boolean changePassword(String username, String oldPassword, String newPassword) throws Exception
    {
        String selectSql = "SELECT password_hash FROM users WHERE username = ?";
        String updateSql = "UPDATE users SET password_hash = ? WHERE username = ?";

        try(Connection connection = DatabaseManager.getConnection())
        {
            //Намираме текущия хеш
            String currentHash = null;
            try(PreparedStatement statement = connection.prepareStatement(selectSql))
            {
                statement.setString(1, username);
                ResultSet resultSet = statement.executeQuery();
                if(resultSet.next())
                {
                    currentHash = resultSet.getString("password_hash");
                }
                else
                {
                    throw new Exception("Didnt find the user");
                }
            }

            //Проверка на съвпадане със старата парола
            if(!Security.checkPassword(oldPassword, currentHash))
            {
                throw new Exception("Wrong password!");
            }

            //Хеширане на новата парола и смяна
            String newHash = Security.hashPassword(newPassword);
            try(PreparedStatement updateStatement = connection.prepareStatement(updateSql))
            {
                updateStatement.setString(1, newHash);
                updateStatement.setString(2, username);
                int rowsUpdated = updateStatement.executeUpdate();
                return rowsUpdated > 0;
            }
        }catch(Exception e)
        {
            throw new Exception("Erron in changing password: " + e.getMessage());
        }

    }
}
