import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public class FileManager {
    //инициируем синглтон хендлера настроек
    private static final FileManager ownInstance = new FileManager();

    public static FileManager getInstance() {
        return ownInstance;
    }

    /**
     * Метод создает в корневой папке приложения(где разворачивается jar-файл)
     * копию файла из jar-архива
     *
     * @param originFilePathname - имя пути к файлу источника в jar-архиве в папке utils/
     * @param targetFilePathname - имя пути к файлу-копии в корневой папке приложения(где запускается jar-файл)
     */
    public void copyFileToRuntimeRoot(String originFilePathname, String targetFilePathname) {
        try (InputStream inputStream = getClass().getResourceAsStream(originFilePathname)) {
            Files.copy(inputStream, Paths.get(targetFilePathname), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
