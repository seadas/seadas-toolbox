package gov.nasa.gsfc.seadas.processing.common;

import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;

/**
 * Created by aabduraz on 8/21/15.
 */
public class FileInfoFinder {

    public static final String FILE_TYPE_VAR_NAME = "fileType";
    public static final String MISSION_NAME_VAR_NAME = "missionName";
    private String fileType;
    private String missionName;
    private String missionDirName;

    public FileInfoFinder(String fileName, OCSSW ocssw){
        if (fileName.contains(" ")) {
            SimpleDialogMessage dialog = new SimpleDialogMessage(null, "WARNING!! " +
                    " Make sure ifile name does not have a space in it");
            dialog.setVisible(true);
            dialog.setEnabled(true);
        } else {
            ocssw.findFileInfo(fileName, this);
            setMissionDirName(OCSSWInfo.getInstance().getOcsswDataDirPath());
        }
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public String getMissionDirName() {
        return missionDirName;
    }

    public void setMissionDirName(String missionDirName) {
        this.missionDirName = missionDirName;
    }
}
