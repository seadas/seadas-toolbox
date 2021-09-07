/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
@OptionsPanelController.ContainerRegistration(
        id = "SeaDAS",
        categoryName = "#LBL_SeadasToolboxOptionsCategory_Name",
        iconBase = "gov/nasa/gsfc/seadas/processing/docs/images/seadas_icon_32x32.png",
        keywords = "#LBL_SeadasToolboxOptionsCategory_Keywords",
        keywordsCategory = "SeaDAS_Toolbox",
        position = 1400
)
@NbBundle.Messages(value = {
    "LBL_SeadasToolboxOptionsCategory_Name=SeaDAS Toolbox",
    "LBL_SeadasToolboxOptionsCategory_Keywords=seadas, ocssw, l2gen"
})
package gov.nasa.gsfc.seadas.processing.preferences;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;