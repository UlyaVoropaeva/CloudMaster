import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * Общий класс предназначен для операций с файловыми объектами
 */
public class FileUtils {
    //инициируем синглтон(объект класса)
    private static FileUtils ownInstance = new FileUtils();

    public static FileUtils getInstance() {
        return ownInstance;
    }

    //объявляем объект защелки
    private CountDownLatch countDownLatch;
    //инициируем контсанту названия алгоритма расчета контрольной суммы
    private final String algorithm = "SHA-512";


    /**
     * Метод читает данные из целого файла в заданной директории в объект файлового сообщения.
     *
     * @param realItemPath - объект реального пути к объекту элемента
     * @param fileMessage  - объект файлового сообщения
     * @return - результат чтения данных из файла
     */

    public boolean readFile(Path realItemPath, FileMessage fileMessage) {
        try {
            //инициируем локальную переменную контрольной суммы целого файла
            String fileChecksum = hashBytes(algorithm, Files.readAllBytes(realItemPath.toFile().toPath()));
            //сохраняем в объект сообщения контрольной суммы целого файла
            fileMessage.setFileChecksum(fileChecksum);
            //считываем данные из файла и записываем их в объект файлового сообщения
            fileMessage.readFileData(realItemPath.toString());
            //записываем размер файла для скачивания
            fileMessage.setFileSize(Files.size(realItemPath));
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Что то не так с файлом");
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * Метод отправки по частям большого файла размером более константы максмального размера фрагмента файла.
     *
     * @param toDirItem    - объект директории назначения
     * @param item         - объект элемента(исходный файл)
     * @param fullFileSize - размер целого файла в байтах
     * @param rootPath     - объект пути к корневой папке
     * @param ctx          - сетевое соединение
     * @param command      - конастанта типа команды
     */
    public void cutAndSendFileByFrags(FileInfo toDirItem, FileInfo item,
                                      long fullFileSize, Path rootPath,
                                      ChannelHandlerContext ctx, Commands command) {
        //запускаем в отдельном процессе, чтобы не тормозить основные процессы
        new Thread(() -> {
            try {
                //***разбиваем файл на фрагменты***
                //рассчитываем количество полных фрагментов файла
                int totalEntireFragsNumber = (int) (fullFileSize / FileFragmentMessage.CONST_FRAG_SIZE);
                //рассчитываем размер последнего фрагмента файла
                int finalFileFragmentSize = (int) (fullFileSize - FileFragmentMessage.CONST_FRAG_SIZE * totalEntireFragsNumber);
                //рассчитываем общее количество фрагментов файла
                //если есть последний фрагмент, добавляем 1 к количеству полных фрагментов файла
                int totalFragsNumber = (finalFileFragmentSize == 0) ?
                        totalEntireFragsNumber : totalEntireFragsNumber + 1;


                //устанавливаем начальные значения номера текущего фрагмента и стартового байта
                long startByte = 0;
                //инициируем байтовый массив для чтения данных для полных фрагментов
                byte[] data = new byte[FileFragmentMessage.CONST_FRAG_SIZE];
                //***в цикле создаем целые фрагменты, читаем в них данные и отправляем***
                for (int i = 1; i <= totalEntireFragsNumber; i++) {
                    //вызываем метод отправки сообщения
                    sendFileFragment(toDirItem, item, fullFileSize,
                            i, totalFragsNumber, FileFragmentMessage.CONST_FRAG_SIZE,
                            data, startByte, rootPath, ctx, command);
                    //инициируем защелку и ждем получения подтверждения получателя
                    countDownLatch = new CountDownLatch(1);
                    countDownLatch.await();
                    //увеличиваем указатель стартового байта на размер фрагмента
                    startByte += FileFragmentMessage.CONST_FRAG_SIZE;
                }

                //***отправляем последний фрагмент, если он есть***
                if (totalFragsNumber > totalEntireFragsNumber) {
                    //инициируем байтовый массив для чтения данных для последнего фрагмента
                    byte[] dataFinal = new byte[finalFileFragmentSize];
                    //вызываем метод отправки сообщения
                    sendFileFragment(toDirItem, item, fullFileSize,
                            totalFragsNumber, totalFragsNumber, finalFileFragmentSize,
                            dataFinal, startByte, rootPath, ctx, command);

                    try {
                        //инициируем объект фрагмента файлового сообщения
                        FileFragmentMessage fileFragmentMessage = new FileFragmentMessage(
                                toDirItem, item, fullFileSize, totalFragsNumber,
                                totalFragsNumber, finalFileFragmentSize, data);
                        //читаем данные во фрагмент с определенного места файла
                        fileFragmentMessage.readFileDataToFragment(
                                getRealPath(item.getFullFilename(), rootPath).toString(),
                                startByte);
                        //вычисляем и сохраняем в объект сообщения контрольную сумму
                        // байтового массива фрагмента файла
                        fileFragmentMessage.setFragChecksum(
                                hashBytes(fileFragmentMessage.getData()));
                        //отправляем на сервер объект сообщения(команды)

                        ctx.writeAndFlush(new CommandMessage(command, fileFragmentMessage));
                    } catch (NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    /**
     * Метод отправки объекта сообщения с объектом фрагментом файла.
     *
     * @param toDirItem        - объект директории назначения
     * @param item             - объект элемента(исходный файл)
     * @param fullFileSize     - размер целого файла в байтах
     * @param fragNumber       - номер фрагмента
     * @param totalFragsNumber - общее количество фрагментов
     * @param fileFragSize     - размер фрагмента в байтах
     * @param data             - байтовый массив с данными фрагмента файла
     * @param startByte        - индекс начального байта фрагмента в целом файле
     * @param rootPath         - объект пути к корневой папке
     * @param ctx              - сетевое соединение
     * @param command          - конастанта типа команды
     */
    public void sendFileFragment(FileInfo toDirItem, FileInfo item, long fullFileSize,
                                 int fragNumber, int totalFragsNumber, int fileFragSize,
                                 byte[] data, long startByte, Path rootPath,
                                 ChannelHandlerContext ctx, Commands command) {
        try {
            //инициируем объект фрагмента файлового сообщения
            FileFragmentMessage fileFragmentMessage = new FileFragmentMessage(
                    toDirItem, item, fullFileSize, fragNumber,
                    totalFragsNumber, fileFragSize, data);
            //читаем данные во фрагмент с определенного места файла
            fileFragmentMessage.readFileDataToFragment(
                    getRealPath(item.getFullFilename(), rootPath).toString(),
                    startByte);
            //вычисляем и сохраняем в объект сообщения контрольную сумму
            // байтового массива фрагмента файла
            fileFragmentMessage.setFragChecksum(
                    hashBytes(fileFragmentMessage.getData()));
            //отправляем на сервер объект сообщения(команды)

            ctx.writeAndFlush(new CommandMessage(command, fileFragmentMessage));
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Метод сохраняет данные из полученного байтового массива в целый файл.
     *
     * @param fileMessage  - объект файлового сообщения
     * @param realItemPath - объект реального пути к объекту элемента
     * @return - результат сохранения данных из байтового массива в файл
     */
    public boolean saveFile(FileMessage fileMessage, Path realItemPath) {
        try {
            //создаем новый файл и записываем в него данные из объекта файлового сообщения
            Files.write(realItemPath, fileMessage.getData(), StandardOpenOption.CREATE);
            //если длина сохраненного файла отличается от длины принятого файла
            if (Files.size(realItemPath) != fileMessage.getFileSize()) {
                System.out.println("Неверный размер сохраненного файла");

                return false;
                //если контрольная сумма сохраненного файла отличается от исходной контрольной суммы
            } else if (!fileMessage.getFileChecksum().
                    equals(hashBytes(algorithm, Files.readAllBytes(realItemPath.toFile().toPath())))) {
                System.out.println("Неправильная контрольная сумма сохраненного файла");
                return false;

            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Что то не так с файлом!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод создает временную директорию, если нет, создает в ней временные файлы-фрагменты,
     * куда сохраняет данные из сообщения фрагмента файла.
     *
     * @param realToTempDirPath - объект пути к временной папке для файлов-фрагментов
     * @param realToFragPath    - объект пути к файлу-фрагменту
     * @param fileFragMsg       - объект сообщения фрагмента файла
     * @return результат сохранения файла-фрагмента
     */
    public boolean saveFileFragment(Path realToTempDirPath, Path realToFragPath,
                                    FileFragmentMessage fileFragMsg) {
        try {
            //если текущий фрагмент первый
            if (fileFragMsg.getCurrentFragNumber() == 1) {
                //инициируем объект временной директории
                File dir = new File(realToTempDirPath.toString());
                //если временная директория уже существует(возможно не пустая)
                if (dir.exists()) {
                    //то предварительно удаляем
                    deleteFolder(dir);
                }
            }
            //создаем новый файл-фрагмент и записываем в него данные из объекта файлового сообщения
            Files.write(realToFragPath, fileFragMsg.getData(), StandardOpenOption.CREATE);
            //если длина сохраненного файла-фрагмента отличается от длины принятого фрагмента файла
            if (Files.size(realToFragPath) != fileFragMsg.getFileFragmentSize()) {
                System.out.println("Направильно сохранен фрагмент файла");
                return false;
                //если контрольная сумма сохраненного файла-фрагмента отличается от исходной контрольной суммы
            } else if (!fileFragMsg.getFragChecksum().
                    equals(hashBytes(algorithm, Files.readAllBytes(realToFragPath.toFile().toPath())))) {
                System.out.println("Неправильная контрольная сумма сохраненного файла");
                return false;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Что то не так с файлом!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод собирает целый файл из файлов-фрагментов, сохраненных во временной папке,
     * сохраняет его в директорию назначения и удаляет временную папку с файлами-фрагментами
     *
     * @param realToTempDirPath - объект реального пути к временной папка для файлов-фрагментов
     * @param realToFilePath    - объект реального пути к итоговому файлу
     * @param fileFragMsg       - объект файлового сообщения
     * @return результат процесса сборки целого файла из файлов-фрагментов
     */
    public boolean compileFileFragments(Path realToTempDirPath, Path realToFilePath,
                                        FileFragmentMessage fileFragMsg) {

        try {
            //инициируем файловый объект для временной папки
            File tempDirFileObject = new File(realToTempDirPath.toString());
            //инициируем массив файлов-фрагментов во временной папке
            File[] fragFiles = tempDirFileObject.listFiles();
            //если количество файлов-фрагментов не совпадает с требуемым
            if (fragFiles == null ||
                    fragFiles.length != fileFragMsg.getTotalFragsNumber()) {
                return false;
            }
            //переписываем данные из канала-источника в канал-назначения данные
            // из файлов-фрагментов в итоговый файл
            transferDataFromFragsToFinalFile(realToFilePath, fragFiles);
            //если длина сохраненного файла отличается от длины полного исходного файла
            if (Files.size(realToFilePath) != fileFragMsg.getFullFileSize()) {
                return false;
                //если файл собран без ошибок
            } else {
                //***удаляем временную папку***
                if (!deleteFolder(tempDirFileObject)) {
                    return false;
                }
            }
        } catch (IOException e) {
            System.out.println("Что то не так с файлом!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод переписывает данные из канала-источника в канал-назначения данные
     * из файлов-фрагментов в итоговый файл.
     *
     * @param realToFilePath - объект пеального пути к итоговому файлу
     * @param fragFiles      - массив файлов-фрагментов
     * @throws IOException - исключение
     */
    private void transferDataFromFragsToFinalFile(Path realToFilePath,
                                                  File[] fragFiles) throws IOException {
        //удаляем файл, если уже существует
        Files.deleteIfExists(realToFilePath);
        //создаем новый файл для сборки загруженных фрагментов файла
        File finalFile = new File(realToFilePath.toString());
        //инициируем выходной поток и канал для записи данных в итоговый файл
        RandomAccessFile toFileRAF = new RandomAccessFile(finalFile, "rw");
        FileChannel toChannel = toFileRAF.getChannel();
        //в цикле листаем временную папку и добавляем в файл данные из файлов-фрагментов
        for (File fragFile : fragFiles) {
            //инициируем входной поток и канал для чтения данных из файла-фрагмента
            FileInputStream fromFileInStream = new FileInputStream(fragFile);
            FileChannel fromChannel = fromFileInStream.getChannel();
            //переписываем данные через каналы
            fromChannel.transferTo(0, fragFile.length(), toChannel);
            //закрываем входные потоки и каналы
            fromFileInStream.close();
            fromChannel.close();
        }
        //закрываем выходные потоки и каналы
        toFileRAF.close();
        toChannel.close();
    }

    /**
     * Метод удаляет файловый объект.
     *
     * @param fileObject - файловый объект
     * @return true - удаление прошло успешно
     */
    public boolean deleteFileObject(File fileObject) {
        boolean result;
        //если это директория
        if (fileObject.isDirectory()) {
            //очищаем и удаляем папку
            result = deleteFolder(fileObject);
        } else {
            //удаляем файл
            result = fileObject.delete();
        }
        return result;
    }

    /**
     * Метод удаляет заданную папку и все объекты в ней.
     *
     * @param folder - файловый объект заданной папки
     * @return true - удалена папка и все объекты в ней
     */
    private boolean deleteFolder(File folder) {
        //если папка недоступна, выходим с false
        if (folder.listFiles() == null) {
            System.out.println("Неправильно сохранненый фрагмент файла");
            return false;
        }
        //в цикле листаем временную папку и удаляем все файлы-фрагменты
        for (File f : Objects.requireNonNull(folder.listFiles())) {
            //если это директория
            if (f.isDirectory()) {
                //очищаем и удаляем папку
                deleteFolder(f);
            } else {
                //удаляем файл
                System.out.println("удаляем файл " + f.delete());
            }
        }
        //теперь можем удалить пустую папку
        return Objects.requireNonNull(folder.listFiles()).length == 0 && folder.delete();
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
     * Перегруженный метод генеририрует строку хэша для байтового массива,
     * используя алгоритм по умолчанию.
     *
     * @param bytes - заданный байтовый массив
     * @return - строку хэша для байтового массива
     */
    public String hashBytes(byte[] bytes) throws NoSuchAlgorithmException {
        //инициируем объект MessageDigest
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        //расчитываем хэш для байтового массива
        messageDigest.update(bytes);
        //форматируем строку хэша
        String fx = "%0" + messageDigest.getDigestLength() + "x";
        return String.format(fx, new BigInteger(1, messageDigest.digest()));
    }

    /**
     * Перегруженный метод генеририрует строку хэша для байтового массива,
     * используя заданный алгоритм.
     *
     * @param algorithm - заданный алгоритм вычисления хэша
     * @param bytes     - заданный байтовый массив
     * @return - строку хэша для байтового массива
     */
    public String hashBytes(String algorithm, byte[] bytes) throws NoSuchAlgorithmException {
        //инициируем объект MessageDigest
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        //расчитываем хэш для байтового массива
        messageDigest.update(bytes);
        //форматируем строку хэша
        String fx = "%0" + messageDigest.getDigestLength() + "x";
        return String.format(fx, new BigInteger(1, messageDigest.digest()));
    }

    /**
     * Метод создает файловый объект новой папки.
     *
     * @param realDirPathname - строка пути к новой папке
     * @return - результат создания файлового объекта новой папки
     */
    public boolean createNewFolder(String realDirPathname) {
        //инициируем новый файловый объект
        File dir = new File(realDirPathname);
        //если такая папке уже существует
        if (dir.exists()) {
            //выходим с false
            System.out.println("CloudStorageServer.createNewFolder() - A folder with this name exists.");
            return false;
        }
        //возвращаем результат создания новой папки
        return dir.mkdir();
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }
}
