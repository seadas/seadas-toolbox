package gov.nasa.gsfc.seadas.earthdatacloud.auth;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

import java.util.Base64;

import gov.nasa.gsfc.seadas.earthdatacloud.action.LinkCellRenderer;
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


    public static void removeEmptyRows(DefaultTableModel model) {
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            boolean isEmpty = true;
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    isEmpty = false; // Found a non-empty value
                    break;
                }
            }
            if (isEmpty) {
                model.removeRow(row);
            }
        }
    }

    public static String[] getCredentials(String machineName) {
        File netrcFile = new File(System.getProperty("user.home"), ".netrc");
        String[] credentials = new String[2];  // [0]: username, [1]: password
        boolean machineFound = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(netrcFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");

                if (tokens.length >= 2 && "machine".equalsIgnoreCase(tokens[0])) {
                    machineFound = machineName.equals(tokens[1]);
                }

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

    public static String getAccessToken(String endpoint)  {
        URL url = null;
        try {
            url = new URL("https://" + endpoint + "/api/users/tokens");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        StringBuilder content = new StringBuilder();

        String[] credentials = getCredentials(endpoint);
        String username;
        String password;

        if (credentials == null) {
            try {
                throw new Exception("Failed to retrieve user credentials for endpoint: " + endpoint);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        username = credentials[0];
        password = credentials[1];

        String auth = username + ":" + password;

        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        }
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        int status = 0;
        try {
            status = connection.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (status == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while (true) {
                    try {
                        if (!((inputLine = in.readLine()) != null)) break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    content.append(inputLine).append("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                throw new Exception("Failed to fetch content. HTTP status code: " + status);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String access_token = "empty";
        JSONArray tokenJSonArray = new JSONArray(content.toString());
        try {
            for (int i = 0; i < tokenJSonArray.length(); i++) {
                JSONObject jsonObject = tokenJSonArray.getJSONObject(i);

                if (jsonObject.has("access_token")) {
                    access_token = jsonObject.getString("access_token");
                     i = tokenJSonArray.length() + 1;
                }


            }
        } catch (JSONException e) {
            System.out.println("Error: " + e.getMessage());
        }

        if (access_token.contains("empty")) {
            try {
                access_token = generateAccessToken(endpoint);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

        if (credentials == null) {
            throw new Exception("Failed to retrieve user credentials for endpoint: " + endpoint);
        }

        username = credentials[0];
        password = credentials[1];

        String auth = username + ":" + password;

        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

        int status = connection.getResponseCode();

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

        String[] columnNames = {"Title", "HREF"};
        Object[][] dataSearchResult = new Object[dataArray.length()][2];


        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataURL = dataArray.getJSONObject(i);
            if (dataURL.has("temporal")) {
                dataSearchResult[i][0] = dataURL.getString("title");
                dataSearchResult[i][1] = dataURL.getString("href");
            }
        }

        DefaultTableModel model = new DefaultTableModel(dataSearchResult, columnNames);

        JTable searchResultTable = new JTable(model);

        searchResultTable.getColumnModel().getColumn(1).setCellRenderer(new LinkCellRenderer());

        searchResultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = searchResultTable.rowAtPoint(e.getPoint());
                int col = searchResultTable.columnAtPoint(e.getPoint());

                if (col == 1) { // If the link column is clicked
                    String url = (String) searchResultTable.getValueAt(row, col);
                    if (url != null && !url.isEmpty()) { // Ensure the URL is not null or empty
                        try {
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

