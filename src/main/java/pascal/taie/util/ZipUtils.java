package pascal.taie.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private ZipUtils() {
    }

    public static void uncompressZipFile(String zipFilePath, String destDir) throws IOException {
        File destDirFile = new File(destDir);
        if (!destDirFile.exists()) {
            destDirFile.mkdirs();
        }
        if(destDir.contains("commons-logging")){
            System.out.println("aaaaaaaaaaaa");
        }
        ZipFile zipFile = new ZipFile(zipFilePath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entry.isDirectory()) {
                File dir = new File(destDir + File.separator + entryName);
                dir.mkdirs();
                continue;
            }
            File file = new File(destDir + File.separator + entryName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            InputStream in = zipFile.getInputStream(entry);
            FileOutputStream out = new FileOutputStream(file);
            // copy stream from in to out
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.close();
            in.close();
        }
        zipFile.close();
    }

    public static void compressDirectory(Path dir,
                                         Path zipFilePath,
                                         boolean includedDirItself) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()));
        zipFileRecursively(dir.toFile(), includedDirItself ? dir.toFile().getName() : "", zipOut);
        zipOut.close();
    }

    /**
     * Zip specification has three undocumented quirks:
     * <ul>
     *     <li>Directory names must end with a '/' slash.</li>
     *     <li>Paths must use '/' slashes, not '\'.</li>
     *     <li>Entries may not begin with a '/' slash.</li>
     * </ul>
     * Some implementation details:
     * <ul>
     *     <li> {@link ZipOutputStream#closeEntry()} must
     *     follow {@link ZipOutputStream#putNextEntry(ZipEntry)}.</li>
     *     <li>First put folder and then its sub-files.</li>
     * </ul>
     */
    private static void zipFileRecursively(File fileToZip,
                                           String fileName,
                                           ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isDirectory()) {
            if (!fileName.isEmpty()) {
                fileName = fileName.endsWith("/") ? fileName : fileName + "/";
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            }
            for (File child : Objects.requireNonNull(fileToZip.listFiles())) {
                zipFileRecursively(child, fileName + child.getName(), zipOut);
            }
        } else if (fileToZip.isFile()) {
            zipOut.putNextEntry(new ZipEntry(fileName));
            Files.copy(fileToZip.toPath(), zipOut);
            zipOut.closeEntry();
        }
    }
}
