package gov.nasa.gsfc.seadas.processing.ocssw;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.*;

public class OCSSWInfoGUITest {

    OCSSWInfoGUI ocsswInfoGUI;
    @Before
    public void setUp() throws Exception {
        ocsswInfoGUI = new OCSSWInfoGUI();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void isValidBranchNew() {
        ArrayList tags = ocsswInfoGUI.getValidOcsswTagsFromURL();
        Iterator itr = tags.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next());
        }
    }

    @Test
    public void isValidTAGCLI() {
        ArrayList tags = ocsswInfoGUI.getValidOcsswTagsFromCLI();
        Iterator itr = tags.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next());
        }
    }

    @Test
    public void main() {
    }

    @Test
    public void init() {
    }

    @Test
    public void getDir() {
    }

    @Test
    public void createConstraints() {
    }

    @Test
    public void isTextFieldValidBranch() {
    }

    @Test
    public void isDefaultBranch() {
    }

    @Test
    public void downloadOCSSWInstaller() {
        assertTrue(ocsswInfoGUI.downloadOCSSWInstaller());
    }

    @Test
    public void isValidBranch() {
    }

    @Test
    public void isTextFieldValidIP() {
    }

    @Test
    public void isTextFieldValidPort() {
    }

    @Test
    public void textfieldHasValue() {
    }

    @Test
    public void isValidPort() {
    }

    @Test
    public void isValidIP() {
    }

    @Test
    public void isNumeric() {
    }
}