import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileMessage extends AbstractMessage {
    //объявляем переменную размера файла(в байтах)
    private long fileSize;
    //объявляем байтовый массив с данными из файла
    private byte[] data;
    //принимаем объект родительской директории элемента в клиенте
    private FileInfo clientDirectoryItem;
    //принимаем объект родительской директории элемента в сетевом хранилище
    private FileInfo storageDirectoryItem;
    //принимаем объект элемента
    private FileInfo item;
    //принимаем переменную нового имени файла
    private String newName;
    //объявляем переменную контрольной суммы целого файла
    private String fileChecksum;

    //Конструкктор предназначен для загрузки всей операции с файлами
    public FileMessage(FileInfo storageDirectoryItem, FileInfo item, long fileSize) {
        this.storageDirectoryItem = storageDirectoryItem;
        this.item = item;
        this.fileSize = fileSize;
    }

    //Конструкктор предназначен для скачивания
    public FileMessage(FileInfo storageDirectoryItem, FileInfo clientDirectoryItem, FileInfo item) {
        this.storageDirectoryItem = storageDirectoryItem;
        this.clientDirectoryItem = clientDirectoryItem;
        this.item = item;
    }

    //Конструкктор предназначен для скачивания
    public FileMessage(FileInfo storageDirectoryItem, FileInfo clientDirectoryItem, FileInfo item, long fileSize) {
        this.storageDirectoryItem = storageDirectoryItem;
        this.clientDirectoryItem = clientDirectoryItem;
        this.item = item;
        this.fileSize = fileSize;
    }


    /**
     * Метод заполняет массив байтами, считанными из файла
     *
     * @param itemPathname - строка имени пути к объекту
     * @throws IOException - исключение ввода вывода
     */
    public void readFileData(String itemPathname) throws IOException {
        //читаем все данные из файла побайтно в байтовый массив
        this.data = Files.readAllBytes(Paths.get(itemPathname));
    }

    public FileInfo getClientDirectoryItem() {
        return clientDirectoryItem;
    }

    public FileInfo getStorageDirectoryFileInfo() {
        return storageDirectoryItem;
    }

    public FileInfo getFileInfo() {
        return item;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getData() {
        return data;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }


}
