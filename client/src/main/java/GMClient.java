import io.netty.channel.ChannelHandlerContext;



public class GMClient {

    //принимаем объект хендлера для операций с директориями
    private Controller controller;
    //принимаем объект соединения
    private ChannelHandlerContext ctx;
    //объявляем переменную IP адреса сервера
    private static String IP_ADDR;
    //объявляем переменную порта соединения
    private static int PORT = 8189;


    public GMClient(Controller controller) {
        //принимаем объект контроллера
        this.controller = controller;
    }


    public void setCtx(ChannelHandlerContext ctx) {
        //ctx - объект сетевого соединения
        this.ctx = ctx;
    }

    /*
     * Метод начала работы клиента сетевого хранилища.
     */
    public void run() throws Exception {
        //инициируем объект соединения
        new NettyClient(this,  PORT).run();
    }

    /*
     * Метод отправляет на сервер запрос на регистрацию нового пользователя в облачное хранилище.
     *  login - логин пользователя
     *  first_name - имя пользователя
     *  last_name - фамилия пользователя
     *  password - пароль пользователя
     */
    public void demandRegistration(String login, String first_name, String last_name, String password) {
        //отправляем на сервер объект сообщения(команды)
        ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_REGISTRATION,
                new AuthMessage(login, first_name, last_name, password)));
    }

    /**
     * Метод отправляет на сервер запрос об отключении.
     */
    public void demandDisconnect() {
        //если соединение установлено
        if(ctx != null && !ctx.isRemoved()){
            //выводим сообщение в метку уведомлений
            showTextInController("Состояние соединения: отключение от сервера...");
            //отправляем на сервер объект сообщения(команды)
            ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_DISCONNECT,
                    new AuthMessage()));
        }
    }


    public Controller getController() {
        return controller;
    }


    /*
     * Метод выводит сообщение в нижнюю метку окна клиента
     * параметр text - сообщение
     */
    public void showTextInController(String text){
        //выводим сообщение в нижнюю метку
        controller.showTextInController(text);
    }

    public void disconnect() {
    }
}
