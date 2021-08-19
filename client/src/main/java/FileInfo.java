import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.commons.io.FilenameUtils;

public class FileInfo {


    public enum FileType {
        FILE("F"), DIRECTORY("D");

        private String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    //полное имя файла
    private String fullfilename;
    //имя файла
    private String filename;
    //расширение
    private String extension;
    //тип файла
    private FileType type;
    //размер файла
    private long size;
    //Локальная дата и время последнего изменения
    private LocalDateTime lastModified;

    public String getFullFilename() {
        return fullfilename;
    }

    public String getFileName() {
        return filename;
    }

    public void setFullFilename(String fullfilename) {
        this.fullfilename = fullfilename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    public String getExtension() {
        return extension;
    }

    public void setExtension() {
        this.extension = extension;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }


    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    //Информация о файле
    public FileInfo(Path path) {
        try {
            this.fullfilename = getFileNameFull(path);
            this.filename = getFileNameRemoveExtension(path);
            this.extension = getExtensionNotFileName(path);
            this.size = Files.size(path);
            //isDirectory определяет, является ли файл или каталог, обозначенный абстрактным именем файла, каталогом или нет.
            //Если каталог, то размер -1L
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.size = -1L;
            }
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
        } catch (IOException e) {
            throw new RuntimeException("Нет данных о файле");
        }
    }

    //	Получаем полное имя файла и преобразуем в стринг
    private String getFileNameFull(Path path) {
        String fullFileName = path.getFileName().toString();
        if (FilenameUtils.getName(fullFileName) == null) {
            return null;
        }
        return FilenameUtils.getName(fullFileName);
        //Получает имя за вычетом пути из полного имени файла и проверяем
    }

    //получаем имя файла и  удаляем расширение из имени файла.
    public String getFileNameRemoveExtension(Path path) {
        String fullFileName = path.getFileName().toString();
        if (FilenameUtils.removeExtension(fullFileName) == null) {
            return null;
        }
        if (Files.isDirectory(path)) {
            return fullFileName;
        }
        return FilenameUtils.removeExtension(fullFileName);
    }

    //получаем расширение файла
    public String getExtensionNotFileName(Path path) {
        String fullFileName = path.getFileName().toString();
        if (Files.isDirectory(path)) {
            return "[ DIR ]";
        }
        return fullFileName;
    }

}
