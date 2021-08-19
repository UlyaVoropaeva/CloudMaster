
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class CommandManagerServer extends ChannelInboundHandlerAdapter {
    //принимаем объект соединения
    private ChannelHandlerContext ctx;
    //принимаем объект контроллера сетевого хранилища
    private final GMServer gmServer;

    //принимаем объект реального пути к корневой директории пользователя в сетевом хранилище
    private Path userStorageRoot = Paths.get("12");

    //объявляем переменную типа команды
    private Commands command;
    //объявляем объект файлового обработчика
    private FileUtils fileUtils;

    public CommandManagerServer(GMServer gmServer) {
        this.gmServer = gmServer;

    }

    /**
     * Метод в полученном объекте сообщения распознает тип команды и обрабатывает ее.
     *
     * @param ctx - объект соединения netty, установленного с клиентом
     * @param msg - входящий объект сообщения
     * @throws Exception - исключение
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //принимаем объект соединения
        this.ctx = ctx;
        //инициируем из объекта сообщения объект команды
        CommandMessage commandMessage = (CommandMessage) msg;
        //если сюда прошли, значит клиент авторизован
        //***блок обработки объектов сообщений(команд), полученных от клиента***
        //выполняем операции в зависимости от типа полученного не сервисного сообщения(команды)
        switch (commandMessage.getCommand()) {
            //обрабатываем полученный от AuthGateway проброшенный запрос на авторизацию клиента в облачное хранилище
            //возвращаем список объектов в корневой директорию пользователя в сетевом хранилище.
            case SERVER_RESPONSE_AUTH_OK:
                //вызываем метод обработки запроса от AuthGateway
                onAuthClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на отсоединение пользователя от сервера
            //в авторизованном режиме
            case REQUEST_SERVER_DISCONNECT:
                //вызываем метод обработки запроса от клиента
                onDisconnectClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на загрузку(сохранение) файла в облачное хранилище
            case REQUEST_SERVER_UPLOAD_ITEM:
                //вызываем метод обработки запроса от клиента на загрузку целого файла клиента
                // в директорию в сетевом хранилище.
                onUploadItemClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на загрузку(сохранение) фрагмента файла в облачное хранилище
            case REQUEST_SERVER_UPLOAD_FILE_FRAG:
                //вызываем метод обработки запроса от клиента на загрузку файла-фрагмента
                //в директорию в сетевом хранилище.
                onUploadFileFragClientRequest(commandMessage);
                break;
            //обрабатываем полученный от клиента запрос на скачивание целого файла из облачного хранилища
            case REQUEST_SERVER_DOWNLOAD_ITEM:
                //вызываем метод обработки запроса от клиента на скачивание целого файла клиента
                // из директории в сетевом хранилище
                onDownloadItemClientRequest(commandMessage);
                break;
            //обрабатываем полученное от клиента подтверждение
            //успешного скачивания(сохранения) фрагмента файла в клиента.
            case CLIENT_RESPONSE_DOWNLOAD_FILE_FRAG_OK:
                //вызываем метод обработки ответа сервера
                onDownloadFileFragOkClientResponse(commandMessage);
                break;
            //обрабатываем полученное от клиента сообщение
            //об ошибке скачивания(сохранения) фрагмента файла в клиента
            case CLIENT_RESPONSE_DOWNLOAD_FILE_FRAG_ERROR:
                //вызываем метод обработки ответа сервера
                onDownloadFileFragErrorClientResponse(commandMessage);
                break;
        }
    }

    /**
     * Метод обработки запроса от клиента на загрузку целого объекта(файла) клиента
     * в заданную директорию в сетевом хранилище.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadItemClientRequest(CommandMessage commandMessage) {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileMessage fileMessage = (FileMessage) commandMessage.getMessageObject();
        //если сохранение прошло удачно
        if (gmServer.uploadItem(fileMessage, userStorageRoot)) {
            //отправляем сообщение на сервер: подтверждение, что все прошло успешно
            command = Commands.SERVER_RESPONSE_UPLOAD_ITEM_OK;
            //если что-то пошло не так
        } else {
            //инициируем переменную типа команды(по умолчанию - ответ об ошибке)
            command = Commands.SERVER_RESPONSE_UPLOAD_ITEM_ERROR;
        }
    }

    private void onAuthClientRequest(CommandMessage commandMessage) {
    }

    /**
     * Метот обрабатывает событие - установление соединения с клиентом.
     * По событию отправляет сообщение-уведомление клиенту.
     *
     * @param ctx - объект соединения netty, установленного с клиентом
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //принимаем объект соединения
        this.ctx = ctx;
        //если соединение установлено, отправляем клиенту сообщение
        ctx.writeAndFlush(new CommandMessage(Commands.SERVER_NOTIFICATION_CLIENT_CONNECTED));

    }


    /**
     * Метод обрабатывает полученный от клиента запрос на отсоединение пользователя
     * от сервера в авторизованном режиме.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDisconnectClientRequest(CommandMessage commandMessage) {
        //отправляем объект сообщения(команды) клиенту
        ctx.writeAndFlush(new CommandMessage(Commands.SERVER_RESPONSE_DISCONNECT_OK));

        //закрываем соединение с клиентом
        ctx.channel().close();
    }

    /**
     * Метод обработки запроса от клиента на загрузку файла-фрагмента
     * в директорию в сетевом хранилище.
     *
     * @param commandMessage - объект сообщения(команды)
     */
    private void onUploadFileFragClientRequest(CommandMessage commandMessage) {
        //вынимаем объект сообщения фрагмента файла из объекта сообщения(команды)
        FileFragmentMessage fileFragMsg = (FileFragmentMessage) commandMessage.getMessageObject();
        //если сохранение полученного фрагмента файла во временную папку сетевого хранилища прошло удачно
        if (gmServer.uploadItemFragment(fileFragMsg, userStorageRoot)) {
            //инициируем переменную типа команды: подтверждение, что все прошло успешно
            command = Commands.SERVER_RESPONSE_UPLOAD_FILE_FRAG_OK;
            //если что-то пошло не так
        } else {
            //выводим сообщение
            printMsg("[server]");
            //инициируем переменную типа команды - ответ об ошибке
            command = Commands.SERVER_RESPONSE_UPLOAD_FILE_FRAG_ERROR;
        }
        //обнуляем байтовый массив в объект сообщения фрагмента файла
        fileFragMsg.setData(null);
        //отправляем объект сообщения(команды) клиенту
        ctx.writeAndFlush(new CommandMessage(command, fileFragMsg));
        //если это последний фрагмент
        if (fileFragMsg.isFinalFileFragment()) {
            //если корректно собран файл из фрагментов сохраненных во временную папку
            if (gmServer.compileItemFragments(fileFragMsg, userStorageRoot)) {
                //ответ сервера, что сборка файла из загруженных фрагментов прошла успешно
                command = Commands.SERVER_RESPONSE_UPLOAD_FILE_FRAGS_OK;
                //если что-то пошло не так
            } else {
                //инициируем переменную типа команды - ответ об ошибке
                command = Commands.SERVER_RESPONSE_UPLOAD_FILE_FRAGS_ERROR;
            }
        }
    }
    /**
     * Метод обработки запроса от клиента на скачивание целого объекта элемента(файла)
     * из директории в сетевом хранилище в клиента.
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadItemClientRequest(CommandMessage commandMessage) throws IOException {
        //вынимаем объект файлового сообщения из объекта сообщения(команды)
        FileMessage fileMessage = (FileMessage) commandMessage.getMessageObject();
        //запускаем процесс скачивания и отправки объекта элемента
        gmServer.downloadItem(fileMessage, userStorageRoot, ctx);
    }

    /**
     * Метод обрабатывает полученное от клиента подтверждение
     * успешного скачивания(сохранения) фрагмента файла в клиента.
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadFileFragOkClientResponse(CommandMessage commandMessage) {
        //вынимаем объект сообщения фрагмента файла из объекта сообщения(команды)
        FileFragmentMessage fileFragMsg = (FileFragmentMessage) commandMessage.getMessageObject();
        //выводим сообщение в лог
        printMsg("[server]CommandMessageManager.onDownloadFileFragOkClientResponse() - " +
                "uploaded fragments: " + fileFragMsg.getCurrentFragNumber() +
                "/" + fileFragMsg.getTotalFragsNumber());
        //сбрасываем защелку в цикле отправки фрагментов
        fileUtils.getCountDownLatch().countDown();
    }

    /**
     * Метод обрабатывает полученное от клиента сообщение
     * об ошибке скачивания(сохранения) фрагмента файла в клиента.
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDownloadFileFragErrorClientResponse(CommandMessage commandMessage) {
        //вынимаем объект сообщения фрагмента файла из объекта сообщения(команды)
        FileFragmentMessage fileFragMsg = (FileFragmentMessage) commandMessage.getMessageObject();
        //выводим сообщение в лог
        printMsg("[server]CommandMessageManager.onDownloadFileFragErrorClientResponse() - " +
                "Error of downloading the fragment: " + fileFragMsg.getCurrentFragNumber() +
                "/" + fileFragMsg.getTotalFragsNumber());
        //повторяем отправку на загрузку этого фрагмента заново
        gmServer.sendFileFragment(fileFragMsg, Commands.REQUEST_SERVER_UPLOAD_FILE_FRAG,
                userStorageRoot, ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void printMsg(String msg) {
        gmServer.printMsg(msg);
    }
}