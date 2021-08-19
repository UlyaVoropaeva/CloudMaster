import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
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
    private static int PORT;
    //объявляем переменную объект пути к корневой директории для списка в клиентской части GUI
    public static Path CLIENT_ROOT_PATH;
    //инициируем константу размера фрагментов файла в байтах
    public static final int CONST_FRAG_SIZE = 1024 * 1024 * 10;
    //объявляем объект защелки
    private CountDownLatch countDownLatch;
    //объявляем объект файлового обработчика
    private FileUtils fileUtils = FileUtils.getInstance();
    private final PropertiesClientHandler propertiesClientHandler = PropertiesClientHandler.getOwnObject();


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
     * Метод отправляет на сервер запрос на авторизацию пользователя в облачное хранилище.
     *
     * @param login    - логин пользователя
     * @param password - пароль пользователя
     */
    public void demandAuthorization(String login, String password) {
        //отправляем на сервер объект сообщения(команды)
        ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_AUTH,
                new AuthMessage(login, password)));
    }

    /**
     * Метод отправляет на сервер запрос на загрузку объекта элемента( только файла)
     * из клиента в облачное хранилище.
     *
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
        Path realClientItemPath = getRealPath(clientItem.getFullFilename(), CLIENT_ROOT_PATH);

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
     * Метод возвращает реальный путь к объекту элемента.
     *
     * @param itemPathname - строка относительного пути к объекту элемента
     * @param rootPath     - объект пути к реальной корневой директории
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
        showTextInController("Загрузка файла...");
        fileUtils.cutAndSendFileByFrags(storageToDirItem, clientItem, fullFileSize,
                CLIENT_ROOT_PATH, ctx, Commands.REQUEST_SERVER_UPLOAD_FILE_FRAG);
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
        if (fileUtils.readFile(getRealPath(clientItem.getFullFilename(), CLIENT_ROOT_PATH),
                fileMessage)) {
            //отправляем на сервер объект сообщения(команды)
            ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_UPLOAD_ITEM,
                    fileMessage));
            //если что-то пошло не так
        } else {

            //выводим сообщение в нижнюю метку GUI
            showTextInController("что-то пошло не так");
        }
    }


    public Controller getController() {
        return controller;
    }


    /**
     * Метод-прокладка запускает процесс отправки отдельного фрагмента файла в сетевое хранилище.
     *
     * @param fileFragMsg - объект сообщения фрагмента файла из объекта сообщения(команды)
     * @param command     - переменная типа команды
     */
    public void sendFileFragmentMsg(FileFragmentMessage fileFragMsg, Commands command) {
        //инициируем новый байтовый массив
        byte[] data = new byte[fileFragMsg.getFileFragmentSize()];
        //вычисляем индекс стартового байта фрагмента в целом файле
        long startByte = FileFragmentMessage.CONST_FRAG_SIZE * fileFragMsg.getCurrentFragNumber();
        //вызываем метод отправки объекта сообщения с новым байтовым массивом данных фрагмента
        fileUtils.sendFileFragment(fileFragMsg.getToDirectoryItem(), fileFragMsg.getItem(),
                fileFragMsg.getFullFileSize(), fileFragMsg.getCurrentFragNumber(),
                fileFragMsg.getTotalFragsNumber(), fileFragMsg.getFileFragmentSize(),
                data, startByte, CLIENT_ROOT_PATH, ctx, command);
    }


    /**
     * Метод инициирует процесс настройки серверного приложения.
     */
    public void initConfiguration() {

        ///запускаем процесс применения конфигурации приложения
        propertiesClientHandler.setConfiguration();
        //инициируем переменную IP адреса сервера
        String ip_addr = propertiesClientHandler.getProperty("IP_ADDR");
        //если пользователем задано другое значение IP адреса
        if (!ip_addr.isEmpty()) {
            //применяем значение пользователя
            IP_ADDR = ip_addr;
        } else {
            //в противном случае применяем дефорлтное значение IP адреса
            IP_ADDR = propertiesClientHandler.getProperty("IP_ADDR_DEFAULT");
        }
        //выводим в лог значение ip-адреса сервера
        System.out.println("CloudStorageClient.initConfiguration() - IP_ADDR: " + IP_ADDR);

        //инициируем переменную порта соединения
        String port = propertiesClientHandler.getProperty("PORT");
        //если пользователем задано другое значение порта
        if (!port.isEmpty()) {
            //применяем значение пользователя
            PORT = Integer.parseInt(port);
        } else {
            //в противном случае применяем дефорлтное значение порта
            PORT = Integer.parseInt(propertiesClientHandler.getProperty("PORT_DEFAULT"));
        }
        //выводим в лог значение порта сервера
        System.out.println("CloudStorageClient.initConfiguration() - PORT: " + PORT);

        //инициируем переменную объект пути к корневой директории для списка в клиентской части GUI
        String root_absolute = propertiesClientHandler.getProperty("Root_absolute");
        //если поле свойства не пустое и путь реально существует(например, usb-флешка вставлена)
        if (!root_absolute.isEmpty() && Files.exists(Paths.get(root_absolute))) {
            //применяем значение пользователя
            CLIENT_ROOT_PATH = Paths.get(root_absolute);
        } else {
            //в противном случае применяем дефорлтное значение пути к корневой директории
            CLIENT_ROOT_PATH = Paths.get(propertiesClientHandler.getProperty("Root_default"));

            try {
                //создаем новую клиентскую директорию, если еще не создана
                Files.createDirectory(CLIENT_ROOT_PATH);
            } catch (IOException e) {
                System.out.println("[client]CloudStorageClient.initConfiguration() - " +
                        "Something wrong with new client root directory creating. Probably it does already exist!");
            }
        }
        //выводим в лог значение корневой директории клиента
        System.out.println("CloudStorageClient.initConfiguration() - CLIENT_ROOT_PATH: " + CLIENT_ROOT_PATH);
    }


    /**
     * Метод выводит сообщение в нижнюю метку окна клиента
     * параметр text - сообщение
     */
    void showTextInController(String s) {
        controller.showTextInController(s);
    }

    /**
     * Метод отправляет на сервер запрос на скачивание объекта элемента из облачного хранилища.
     *
     * @param storageFromDirItem - объект директории источника в сетевом хранилище
     * @param clientToDirItem    - объект директории назначения в клиенте
     * @param storageItem        - объект объекта элемента(источника) в сетевом хранилище
     */
    public void demandDownloadItem(FileInfo storageFromDirItem, FileInfo clientToDirItem, FileInfo storageItem) {
        //инициируем объект файлового сообщения
        FileMessage fileMessage = new FileMessage(storageFromDirItem, clientToDirItem, storageItem);
        //отправляем на сервер объект сообщения(команды)
        ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_DOWNLOAD_ITEM,
                fileMessage));
    }

    public FileUtils getFileUtils() {
        return fileUtils;
    }

    /**
     * Метод запускает процесс сохранения полученного от сервера объекта(файла)
     * в заданную директорию в клиенте.
     *
     * @param fileMessage - объект фалового сообщения
     * @return - результат сохранения объекта
     */
    public boolean downloadItem(FileMessage fileMessage) {
        //инициируем локальную переменную объекта директории назначения в клиенте
        FileInfo clientToDirItem = fileMessage.getClientDirectoryItem();
        //инициируем строку имени реального пути к папке с объектом элемента
        String realDirPathname = getRealPath(clientToDirItem.getFullFilename(), CLIENT_ROOT_PATH).toString();
        //инициируем новый объект пути к объекту
        Path realNewToItemPath = Paths.get(realDirPathname, fileMessage.getFileInfo().getFileName());
        return fileUtils.saveFile(fileMessage, realNewToItemPath);
    }

    /**
     * Метод запускает процесс сохранения файла-фрагмента из полученного байтового массива
     * во временной директории в клиенте.
     *
     * @param fileFragMsg - объект файлового сообщения
     * @return результат процесс сохранения файла-фрагмента из полученного байтового массива
     */

    public boolean downloadItemFragment(FileFragmentMessage fileFragMsg) {
        //инициируем реальный путь к временной папке для файлов-фрагментов
        Path realToTempDirPath = getRealPath(
                Paths.get(
                        fileFragMsg.getToDirectoryItem().getFullFilename(),
                        fileFragMsg.getToTempDirName()).toString(),
                CLIENT_ROOT_PATH);
        //инициируем реальный путь к файлу-фрагменту
        Path realToFragPath = Paths.get(
                realToTempDirPath.toString(), fileFragMsg.getFragName());
        //если сохранение полученного фрагмента файла во временную папку сетевого хранилища прошло удачно
        return fileUtils.saveFileFragment(realToTempDirPath, realToFragPath, fileFragMsg);

    }

    /**
     * Метод запускает процесс сборки целого файла из файлов-фрагментов.
     *
     * @param fileFragMsg - объект файлового сообщения
     * @return результат процесса сборки целого файла из файлов-фрагментов
     */

    public boolean compileItemFragments(FileFragmentMessage fileFragMsg) {
        //инициируем реальный путь к временной папке для файлов-фрагментов
        Path realToTempDirPath = getRealPath(
                Paths.get(
                        fileFragMsg.getToDirectoryItem().getFullFilename(),
                        fileFragMsg.getToTempDirName()).toString(),
                CLIENT_ROOT_PATH);
        //инициируем реальный путь к файлу-фрагменту
        Path realToFilePath = getRealPath(
                Paths.get(
                        fileFragMsg.getToDirectoryItem().getFullFilename(),
                        fileFragMsg.getItem().getFullFilename()).toString(),
                CLIENT_ROOT_PATH);
        //возвращаем результат процесса сборки целого объекта(файла) из файлов-фрагментов
        return fileUtils.compileFileFragments(realToTempDirPath, realToFilePath, fileFragMsg);

    }

    /**
     * Метод отправляет на сервер запрос об отключении.
     */
    public void demandDisconnect() {
        //если соединение установлено
        if (ctx != null && !ctx.isRemoved()) {
            //выводим сообщение в метку уведомлений
            showTextInController("Disconnecting the Cloud Storage server...");
            //отправляем на сервер объект сообщения(команды)
            ctx.writeAndFlush(new CommandMessage(Commands.REQUEST_SERVER_DISCONNECT,
                    new AuthMessage()));
        }
    }

    /**
     * Метод закрывает соединение с сервером и устанавливает режим отображения GUI "Отсоединен".
     */
    public void disconnect() {
        //если соединение установлено
        if (ctx != null && !ctx.isRemoved()) {
            //закрываем соединение
            ctx.close();
        }
        //устанавливаем режим отображения GUI "Отсоединен"
        controller.setDisconnectedMode(true);
    }
}
