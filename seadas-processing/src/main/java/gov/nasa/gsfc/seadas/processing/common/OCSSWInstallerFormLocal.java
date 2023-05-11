package gov.nasa.gsfc.seadas.processing.common;

import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import org.esa.snap.ui.AppContext;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_SRC_DIR_NAME;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 1/20/15
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class OCSSWInstallerFormLocal extends OCSSWInstallerForm {

    public OCSSWInstallerFormLocal(AppContext appContext, String programName, String xmlFileName, OCSSW ocssw) {
        super(appContext, programName, xmlFileName, ocssw);

    }

    void updateMissionStatus() {
        missionDataStatus = new HashMap<String, Boolean>();
        missionDataStatus.put("SEAWIFS", ocssw.isMissionDirExist("seawifs"));
        missionDataStatus.put("MODISA", ocssw.isMissionDirExist("modisa"));
        missionDataStatus.put("MODIST", ocssw.isMissionDirExist("modist"));
        missionDataStatus.put("VIIRSN", ocssw.isMissionDirExist("viirsn"));
        missionDataStatus.put("VIIRSJ1", ocssw.isMissionDirExist("viirsj1"));
        missionDataStatus.put("VIIRSJ2", ocssw.isMissionDirExist("viirsj2"));
        missionDataStatus.put("MERIS", ocssw.isMissionDirExist("meris"));
        missionDataStatus.put("MSIS2A", ocssw.isMissionDirExist("msis2a"));
        missionDataStatus.put("MSIS2B", ocssw.isMissionDirExist("msis2b"));
        missionDataStatus.put("CZCS", ocssw.isMissionDirExist("czcs"));
//        missionDataStatus.put("AQUARIUS", ocssw.isMissionDirExist("aquarius"));
        missionDataStatus.put("OCTS", ocssw.isMissionDirExist("octs"));
        missionDataStatus.put("OLIL8", ocssw.isMissionDirExist("olil8"));
        missionDataStatus.put("OLIL9", ocssw.isMissionDirExist("olil9"));
        missionDataStatus.put("OSMI", ocssw.isMissionDirExist("osmi"));
        missionDataStatus.put("MOS", ocssw.isMissionDirExist("mos"));
        missionDataStatus.put("OCM2", ocssw.isMissionDirExist("ocm2"));
        missionDataStatus.put("OCM1", ocssw.isMissionDirExist("ocm1"));
        missionDataStatus.put("OLCIS3A", ocssw.isMissionDirExist("olcis3a"));
        missionDataStatus.put("OLCIS3B", ocssw.isMissionDirExist("olcis3b"));
        missionDataStatus.put("AVHRR", ocssw.isMissionDirExist("avhrr"));
        missionDataStatus.put("HICO", ocssw.isMissionDirExist("hico"));
        missionDataStatus.put("GOCI", ocssw.isMissionDirExist("goci"));
        missionDataStatus.put("HAWKEYE", ocssw.isMissionDirExist("HAWKEYE"));
        missionDataStatus.put("SGLI", ocssw.isMissionDirExist("SGLI"));
    }

    void init(){
        updateMissionStatus();
    }

    void updateMissionValues() {

        for (Map.Entry<String, Boolean> entry : missionDataStatus.entrySet()) {
            String missionName = entry.getKey();
            Boolean missionStatus = entry.getValue();

            if (missionStatus) {
                processorModel.setParamValue("--" + missionName.toLowerCase(), "1");
            } else {
                processorModel.setParamValue("--" + missionName.toLowerCase(), "0");
            }

        }
        if (new File(OCSSWInfo.getInstance().getOcsswRoot(), OCSSW_SRC_DIR_NAME).exists()) {
            processorModel.setParamValue("--src", "1");
        }
        if (new File(OCSSWInfo.getInstance().getOcsswRoot(), "share/viirs/dem").exists()) {
            processorModel.setParamValue("--viirsdem", "1");
        }
    }
}
