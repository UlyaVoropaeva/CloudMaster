
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
    TextField pathField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        updateFilesList(path);
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

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFullFilename();
    }

    public String getCurrentPath() {
        return pathField.getText();
    }

    public String get_size(long bytes) {
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
}

