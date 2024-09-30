package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class WebPageFetcherWithJWT {

    public WebPageFetcherWithJWT() {
    }


    // Method to remove empty rows from the JTable's model
    public static void removeEmptyRows(DefaultTableModel model) {
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            boolean isEmpty = true;
            // Check if all cells in the row are empty (null or empty string)
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    isEmpty = false; // Found a non-empty value
                    break;
                }
            }
            // If the row is empty, remove it from the model
            if (isEmpty) {
                model.removeRow(row);
            }
        }
    }

    public static String[]
    getCredentials(String machineName) {
        File netrcFile = new File(System.getProperty("user.home"), ".netrc");
        String[] credentials = new String[2];  // [0]: username, [1]: password
        boolean machineFound = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(netrcFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split the line by spaces
                String[] tokens = line.trim().split("\\s+");

                // Check if we are reading the credentials for the specific machine
                if (tokens.length >= 2 && "machine".equalsIgnoreCase(tokens[0])) {
                    machineFound = machineName.equals(tokens[1]);
                }

                // If the machine is found, extract the login and password
                if (machineFound) {
                    if(tokens.length >= 6) { // one-line netrc format: machine <machine-name> login <login> password <password>
                        credentials[0] = tokens[3];
                        credentials[1] = tokens[5];
                        break;
                    } else if (tokens.length >= 2 && "login".equalsIgnoreCase(tokens[0])) {
                        credentials[0] = tokens[1];  // Set the username
                    } else if (tokens.length >= 2 && "password".equalsIgnoreCase(tokens[0])) {
                        credentials[1] = tokens[1];  // Set the password
                        break;  // Credentials found, exit the loop
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the .netrc file: " + e.getMessage());
        }

        if (credentials[0] != null && credentials[1] != null) {
            return credentials;  // Return username and password if found
        } else {
            System.err.println("Credentials for machine " + machineName + " not found.");
            return null;  // Return null if credentials not found
        }
    }

    public static String getAccessToken(String endpoint) throws Exception {
        URL url = new URL("https://" + endpoint + "/api/users/tokens");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        StringBuilder content = new StringBuilder();

        String[] credentials = getCredentials(endpoint);
        String username;
        String password;

        // Output the credentials (username and password)
        if (credentials == null) {
            throw new Exception("Failed to retrieve user credentials for endpoint: " + endpoint);
        }

        username = credentials[0];
        password = credentials[1];

        // Combine username and password into a single string
        String auth = username + ":" + password;

        // Encode the string into Base64
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        // Set request method to GET and add Bearer token to the Authorization header
        connection.setRequestMethod("GET");
        // Set the Authorization header with Basic authentication
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        int status = connection.getResponseCode();

        // Check if the response is OK (200)
        if (status == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
            }
        } else {
            throw new Exception("Failed to fetch content. HTTP status code: " + status);
        }

        String access_token = "empty";
        JSONArray tokenJSonArray = new JSONArray(content.toString());
        try {
            // Loop through the array
            for (int i = 0; i < tokenJSonArray.length(); i++) {
                // Get each JSONObject (each row of the table)
                JSONObject jsonObject = tokenJSonArray.getJSONObject(i);

                // Access individual elements (columns) from the JSONObject
                if (jsonObject.has("access_token")) {
                    access_token = jsonObject.getString("access_token");
                     i = tokenJSonArray.length() + 1;
                }


            }
        } catch (JSONException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.print("access_token: " + access_token + "  !!! end of access token!");

        if (access_token.contains("empty")) {
            access_token = generateAccessToken(endpoint);
        }
        return access_token;
    }

    public static String generateAccessToken(String endpoint) throws Exception {
        URL url = new URL("https://" + endpoint + "/api/users/token");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        StringBuilder content = new StringBuilder();

        String[] credentials = getCredentials(endpoint);
        String username;
        String password;

        // Output the credentials (username and password)
        if (credentials == null) {
            throw new Exception("Failed to retrieve user credentials for endpoint: " + endpoint);
        }

        username = credentials[0];
        password = credentials[1];

        // Combine username and password into a single string
        String auth = username + ":" + password;

        // Encode the string into Base64
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        // Set request method to GET and add Bearer token to the Authorization header
        connection.setRequestMethod("POST");
        // Set the Authorization header with Basic authentication
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        int status = connection.getResponseCode();

        // Check if the response is OK (200)
        if (status == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
            }
        } else {
            throw new Exception("Failed to fetch content. HTTP status code: " + status);
        }

        String access_token = "empty";
        var jsonObject = new JSONObject(content.toString());

        if (jsonObject.has("access_token")) {
            return jsonObject.getString("access_token");
        }
        throw new Exception("Could not generate an access token using netrc credentials. Server response: " + content);
    }

    public JTable getCollectionList(JSONObject jsonResponse) {

        JSONArray tableArray = null;
        JTable jTable = null;
        tableArray = (JSONArray) jsonResponse.get("collections");
        System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
        jTable = getJTableNew(tableArray);
        return jTable;

    }


    public JTable getSearchDataList(JSONObject jsonResponse) {

        JSONArray tableArray = null;
        JTable jTable = null;
        tableArray = (JSONArray) jsonResponse.get("links");
        System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
        jTable = getJTableNew(tableArray);
        return jTable;

    }

    public JTable getJTableNew(JSONArray dataArray) {

        //String[] columnNames = { "Rel", "HREF", "Title", "Temporal", "BBox" };
        String[] columnNames = {"Title", "HREF"};
        Object[][] dataSearchResult = new Object[dataArray.length()][2];


        // Iterate over the students array and add each row to the searchResultTable
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataURL = dataArray.getJSONObject(i);
            if (dataURL.has("temporal")) {
                dataSearchResult[i][0] = dataURL.getString("title");
                dataSearchResult[i][1] = dataURL.getString("href");
            }
        }

        DefaultTableModel model = new DefaultTableModel(dataSearchResult, columnNames);

        JTable searchResultTable = new JTable(model);

        // Set a custom renderer for the 'Link' column to display as a clickable link
        searchResultTable.getColumnModel().getColumn(1).setCellRenderer(new LinkCellRenderer());

        // Add a mouse listener to detect clicks on the link
        searchResultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = searchResultTable.rowAtPoint(e.getPoint());
                int col = searchResultTable.columnAtPoint(e.getPoint());

                if (col == 1) { // If the link column is clicked
                    String url = (String) searchResultTable.getValueAt(row, col);
                    if (url != null && !url.isEmpty()) { // Ensure the URL is not null or empty
                        try {
                            // Open the link in the default browser
                            Desktop.getDesktop().browse(new URI(url));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        System.out.println("No valid URL in this cell.");
                    }
                }
            }
        });

        removeEmptyRows(model);
        return searchResultTable;
    }
}

// Custom TableCellRenderer to display clickable links in the table
class LinkCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value != null) {  // Check if value is not null
            String link = value.toString();
            label.setText("<html><a href=''>" + link + "</a></html>");
        } else {
            label.setText("No link"); // Handle null values
        }
        return label;
    }
}