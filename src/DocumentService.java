import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import dto.DocumentDTO;
import dto.PendingVersionDTO;
import models.Document;
import models.DocumentVersion;

public class DocumentService {

    //Създава нов документ
    public int createDocument(User currentUser, String title, String initialContent) throws Exception {
        if(currentUser.getRole() != Role.AUTHOR && currentUser.getRole() != Role.ADMIN)
        {
            throw new Exception("You are not allowed to perform this action");
        }

        if(title == null || title.trim().isEmpty() || initialContent == null || initialContent.trim().isEmpty())
        {
            throw new Exception("Title or Content cannot be empty");
        }

        String insertDocSql = "INSERT INTO documents (title, created_by) VALUES (?, ?)";
        String insertVersionSql = "INSERT INTO document_versions (document_id, version_number, content, status, created_by) VALUES (?, 1, ?, 'PENDING_APPROVAL', ?)";
        String insertAuditSql = "INSERT INTO audit_logs (user_id, action, details) VALUES (?, 'CREATE_DOCUMENT', ?)";

        try(Connection connection = DatabaseManager.getConnection())
        {
            //Изключва автоматично запазване - за да всичко мине заедно
            connection.setAutoCommit(false);

            try{
                int generatedDocumentId = -1;

                //Създаваме документа. Generate keys - вземаме новото ид на документа
                try(PreparedStatement preparedStatement = connection.prepareStatement(insertDocSql, Statement.RETURN_GENERATED_KEYS))
                {
                    preparedStatement.setString(1, title);
                    preparedStatement.setInt(2, currentUser.getId());
                    preparedStatement.executeUpdate();

                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    if(rs.next())
                    {
                        generatedDocumentId = rs.getInt(1);
                    }else
                    {
                        throw new Exception("No Generated Keys returned");
                    }
                }

                //Създава версия 1
                try(PreparedStatement preparedStatement = connection.prepareStatement(insertVersionSql))
                {
                    preparedStatement.setInt(1, generatedDocumentId);
                    preparedStatement.setString(2, initialContent);
                    preparedStatement.setInt(3, currentUser.getId()); //Автор на версията
                    preparedStatement.executeUpdate();
                }

                //Записва в Audio Log
                try(PreparedStatement preparedStatement = connection.prepareStatement(insertAuditSql))
                {
                    preparedStatement.setInt(1, currentUser.getId());
                    preparedStatement.setString(2,  "New document with ID: " + generatedDocumentId + " Title: " + title);
                    preparedStatement.executeUpdate();
                }

                connection.commit();

                return generatedDocumentId;
            }catch(Exception e)
            {
                connection.rollback();
                throw new Exception("Error int the inserting: " + e.getMessage());
            }finally
            {
                connection.setAutoCommit(true);
            }
        }
    }

    //Създава нова версия
    public int createNewVersion(User currentUser, int documentId, String content) throws Exception {
        if(currentUser.getRole() != Role.AUTHOR && currentUser.getRole() != Role.ADMIN)
        {
            throw new Exception("You are not allowed to perform this action");
        }

        if(content == null || content.trim().isEmpty())
        {
            throw new Exception("Content cannot be empty");
        }

        String getMaxVersionSql = "SELECT MAX(version_number) FROM document_versions WHERE document_id = ?";
        String insertVersionSql = "INSERT INTO document_versions (document_id, version_number, content, status, created_by) VALUES (?, ?, ?, 'PENDING_APPROVAL', ?)";
        String insertAuditSql = "INSERT INTO audit_logs (user_id, action, details) VALUES (?, 'CREATE_VERSION', ?)";

        try(Connection connection = DatabaseManager.getConnection())
        {
            connection.setAutoCommit(false);

            try{
                //намира последната версия
                int nextVersionNumber = 1;
                try(PreparedStatement preparedStatement = connection.prepareStatement(getMaxVersionSql))
                {
                    preparedStatement.setInt(1, documentId);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if(resultSet.next() && resultSet.getInt(1) > 0)
                    {
                        nextVersionNumber = resultSet.getInt(1) + 1;
                    }else {
                        throw new Exception("No version number returned");
                    }
                }

                //Записва нова версия
                try(PreparedStatement preparedStatement = connection.prepareStatement(insertVersionSql)) {
                    preparedStatement.setInt(1, documentId);
                    preparedStatement.setInt(2, nextVersionNumber);
                    preparedStatement.setString(3, content);
                    preparedStatement.setInt(4, currentUser.getId());
                    preparedStatement.executeUpdate();
                }

                try(PreparedStatement preparedStatement = connection.prepareStatement(insertAuditSql))
                {
                    preparedStatement.setInt(1, currentUser.getId());
                    preparedStatement.setString(2,  "New version with ID: " + nextVersionNumber + " Document ID: " + documentId);
                    preparedStatement.executeUpdate();
                }

                connection.commit();
                return nextVersionNumber;
            }
            catch(Exception e)
            {
                connection.rollback(); //При грешка връща всичко назад
                throw new Exception("Error int the inserting: " + e.getMessage());
            }finally
            {
                connection.setAutoCommit(true);
            }
        }

    }

    public boolean reviewVersion(User currentUser, int documentId, int versionNumber, String newStatus) throws Exception
    {
        if(currentUser.getRole() != Role.REVIEWER && currentUser.getRole() != Role.ADMIN)
        {
            throw new Exception("You are not allowed to perform this action");
        }

        if(!newStatus.equals("APPROVED") && !newStatus.equals("REJECTED"))
        {
            throw new Exception("New status must be APPROVED or REJECTED");
        }

        String updateSql = "UPDATE document_versions SET status = ? WHERE document_id = ? AND version_number = ?";
        String auditSql = "INSERT INTO audit_logs (user_id, action, details) VALUES (?, ?, ?)";

        try(Connection connection = DatabaseManager.getConnection()) {
            try {
                connection.setAutoCommit(false);

                //обновява статуса на конкретна версия
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
                    preparedStatement.setString(1, newStatus);
                    preparedStatement.setInt(2, documentId);
                    preparedStatement.setInt(3, versionNumber);

                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new Exception("No rows affected");
                    }
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(auditSql)) {
                    preparedStatement.setInt(1, currentUser.getId());
                    preparedStatement.setString(2, "REVIEW_VERSION");
                    preparedStatement.setString(3, "Document Statis" + documentId + " Version Number: " + versionNumber + " changed to " + newStatus);
                    preparedStatement.executeUpdate();
                }

                connection.commit();
                return true;
            }catch(Exception e)
            {
                connection.rollback();
                throw new Exception("Error while updating version: " + e.getMessage());
            }finally
            {
                connection.setAutoCommit(true);
            }
        }
    }

    public Document getDocumentWithHistory(int documentId) throws Exception
    {
        String documentSql = "SELECT * FROM documents WHERE id = ?";
        // Взимаме версиите, подредени от най-новата към най-старата
        String versionsSql = "SELECT * FROM document_versions WHERE document_id = ? ORDER BY version_number DESC";

        Document document = null;

        try(Connection connection = DatabaseManager.getConnection())
        {
            //Взима основната информация на документа
            try(PreparedStatement preparedStatement = connection.prepareStatement(documentSql)){
                preparedStatement.setInt(1, documentId);
                ResultSet resultSet = preparedStatement.executeQuery();

                if(resultSet.next())
                {
                    document = new Document(
                            resultSet.getInt("id"),
                            resultSet.getString("title"),
                            resultSet.getInt("created_by"),
                            resultSet.getTimestamp("created_at")
                    );
                }else
                {
                    throw new Exception("No document with ID: " + documentId);
                }
            }

            List<DocumentVersion> history = new ArrayList<>();
            try(PreparedStatement preparedStatement = connection.prepareStatement(versionsSql)){
                preparedStatement.setInt(1, documentId);
                ResultSet resultSet = preparedStatement.executeQuery();

                while(resultSet.next())
                {
                    history.add(new DocumentVersion(
                            resultSet.getInt("id"),
                            resultSet.getInt("document_id"),
                            resultSet.getInt("version_number"),
                            resultSet.getString("content"),
                            resultSet.getString("status"),
                            resultSet.getInt("created_by"),
                            resultSet.getTimestamp("created_at")
                    ));
                }
            }

            document.setHistory(history);
            return document;
        }catch(Exception e)
        {
            throw new Exception("Error while retrieving document: " + e.getMessage());
        }
    }

    public List<DocumentDTO> getDocumentList(String authorFilter) throws  Exception
    {
        List<DocumentDTO> list = new ArrayList<>();

        //Ако има автор филтрираме по него, Ако няма само гледаме ACTIVE документа
        String sql = (authorFilter == null)
                ? "SELECT d.id, d.title, u.username, v.version_number, v.status " +
                "FROM documents d JOIN users u ON d.created_by = u.id " +
                "JOIN document_versions v ON d.id = v.document_id " +
                "WHERE v.status = 'ACTIVE'"
                : "SELECT d.id, d.title, u.username, v.version_number, v.status " +
                "FROM documents d JOIN users u ON d.created_by = u.id " +
                "JOIN document_versions v ON d.id = v.document_id " +
                "WHERE u.username = ?";

        try(Connection connection = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql))
        {
            if(authorFilter != null)
            {
                preparedStatement.setString(1, authorFilter);
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next())
            {
                list.add(new DocumentDTO(
                        resultSet.getInt("id"),
                        resultSet.getString("title"),
                        resultSet.getString("username"),
                        resultSet.getInt("version_number"),
                        resultSet.getString("status")
                ));
            }
        }

        return list;
    }

    public List<PendingVersionDTO> getPendingVersions() throws Exception
    {
        List<PendingVersionDTO> list = new ArrayList<>();
        // Търсим версии, които чакат одобрение
        String sql = "SELECT d.id AS doc_id, d.title, v.version_number, u.username, v.content, v.created_at " +
                "FROM document_versions v " +
                "JOIN documents d ON v.document_id = d.id " +
                "JOIN users u ON v.created_by = u.id " +
                "WHERE v.status = 'PENDING_APPROVAL'";

        try(Connection connection = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery())
        {
            while(resultSet.next())
            {
                list.add(new PendingVersionDTO(
                        resultSet.getInt("doc_id"),
                        resultSet.getString("title"),
                        resultSet.getInt("version_number"),
                        resultSet.getString("username"),
                        resultSet.getString("content"),
                        resultSet.getString("created_at")
                ));
            }
        }

        return list;
    }


    public void addComment(int documentId, int versionNumber, int userId, String comment) throws Exception {
        String sql = "INSERT INTO document_comments (document_id, version_number, user_id, comment) VALUES (?, ?, ?, ?)";

        try(Connection connection = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);)
        {
            preparedStatement.setInt(1, documentId);
            preparedStatement.setInt(2, versionNumber);
            preparedStatement.setInt(3, userId);
            preparedStatement.setString(4, comment);
            preparedStatement.execute();
        }
    }

}