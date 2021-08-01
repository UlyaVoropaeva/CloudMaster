
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class CommandManager extends ChannelInboundHandlerAdapter {
    //принимаем объект исходящего хэндлера
    private GMClient storageClient;
    //принимаем объект соединения
    private ChannelHandlerContext ctx;
    //принимаем объект контроллера
    private Controller controller;

    public CommandManager(GMClient storageClient) {
        this.storageClient = storageClient;
        //принимаем объект контроллера
        controller = storageClient.getController();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        //принимаем объект соединения
        this.ctx = ctx;
        //передаем объект соединения в объект клиента сетевого хранилища
        storageClient.setCtx(ctx);
    }

    /*
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
        }
        finally {
            ReferenceCountUtil.release(msgObject);
        }
    }

    /*
     * Метод распознает тип команды и обрабатывает ее.
     * commandMessage - объект сообщения(команды)
     */

    public void recognizeAndArrangeMessageObject(CommandMessage commandMessage) {
        //выполняем операции в зависимости от типа полученного сообщения(команды)
        switch (commandMessage.getCommand()) {
            //обрабатываем полученное от сервера подтверждение успешного подключения клиента
            case SERVER_NOTIFICATION_CLIENT_CONNECTED:
                //вызываем метод обработки ответа сервера
                onServerConnectedResponse(commandMessage);
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
        }
    }

    /*
     * Метод обрабатывает полученное от сервера подтверждение успешного подключения клиента
     *  commandMessage - объект сообщения(команды)
     */
    private void onServerConnectedResponse(CommandMessage commandMessage) {
        //открываем окно авторизации
        controller.openAuthWindowInController();
    }


    /*
     * Метод обрабатывает полученное от сервера сообщение об ошибке при попытке отключения клиента.
     *  commandMessage - объект сообщения(команды)
     */
    private void onServerDisconnectErrorServerResponse(CommandMessage commandMessage) {
        showTextInController("The disconnecting from server is not allowed!");
    }

    /*
     * Метод обрабатывает полученное от сервера подтверждение успешной регистрации
     * нового пользователя в облачное хранилище.
     * commandMessage - объект сообщения(команды)
     */
    private void onRegistrationOKServerResponse(CommandMessage commandMessage) {
        //выводим сообщение в метку уведомлений в GUI
        showTextInController("You have registered in the Cloud Storage. Press \"Authorization\" button.");

    }

    /*
     * Метод обрабатывает полученное от сервера сообщение об ошибке регистрации
     * нового пользователя в облачное хранилище.
     *  commandMessage - объект сообщения(команды)
     */
    private void onRegistrationErrorServerResponse(CommandMessage commandMessage) {
        //выводим сообщение в нижнюю метку GUI
        showTextInController("Probably this login has been registered before! Try again.");
    }


    /*
     * Метод обрабатывает полученное от сервера сообщение об ошибке авторизации в облачное хранилище
     * commandMessage - объект сообщения(команды)
     */
    private void onAuthErrorServerResponse(CommandMessage commandMessage) {
        //выводим сообщение в нижнюю метку
        showTextInController("Something wrong with your login or password! Are you registered?");
    }



    /*
     * Метод выводит сообщение в нижнюю метку GUI
     * text - сообщение
     */
    public void showTextInController(String text){
        //выводим сообщение в нижнюю метку контроллера
        controller.showTextInController(text);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}