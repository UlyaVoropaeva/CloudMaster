import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;


public class GMClient {

    //принимаем объект хендлера для операций с директориями
    private Controller controller;
    //принимаем объект соединения
    private ChannelHandlerContext ctx;
    //объявляем переменную IP адреса сервера
    private static String IP_ADDR;
    //объявляем переменную порта соединения
    private static int PORT = 8189;
    //инициируем переменную объект пути к корневой директории для списка в клиентской части GUI

    //объявляем переменную объект пути к корневой директории для списка в клиентской части
    public static Path CLIENT_ROOT_PATH = Paths.get("12");
    //инициируем константу размера фрагментов файла в байтах
    public static final int CONST_FRAG_SIZE = 1024 * 1024 * 10;
    //объявляем объект защелки
    private CountDownLatch countDownLatch;
    //объявляем объект файлового обработчика
    private FileUtils fileUtils = FileUtils.getInstance();


    public GMClient(Controller controller) {
        //принимаем объект контроллера
        this.controller = controller;
    }


    public void setCtx(ChannelHandlerContext ctx) {
        //ctx - объект сетевого соединения
        this.ctx = ctx;
    }

    /**
     * Метод начала работы клиента сетевого хранилища.
     */
    public void run() throws Exception {
        //инициируем объект соединения
        new NettyClient(this, PORT).run();
    }

    /**
     * Метод отправляет на сервер запрос на регистрацию нового пользователя в облачное хранилище.
     * login - логин пользователя
     * first_name - имя пользователя
     * last_name - фамилия пользователя
     * password - пароль пользователя
     */
    public void demandRegistration(String login, String first_name, String last_name, String password) {
        //отправляем на сервер объект сообщения(команды)
        ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_REGISTRATION,
                new AuthMessage(login, first_name, last_name, password)));
    }

    /**
     * Метод отправляет на сервер запрос на загрузку объекта элемента( только файла)
     * из клиента в облачное хранилище.
     * @param storageToDirItem - объект директории назначения в сетевом хранилище
     * @param clientItem       - объект элемента списка(файла) на клиенте
     */
    public void demandUploadItem(FileInfo storageToDirItem, FileInfo clientItem) throws IOException {
        //если объект элемента - это директория
        if (clientItem.getType() == FileInfo.FileType.DIRECTORY) {
            //выводим сообщение в нижнюю метку
            showTextInController("Директория (каталог) загружать не разрешается");
            return;
        }
        //инициируем объект реального пути к объекту элемента в клиенте
        Path realClientItemPath = getRealPath(clientItem.getFullFilename(),CLIENT_ROOT_PATH);

        //вычисляем размер файла
        long fileSize = Files.size(realClientItemPath);
        //если размер файла больше константы размера фрагмента
        if (fileSize > 1024) {
            //запускаем метод отправки файла по частям
          uploadFileByFrags(storageToDirItem, clientItem, fileSize);
            //если файл меньше
        } else {
            //запускаем метод отправки целого файла
            uploadEntireFile(storageToDirItem, clientItem, fileSize);
        }
    }

    /**
     * Метод выводит сообщение в нижнюю метку окна клиента
     * параметр text - сообщение
     */
    void showTextInController(String s) {
        controller.showTextInController(s);
    }
    

    /**
     * Метод возвращает реальный путь к объекту элемента.
     * @param itemPathname - строка относительного пути к объекту элемента
     * @param rootPath - объект пути к реальной корневой директории
     * @return - реальный путь к объекту элемента
     */
    public Path getRealPath(String itemPathname, Path rootPath) {
        //возвращаем объект реального пути к заданому объекту элемента списка
        return Paths.get(rootPath.toString(), itemPathname);
    }

    /**
     * Метод-прокладка запускает процесс нарезки и отправки клиенту по частям большого файла
     * размером более константы максимального размера фрагмента файла.
     *
     * @param storageToDirItem - объект директори назначения в сетевом хранилище
     * @param clientItem       - объект элемента в клиенте
     * @param fullFileSize     - размер целого файла в байтах
     */
    private void uploadFileByFrags(FileInfo storageToDirItem, FileInfo clientItem, long fullFileSize) {
        //выводим сообщение
        showTextInController("File uploading. Cutting into fragments...");
        fileUtils.cutAndSendFileByFrags(storageToDirItem, clientItem, fullFileSize,
                CLIENT_ROOT_PATH, ctx, Commands.REQUEST_SERVER_UPLOAD_FILE_FRAG);

        //запускаем в отдельном процессе, чтобы не тормозить основные процессы
        new Thread(() -> {
            try {
                //***разбиваем файл на фрагменты***
                //рассчитываем количество полных фрагментов файла
                //Приводимое выражение должно быть в скобках!
                // Иначе сначала fullFileSize(long) приводится к int, что приводит к ошибке при больших чем int значениях.
                int totalEntireFragsNumber = (int) (fullFileSize / CONST_FRAG_SIZE);
                //рассчитываем размер последнего фрагмента файла
                int finalFileFragmentSize = (int) (fullFileSize - CONST_FRAG_SIZE * totalEntireFragsNumber);
                //рассчитываем общее количество фрагментов файла
                //если есть последний фрагмент, добавляем 1 к количеству полных фрагментов файла
                int totalFragsNumber = (finalFileFragmentSize == 0) ?
                        totalEntireFragsNumber : totalEntireFragsNumber + 1;
                //устанавливаем начальные значения номера текущего фрагмента и стартового байта
                long startByte = 0;
                //инициируем байтовый массив для чтения данных для полных фрагментов
                byte[] data = new byte[CONST_FRAG_SIZE];
                //***в цикле создаем целые фрагменты, читаем в них данные и отправляем***
                for (int i = 1; i <= totalEntireFragsNumber; i++) {
                    //вызываем метод отправки сообщения
                    sendFileFragment(storageToDirItem, clientItem, fullFileSize,
                            i, totalFragsNumber, CONST_FRAG_SIZE,
                            data, startByte, CLIENT_ROOT_PATH, ctx, Commands.REQUEST_SERVER_UPLOAD_FILE_FRAG);
                    //инициируем защелку и ждем получения подтверждения получателя
                    countDownLatch = new CountDownLatch(1);
                    countDownLatch.await();
                    //увеличиваем указатель стартового байта на размер фрагмента
                    startByte += CONST_FRAG_SIZE;
                }
                //***отправляем последний фрагмент, если он есть***
                if (totalFragsNumber > totalEntireFragsNumber) {
                    //инициируем байтовый массив для чтения данных для последнего фрагмента
                    byte[] dataFinal = new byte[finalFileFragmentSize];
                    //вызываем метод отправки сообщения
                    sendFileFragment(storageToDirItem, clientItem, fullFileSize,
                            totalFragsNumber, totalFragsNumber, finalFileFragmentSize,
                            dataFinal, startByte, CLIENT_ROOT_PATH, ctx, Commands.REQUEST_SERVER_UPLOAD_FILE_FRAG);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Метод отправки объекта сообщения с объектом фрагментом файла.
     *  @param toDirItem        - объект директории назначения
     * @param clientItem             - объект элемента(исходный файл)
     * @param fullFileSize     - размер целого файла в байтах
     * @param fragNumber       - номер фрагмента
     * @param totalFragsNumber - общее количество фрагментов
     * @param fileFragSize     - размер фрагмента в байтах
     * @param data             - байтовый массив с данными фрагмента файла
     * @param startByte        - индекс начального байта фрагмента в целом файле
     * @param clientRootPath
     * @param ctx              - сетевое соединение
     * @param command          - конастанта типа команды
     */
    public void sendFileFragment(FileInfo toDirItem, FileInfo clientItem, long fullFileSize,
                                 int fragNumber, int totalFragsNumber, int fileFragSize,
                                 byte[] data, long startByte,
                                 Path clientRootPath, ChannelHandlerContext ctx, Commands command) {
        try {
            //инициируем объект реального пути к объекту элемента в клиенте
            Path realClientItemPath = Paths.get(clientItem.getFullFilename());

            RandomAccessFile raf = new RandomAccessFile(realClientItemPath.toString(), "r");
            //инициируем объект входного буферезированного потока с преобразованием raf в поток
            BufferedInputStream bis = new BufferedInputStream(Channels.newInputStream(raf.getChannel()));
            // ставим указатель на нужный вам символ
            raf.seek(startByte);
            //вычитываем данные из файла
            //считывает байты данных длиной до b.length из этого файла в массив байтов.
            bis.read(data);
            //выгружаем потоки из памяти
            raf.close();
            bis.close();

            //отправляем на сервер объект сообщения(команды)
            ctx.writeAndFlush(new CommandMessage(command, new AuthMessage(data)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод отправки целого файла размером менее константы максмального размера фрагмента файла.
     *
     * @param storageToDirItem - объект директории назначения в сетевом хранилище
     * @param clientItem       - объект элемента списка(файла) на клиенте
     * @param fileSize         - размер файла в байтах
*/
    private void uploadEntireFile(FileInfo storageToDirItem, FileInfo clientItem, long fileSize) {
        //инициируем объект файлового сообщения
        FileMessage fileMessage = new FileMessage(storageToDirItem,
                clientItem, fileSize);
        //читаем файл и записываем данные в байтовый массив объекта файлового сообщения
        //если скачивание прошло удачно
        if(fileUtils.readFile(getRealPath(clientItem.getFullFilename(), CLIENT_ROOT_PATH),
                fileMessage)){
            //отправляем на сервер объект сообщения(команды)
            ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_UPLOAD_ITEM,
                    fileMessage));
            //если что-то пошло не так
        } else {

            //выводим сообщение в нижнюю метку GUI
            showTextInController("что-то пошло не так");
        }
    }


    /**
     * Метод отправляет на сервер запрос об отключении.
     */
    public void demandDisconnect() {
        //если соединение установлено
        if (ctx != null && !ctx.isRemoved()) {
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


    /**
     * Метод-прокладка запускает процесс отправки отдельного фрагмента файла в сетевое хранилище.
     * @param fileFragMsg - объект сообщения фрагмента файла из объекта сообщения(команды)
     * @param command - переменная типа команды
     */
    public void sendFileFragment(FileFragmentMessage fileFragMsg, Commands command) {
        //инициируем новый байтовый массив
        byte[] data = new byte[fileFragMsg.getFileFragmentSize()];
        //вычисляем индекс стартового байта фрагмента в целом файле
        long startByte = FileFragmentMessage.CONST_FRAG_SIZE * fileFragMsg.getCurrentFragNumber();
        //вызываем метод отправки объекта сообщения с новым байтовым массивом данных фрагмента
        sendFileFragment(fileFragMsg.getToDirectoryItem(), fileFragMsg.getItem(),
                fileFragMsg.getFullFileSize(), fileFragMsg.getCurrentFragNumber(),
                fileFragMsg.getTotalFragsNumber(), fileFragMsg.getFileFragmentSize(),
                data, startByte, CLIENT_ROOT_PATH, ctx, command);
    }


    /**
     * Метод инициирует процесс настройки серверного приложения.
     */
    public void initConfiguration() {

    }
    /**
     * Метод закрывает соединение с сервером и устанавливает режим отображения GUI "Отсоединен".
     */
    public void disconnect() {
        //если соединение установлено
        if(ctx != null && !ctx.isRemoved()){
            //закрываем соединение
            ctx.close();
        }
        //устанавливаем режим отображения "Отсоединен"
       controller.setDisconnectedMode(true);
    }
}
