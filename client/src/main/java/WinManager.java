import com.sun.deploy.panel.JreFindDialog;
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

    public void init(Controller controller){
        this.controller = controller;
    }

    /**
     * Метод открывает модальное окно с регистрационной формой пользователя.
     */
    void openRegistrationForm() throws IOException {

        //выводим сообщение в нижнюю метку
        controller.showTextInController("Для регистрации нового клиента заполните все поля");

        try {
            Stage stage = new Stage();
            FXMLLoader loader = FXMLLoader.load(getClass().getResource("/registration_form.fxml"));
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
            stage.setScene(new Scene(root, 300, 300));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
