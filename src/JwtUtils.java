import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

public class JwtUtils {

    //Алгоритъм за криптиране (HMAC256)
    // Взимаме ключа от компютъра. Ако не го намери (защото забравиш да го настроиш),
    // ползваме резервен, за да не гърми програмата докато разработваш.
    private static final String SECRET_KEY = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "my_super_secret_sap_project_key_fallback";

    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);;

    private static final long EXPIRATION_TIME = 72000000;

    //Генериране на токена - трябва да се извика след вход
    public static String generateToken(User user)
    {
        return JWT.create()
                .withIssuer("SapVersionApp") //Кой издава ключа
                .withSubject(String.valueOf(user.getId())) //ID на потребителя
                .withClaim("username", user.getUsername()) //Допълнителни данни
                .withClaim("role", user.getRole().name()) // Роля
                .withIssuedAt(new Date()) // Кога е издаден
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) //Кога изтича
                .sign(algorithm); //подписване с тайния ключ
    }

    //Валидиране и прочитане на токен - извиква се след всяка заявка
    public static User validateTokenAndGetUser(String token) throws Exception
    {
        try{
            //Верификатор който проверява подписа и дали не е изтекъл
            JWTVerifier verifier = JWT.require(algorithm).withIssuer("SapVersionApp").build();

            //Aко токенът е фълши това дава Ексептион
            DecodedJWT decodedJWT = verifier.verify(token);

            int id = Integer.parseInt(decodedJWT.getSubject());
            String username = decodedJWT.getClaim("username").asString();
            String role = decodedJWT.getClaim("role").asString();

            return new User(id, username, role);
        }catch(Exception e){
            throw new Exception("Invalid token. Try again!");
        }
    }

}
