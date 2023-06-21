package gov.nasa.gsfc.seadas.imageanimator.data;

import org.esa.snap.core.datamodel.Band;

import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 9/5/13
 * Time: 1:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageAnimatorData {


    static final String NEW_BAND_SELECTED_PROPERTY = "newBandSelected";
    static final String CONTOUR_LINES_BASE_NAME = "contour_";
    static final String NEW_FILTER_SELECTED_PROPERTY = "newFilterSelected";
    static final String DATA_CHANGED_PROPERTY = "dataChanged";


    Band band;
    int bandIndex;
    String contourBaseName;
    private Double startValue;
    private Double endValue;
    private int numOfLevels;
    private boolean log;
    private boolean filtered;
    private boolean contourCustomized;
    private String filterName;
    private String oldFilterName;
    private double ptsToPixelsMultiplier;
    private boolean deleted;
    private boolean contourInitialized;
    private boolean contourValuesChanged;

    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);


    public ImageAnimatorData() {
        this(null, null, null, 1);
    }

    public ImageAnimatorData(Band band, String unfiltereBandName, String filterName, double ptsToPixelsMultiplier) {
        contourBaseName = CONTOUR_LINES_BASE_NAME;
        if (band != null) {
            contourBaseName = contourBaseName + unfiltereBandName + "_";
        }
        startValue = Double.MIN_VALUE;
        endValue = Double.MAX_VALUE;
        this.band = band;
        log = false;
        filtered = true;
        contourCustomized = false;
        contourInitialized = true;
        contourValuesChanged = false;
        deleted = false;
        this.filterName = filterName;
        this.ptsToPixelsMultiplier = ptsToPixelsMultiplier;
        //propertyChangeSupport.addPropertyChangeListener(NEW_FILTER_SELECTED_PROPERTY, getFilterButtonPropertyListener());
        //propertyChangeSupport.addPropertyChangeListener(NEW_FILTER_SELECTED_PROPERTY, getFilterButtonPropertyListener());
    }


    public void setBand(Band band) {
        String oldBandName = this.band.getName();
        this.band = band;
        //contourBaseName = CONTOUR_LINES_BASE_NAME + band.getName() + "_";
        propertyChangeSupport.firePropertyChange(NEW_BAND_SELECTED_PROPERTY, oldBandName, band.getName());
    }

    public Band getBand() {
        return band;
    }

    public void setBandIndex(int bandIndex) {
        this.bandIndex = bandIndex;

    }

    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }

    public SwingPropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public void appendPropertyChangeSupport(SwingPropertyChangeSupport propertyChangeSupport) {
        PropertyChangeListener[] pr = propertyChangeSupport.getPropertyChangeListeners();
        for (int i = 0; i < pr.length; i++) {
            this.propertyChangeSupport.addPropertyChangeListener(pr[i]);
        }
    }

    public void clearPropertyChangeSupport() {
        PropertyChangeListener[] pr = propertyChangeSupport.getPropertyChangeListeners();
        for (int i = 0; i < pr.length; i++) {
            this.propertyChangeSupport.removePropertyChangeListener(pr[i]);
        }

    }

}



