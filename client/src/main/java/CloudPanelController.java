
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;


import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CloudPanelController implements Initializable {

    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    TextField pathField, rightPanel;

    //принимаем объект исходящего хэндлера
    private GMClient storageClient;
    //принимаем объект главного контроллера GUI
    private Controller backController;
    //инициируем объекты контестного меню
    private ContextMenu serverContextMenu = new ContextMenu();
    //инициируем константу строки названия директории по умолчанию относительно корневой директории
    // для списка в клиентской части
    private final String CLIENT_DEFAULT_DIR = "";
    //инициируем константу строки названия корневой директории для списка в серверной части GUI
    private final String STORAGE_DEFAULT_DIR = "";

    //объявляем объект менеджера окон
    private static WinManager winManager;
    //объявляем объекты директории по умолчанию в клиентской и серверной части GUI
    private FileInfo clientDefaultDirItem;
    private FileInfo storageDefaultDirItem;
    //объявляем объекты текущей папки списка объектов элемента в клиентской и серверной части GUI
    private FileInfo clientCurrentDirItem;
    private FileInfo storageCurrentDirItem;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //инициируем объект менеджера окон
        winManager = WinManager.getInstance();
        //передаем ему настройки
        winManager.init(backController);
        //инициируем объект  облачного хранилища
        storageClient = new GMClient(backController);
        //инициируем объекты директории по умолчанию в клиентской и серверной части
        clientDefaultDirItem = new FileInfo(Paths.get(CLIENT_DEFAULT_DIR));
        storageDefaultDirItem = new FileInfo(Paths.get(STORAGE_DEFAULT_DIR));

        setServerContextMenu(filesTable, serverContextMenu);

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


        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                if (event.getButton().name().equals("PRIMARY")) {
                    //если контекстное меню показывается
                    if (event.getClickCount() == 1) {
                        if (serverContextMenu.isShowing()) {
                            //закрываем контекстное меню
                            serverContextMenu.hide();
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


    public void updateFilesList(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Диск не доступен");
            alert.showAndWait();
        }
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            updateFilesList(upperPath);
        }
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

    /**
     * Метод инициирует контекстное меню для листвью сервера.
     *
     * @param filesTable  - коллекция объектов элемента
     * @param contextMenu - объект контекстного меню
     */
    public void setServerContextMenu(TableView<FileInfo> filesTable, ContextMenu contextMenu) {
        // добавляем скопом элементы в контестное меню
        contextMenu.getItems().addAll(menuItemDownload(filesTable));

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
     * Метод инициирует элемент контекстного меню "Скачать из облачного хранилища"
     *
     * @param filesTable - текущий список объектов элемента
     * @return - объект элемента контекстного меню "Download"
     */
    private MenuItem menuItemDownload(TableView<FileInfo> filesTable) {
        //инициируем пункт контекстного меню "Скачать из облачного хранилища"
        MenuItem menuItemDownload = new MenuItem("Скачать");
        //устанавливаем обработчика нажатия на этот пункт контекстного меню
        menuItemDownload.setOnAction(event -> {
            //запоминаем кликнутый элемент списка
            FileInfo fileInfo = filesTable.getSelectionModel().getSelectedItem();
            //выводим сообщение в нижнюю метку
            backController.showTextInController("Загрузка файла. А ожидание ответа от сервера...");
            //отправляем на сервер запрос на скачивание файла из облачного хранилища
            storageClient.demandDownloadItem(storageCurrentDirItem, clientCurrentDirItem, fileInfo);
            //сбрасываем выделение после действия
            filesTable.getSelectionModel().clearSelection();
        });
        return menuItemDownload;
    }


}

