
public class AuthMessage extends AbstractMessage {

    //принимаем переменную логина пользователя
    private String login;
    //принимаем переменную имени пользователя
    private String first_name;
    //принимаем переменную фамилии пользователя
    private String last_name;
    //принимаем переменную пароля пользователя
    private String password;

    private byte[] data;

    public AuthMessage(byte[] data) {
        this.data = data;
    }
    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public AuthMessage() {
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    //конструктор для авторизации пользователя
    public AuthMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }

    //конструктор для регистрации нового пользователя
    public AuthMessage(String login, String first_name, String last_name, String password) {
        this.login = login;
        this.first_name = first_name;
        this.last_name = last_name;
        this.password = password;
    }


}

