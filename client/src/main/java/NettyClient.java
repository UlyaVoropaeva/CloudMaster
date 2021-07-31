
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class NettyClient {
    //принимаем объект исходящего хэндлера Gloud Master
    private GMClient storageClient;

    //принимаем константу порта соединения
    private final int PORT;

    public NettyClient(GMClient storageClient, int PORT) {
        this.storageClient = storageClient;
        this.PORT = PORT;
    }

    public void run() throws Exception {
        //инициируем пул потоков для обработки потоков данных
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //позволяет настроить клиента перед запуском
            Bootstrap b = new Bootstrap();
            //в параметрах workerGroup отвечает и за соединение и за обмен данными
            b.group(workerGroup)
                    //Указываем использование класса NioSocketChannel для создания канала после того,
                    //как установлено входящее соединение
                    .channel(NioSocketChannel.class);
                    //настраиваем опции для обрабатываемых каналов(клиентских соединений)
                    b.option(ChannelOption.SO_KEEPALIVE, true);
            //Указываем обработчики, которые будем использовать для открытого канала.
            //ChannelInitializer помогает пользователю сконфигурировать новый канал

            b.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel)/* throws Exception*/ {
                //наполняем трубу обработчиками сообщений(потоков данных)
                socketChannel.pipeline().addLast(
                    //десериализатор netty входящего потока байтов в объект сообщения
                    new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                    //сериализатор netty объекта сообщения в исходящих поток байтов
                    new ObjectEncoder(),
                    //инициируем объект входящего обработчика объектов сообщений(команд)
                    new NettyServerDecoder(storageClient)
                    );
                }
            });
            //устанавливаем подключение к серверу и начинаем принимать входящие сообщения
            ChannelFuture future;
            future = b.connect("localhost",PORT).sync();
            //если соединение установлено
            onConnectionReady(future);

            // закрываем соединение с сервером
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void onConnectionReady(ChannelFuture future) {
        printMsg("Waiting for the server answer...");
    }

    public void printMsg(String msg){
        storageClient.showTextInController(msg);
    }

}