
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegistrationController {

    @FXML
    private VBox globParent;

    @FXML
    private TextField login, first_name, last_name;

    @FXML
    private PasswordField password;

    //принимаем объект главного контроллера
    private Controller backController;

    private final int MIN_LOGIN_LENGTH = 5; //минимальная длина логина
    private final int MIN_PASS_LENGTH = 8; //минимальная длина пароля

    /**
     * Метод отрабатывает клик кнопки на кнопку "Регистрация".
     * Открывает Авторизационную форму и запускает процесс отправки запроса на сервер
     * для регистрации в сетевом хранилище.
     * actionEvent - событие клик мыши
     */
    @FXML
    public void onRegistrationButtonClick(ActionEvent actionEvent) {
        //если введенные регистрационные данные корректны
        if (isRegistrationCorrect(login.getText(), password.getText())) {
            //выводим сообщение в метку оповещения
            backController.showTextInController("Регистрация нового клиент...");
            //запускаем процесс регистрации в сетевом хранилище
            backController.demandRegistration(login.getText(),
                    first_name.getText(), last_name.getText(), password.getText());
        }
    }

    private boolean isRegistrationCorrect(String login, String password) {
        if ((password.equals("111")) && (login.equals("111"))) {
            return true;
        } else
            backController.showTextInController("Некоректно введены регистрационные данные...111");
        return false;
    }


    /**
     * Метод отрабатывает клик линка "Authorization" в регистрационной форме.
     * Открывает Авторизационную форму.
     * actionEvent - событие клик мыши
     */
    @FXML
    public void onAuthorizationLinkClick(ActionEvent actionEvent) {
        //выводим сообщение в метку уведомлений
        backController.getNoticeLabel().setText("Введите пожалуйста логин и паспорт");
        //очищаем все поля формы авторизации/регистрации
        clearRegistrationForm();
    }


    /*
     * Метод очистки полей в регистрационной/авторизационной форме.
     */
    private void clearRegistrationForm() {
        login.setText("");
        first_name.setText("");
        last_name.setText("");
        password.setText("");
    }

    /*
     * Метод закрывает окно.
     */
    public void hideWindow() {
        //если окно показывается
        if (globParent.getScene().getWindow().isShowing()) {
            //закрываем окно
            globParent.getScene().getWindow().hide();
        }
    }

    public String getLoginString() {
        return login.getText();
    }

    public String getPasswordString() {
        return password.getText();
    }

    public void setBackController(Controller backController) {
        this.backController = backController;
    }

    private void showNoticeInGUI(String notice) {
        backController.getNoticeLabel().setText(notice);
    }


}
