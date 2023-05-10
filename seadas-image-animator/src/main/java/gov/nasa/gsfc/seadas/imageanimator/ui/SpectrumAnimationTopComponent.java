package gov.nasa.gsfc.seadas.imageanimator.ui;

import org.esa.snap.rcp.angularview.AngularTopComponent;
import org.esa.snap.rcp.spectrum.SpectrumTopComponent;

/**
 * Created with IntelliJ IDEA.
 * User: Yang
 * Date: 5/10/23
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpectrumAnimationTopComponent extends SpectrumTopComponent {

    public SpectrumAnimationTopComponent() {
        super();
        open();
        requestActive();
    }

}