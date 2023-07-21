package gov.nasa.gsfc.seadas.imageanimator.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

public class AnimationWithSpeedControl extends JPanel
        implements ActionListener,
//        WindowListener,
        ChangeListener {
    //Set up animation parameters.
    static final int FPS_MIN = 0;
    static final int FPS_MAX = 30;
    static final int FPS_INIT = 5;    //initial frames per second
    int frameNumber = 0;
    int NUM_FRAMES = 2;
    ImageIcon[] images;
    int delay;
    Timer timer;
    boolean frozen = false;
    static boolean windowClosedBool = false;
    static JFrame frame;

    //This label uses ImageIcon to show the doggy pictures.
    JLabel picture;
    public AnimationWithSpeedControl(){

    }
//    public AnimationWithSpeedControl() {
//        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
//
//        delay = 1000 / FPS_INIT;
//
//        //Create the label.
//        JLabel sliderLabel = new JLabel("Frames Per Second", JLabel.CENTER);
//        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//
//        //Create the slider.
//        JSlider framesPerSecond = new JSlider(JSlider.HORIZONTAL,
//                FPS_MIN, FPS_MAX, FPS_INIT);
//
//
//        framesPerSecond.addChangeListener(this);
//
//        //Turn on labels at major tick marks.
//
//        framesPerSecond.setMajorTickSpacing(10);
//        framesPerSecond.setMinorTickSpacing(1);
//        framesPerSecond.setPaintTicks(true);
//        framesPerSecond.setPaintLabels(true);
//        framesPerSecond.setBorder(
//                BorderFactory.createEmptyBorder(0,0,10,0));
//        Font font = new Font("Serif", Font.ITALIC, 15);
//        framesPerSecond.setFont(font);
//
//        //Create the label that displays the animation.
//        picture = new JLabel();
//        picture.setHorizontalAlignment(JLabel.CENTER);
//        picture.setAlignmentX(Component.CENTER_ALIGNMENT);
//        picture.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createLoweredBevelBorder(),
//                BorderFactory.createEmptyBorder(10,10,10,10)));
//        updatePicture(0); //display first frame
//
//        //Put everything together.
//        add(sliderLabel);
//        add(framesPerSecond);
//        add(picture);
//        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
//
//        //Set up a timer that calls this object's action handler.
//        timer = new Timer(delay, this);
//        timer.setInitialDelay(delay * 7); //We pause animation twice per cycle
//        //by restarting the timer
//        timer.setCoalesce(true);
//    }

    public AnimationWithSpeedControl(ImageIcon[] images) {
        NUM_FRAMES = images.length;
        this.images = images;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        delay = 1000 / FPS_INIT;

        //Create the label.
        JLabel sliderLabel = new JLabel("Frames Per Second", JLabel.CENTER);
        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        //Create the slider.
        JSlider framesPerSecond = new JSlider(JSlider.HORIZONTAL,
                FPS_MIN, FPS_MAX, FPS_INIT);


        framesPerSecond.addChangeListener(this);

        //Turn on labels at major tick marks.

        framesPerSecond.setMajorTickSpacing(10);
        framesPerSecond.setMinorTickSpacing(1);
        framesPerSecond.setPaintTicks(true);
        framesPerSecond.setPaintLabels(true);
        framesPerSecond.setBorder(
                BorderFactory.createEmptyBorder(0,0,10,0));
        Font font = new Font("Serif", Font.ITALIC, 15);
        framesPerSecond.setFont(font);

        //Create the label that displays the animation.
        picture = new JLabel();
        picture.setHorizontalAlignment(JLabel.CENTER);
        picture.setAlignmentX(Component.CENTER_ALIGNMENT);
        picture.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(10,10,10,10)));
        updatePicture(0); //display first frame

        //Put everything together.
        add(sliderLabel);
        add(framesPerSecond);
        add(picture);
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Set up a timer that calls this object's action handler.
        timer = new Timer(delay, this);
        timer.setInitialDelay(delay * 7); //We pause animation twice per cycle
        //by restarting the timer
        timer.setCoalesce(true);
    }

    protected void updatePicture(int frameNum) {
        //Get the image if we haven't already.
//        if (images[frameNumber] == null) {
//            images[frameNumber] = createImageIcon("/images/A2020113184000_L2_LAC_OC_"
//                    + frameNumber
//                    + ".png");
//        }

        //Set the image.
        if (images[frameNumber] != null) {
            picture.setIcon(images[frameNumber]);
        } else { //image not found
            picture.setText("image #" + frameNumber + " not found");
        }
        picture.repaint();
        this.repaint();
    }

//    /** Add a listener for window events. */
//    void addWindowListener(Window w) {
//        w.addWindowListener(this);
//    }
//
//    //React to window events.
//    public void windowIconified(WindowEvent e) {
//        stopAnimation();
//    }
//    public void windowDeiconified(WindowEvent e) {
//        startAnimation();
//    }
//    public void windowOpened(WindowEvent e) {}
//    public void windowClosing(WindowEvent e) {
//        windowClosedBool = true;
//        stopAnimation();
//    }
//    public void windowClosed(WindowEvent e) {
//        windowClosedBool = true;
//        stopAnimation();
//    }
//    public void windowActivated(WindowEvent e) {}
//    public void windowDeactivated(WindowEvent e) {}

    /** Listen to the slider. */
    public void stateChanged(ChangeEvent e) {
        if(windowClosedBool) {
            stopAnimation();
            return;
        }
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting()) {
            int fps = (int)source.getValue();
            if (fps == 0) {
                if (!frozen) stopAnimation();
            } else {
                delay = 1000 / fps;
                timer.setDelay(delay);
                timer.setInitialDelay(delay * 10);
                if (frozen) startAnimation();
            }
        }
    }

    public void startAnimation() {
        //Start (or restart) animating!
        timer.start();
        frozen = false;
    }

    public void stopAnimation() {
        //Stop the animating thread.
        timer.stop();
        frozen = true;
    }

    //Called when the Timer fires.
    public void actionPerformed(ActionEvent e) {
        if(frozen || windowClosedBool) {
            return;
        }
        //Advance the animation frame.
        if (frameNumber == (NUM_FRAMES - 1)) {
            frameNumber = 0;
        } else {
            frameNumber++;
        }

        System.out.println("frame number = " + frameNumber);

        updatePicture(frameNumber); //display the next picture

        if ( frameNumber==(NUM_FRAMES - 1) ) {
            timer.restart();
        }
    }

    /** Update the label to display the image for the current frame. */
    protected void updatePictureOriginal(int frameNum) {
        //Get the image if we haven't already.
        if (images[frameNumber] == null) {
            images[frameNumber] = createImageIcon("/images/A2020113184000_L2_LAC_OC_"
                    + frameNumber
                    + ".png");
        }

        //Set the image.
        if (images[frameNumber] != null) {
            picture.setIcon(images[frameNumber]);
        } else { //image not found
            picture.setText("image #" + frameNumber + " not found");
        }
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = SliderDemo.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    public static void closeGUI() {
        frame.dispose();
    };
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public static void createAndShowGUI(ImageIcon[] images) {
        //Create and set up the window.
//        JFrame frame = new JFrame("Band Images Animation");
        frame = new JFrame("Band Images Animation");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        AnimationWithSpeedControl animator = new AnimationWithSpeedControl(images);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
               windowClosedBool = true;
               animator.stopAnimation();
            }
            @Override
            public void windowClosing(WindowEvent e) {
                windowClosedBool = true;
                animator.stopAnimation();
            }
            @Override
            public void windowIconified(WindowEvent e) {
                animator.stopAnimation();
            }
            @Override
            public void windowDeiconified(WindowEvent e) {
                animator.startAnimation();
            }
        });
        //Add content to the window.
        frame.add(animator, BorderLayout.CENTER);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        animator.startAnimation();
    }

    public static void animate(ImageIcon[] images){
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);


        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(images);
            }
        });
    }

//    public static void main(String[] args) {
//        /* Turn off metal's use of bold fonts */
//        UIManager.put("swing.boldMetal", Boolean.FALSE);
//
//
//        //Schedule a job for the event-dispatching thread:
//        //creating and showing this application's GUI.
//        javax.swing.SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                createAndShowGUI();
//            }
//        });
//    }
}
