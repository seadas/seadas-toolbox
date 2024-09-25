package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.net.*;

public class FileDownloader {
    public static void downloadFile(String fileURL, String saveDir) throws IOException {
        URL url = new URL(fileURL);
        Path targetPath = new File(saveDir + "/" + Paths.get(url.getPath()).getFileName().toString()).toPath();
        Files.copy(url.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
