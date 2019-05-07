package gov.nasa.gsfc.seadas.processing.ocssw;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;

import java.io.File;
import java.util.Properties;

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
    final static String SEADAS_OCSSW_BRANCH_PROPERTY = "seadas.ocssw.branch";
    final static String SEADAS_OCSSW_ROOT_PROPERTY = "seadas.ocssw.root";
    final static String SEADAS_OCSSW_LOCATION_PROPERTY = "seadas.ocssw.location";
    final static String SEADAS_OCSSW_PORT_PROPERTY = "seadas.ocssw.port";
    final static String SEADAS_OCSSW_KEEPFILEONSERVER_PROPERTY = "seadas.ocssw.keepFilesOnServer";
    final static String SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY = "seadas.ocssw.processInputStreamPort";
    final static String SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY = "seadas.ocssw.processErrorStreamPort";
    final static String SEADAS_CLIENT_ID_PROPERTY = "seadas.client.id";
    final static String SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY = "seadas.ocssw.sharedDir";

    final static String SEADAS_LOG_DIR_DEFAULT_VALUE = System.getProperty("user.home") + File.separator + ".seadas" + File.separator +"log";
    final static String SEADAS_OCSSW_BRANCH_DEFAULT_VALUE =  "7.5";
    final static String SEADAS_OCSSW_ROOT_DEFAULT_VALUE = System.getProperty("user.home") + File.separator + "ocssw";
    final static String SEADAS_OCSSW_LOCATION_DEFAULT_VALUE = "local";
    final static  String SEADAS_OCSSW_PORT_DEFAULT_VALUE = "6400";
    final static String SEADAS_OCSSW_KEEPFILEONSERVER_DEFAULT_VALUE = "false";
    final static  String SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE = "6402";
    final static String SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE = "6403";
    final static String SEADAS_CLIENT_ID_DEFAULT_VALUE = System.getProperty("user.name");
    final static String SEADAS_CLIENT_SERVER_SHARED_DIR_DEFAULT_VALUE =  System.getProperty("user.home") + File.separator + "seadasOCSSWSharedDir";


    public static Properties properties = new Properties(System.getProperties());

    public OCSSWConfigData(){
        initConfigDefauls();
    }

    private void initConfigDefauls(){

        properties.put(SEADAS_LOG_DIR_PROPERTY, SEADAS_LOG_DIR_DEFAULT_VALUE);
        properties.put(SEADAS_OCSSW_ROOT_PROPERTY, SEADAS_OCSSW_ROOT_DEFAULT_VALUE);
        properties.put(SEADAS_OCSSW_LOCATION_PROPERTY, SEADAS_OCSSW_LOCATION_DEFAULT_VALUE);
        properties.put(SEADAS_OCSSW_PORT_PROPERTY, SEADAS_OCSSW_PORT_DEFAULT_VALUE);
        properties.put(SEADAS_OCSSW_KEEPFILEONSERVER_PROPERTY, SEADAS_OCSSW_KEEPFILEONSERVER_DEFAULT_VALUE);
        properties.put(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE);
        properties.put(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE);
        properties.put(SEADAS_CLIENT_ID_PROPERTY, SEADAS_CLIENT_ID_DEFAULT_VALUE);
        properties.put(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, SEADAS_CLIENT_SERVER_SHARED_DIR_DEFAULT_VALUE);
        // set the system properties
        System.setProperties(properties);
        // display new properties
        //System.getProperties().list(System.out);
    }

    public void updateconfigData(PropertyContainer pc) {
        Property[] newProperties = pc.getProperties();
        String key, value;
        for (int i = 0; i < newProperties.length; i++) {
            key = newProperties[i].getName();
            value = newProperties[i].getValue();
            properties.setProperty(key, value);
            //System.out.println(newProperties[i].getName() + " = " + newProperties[i].getValue());
        }

        // set the system properties
        System.setProperties(properties);
        // display new properties
        //System.getProperties().list(System.out);
    }
}
