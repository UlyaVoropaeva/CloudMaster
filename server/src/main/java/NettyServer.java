
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


public class NettyServer {

    //принимаем объект контроллера сетевого хранилища
    private final GMServer gmServer;
    //принимаем константу порта
    private final int port;

    public NettyServer(GMServer gmServer, int port) {
        this.gmServer = gmServer;
        this.port = port;
    }

    public void run() throws Exception {
        //инициируем пул потоков для приема входящих подключений
        NioEventLoopGroup mainGroup = new NioEventLoopGroup(1);
        //инициируем пул потоков для обработки потоков данных от клиентов
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //позволяет настроить сервер перед запуском
            ServerBootstrap b = new ServerBootstrap();
            //в параметрах mainGroup - parentGroup (обработка соединения),
            // workerGroup - childGroup (обработка потоков от клиентов)
            b.group(mainGroup, workerGroup)
                    //Указываем использование класса NioServerSocketChannel для создания канала после того,
                    //как принято входящее соединение.
                    .channel(NioServerSocketChannel.class)
                    //Указываем обработчики, которые будем использовать для открытого канала .
                    //ChannelInitializer ( - принимает запросы) помогает сконфигурировать как будем обрабатывать входящие сообщения от клиента
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        //
                        protected void initChannel(NioSocketChannel nioSocketChannel) {
                            //прописываем набор "преобразователей"
                            nioSocketChannel.pipeline().addLast(
                                    //десериализатор netty входящего потока байтов в объект сообщения
                                    new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                    //сериализатор netty объекта сообщения в исходящии поток байтов
                                    new ObjectEncoder(),

                                    //входящий обработчик объектов-сообщений(команд) по управлению сетевым хранилищем
                                    new CommandManagerServer(gmServer)
                            );
                        }
                    })
                    //настраиваем опции для обрабатываемых каналов(клиентских соединений) на чтение и запись
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            //начинаем принимать входящие сообщения
            //работаем до последнего потока
            ChannelFuture future = b.bind(port).sync();
            //если соединение установлено
            onConnectionReady(future);

            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void onConnectionReady(ChannelFuture future) {
        printMsg("Сервер ждет нового клиента...");
    }

    public void printMsg(String msg) {
        gmServer.printMsg(msg);
    }

}

