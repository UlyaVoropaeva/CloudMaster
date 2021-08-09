
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;


public class GMServer {
    //инициируем переменную для печати сообщений в консоль
    private final PrintStream log = System.out;
    //объявляем переменную порта сервера
    private int PORT =8189;
    private FileUtils fileUtils = new FileUtils();

    /**
     * Метод запускает приложение сервера.
     */
    public void run() throws Exception {

        //инициируем объект сетевого подключения
        new NettyServer(this, PORT).run();
    }
    /**
     * Метод запускает процесс сохранения полученного от клиента объекта(файла)
     * в заданную директорию в сетевом хранилище.
     * @param fileMessage - объект фалового сообщения
     * @param userStorageRoot - объект пути к корневой директории пользователя в сетевом хранилище
     * @return - результат сохранения объекта
     */
    public boolean uploadItem(FileMessage fileMessage, Path userStorageRoot){
        //инициируем локальную переменную объекта директории назначения в сетевом хранилище
        FileInfo storageToDirItem = fileMessage.getStorageDirectoryFileInfo();
        //инициируем новый объект пути к объекту
        Path realNewToItemPath = Paths.get(
                getRealPath(storageToDirItem.getFullFilename(), userStorageRoot).toString(),
                fileMessage.getFileInfo().getFileName());
        return fileUtils.saveFile(fileMessage, realNewToItemPath);
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
     * Метод запускает процесс сохранения файла-фрагмента из полученного байтового массива.
     * @param fileFragMsg - объект файлового сообщения
     * @param userStorageRoot - объект пути к корневой директории пользователя в сетевом хранилище
     * @return результат процесс сохранения файла-фрагмента из полученного байтового массива
     */
    public boolean uploadItemFragment(FileFragmentMessage fileFragMsg, Path userStorageRoot) {
        //инициируем реальный путь к временной папке для файлов-фрагментов
        Path realToTempDirPath = getRealPath(
                Paths.get(
                        fileFragMsg.getToDirectoryItem().getFullFilename(),
                        fileFragMsg.getToTempDirName()).toString(),
                userStorageRoot);
        //инициируем реальный путь к файлу-фрагменту
        Path realToFragPath = Paths.get(
                realToTempDirPath.toString(), fileFragMsg.getFragName());
        //если сохранение полученного фрагмента файла во временную папку сетевого хранилища прошло удачно
        return fileUtils.saveFileFragment(realToTempDirPath, realToFragPath, fileFragMsg);
    }

    /**
     * Метод запускает процесс сборки целого файла из файлов-фрагментов.
     * @param fileFragMsg - объект файлового сообщения
     * @param userStorageRoot - объект пути к корневой директории пользователя в сетевом хранилище
     * @return результат процесса сборки целого файла из файлов-фрагментов
     */
    public boolean compileItemFragments(FileFragmentMessage fileFragMsg, Path userStorageRoot) {
        //инициируем реальный путь к временной папке для файлов-фрагментов
        Path realToTempDirPath = getRealPath(
                Paths.get(
                        fileFragMsg.getToDirectoryItem().getFullFilename(),
                        fileFragMsg.getToTempDirName()).toString(),
                userStorageRoot);
        //инициируем реальный путь к файлу-фрагменту
        Path realToFilePath = getRealPath(
                Paths.get(
                        fileFragMsg.getToDirectoryItem().getFullFilename(),
                        fileFragMsg.getItem().getFileName()).toString(),
                userStorageRoot);
        //возвращаем результат процесса сборки целого объекта(файла) из файлов-фрагментов
        return fileUtils.compileFileFragments(realToTempDirPath, realToFilePath, fileFragMsg);
    }

    public void printMsg(String msg){
        log.append(msg).append("\n");
    }

}