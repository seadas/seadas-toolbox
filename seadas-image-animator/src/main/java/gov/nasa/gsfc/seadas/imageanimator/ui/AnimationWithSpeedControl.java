package gov.nasa.gsfc.seadas.imageanimator.ui;

import gov.nasa.gsfc.seadas.imageanimator.util.GifSequenceWriter;
import org.esa.snap.rcp.SnapApp;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
    static boolean windowClosedBool;
    static JFrame frame;

    //This label uses ImageIcon to show the doggy pictures.
    JLabel picture;
    public AnimationWithSpeedControl(){

    }

    public AnimationWithSpeedControl(ImageIcon[] images) {
        NUM_FRAMES = images.length;
        this.images = images;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        delay = 1000 / FPS_INIT;

        //Create the label.
        JLabel sliderLabel = new JLabel("Frames Per Second", JLabel.CENTER);
        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton pauseButton = new JButton("Pause");

        //Create the slider.
        JSlider framesPerSecond = new JSlider(JSlider.HORIZONTAL,
                FPS_MIN, FPS_MAX, FPS_INIT);

        framesPerSecond.setToolTipText("Slider that controls the animation speed");
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
        //Set the image.
        if (images[frameNumber] != null) {
            picture.setIcon(images[frameNumber]);
        } else { //image not found
            picture.setText("image #" + frameNumber + " not found");
        }
        picture.repaint();
        this.repaint();
    }

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
                timer.setInitialDelay(delay * 3);
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
            stopAnimation();
            return;
        }
        //Advance the animation frame.
        if (frameNumber == (NUM_FRAMES - 1)) {
            frameNumber = 0;
        } else {
            frameNumber++;
        }

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
        windowClosedBool = false;
        JDialog frame = new JDialog(SnapApp.getDefault().getMainFrame(), "Animation With Speed Control", true);
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

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
        JPanel controllerPanel = new JPanel(new GridBagLayout());

        JLabel filler = new JLabel("                                        ");

        JButton pauseButton = new JButton("Pause");
        pauseButton.setPreferredSize(pauseButton.getPreferredSize());
        pauseButton.setToolTipText("Pause or Resume the animation");
        pauseButton.setMinimumSize(pauseButton.getPreferredSize());
        pauseButton.setMaximumSize(pauseButton.getPreferredSize());
        pauseButton.addActionListener(new ActionListener() {
            boolean isPaused = false;
            public void actionPerformed(ActionEvent event) {
                if (!isPaused ) {
                    pauseButton.setText("Resume");
                    isPaused = true;
                    animator.stopAnimation();
                }
                else {
                    pauseButton.setText("Pause");
                    animator.startAnimation();
                    isPaused = false;
                }
            }
        });

        JButton videoButton = new JButton("Save2Video");
        videoButton.setToolTipText("Save the animation as video.");
        videoButton.setPreferredSize(videoButton.getPreferredSize());
        videoButton.setMinimumSize(videoButton.getPreferredSize());
        videoButton.setMaximumSize(videoButton.getPreferredSize());
        videoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // grab the output image type from the first image in the sequence
                BufferedImage firstImage = (BufferedImage) images[0].getImage();
                ImageOutputStream output = null;
                GifSequenceWriter writer = null;
                try {
                    // create a new BufferedOutputStream with the output file name
                    //todo This should be done differently
                    output = new FileImageOutputStream(new File("AnimationVideoOutput"));

                    // create a gif sequence with the type of the first image, 1 second between frames, which loops continuously
                    writer = new GifSequenceWriter(output, firstImage.getType(), 1, false);
                    // write out the first image to our sequence...
                    writer.writeToSequence(firstImage);

                    for (int i = 1; i < images.length; i++) {
                        BufferedImage nextImage = (BufferedImage) images[i].getImage();
                        writer.writeToSequence(nextImage);
                    }
                    writer.close();
                    output.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close the animation window");
        cancelButton.setPreferredSize(cancelButton.getPreferredSize());
        cancelButton.setMinimumSize(cancelButton.getPreferredSize());
        cancelButton.setMaximumSize(cancelButton.getPreferredSize());
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                animator.stopAnimation();
                frame.dispose();
            }
        });

        controllerPanel.add(filler,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        controllerPanel.add(pauseButton,
                new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        controllerPanel.add(videoButton,
                new ExGridBagConstraints(2, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        controllerPanel.add(cancelButton,
                new ExGridBagConstraints(3, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));


        //Add content to the window.
        frame.getContentPane().add(animator, BorderLayout.CENTER);
        frame.getContentPane().add(controllerPanel, BorderLayout.SOUTH);

        //Display the window.
        animator.startAnimation();
        frame.pack();
        frame.setVisible(true);
    }

    private static JButton getCancelButton(AnimationWithSpeedControl animator, JDialog frame) {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(cancelButton.getPreferredSize());
        cancelButton.setMinimumSize(cancelButton.getPreferredSize());
        cancelButton.setMaximumSize(cancelButton.getPreferredSize());
        cancelButton.setToolTipText("Close the animation window");

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                animator.stopAnimation();
                frame.dispose();
            }
        });
        return cancelButton;
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
}
