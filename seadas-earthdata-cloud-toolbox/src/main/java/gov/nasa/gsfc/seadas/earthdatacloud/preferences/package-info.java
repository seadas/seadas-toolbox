/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
@OptionsPanelController.ContainerRegistration(
        id = "EarthdataCloud",
        categoryName = "#LBL_EarthdataCloudOptionsCategory_Name",
        iconBase = "gov/nasa/gsfc/seadas/processing/docs/images/seadas_icon_32x32.png",
        keywords = "#LBL_EarthdataCloudOptionsCategory_Keywords",
        keywordsCategory = "Earthdata Cloud",
        position = 1410
)
@NbBundle.Messages(value = {
        "LBL_EarthdataCloudOptionsCategory_Name=Earthdata Cloud",
        "LBL_EarthdataCloudOptionsCategory_Keywords=Earthdata,cloud"
})
package gov.nasa.gsfc.seadas.earthdatacloud.preferences;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;