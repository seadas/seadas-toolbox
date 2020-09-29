package gov.nasa.gsfc.seadas.processing.ocssw;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class OCSSWConfigData {

    /**
     seadas.log.dir = /accounts/aabduraz/SeaDAS/log
     #seadas.ocssw.root = /accounts/aabduraz/SeaDAS/ocssw
     #seadas.ocssw.root = /accounts/aabduraz/SeaDAS/ocsswserver/ocssw
     seadas.ocssw.root = /accounts/aabduraz/SeaDAS/dev/seadas-7.4/seadas/ocssw

     # set physical location of OCSSW package
     # the seadas.ocssw.location property should be assigned one of these three values: "local", "virtualMachine",
     # or the remote server IP address.
     seadas.ocssw.location = virtualMachine
     seadas.ocssw.port=6400
     seadas.ocssw.sharedDir=/accounts/aabduraz/clientServerSharedDir
     seadas.client.id=seadas
     seadas.ocssw.keepFilesOnServer=false
     seadas.ocssw.processInputStreamPort=6402
     seadas.ocssw.processErrorStreamPort=6403
    */

    final static String SEADAS_LOG_DIR_PROPERTY = "seadas.log.dir";
    public final static String SEADAS_OCSSW_TAG_PROPERTY = "seadas.ocssw.tag";
    final static String SEADAS_OCSSW_ROOT_PROPERTY = "seadas.ocssw.root";
    final static String SEADAS_OCSSW_ROOT_ENV = "OCSSWROOT";
    final static String SEADAS_OCSSW_LOCATION_PROPERTY = "seadas.ocssw.location";
    final static String SEADAS_OCSSW_PORT_PROPERTY = "seadas.ocssw.port";
    final static String SEADAS_OCSSW_KEEPFILEONSERVER_PROPERTY = "seadas.ocssw.keepFilesOnServer";
    final static String SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY = "seadas.ocssw.processInputStreamPort";
    final static String SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY = "seadas.ocssw.processErrorStreamPort";
    final static String SEADAS_OCSSW_SERVER_ADDRESS_PROPERTY = "seadas.ocssw.serverAddress";
    final static String SEADAS_CLIENT_ID_PROPERTY = "seadas.client.id";
    final static String SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY = "seadas.ocssw.sharedDir";
    final static String SEADAS_OCSSW_VERSION_NUMBER_PROEPRETY ="seadas.ocssw.version";
    final static String SEADAS_OCSSW_DEBUG ="seadas.ocssw.debug";

    final static String SEADAS_LOG_DIR_DEFAULT_VALUE = System.getProperty("user.home") + File.separator + ".seadas" + File.separator +"log";
    final static String SEADAS_OCSSW_ROOT_DEFAULT_VALUE = System.getProperty("user.home") + File.separator + "ocssw";
    final static String SEADAS_OCSSW_LOCATION_DEFAULT_VALUE = "local";
    final static  String SEADAS_OCSSW_PORT_DEFAULT_VALUE = "6400";
    final static String SEADAS_OCSSW_KEEPFILEONSERVER_DEFAULT_VALUE = "false";
    final static  String SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE = "6402";
    final static String SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE = "6403";
    final static String SEADAS_OCSSW_SERVER_ADDRESS_DEFAULT_VALUE = "";
    final static String SEADAS_CLIENT_ID_DEFAULT_VALUE = System.getProperty("user.name");
    final static String SEADAS_CLIENT_SERVER_SHARED_DIR_DEFAULT_VALUE =  System.getProperty("user.home") + File.separator + "seadasOCSSWSharedDir";
    final static String SEADAS_OCSSW_DEBUG_DEFAULT_VALUE =  "false";
    public final static String SEADAS_OCSSW_TAG_DEFAULT_VALUE = "don";


    final static String OCSSW_TAG_LABEL = "Valid OCSSW Tags";
    final static String OCSSW_LOCATION_LABEL = "OCSSW Location";
    final static String OCSSW_SHARED_DIR_LABEL = "OCSSW Shared Dir";
    final static String OCSSW_ROOT_LABEL = "OCSSW ROOT";
    final static String OCSSW_SERVER_ADDRESS_LABEL = "OCSSW Server Address";
    final static String SERVER_PORT_LABEL = "Server Port";
    final static String SERVER_INPUT_STREAM_PORT_LABEL = "Server Input Stream Port";
    final static String SERVER_ERROR_STREAM_PORT_LABEL = "Server Error Stream Port";



    public static Properties properties = new Properties(System.getProperties());

    public OCSSWConfigData(){
        //initConfigDefauls();
    }

    private void initConfigDefauls(){

        final Preferences preferences = Config.instance("seadas").load().preferences();
        preferences.put(SEADAS_LOG_DIR_PROPERTY, SEADAS_LOG_DIR_DEFAULT_VALUE);
        preferences.put(SEADAS_OCSSW_ROOT_PROPERTY, SEADAS_OCSSW_ROOT_DEFAULT_VALUE);
        preferences.put(SEADAS_OCSSW_LOCATION_PROPERTY, SEADAS_OCSSW_LOCATION_DEFAULT_VALUE);
        preferences.put(SEADAS_OCSSW_PORT_PROPERTY, SEADAS_OCSSW_PORT_DEFAULT_VALUE);
        preferences.put(SEADAS_OCSSW_KEEPFILEONSERVER_PROPERTY, SEADAS_OCSSW_KEEPFILEONSERVER_DEFAULT_VALUE);
        preferences.put(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE);
        preferences.put(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE);
        preferences.put(SEADAS_CLIENT_ID_PROPERTY, SEADAS_CLIENT_ID_DEFAULT_VALUE);
        preferences.put(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, SEADAS_CLIENT_SERVER_SHARED_DIR_DEFAULT_VALUE);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
    }

    public void updateconfigData(PropertyContainer pc) {
        final Preferences preferences = Config.instance("seadas").load().preferences();
        Property[] newProperties = pc.getProperties();
        String key, value;
        for (int i = 0; i < newProperties.length; i++) {
            key = newProperties[i].getName();
            value = newProperties[i].getValue();
            preferences.put(key, value);
        }
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
        OCSSWInfo.updateOCSSWInfo();
    }
}
