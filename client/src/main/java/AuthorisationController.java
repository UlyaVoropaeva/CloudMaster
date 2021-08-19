import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AuthorisationController {

    @FXML
    private VBox globParent;

    @FXML
    private TextField login;

    @FXML
    private PasswordField password;

    //принимаем объект главного контроллера GUI
    private Controller backController;

    /**
     * Метод обрабатывает клик мыши по кнопке "Authorization" в диалоговом окне.
     * Запускает процесс отправки данных на сервер для автторизации.
     *
     * @param actionEvent - клик мыши по кнопке "Authorization"
     */
    @FXML
    public void onAuthorizationBtnClick(ActionEvent actionEvent) {
        //запускаем процесс авторизации
        backController.demandAuthorisation(login.getText(), password.getText());
        clearAuthorisationForm();
        hideWindow();
    }

    /**
     * Метод очистки всех полей авторизационной формы.
     */
    public void clearAuthorisationForm() {
        login.setText("");
        password.setText("");
    }

    /**
     * Метод закрывает окно.
     */
    public void hideWindow() {
        //если окно показывается
        if (globParent.getScene().getWindow().isShowing()) {
            //закрываем окно
            globParent.getScene().getWindow().hide();
        }
    }

    public void setBackController(Controller backController) {
        this.backController = backController;
    }

    public void setLoginString(String login) {
        this.login.setText(login);
    }

    public void setPasswordString(String password) {
        this.password.setText(password);
    }
}
