package pascal.taie.project;

import java.nio.file.attribute.FileTime;

public interface AnalysisFile {

    FileTime getTimeStamp();

    Resource getResource();

    /**
     * @return file name of this file (with extension name)
     */
    String getFileName();

}
