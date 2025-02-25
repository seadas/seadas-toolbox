package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class OBDAACApp extends Application {

    private TreeView<String> treeView;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("OBDAAC Data Browser");

        // Create TreeView for browsing
        TreeItem<String> rootItem = new TreeItem<>("OBDAAC Collections");
        rootItem.setExpanded(true);
        treeView = new TreeView<>(rootItem);

        // Load JSON data into TreeView
        loadJsonData(rootItem);

        // Layout setup
        BorderPane layout = new BorderPane();
        layout.setLeft(treeView);

        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadJsonData(TreeItem<String> rootItem) {
        File folder = new File(System.getProperty("user.home") + "/Documents/json-files");
        ObjectMapper objectMapper = new ObjectMapper();

        if (folder.exists() && folder.isDirectory()) {
            File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (jsonFiles != null) {
                for (File file : jsonFiles) {
                    try {
                        JsonNode rootNode = objectMapper.readTree(file);
                        TreeItem<String> satelliteNode = new TreeItem<>(file.getName().replace(".json", ""));

                        for (JsonNode levelNode : rootNode) {
                            String level = levelNode.fieldNames().next();
                            TreeItem<String> levelItem = new TreeItem<>(level);

                            for (JsonNode productNode : levelNode.get(level)) {
                                String productName = productNode.get("product_name").asText();
                                levelItem.getChildren().add(new TreeItem<>(productName));
                            }
                            satelliteNode.getChildren().add(levelItem);
                        }
                        rootItem.getChildren().add(satelliteNode);
                    } catch (IOException e) {
                        System.out.println("Error reading file: " + file.getName());
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
