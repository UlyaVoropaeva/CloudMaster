import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class WinManager {

    private static final WinManager ownInstance = new WinManager();


    public static WinManager getInstance() {
        return ownInstance;
    }

    private Controller controller;
    //объявляем переменную стадии приложения
    private Stage stage;
    //объявляем объект контроллера окна регистрации
    private RegistrationController registrationController;
    //объявляем объект контроллера окна авторизации
    private AuthorisationController authorisationController;

    public void init(Controller controller) {
        this.controller = controller;
    }


    /**
     * Перегруженный метод открывает модальное окно для ввода логина и пароля пользователя.
     */
    public void openAuthorisationWindow() {

        openAuthorisationWindow("", "");

    }

    /**
     * Перегруженный метод открывает модальное окно для ввода логина и пароля пользователя.
     * @param login - логин пользователя
     * @param password - пароль пользователя
     */
    public void openAuthorisationWindow(String login, String password) {
        //выводим сообщение в нижнюю метку GUI
        controller.showTextInController("Подключение к серверу. Введите логин/пароль");

        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("authorisation.fxml"));
            Parent root = loader.load();
            authorisationController = loader.getController();
            //сохраняем ссылку на контроллер открываемого окна авторизации/регистрацииa
            authorisationController.setBackController(controller);
            //устанавливаем значения из формы регистрации
            authorisationController.setLoginString(login);
            authorisationController.setPasswordString(password);

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                //запускаем процесс отправки запроса на отключение
                controller.getStorageClient().demandDisconnect();
            });

            stage.setTitle("Авторизация");
            stage.setScene(new Scene(root, 300, 200));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод открывает модальное окно с регистрационной формой пользователя.
     */
    public void openRegistrationForm() {

        //выводим сообщение в нижнюю метку
        controller.showTextInController("Для регистрации нового клиента заполните все поля.");

        try {
            Stage stage = new Stage();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("registration_form.fxml"));
            Parent root = loader.load();
            registrationController = loader.getController();
            //сохраняем ссылку на контроллер открываемого окна авторизации/регистрации
            registrationController.setBackController(controller);

            //определяем действия по событию закрыть окно по крестику через лямбда
            stage.setOnCloseRequest(event -> {
                //запускаем процесс отправки запроса на отключение
                controller.getStorageClient().demandDisconnect();
            });

            stage.setTitle("Регистрационная форма ");
            stage.setScene(new Scene(root, 250, 200));
            stage.isAlwaysOnTop();
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RegistrationController getRegistrationController() {
        return registrationController;
    }

    public AuthorisationController getAuthorisationController() {
        return authorisationController;

    }
}
