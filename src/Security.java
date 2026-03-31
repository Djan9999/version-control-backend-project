import org.mindrot.jbcrypt.BCrypt; //BCRypt позволява криптиране еднопосочно
//помага и против колизия. Ако 2 клиента са с един и същи стринг хашовете са различни

public class Security {

    //хеширане при регистрация
    public static String hashPassword(String TextPassword) {
        return BCrypt.hashpw(TextPassword, BCrypt.gensalt(12)); //12 - фактор на сложност
    }

    //проверка на паролата при вход
    public static boolean checkPassword(String TextPassword, String hashedPassword) {
        return BCrypt.checkpw(TextPassword, hashedPassword);
    }
}
