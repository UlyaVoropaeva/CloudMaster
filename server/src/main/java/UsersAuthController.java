
import io.netty.channel.ChannelHandlerContext;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс для организации сервиса авторизации и связи с БД
 * Связь БД и приложения осуществляется через посредника, JDBC драйвер(библиотека).
 */
public class UsersAuthController {
    //инициируем объект класса
    private static UsersAuthController ownInstance = new UsersAuthController();
    private String secureHasher = "";

    public static UsersAuthController getOwnInstance() {
        return ownInstance;
    }

    //принимаем объект сервера
    private GMServer storageServer;

    //объявляем множество авторизованных клиентов <логин, соединение>
    private Map<String, ChannelHandlerContext> authorizedUsers;
    //объявляем объект соединения с БД
    Connection connection;
    //объявляем объект подготовленного запрос в БД
    private PreparedStatement preparedStatement;

    /**
     * Метод инициирует необходимые объекты и переменные
     *
     * @param storageServer - объект сервера
     */
    public void init(GMServer storageServer) {
        ownInstance.storageServer = storageServer;
        //инициируем множество авторизованных клиентов
        ownInstance.authorizedUsers = new HashMap<>();
        //инициируем объект соединения с БД
        connection = new MySQLConnect().connect();
    }

    /**
     * Метод-прокладка запускает процесс регистрации нового пользователя в БД
     *
     * @param authMessage - объект авторизационного сообщения
     * @return - результат операции регистрации в БД
     */
    public boolean registerUser(AuthMessage authMessage) {
        //если директория с таким логином уже есть в сетевом хранилище
        if (isUserRootDirExist(authMessage.getLogin())) {
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.registerUser() - " +
                    "директория с таким логином уже есть в сетевом хранилище");
            //и выходим с false
            return false;
        }
        //если пользователь с таким логином уже зарегистрирован в БД
        if (isUserRegistered(authMessage.getLogin())) {
            //выводим сообщение
            printMsg("[server]UsersAuthController.registerUser() - " +
                    " пользователь с таким логином уже зарегистрирован в БД");
            //и выходим с false
            return false;
        }
        //если регистрация нового пользователя в БД прошла не удачно
        if (!insertUserIntoDBSecurely(authMessage.getLogin(), authMessage.getFirst_name(),
                authMessage.getLast_name(), authMessage.getPassword())) {
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser() - " +
                    "регистрация нового пользователя в БД прошла не удачно");
            //и выходим с false
            return false;
        }
        //если создание конрневой директории для нового пользователяпрошла не удачно
        if (!storageServer.createNewUserRootFolder(authMessage.getLogin())) {
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser() - " +
                    "создание конрневой директории для нового пользователяпрошла не удачно");
            //и выходим с false
            return false;
        }
        return true;
    }

    /**
     * Метод обработки авторизации клиента в сетевом хранилище.
     *
     * @param ctx         - сетевое соединение
     * @param authMessage - объект авторизационного сообщения
     * @return true, если авторизация прошла успешно
     */
    public boolean authorizeUser(AuthMessage authMessage, ChannelHandlerContext ctx) {
        //если пользователь еще не зарегистрирован в БД
        if (!isUserRegistered(authMessage.getLogin())) {
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser() - " +
                    "пользователь еще не зарегистрирован в БД");
            //и выходим с false
            return false;
        }
        //если пользователь с таким логином уже авторизован
        if (isUserAuthorized(authMessage.getLogin(), ctx)) {
            //выводим сообщение в консоль
            printMsg("[server]UsersAuthController.authorizeUser - " +
                    "пользователь с таким логином уже авторизован");
            //и выходим с false
            return false;
        }
        //если пара логина и пароля релевантна
        if (checkLoginAndPasswordInDBSecurely(authMessage.getLogin(), authMessage.getPassword())) {
            //добавляем пользователя в список авторизованных
            authorizedUsers.put(authMessage.getLogin(), ctx);
            //возвращаем true, чтобы завершить процесс регистрации пользователя
            return true;
        }
        return false;
    }


    /**
     * Перегруженный метод удаляет клиента из списка авторизованных(по значению), если оно было авторизовано.
     *
     * @param ctx - значение - сетевое соединение клиента
     */
    public void deAuthorizeUser(ChannelHandlerContext ctx) {
        //в цикле ищем ключ со значение заданого логина
        for (Map.Entry<String, ChannelHandlerContext> keys : authorizedUsers.entrySet()) {
            if (keys.getValue().equals(ctx)) {
                //и удаляем его из списка
                authorizedUsers.remove(keys.getKey());
            }
        }
    }

    /**
     * Метод-прокладка запускает проверку есть ли уже корневая директория для заданного логина.
     *
     * @param login - логин нового пользователя
     * @return - результат проверки есть ли уже корневая директория для заданного логина
     */
    public boolean isUserRootDirExist(String login) {
        return storageServer.isUserRootDirExist(login);
    }

    /**
     * Метод проверяет не авторизован ли уже пользователь с таким логином и объектом соединения.
     *
     * @param login - логин пользователя
     * @param ctx   - сетевое соединение
     * @return - результат проверки
     */
    private boolean isUserAuthorized(String login, ChannelHandlerContext ctx) {
        //если есть элемент с таким логином в списке авторизованных пользователей
        if (authorizedUsers.containsKey(login)) {
            return true;
        }
        //проверяем все элементы списка по значениям(на всякий случай)
        for (Map.Entry<String, ChannelHandlerContext> user : authorizedUsers.entrySet()) {
            //если есть элемент в списке авторизованных с такими объектом соединения
            if (user.getValue().channel().equals(ctx.channel())) {
                return true;
            }
        }
        //возвращаем результат проверки  или логином
        return false;
    }

    /**
     * Метод проверяет введенный логин в БД на уникальность(не зарегистрирован ли уже такой логин?).
     *
     * @param login - проверяемый логин
     * @return - результат проверки
     */
    private boolean isUserRegistered(String login) {
        try {
            //инициируем объект запроса в БД
            Statement statement = connection.createStatement();
            // формирование запроса. '%s' - для последовательного подставления значений в соотвествующее место
            String sql = String.format("SELECT login FROM users WHERE login = '%s'", login);
            // оправка запроса и получение ответа из БД
            ResultSet rs = statement.executeQuery(sql);
            // если есть строка, то rs.next() возвращает true, если нет - false
            if (rs.next()) {
                //такой логин есть в БД
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Метод безопасно верифицирует заданные логин и пароль с данными пользователя в БД.
     *
     * @param login    - заданный логин пользователя
     * @param password - заданный пароль пользователя
     * @return - результат проверки данных в БД
     */
    private boolean checkLoginAndPasswordInDBSecurely(String login, String password) {
        try {
            //формируем строку для запроса PreparedStatement
            // ? - для последовательного подставления значений в соотвествующее место
            String sql = "SELECT * FROM users WHERE login = ?";
            //инициируем объект подготовленнного запроса
            preparedStatement = connection.prepareStatement(sql);
            //добавляем в запрос параметр 1 - строку логина
            preparedStatement.setString(1, login);
            //оправляем запрос и получяем ответ из БД
            ResultSet rs = preparedStatement.executeQuery();
            // если есть строка, то rs.next() возвращает true, если нет - false
            if (rs.next()) {

                //если заданный пароль совпадает с безопасным паролем в БД
                if (secureHasher.equals(password)) {
                    //пара логина и пароля релевантна
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Метод безопасно добавляет нового пользователя в БД.
     *
     * @param login      - логин пользователя
     * @param first_name - имя пользователя
     * @param last_name  - фамилия пользователя
     * @param password   - пароль пользователя
     * @return - результат добавляения новой строки в БД.
     */
    private boolean insertUserIntoDBSecurely(String login, String first_name, String last_name, String password) {
        try {

            // формируем строку для запроса PreparedStatement
            // ? - для последовательного подставления значений в соотвествующее место
            String sql = "INSERT INTO users (login, first_name, last_name, email, secure_hash, secure_salt) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            //инициируем объект подготовленнного запроса
            preparedStatement = connection.prepareStatement(sql);
            //добавляем в запрос параметр 1 - строку логина
            preparedStatement.setString(1, login);
            //добавляем в запрос параметр 2 - строку имени пользователя
            preparedStatement.setString(2, first_name);
            //добавляем в запрос параметр 3 - строку фамилии пользователя
            preparedStatement.setString(3, last_name);

            //добавляем в запрос параметр 4 -
            preparedStatement.setString(4, password);

            //оправляем запрос и получяем ответ из БД
            int rs = preparedStatement.executeUpdate();
            // если строка добавлена, то возвращается 1, если нет, то вернеться 0?
            if (rs != 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void printMsg(String msg) {
        storageServer.printMsg(msg);
    }

    public Map<String, ChannelHandlerContext> getAuthorizedUsers() {
        return authorizedUsers;
    }

}

