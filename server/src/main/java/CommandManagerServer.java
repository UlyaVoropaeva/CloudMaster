
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


public class CommandManagerServer extends ChannelInboundHandlerAdapter {
    //принимаем объект соединения
    private ChannelHandlerContext ctx;
    //принимаем объект контроллера сетевого хранилища
    private final GMServer gmServer;

    //объявляем переменную типа команды
    private Commands command;

    public CommandManagerServer(GMServer gmServer) {
        this.gmServer = gmServer;

    }

    /**
     * Метот обрабатывает событие - установление соединения с клиентом.
     * По событию отправляет сообщение-уведомление клиенту.
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
     * @param commandMessage - объект сообщения(команды)
     */
    private void onDisconnectClientRequest(CommandMessage commandMessage) {
        //отправляем объект сообщения(команды) клиенту
        ctx.writeAndFlush(new CommandMessage(Commands.SERVER_RESPONSE_DISCONNECT_OK));

        //закрываем соединение с клиентом
        ctx.channel().close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void printMsg(String msg){
        gmServer.printMsg(msg);
    }
}