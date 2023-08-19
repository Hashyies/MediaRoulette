package me.hash.mediaroulette.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequest {
    public static String get(String url) {
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
        try {
            // Create connection
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
    
            // Get response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response.toString();
    }

    public static String post(String url, String data) {
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
        try {
            // Create connection
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("POST");
            connection.setInstanceFollowRedirects(true);
            connection.setDoOutput(true);
    
            // Send data
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                writer.write(data);
                writer.flush();
            }
    
            // Get response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response.toString();
    }
    
    public static String put(String url, String data) {
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
        try {
            // Create connection
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("PUT");
            connection.setInstanceFollowRedirects(true);
            connection.setDoOutput(true);
    
            // Send data
            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
                writer.write(data);
                writer.flush();
            }
    
            // Get response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response.toString();
    }
    
    public static String delete(String url) {
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
        try {
            // Create connection
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setInstanceFollowRedirects(true);
    
            // Get response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (connection != null) {
              connection.disconnect();
          }
        }
        return response.toString();
    }    
}