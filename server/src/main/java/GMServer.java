import io.netty.channel.ChannelHandlerContext;



import java.io.PrintStream;


public class GMServer {
    //инициируем переменную для печати сообщений в консоль
    private final PrintStream log = System.out;
    //объявляем переменную порта сервера
    private int PORT =8189;

    /**
     * Метод запускает приложение сервера.
     */
    public void run() throws Exception {

        //инициируем объект сетевого подключения
        new NettyServer(this, PORT).run();
    }



    public void printMsg(String msg){
        log.append(msg).append("\n");
    }

}