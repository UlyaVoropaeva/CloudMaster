
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import javax.sound.midi.Patch;
import java.nio.file.Paths;


/**
 * Класс клиента для распознавания командных сообщений и обработчиков команд управления
 */

public class NettyClientDecoder extends ChannelInboundHandlerAdapter {
    //принимаем объект исходящего хэндлера
    private GMClient storageClient;
    //принимаем объект файлового обработчика
    private FileUtils fileUtils;
    //принимаем объект соединения
    private ChannelHandlerContext ctx;
    //принимаем объект контроллера
    private Controller controller;

    public NettyClientDecoder(GMClient storageClient) {
        this.storageClient = storageClient;
        //принимаем объект контроллера
        controller = storageClient.getController();
        //принимаем объект файлового обработчика
        fileUtils = storageClient.getFileUtils();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //принимаем объект соединения
        this.ctx = ctx;
        //передаем объект соединения в объект клиента сетевого хранилища
        storageClient.setCtx(ctx);
    }

    /**
     * Метод отрабатываем событие получение объекта сообщения.
     * Преобразует объект сообщения в объект команды и запускает его обработку.
     * ctx - объект сетевого соединения
     * msgObject - объект сообщения
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msgObject) {
        try {
            //десериализуем объект сообщения(команды)
            CommandMessage commandMessage = (CommandMessage) msgObject;

            //распознаем и обрабатываем полученный объект сообщения(команды)
            recognizeAndArrangeMessageObject(commandMessage);
        } finally {
            ReferenceCountUtil.release(msgObject);
        }
    }

    /**
     * Метод распознает тип команды и обрабатывает ее.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    public void recognizeAndArrangeMessageObject(CommandMessage commandMessage) {
        //выполняем операции в зависимости от типа полученного сообщения(команды)
        switch (commandMessage.getCommand()) {
            //обрабатываем полученное от сервера подтверждение успешного подключения клиента
            case SERVER_NOTIFICATION_CLIENT_CONNECTED:
                //вызываем метод обработки ответа сервера
                onServerConnectedResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера подтверждение готовности отключения клиента
            case SERVER_RESPONSE_DISCONNECT_OK:
                //вызываем метод обработки ответа сервера
                onServerDisconnectOKServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера сообщение об ошибке при попытке отключения клиента
            case SERVER_RESPONSE_DISCONNECT_ERROR:
                //вызываем метод обработки ответа сервера
                onServerDisconnectErrorServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера подтверждение успешной регистрации
            // нового пользователя в облачное хранилище
            case SERVER_RESPONSE_REGISTRATION_OK:
                //вызываем метод обработки ответа сервера
                onRegistrationOKServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера подтверждение успешной авторизации в облачное хранилище
            case SERVER_RESPONSE_AUTH_OK:
                //вызываем метод обработки ответа сервера
                onAuthOKServerResponse(commandMessage);
                break;

            //обрабатываем полученное от сервера подтверждение успешной загрузки(сохранении)
            // файла в облачное хранилище
            case SERVER_RESPONSE_UPLOAD_ITEM_OK:
                break;
                //обрабатываем полученное от сервера подтверждение успешной загрузки(сохранении)
                // всего большого файла(по фрагментно) в облачное хранилище
            case SERVER_RESPONSE_UPLOAD_FILE_FRAGS_OK:
                onUploadFileFragOkServerResponse(commandMessage);
                break;
                //обрабатываем полученное от сервера сообщение об ошибке регистрации в облачное хранилище
            case SERVER_RESPONSE_REGISTRATION_ERROR:
                //вызываем метод обработки ответа сервера
                onRegistrationErrorServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера сообщение об ошибке авторизации в облачное хранилище
            case SERVER_RESPONSE_AUTH_ERROR:
                //вызываем метод обработки ответа сервера
                onAuthErrorServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера сообщение об ошибке загрузки(сохранения)
            // файла в облачное хранилище
            case SERVER_RESPONSE_UPLOAD_ITEM_ERROR:
                //вызываем метод обработки ответа сервера
                onUploadItemErrorServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера подтверждение успешной загрузки(сохранении)
            // фрагмента файла в облачное хранилище
            case SERVER_RESPONSE_UPLOAD_FILE_FRAG_OK:
                //вызываем метод обработки ответа сервера
                onUploadFileFragOkServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера сообщение об ошибке загрузки(сохранения)
            // фрагмента файла в облачное хранилище
            case SERVER_RESPONSE_UPLOAD_FILE_FRAG_ERROR:
                //вызываем метод обработки ответа сервера
                onUploadFileFragErrorServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера подтверждение успешного скачивания файла из облачного хранилища
            case SERVER_RESPONSE_DOWNLOAD_ITEM_OK:
                //вызываем метод обработки ответа сервера со скачанным целым файлом внутри
                onDownloadItemOkServerResponse(commandMessage);
                break;
            //обрабатываем полученное от сервера сообщение об ошибке скачивания файла из облачного хранилища
            case SERVER_RESPONSE_DOWNLOAD_ITEM_ERROR:
                //вызываем метод обработки ответа сервера
                onDownloadFileErrorServerResponse(commandMessage);
                break;
            //обрабатываем полученный от сервера ответ на запрос на скачивание с фрагментом файла из облачного хранилища
            case SERVER_RESPONSE_DOWNLOAD_FILE_FRAG_OK:
                //вызываем метод обработки ответа от сервера с файлом-фрагментом
                //в директорию клиента
                onDownloadFileFragOkServerResponse(commandMessage);
                break;

        }
    }

    /**
     * Метод обрабатывает полученное от сервера сообщение об ошибке
     * скачивания объекта элемента(файла) из облачного хранилища
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadFileErrorServerResponse(CommandMessage commandMessage) {
        storageClient.showTextInController("сообщение об ошибке: скачивания объекта элемента(файла) из облачного хранилища");
    }

    /**
     * Метод обработки ответа сервера со скачанным целым объектом элемента(файлом) внутри
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadItemOkServerResponse(CommandMessage commandMessage) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileMessage fileMessage = (FileMessage) commandMessage.getMessageObject();
        //если сохранение прошло удачно
        if (storageClient.downloadItem(fileMessage)) {
            //очищаем метку уведомлений
            storageClient.showTextInController("");
            //обновляем список файловых объектов на клиенте

            //если что-то пошло не так
        } else {

            //выводим сообщение в GUI
            storageClient.showTextInController("что то пошло не так  со скачанным целым объектом элемента(файлом) ");
        }
    }

    /**
     * Метод обработки ответа от сервера на скачивание файла-фрагмента
     * в директорию в клиенте.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadFileFragOkServerResponse(CommandMessage commandMessage) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileFragmentMessage fileFragMsg = (FileFragmentMessage) commandMessage.getMessageObject();
        //объявляем переменную типа команды
        Commands command;
        //если сохранение полученного фрагмента файла во временную папку клиента прошло удачно
        if (storageClient.downloadItemFragment(fileFragMsg)) {
            //выводим в GUI информацию с номером загруженного фрагмента файла
            storageClient.showTextInController("File downloading. Completed fragment: " +
                    fileFragMsg.getCurrentFragNumber() +
                    "/" + fileFragMsg.getTotalFragsNumber());
            //отправляем сообщение на сервер: подтверждение, что все прошло успешно
            command = Commands.CLIENT_RESPONSE_DOWNLOAD_FILE_FRAG_OK;
            //если что-то пошло не так
        } else {
            //выводим сообщение в GUI
            showTextInController("что-то пошло не так, сообщение не отправлено");
            //инициируем переменную типа команды - ответ об ошибке
            command = Commands.CLIENT_RESPONSE_DOWNLOAD_FILE_FRAG_ERROR;
        }
        //обнуляем байтовый массив в объект сообщения фрагмента файла
        fileFragMsg.setData(null);
        //отправляем объект сообщения(команды) серверу
        ctx.writeAndFlush(new CommandMessage(command, fileFragMsg));
        //если это последний фрагмент
        if (fileFragMsg.isFinalFileFragment()) {
            //выводим в GUI информацию о компиляции итогового файла из фрагментов в сетевом хранилише
            storageClient.showTextInController("Фаил загружен...");
            //если корректно собран файл из фрагментов сохраненных во временную папку
            if (storageClient.compileItemFragments(fileFragMsg)) {
                //очищаем метку уведомлений
                showTextInController("");
                //обновляем список файловых объектов на клиенте
                controller.updateFilesList(Paths.get(
                        fileFragMsg.getToDirectoryItem().getFullFilename()));
                //если что-то пошло не так
            } else {
                //выводим сообщение в GUI
                showTextInController("что-то пошло не так, сообщение не отправлено");
            }
        }
    }

    /**
     * Метод обрабатывает полученное от сервера сообщение
     * об ошибке загрузки(сохранения) фрагмента файла в облачное хранилище.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadFileFragErrorServerResponse(CommandMessage commandMessage) {
        //вынимаем объект сообщения фрагмента файла из объекта сообщения(команды)
        FileFragmentMessage fileFragMsg = (FileFragmentMessage) commandMessage.getMessageObject();
        //выводим сообщение в лог
        storageClient.showTextInController("[client]CommandMessageManager.onUploadFileFragErrorServerResponse() - " +
                "Ошибка загрузки фрагмента: " + fileFragMsg.getCurrentFragNumber() +
                "/" + fileFragMsg.getTotalFragsNumber());
        //повторяем отправку на загрузку этого фрагмента заново
        storageClient.sendFileFragmentMsg(fileFragMsg, Commands.REQUEST_SERVER_UPLOAD_FILE_FRAG);
    }

    /**
     * Метод обрабатывает полученное от сервера подтверждение
     * успешной загрузки(сохранении) фрагмента файла в облачное хранилище.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadFileFragOkServerResponse(CommandMessage commandMessage) {
        //вынимаем объект сообщения фрагмента файла из объекта сообщения(команды)
        FileFragmentMessage fileFragMsg = (FileFragmentMessage) commandMessage.getMessageObject();
        //выводим сообщение
        storageClient.showTextInController("[client]CommandMessageManager.onUploadFileFragOkServerResponse() - " +
                "загрузка фрагмента: " + fileFragMsg.getCurrentFragNumber() +
                "/" + fileFragMsg.getTotalFragsNumber());
        //выводим в GUI информацию с номером загруженного фрагмента файла
        storageClient.showTextInController("Загрузка файла. завершен фрагмент: " +
                fileFragMsg.getCurrentFragNumber() +
                "/" + fileFragMsg.getTotalFragsNumber());

        //если это финальный фрагмент
        if (fileFragMsg.getCurrentFragNumber() == fileFragMsg.getTotalFragsNumber()) {
            //выводим в GUI информацию о компиляции итогового файла из фрагментов в сетевом хранилише
            storageClient.showTextInController("загрузка всего файла ...");
        }
    }

    /**
     * Метод обрабатывает полученное от сервера сообщение об ошибке загрузки(сохранения)
     * объекта элемента(файла) в облачное хранилище
     *
     * @param commandMessage - объект сообщения(команды)
     */

    private void onUploadItemErrorServerResponse(CommandMessage commandMessage) {
        storageClient.showTextInController("[client]CommandMessageManager.onUploadFileErrorServerResponse() command: " + commandMessage.getCommand());
    }


    /**
     * Метод обрабатывает полученное от сервера подтверждение успешного подключения клиента
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onServerConnectedResponse(CommandMessage commandMessage) {
        //открываем окно авторизации
        controller.openAuthWindowInGUI();
        //устанавливаем режим отображения "Подключен"
        controller.setDisconnectedMode(false);
    }

    /**
     * Метод обрабатывает полученное от сервера подтверждение готовности отключения клиента.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onServerDisconnectOKServerResponse(CommandMessage commandMessage) {
        //запускаем процесс отключения от сервера
        storageClient.disconnect();
        //выводим текст в метку
        showTextInController("Сервер отключен.");

    }

    /**
     * Метод обрабатывает полученное от сервера сообщение об ошибке при попытке отключения клиента.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onServerDisconnectErrorServerResponse(CommandMessage commandMessage) {
        showTextInController(" полученно от сервера сообщение об ошибке при попытке отключения клиента...");
    }

    /**
     * Метод обрабатывает полученное от сервера подтверждение успешной регистрации
     * нового пользователя в облачное хранилище.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onRegistrationOKServerResponse(CommandMessage commandMessage) {
        //выводим сообщение в метку уведомлений
        showTextInController("полученно от сервера подтверждение успешной регистрации");
        //закрываем регистрационное окно и открываем авторизационное окно
        //  controller.setRegisteredAndUnauthorisedMode();
    }

    /**
     * Метод обрабатывает полученное от сервера сообщение об ошибке регистрации
     * нового пользователя в облачное хранилище.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onRegistrationErrorServerResponse(CommandMessage commandMessage) {
        //выводим сообщение в нижнюю метку
        showTextInController("полученно от сервера сообщение об ошибке регистрации...");
    }

    /**
     * Метод обрабатывает полученное от сервера подтверждение успешной авторизации в облачное хранилище
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onAuthOKServerResponse(CommandMessage commandMessage) {
        //устанавливаем режим отображения "Авторизован"
        controller.setAuthorizedMode(true);
    }

    /**
     * Метод обрабатывает полученное от сервера сообщение об ошибке авторизации в облачное хранилище
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onAuthErrorServerResponse(CommandMessage commandMessage) {
        //выводим сообщение в нижнюю метку GUI
        showTextInController("Ошибка: клиент с таким логином и паролем не существует ");
    }

    /**
     * Метод выводит сообщение в нижнюю метку GUI
     *
     * @param text - сообщение
     */
    public void showTextInController(String text) {
        //выводим сообщение в нижнюю метку GUI
        controller.showTextInController(text);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}