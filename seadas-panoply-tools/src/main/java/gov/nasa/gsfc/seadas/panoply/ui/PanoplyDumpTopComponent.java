package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Tool window that renders a plain ncdump-style text block for the selected
 * Panoply Geophysical_Data variable element.
 */
@TopComponent.Description(
        preferredID = "PanoplyDumpTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "gov.nasa.gsfc.seadas.panoply.ui.PanoplyDumpTopComponent")
@ActionReference(path = "Menu/Window", position = 19300)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PanoplyDumpAction",
        preferredID = "PanoplyDumpTopComponent"
)
@Messages({
        "CTL_PanoplyDumpAction=Panoply Dump",
        "CTL_PanoplyDumpTopComponent=Panoply Dump",
        "HINT_PanoplyDumpTopComponent=Shows Panoply-style (ncdump) text for the selected variable"
})
public final class PanoplyDumpTopComponent extends TopComponent implements LookupListener {

    private final JTextArea textArea = new JTextArea();
    private Lookup.Result<MetadataElement> metaSel;

    public PanoplyDumpTopComponent() {
        setName(Bundle.CTL_PanoplyDumpTopComponent());
        setToolTipText(Bundle.HINT_PanoplyDumpTopComponent());

        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(false);

        setLayout(new BorderLayout());
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        textArea.setText("Select a variable under Metadata → Panoply → Geophysical_Data …");
    }

    @Override
    public void componentOpened() {
        metaSel = Utilities.actionsGlobalContext().lookupResult(MetadataElement.class);
        metaSel.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void componentClosed() {
        if (metaSel != null) {
            metaSel.removeLookupListener(this);
            metaSel = null;
        }
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        MetadataElement el = Utilities.actionsGlobalContext().lookup(MetadataElement.class);
        if (el == null) {
            textArea.setText("Select a variable under Metadata → Panoply → Geophysical_Data …");
            return;
        }

        // Only handle our Panoply variable nodes: parent is "Geophysical_Data", grandparent is "Panoply".
        MetadataElement parent = el.getParentElement();
        MetadataElement grand = (parent != null) ? parent.getParentElement() : null;
        if (parent == null || grand == null
                || !"Geophysical_Data".equals(parent.getName())
                || !"Panoply".equals(grand.getName())) {
            textArea.setText("Select a variable under Metadata → Panoply → Geophysical_Data …");
            return;
        }

        textArea.setText(buildDumpFromElement(el));
        textArea.setCaretPosition(0);
    }

    /** Reconstruct the text by concatenating attribute NAMES in their stored order. */
    private static String buildDumpFromElement(MetadataElement varElem) {
        List<MetadataAttribute> attrs = Arrays.asList(varElem.getAttributes());
        // Sort by the zero-width numeric suffix we appended ( \u200B + 6 digits )
        attrs.sort(Comparator.comparingInt(a -> extractOrder(a.getName())));

        StringBuilder sb = new StringBuilder(2048);
        for (MetadataAttribute a : attrs) {
            String name = a.getName();
            int zw = name.lastIndexOf('\u200B');
            if (zw >= 0) name = name.substring(0, zw); // strip suffix
            sb.append(name).append('\n');
        }
        return sb.toString();
    }

    private static int extractOrder(String name) {
        int zw = name.lastIndexOf('\u200B');
        if (zw < 0 || zw + 1 >= name.length()) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(name.substring(zw + 1));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}

