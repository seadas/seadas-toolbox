package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.swing.TableLayout;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.utilities.SheetCell;
import gov.nasa.gsfc.seadas.processing.utilities.SpreadSheet;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * @author Aynur Abdurazik (aabduraz)
 * Date: 4/29/14
 * Time: 1:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class CallL3BinDumpAction extends CallCloProgramAction {

    @Override
    void displayOutput(final ProcessorModel processorModel) {
        String output = processorModel.getExecutionLogMessage();
        StringTokenizer st = new StringTokenizer(output, "\n");
        int numRows = st.countTokens();
        String line = st.nextToken();
        StringTokenizer stLine = new StringTokenizer(line, " ");
        String prodName = null;
        boolean skipFirstLine = false;

        if (stLine.countTokens() < 14) {
            prodName = stLine.nextToken();
            numRows--;
            skipFirstLine = true;
        }
        if (!skipFirstLine) {
            st = new StringTokenizer(output, "\n");
        }
        final SheetCell[][] cells = new SheetCell[numRows][14];
        int i = 0;
        while (st.hasMoreElements()) {
            line = st.nextToken();
            stLine = new StringTokenizer(line, " ");
            int j = 0;
            while (stLine.hasMoreElements()) {
                cells[i][j] = new SheetCell(i, j, stLine.nextToken(), null);
                j++;
            }
            i++;
        }

        if (skipFirstLine) {
            cells[0][10] = new SheetCell(0, 10, prodName + " " + cells[0][10].getValue(), null);
            cells[0][11] = new SheetCell(0, 11, prodName + " " + cells[0][11].getValue(), null);
        }

        SpreadSheet sp = new SpreadSheet(cells);

        final JFrame frame = new JFrame("l3bindump Output");
        JPanel content = new JPanel(new BorderLayout());
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        TableLayout buttonPanelLayout = new TableLayout(2);
        JPanel buttonPanel = new JPanel(buttonPanelLayout);
        /*
       * Allows the user to exit the application
       * from the window manager's dressing.
       */
        frame.addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                System.exit(0);
//            }
        });


        sp.getScrollPane().getVerticalScrollBar().setAutoscrolls(true);
        content.add(sp.getScrollPane(), BorderLayout.NORTH);
        buttonPanel.add(save);
        buttonPanel.add(cancel);

        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //To change body of implemented methods use File | Settings | File Templates.

                String outputFileName = processorModel.getParamValue(processorModel.getPrimaryInputFileOptionName()) + "_l3bindump_output.txt";
                saveForSpreadsheet(outputFileName, cells);

                String message = "l3bindump output is saved in file \n"
                                  + outputFileName +"\n"
                                  + " in spreadsheet format.";
                final ModalDialog modalDialog = new ModalDialog(SnapApp.getDefault().getMainFrame(), "", message, ModalDialog.ID_OK, "test");
                            final int dialogResult = modalDialog.show();
                            if (dialogResult != ModalDialog.ID_OK) {

                            }
                frame.setVisible(false);
            }
        });

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
                frame.setVisible(false);
            }
        });
        content.add(buttonPanel, BorderLayout.SOUTH);
        frame.getContentPane().add(content);

        frame.pack();
        frame.setVisible(true);

    }

    private void saveForSpreadsheet(String outputFileName, SheetCell[][] cells) {
        String cell, all = new String();
        for (int i = 0; i < cells.length; i++) {
            cell = new String();
            for (int j = 0; j < cells[i].length; j++) {
                cell = cell + cells[i][j].getValue() + "\t";
            }
            all = all + cell + "\n";
        }

        try {
            final File excelFile = new File(outputFileName);
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(excelFile);
                fileWriter.write(all);
            } finally {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        } catch (IOException e) {
            Logger.getGlobal().warning(outputFileName + " is not created. " + e.getMessage());
        }

    }
}
