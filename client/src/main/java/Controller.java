import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;



public class Controller {
    //Объявляем объекты верхнего меню
    @FXML
    HBox connectPanel;

    // Объявляем объекты  панели leftPanel - локальная панель (пользователя)
    //rightPanel -  панель сетевого хранилища
    @FXML
    VBox leftPanel, rightPanel;

    //объявляем объект метки уведомлений
    @FXML
    private Label noticeLabel;



    //объявляем объект менеджера окон
    private WinManager winManager;

    //объявляем объект контроллера клиента облачного хранилища
    private GMClient storageClient = new GMClient(Controller.this);




    @FXML
    private void onConnectButtonClick() throws Exception {
        //выводим текст в метку
        noticeLabel.setText("Происходит соединение с сервером, пожалуйта подождите...");
        //в отдельном потоке
        new Thread(() -> {
            try {
                //запускаем логику клиента облачного хранилища
                storageClient.run();
            } catch (Exception e) {
              e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void onDisconnectClick(ActionEvent actionEvent) {

        //запускаем процесс отправки запроса на отключение
        storageClient.demandDisconnect();
    }


    // Метод инициирует элемент контекстного меню "Переименовать"
    public void  renameBtnAction (ActionEvent actionEvent){

    }

    //Метод инициирует элемент контекстного меню "Скачать из облачного хранилища"
    public void  downloadBtnAction (ActionEvent actionEvent){


     
    }
    //Mетод инициирует элемент контекстного меню "Загрузить в облачное хранилище"
    public void  uploadBtnAction (ActionEvent actionEvent){

    }
    // Метод инициирует элемент контекстного меню "Удалить"
    public void  deleteBtnAction(ActionEvent actionEvent){

    }

    // Метод инициирует элемент контекстного меню  "Выход из приложения"
    public void btnExitAction(ActionEvent actionEvent) {

        Platform.exit();
    }

    // Метод инициирует элемент контекстного меню
    public void copyBtnAction(ActionEvent actionEvent) {

    }

    /*
     * Метод-прокладка запускает процессы: показа окна авторизации в режиме авторизации
     * и процесс регистрации пользователя в сетевом хранилище.
     *  login - логин пользователя
     *  first_name - имя пользователя
     *  last_name - фамилия пользователя
     *  password - пароль пользователя
     */

    public void demandRegistration(String login, String first_name, String last_name, String password) {
        storageClient.demandRegistration(login, first_name, last_name, password);
    }
    public Label getNoticeLabel() {
        return noticeLabel;
    }

    public GMClient getStorageClient() {
        return storageClient;
    }

    /**
     * Метод выводит в отдельном потоке(не javaFX) переданное сообщение в метку уведомлений.
     *  text - строка сообщения
     */
    public void showTextInController(String text) {

            //в отдельном потоке запускаем обновление интерфейса
            Platform.runLater(() -> {
                //выводим сообщение в нижнюю метку
                noticeLabel.setText(text);
            });

    }

    public void openAuthWindowInController() {
    }

    public void onRegistrationBtnClick(ActionEvent actionEvent) throws IOException {
        //инициируем объект менеджера окон
        winManager = WinManager.getInstance();
        //передаем ему настройки
        winManager.init(Controller.this);
        winManager.openRegistrationForm();
    }
    

    public void setDisconnectedMode(boolean b) {
    }

    public void setRegisteredAndUnauthorisedMode() {
    }

    public void setAuthorizedMode(boolean b) {
    }

}