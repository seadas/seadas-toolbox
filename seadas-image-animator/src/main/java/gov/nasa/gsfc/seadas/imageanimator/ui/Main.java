package gov.nasa.gsfc.seadas.imageanimator.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application{

    List<String> imagesFiles = new ArrayList<>();
    int currentIndexImageFile = -1;
    public static int NEXT = 1;
    public static int PREV = -1;
    private final String filePrefix = "file:";
    //  w w  w  .  j ava 2  s  . co m

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("java2s.com");
        Group root = new Group();
        final Scene scene = new Scene(root, 300, 250, Color.BLACK);

        final ImageView currentImageView = new ImageView();

        currentImageView.setPreserveRatio(true);

        currentImageView.fitWidthProperty().bind(scene.widthProperty());

        final HBox pictureRegion = new HBox();
        pictureRegion.getChildren().add(currentImageView);
        root.getChildren().add(pictureRegion);

        scene.setOnDragOver((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });

        scene.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                db.getFiles().stream().map((file) -> file.getAbsolutePath()).map((filePath) -> {
                    Image imageimage = new Image(filePrefix + filePath);
                    currentImageView.setImage(imageimage);
                    currentIndexImageFile += 1;
                    return filePath;
                }).map((filePath) -> {
                    imagesFiles.add(currentIndexImageFile, filePrefix + filePath);
                    return filePath;
                }).map((filePath) -> {
                    System.out.println("Dropfile: " + filePrefix + filePath);
                    return filePath;
                }).forEach((_item) -> {
                    System.out.println("currentIndexImageFile: " + currentIndexImageFile);
                });
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // previous button
        Button prevButton = new Button();

        prevButton.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent me) -> {
            int indx = gotoImageIndex(PREV);
            if (indx > -1) {
                String namePict = imagesFiles.get(indx);
                final Image nextImage = new Image(namePict);
                SequentialTransition seqTransition = transitionByFading(nextImage, currentImageView);
                seqTransition.play();
            }
        });

        Button nextButton = new Button();
        nextButton.setOnAction(e -> {
            int indx = gotoImageIndex(NEXT);
            if (indx > -1) {
                String namePict = imagesFiles.get(indx);
                final Image nextImage = new Image(namePict);
                SequentialTransition seqTransition = transitionByFading(nextImage, currentImageView);
                seqTransition.play();

            }
        });

        root.getChildren().addAll(prevButton,nextButton);

        scene.setOnMouseEntered((MouseEvent me) -> {
            FadeTransition fadeButtons = new FadeTransition(Duration.millis(500), nextButton);
            fadeButtons.setFromValue(0.0);
            fadeButtons.setToValue(1.0);
            fadeButtons.play();
        });

        scene.setOnMouseExited((MouseEvent me) -> {
            FadeTransition fadeButtons = new FadeTransition(Duration.millis(500), nextButton);
            fadeButtons.setFromValue(1);
            fadeButtons.setToValue(0);
            fadeButtons.play();
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public int gotoImageIndex(int direction) {
        int size = imagesFiles.size();
        if (size == 0) {
            currentIndexImageFile = -1;
        } else if (direction == NEXT && size > 1 && currentIndexImageFile < size - 1) {
            currentIndexImageFile += 1;
        } else if (direction == PREV && size > 1 && currentIndexImageFile > 0) {
            currentIndexImageFile -= 1;
        }

        return currentIndexImageFile;
    }
    public SequentialTransition transitionByFading(final Image nextImage, final ImageView imageView) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), imageView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished((ActionEvent ae) -> {
            imageView.setImage(nextImage);
        });
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), imageView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        SequentialTransition seqTransition = new SequentialTransition();
        seqTransition.getChildren().addAll(fadeOut, fadeIn);
        return seqTransition;
    }
}
