import javafx.application.Application;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    //создаем экземпляр контроллера
    Controller contr;

    @Override
    public void start(Stage primaryStage) throws Exception {
        //чтобы получить доступ к контроллеру
        //лоадер вынесли отдельно, чтобы с ним удобнее было работать
        FXMLLoader loader = new FXMLLoader();
        //с помощью метода getResourceAsStream извлекаем данные из лоадера, чтобы
        //вызвать метод getController для получения контроллера
        Parent root = loader.load(getClass().getResource("controller.fxml"));
        contr = loader.getController();

        //определяем действия по событию закрыть окно по крестику через лямбда
        //лямбда здесь - это замена анонимного класса типа new Runnable
        //в лямбда event - аргумент(здесь некое событие), {тело лямбды - операции}
        primaryStage.setOnCloseRequest(event -> {
            contr.dispose();//dispose - располагать, размещать
            //сворачиваем окно
            Platform.exit();
            //указываем системе, что выход без ошибки
            System.exit(0);
        });

        primaryStage.setTitle("Cloud Master");
        Scene scene = new Scene(root, 512, 600);
        primaryStage.setScene(scene);
        primaryStage.show();


    }

}
