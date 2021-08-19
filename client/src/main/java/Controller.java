
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;


import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class Controller implements Initializable {

    //Объявляем объекты верхнего меню
    @FXML
    HBox connectPanel;


    //объявляем объект метки уведомлений
    @FXML
    private Label noticeLabel;

    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    //позволяет создать выпадающий список. Данный класс типизируется типом элементов, которые будут храниться в списке.
    ComboBox<String> disksBox;

    @FXML
    //Класс TextField совершает управление интерфейса пользователя, который принимает и отображает ввход текста.
    TextField pathField;


    //инициируем константу строки названия директории по умолчанию относительно корневой директории
    // для списка в клиентской части
    private final String CLIENT_DEFAULT_DIR = "";
    //инициируем константу строки названия корневой директории для списка в серверной части GUI
    private final String STORAGE_DEFAULT_DIR = "";

    //инициируем объекты контестного меню
    private ContextMenu clientContextMenu = new ContextMenu();
    //объявляем объект менеджера окон
    private static WinManager winManager;
    private CloudPanelController cloudPanelController;

    //объявляем объект контроллера клиента облачного хранилища
    private GMClient storageClient;

    //объявляем объекты текущей папки списка объектов элемента в клиентской и серверной части GUI
    private FileInfo clientCurrentDirItem;
    private FileInfo storageCurrentDirItem;
    //объявляем объекты директории по умолчанию в клиентской и серверной части GUI
    private FileInfo clientDefaultDirItem;
    private FileInfo storageDefaultDirItem;

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    @Override

    public void initialize(URL location, ResourceBundle resources) {

        //инициируем объект менеджера окон
        winManager = WinManager.getInstance();
        //передаем ему настройки
        winManager.init(Controller.this);
        //инициируем объект клиента облачного хранилища
        storageClient = new GMClient(Controller.this);
        //устанавливаем настройки приложения
        storageClient.initConfiguration();
        //выводим текст в метку
        noticeLabel.setText("Cвязь с сервером не установлена. Произведите подключение");

        //инициируем объекты директории по умолчанию в клиентской и серверной части
        clientDefaultDirItem = new FileInfo(Paths.get(CLIENT_DEFAULT_DIR));
        storageDefaultDirItem = new FileInfo(Paths.get(STORAGE_DEFAULT_DIR));
        clientItemListView();
        serverItemListView(location, resources);
    }

    private void clientItemListView() {
        setClientContextMenu(filesTable, clientContextMenu);

        // создаем таблицу
        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        filenameColumn.setPrefWidth(200);

        TableColumn<FileInfo, String> fileExtensionColumn = new TableColumn<>("Тип");
        fileExtensionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getExtension()));
        fileExtensionColumn.setPrefWidth(80);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = get_size(item);
                        if (item == -1L) {
                            text = "";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(filenameColumn, fileExtensionColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileExtensionColumn);

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);

        //при двойном нажатии на кнопку мыши
        filesTable.setOnMouseClicked(new EventHandler<javafx.scene.input.MouseEvent>() {

            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                if (event.getButton().name().equals("PRIMARY")) {
                    //если контекстное меню показывается
                    if (event.getClickCount() == 1) {
                        if (clientContextMenu.isShowing()) {
                            //закрываем контекстное меню
                            clientContextMenu.hide();
                            //если двойное нажатие
                        }
                    } else if (event.getClickCount() == 2) {
                        Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFileName());
                        //если нажатый элемент списка не пустой или директория
                        if (Files.isDirectory(path)) {
                            //Производим обнавление списока файлов
                            updateFilesList(path);
                        }
                    }
                }
            }
        });

        updateFilesList(Paths.get("."));

    }

    @FXML
    private void onConnectButtonClick() {
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


    /**
     * Метод устанавливает режим авторизован или нет, в зависимости от параметра
     */
    public static void setAuthorizedMode(boolean isAuthMode) {

        //если авторизация получена
        if (isAuthMode) {
            //если объект контроллера регистрации не нулевой
            if (winManager.getRegistrationController() != null) {
                //закрываем окно формы в потоке JavaFX
                Platform.runLater(() -> winManager.getRegistrationController().hideWindow());
            }
            //если объект контроллера авторизации не нулевой
            if (winManager.getAuthorisationController() != null) {
                //закрываем окно формы в потоке JavaFX
                Platform.runLater(() -> winManager.getAuthorisationController().hideWindow());
            }

        }
    }

    /**
     * Метод-прокладка запускает процессы: показа окна авторизации в режиме авторизации
     * и процесс регистрации пользователя в сетевом хранилище.
     * login - логин пользователя
     * first_name - имя пользователя
     * last_name - фамилия пользователя
     * password - пароль пользователя
     */
    public void demandRegistration(String login, String first_name, String last_name, String password) {
        storageClient.demandRegistration(login, first_name, last_name, password);
    }

    /**
     * Метод-прокладка запускает процесс показа основного окна и процесс
     * авторизации пользователя в сетевом хранилище.
     *
     * @param login    - логин пользователя
     * @param password - пароль пользователя
     */
    public void demandAuthorisation(String login, String password) {
        storageClient.demandAuthorization(login, password);
    }

    public Label getNoticeLabel() {
        return noticeLabel;
    }

    public GMClient getStorageClient() {
        return storageClient;
    }

    /**
     * Метод выводит в отдельном потоке(не javaFX) переданное сообщение в метку уведомлений.
     * text - строка сообщения
     */
    public void showTextInController(String text) {

        //в отдельном потоке запускаем обновление интерфейса
        Platform.runLater(() -> {
            //выводим сообщение в нижнюю метку
            noticeLabel.setText(text);
        });

    }


    /**
     * Метод устанавливает режим отображения GUI "Отсоединен" или "Подсоединен".
     *
     * @param isDisconnectedMode - если true - "Отсоединен"
     */
    public void setDisconnectedMode(boolean isDisconnectedMode) {


        //если установлен режим соединен
        if (!isDisconnectedMode) {

            showTextInController("Connect");
        } else {

            showTextInController("Disconnect");
        }

    }


    //Производим обнавление списока файлов
    public void updateFilesList(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateFilesList(upperPath);
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateFilesList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public static String get_size(long bytes) {
        if (bytes < 1000) {
            return String.format("%,d bytes", bytes);
        } else if (bytes < 1000 * Math.pow(2, 10)) {
            return String.format("%,d KB", (long) (bytes / Math.pow(2, 10)));
        } else if (bytes < 1000 * Math.pow(2, 20)) {
            return String.format("%,d MB", (long) (bytes / Math.pow(2, 20)));
        } else if (bytes < 1000 * Math.pow(2, 30)) {
            return String.format("%,d GB", (long) (bytes / Math.pow(2, 30)));
        } else if (bytes < 1000 * Math.pow(2, 40)) {
            return String.format("%,d TB", (long) (bytes / Math.pow(2, 40)));
        }
        return "n/a";
    }
    public void serverItemListView(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
           cloudPanelController.initialize(location, resources);
        });
    }

    /**
     * Метод инициирует контекстное меню для листвью клиента.
     *
     * @param filesTable  - коллекция объектов элемента
     * @param contextMenu - объект контекстного меню
     */
    private void setClientContextMenu(TableView<FileInfo> filesTable, ContextMenu contextMenu) {
        // добавляем скопом элементы в контестное меню
        contextMenu.getItems().addAll(menuItemUpload(filesTable));

        //устанавливаем настройки контектстного меню
        setContextMenu(filesTable, contextMenu);
    }


    /**
     * Приватный общий метод устанавливает настройки вызова заданного контекстного меню.
     *
     * @param filesTable  - объект заданного листвью
     * @param contextMenu - объект заданного контектстного меню
     */
    private void setContextMenu(TableView<FileInfo> filesTable, ContextMenu contextMenu) {
        //устаналиваем событие на клик правой кнопки мыши по элементу списка
        filesTable.setOnContextMenuRequested(event -> {
            //если контекстное меню уже показывается или снова кликнуть на пустой элемент списка
            if (contextMenu.isShowing() ||
                    filesTable.getSelectionModel().getSelectedItems().isEmpty()) {
                //скрываем контекстное меню
                contextMenu.hide();
                //очищаем выделение
                filesTable.getSelectionModel().clearSelection();
                return;
            }
            //показываем контекстное меню в точке клика(позиция левого-верхнего угла контекстного меню)
            contextMenu.show(filesTable, event.getScreenX(), event.getScreenY());
        });
    }

    /**
     * Метод инициирует элемент контекстного меню "Загрузить в облачное хранилище"
     *
     * @param filesTable - текущий список объектов элемента
     * @return - объект элемента контекстного меню "Upload"
     */
    private MenuItem menuItemUpload(TableView<FileInfo> filesTable) {
        //инициируем пункт контекстного меню "Загрузить в облачное хранилище"
        MenuItem menuItemUpload = new MenuItem("Загрузить");
        //устанавливаем обработчика нажатия на этот пункт контекстного меню
        menuItemUpload.setOnAction(event -> {
            //запоминаем кликнутый элемент списка
            FileInfo fileInfo = filesTable.getSelectionModel().getSelectedItem();
            try {
                //выводим сообщение в нижнюю метку
                showTextInController("Загрузка файла...");
                //отправляем на сервер запрос на загрузку файла в облачное хранилище
                storageClient.demandUploadItem(storageDefaultDirItem, fileInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //сбрасываем выделение после действия
            filesTable.getSelectionModel().clearSelection();
        });
        return menuItemUpload;
    }


    //Метод отправки запроса об отключении на сервер
    public void dispose() {
        //запускаем процесс отправки запроса серверу на разрыв соединения
        storageClient.demandDisconnect();
        Platform.exit();

    }

    /**
     * Метод-прокладка, чтобы открывать окно GUI в других потоках
     */
    public void openAuthWindowInGUI() {
        //в отдельном потоке запускаем обновление интерфейса
        //открываем окно авторизации
        Platform.runLater(() -> winManager.openAuthorisationWindow());
    }

    /**
     * Метод отрабатывает клик Авторизации .
     * Открывает Авторизационную форму.
     * actionEvent - событие клик мыши
     */
    @FXML
    public void onAuthorizationLinkClick(ActionEvent actionEvent) {
        //переходим в режим авторизации
        setAuthorizationFormMode();
    }
    /**
     * Метод открывает окно авторизации.
     */
    public void setAuthorizationFormMode() {
        //если объект контроллера регистрации не нулевой
        if(winManager.getRegistrationController() != null){
            //закрываем окно формы в потоке JavaFX
            Platform.runLater(() -> winManager.getRegistrationController().hideWindow());
        }
        //открываем окно авторизации с пустыми логином и паролем
        winManager.openAuthorisationWindow();
    }

    public void onRegistrationBtnClick(ActionEvent actionEvent) {

        winManager.openRegistrationForm();
        if (winManager.getRegistrationController() != null) {
            //закрываем окно формы в потоке JavaFX
            Platform.runLater(() -> winManager.getRegistrationController().hideWindow());

        }
    }

}
