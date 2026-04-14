import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import dto.*;
import models.*;

import java.util.List;
import java.util.Map;

public class MainServer {
    public static void main(String[] args) {

        RoleService roleService = new RoleService();
        AuthService authServices = new AuthService();
        DocumentService documentService = new DocumentService();

        //Инициализацция на Javalin сървър
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(7000);

        System.out.println("Server started at port 7000");

        //Енд поинт за Регистрация (POST /api/register)
        app.post("api/register", ctx -> {

            RegisterRequest registerRequest = ctx.bodyAsClass(RegisterRequest.class);

            try{
                boolean isRegistered = authServices.register(registerRequest.getUsername(),  registerRequest.getPassword(), registerRequest.getRole());
                //Връща статис 201 (Created) и JSON
                if(isRegistered) {
                    ctx.status(HttpStatus.CREATED).json("{\"message\": \"Successfully registered!\"}");
                }
            }catch(Exception e){
                ctx.status(HttpStatus.BAD_REQUEST).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        //Енд поинт за ВХОД (POST /api/login)
        app.post("api/login", ctx -> {
           LoginRequest loginRequest = ctx.bodyAsClass(LoginRequest.class);

           try{
               //Логва потребителя с базата данни
                User user = authServices.login(loginRequest.getUsername(), loginRequest.getPassword());

                //Генерира JWT токен
                String token = JwtUtils.generateToken(user);

                //Връща данните и токена
                ctx.json(new LoginResponse(token, user.getUsername(), user.getRole().name()));

           }catch(Exception e){
               ctx.status(HttpStatus.UNAUTHORIZED).json("{\"error\": \"" + e.getMessage() + "\"}");
           }
        });

        //Endpoint за  смяна на парола (PUT /api/change-password)
        app.put("/api/change-password", ctx ->{
            ChangePasswordRequest request = ctx.bodyAsClass(ChangePasswordRequest.class);

            try{
                boolean success = authServices.changePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
                if(success) {
                    ctx.status(HttpStatus.OK).json("{\"message\": \"Паролата е сменена успешно!\"}");
                }
            }catch(Exception e){
                ctx.status(HttpStatus.BAD_REQUEST).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        //Security Filter: ще се изпълни преди всяка заявка /api/*
        app.before("/api/*", ctx ->{
            String path = ctx.path();

            if(path.equals("/api/login") || path.equals("/api/register"))
            {
                return;
            }

            //Authorization хедър в заявката
            String authHeader = ctx.header("Authorization");
            if(authHeader == null || !authHeader.startsWith("Bearer "))
            {
                ctx.status(HttpStatus.UNAUTHORIZED).json("{\"error\": \"Липсва достъп! Моля, влезте в профила си.\"}");
                return;
            }

            String token = authHeader.substring(7);

            try{

                User currentUser = JwtUtils.validateTokenAndGetUser(token);

                //Запазваме текущия потребител в контекста на заявката - всеки ендпоинт има инф кой е викнал командата
                ctx.attribute("user", currentUser);
            } catch (Exception e) {
                ctx.status(HttpStatus.UNAUTHORIZED).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        //Ендпойнт за създаване на нов документ (POST /api/documents)
        app.post("/api/documents", ctx -> {

            //Взима потребителя
            User currentUser = ctx.attribute("user");

            //Взима данните
            CreateDocumentRequest request = ctx.bodyAsClass(CreateDocumentRequest.class);

            try{
                int newDocId = documentService.createDocument(currentUser, request.getTitle(), request.getContent());

                //Отговор + ИД на новата папка
                ctx.status(HttpStatus.CREATED).json("{\"message\": \"Документът е създаден успешно!\", \"documentId\": " + newDocId + "}");
            }catch(Exception e){
                ctx.status(HttpStatus.FORBIDDEN).json("{\"error\": \"" + e.getMessage() + "\"}");
            }

        });

        app.post("/api/documents/{id}/versions", ctx ->{
            //Това позволява да взема ИД-то директно от УРЛ адреса
            int documentId = Integer.parseInt(ctx.pathParam("id"));

            CreateVersionRequest request = ctx.bodyAsClass(CreateVersionRequest.class);

            User currentUser = ctx.attribute("user");

            try{
                int newVersionNum = documentService.createNewVersion(currentUser, documentId, request.getContent());

                ctx.status(HttpStatus.CREATED).json("{\"message\": \"Успешно създадена версия " + newVersionNum + " и изпратена за одобрение!\"}");
            } catch(Exception e)
            {
                ctx.status(HttpStatus.BAD_REQUEST).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        //Ендпоинт одобряване (PUT /api/documents/{id}/versions/{vNum}/review)
        app.put("/api/documents/{id}/versions/{vNum}/review", ctx ->{

            int documentId = Integer.parseInt(ctx.pathParam("id"));
            int versionNum = Integer.parseInt(ctx.pathParam("vNum"));

            ReviewVersionRequest request = ctx.bodyAsClass(ReviewVersionRequest.class);

            User currentUser = ctx.attribute("user");

            try{
                documentService.reviewVersion(currentUser, documentId, versionNum, request.getStatus());

                ctx.status(HttpStatus.OK).json("{\"message\": \"Статусът е променен успешно на " + request.getStatus() + "\"}");
            }catch(Exception e){
                ctx.status(HttpStatus.BAD_REQUEST).json("{\"error\": \"" + e.getMessage() + "\"}");
            }

        });

        app.get("/api/documents/pending", ctx ->{
           try{
               List<PendingVersionDTO> pending = documentService.getPendingVersions();
               ctx.status(HttpStatus.OK).json(pending);
           }catch (Exception e)
           {
               ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("{\"error\": \"" + e.getMessage() + "\"}");
           }
        });

        app.get("/api/documents/{id}", ctx ->{
            int documentId = Integer.parseInt(ctx.pathParam("id"));

            try{
                //Взима пълния обект
                Document doc = documentService.getDocumentWithHistory(documentId);

                //Javalin и Jackson автоматично превръщат Java обект в форматиран JSON
                ctx.status(HttpStatus.OK).json(doc);

            }catch(Exception e){
                ctx.status(HttpStatus.NOT_FOUND).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        //Взема всички активни документи - за всички роли
        app.get("/api/documents", ctx ->{
            try{
                //извикваме метода без филтър за автор за да вземем всички ACTIVE
                List<DocumentDTO> documents = documentService.getDocumentList(null);
                ctx.status(HttpStatus.OK).json(documents);
            }catch(Exception e){
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });


        app.get("/api/my-documents",  ctx ->{
            try{

                User currentUser = ctx.attribute("user");

                if (currentUser == null) {
                    ctx.status(HttpStatus.UNAUTHORIZED).json("{\"error\": \"Не сте логнати!\"}");
                    return;
                }

                List<DocumentDTO> myDocuments = documentService.getDocumentList(currentUser.getUsername());
                ctx.status(HttpStatus.OK).json(myDocuments);
            }catch(Exception e){
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });


        //Потребителя взима новя роля
        app.post("/api/role-request", ctx ->{
           try{
               User user = ctx.attribute("user");
               Map<String,String> body = ctx.bodyAsClass(Map.class);

               roleService.createRequest(user.getId(),  body.get("requestedRole"));

               ctx.status(HttpStatus.CREATED).json("{\"message\": \"Role request submitted\"}");
           }catch(Exception e){
               ctx.status(HttpStatus.BAD_REQUEST).json("{\"error\": \"" + e.getMessage() + "\"}");
           }
        });

        //Админ вижда заявките
        app.get("/api/role-requests", ctx ->{
            try {
                List<RoleRequest> pendingRequests = roleService.getPendingRequests();

                ctx.status(HttpStatus.OK).json(pendingRequests);
            }catch(Exception e){
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        //Админ одобрява или отхвърля
        app.put("/api/role-requests/{id}", ctx ->{
            int requestId = Integer.parseInt(ctx.pathParam("id"));
            try{
                Map<String, String> body = ctx.bodyAsClass(Map.class);

                roleService.handleRequest(requestId, body.get("status"));

                ctx.status(HttpStatus.OK).json("{\"message\": \"Request handled successfully\"}");
            }catch(Exception e){
                ctx.status(HttpStatus.BAD_REQUEST).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        //Добавя комента на документа
        app.post("/api/documents/{id}/comments", ctx ->{
            int documentId = Integer.parseInt(ctx.pathParam("id"));
            try{
                User user = ctx.attribute("user");

                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int versionNumber = Integer.parseInt(body.get("versionNumber").toString());
                String commentText = body.get("comment").toString();

                documentService.addComment(documentId, versionNumber, user.getId(), commentText);

                ctx.status(HttpStatus.CREATED).json("{\"message\": \"Коментарът е добавен успешно\"}");
            }catch(Exception e){
                ctx.status(HttpStatus.BAD_REQUEST).json("{\"error\": \"" + e.getMessage() + "\"}");
            }
        });
    }

}
