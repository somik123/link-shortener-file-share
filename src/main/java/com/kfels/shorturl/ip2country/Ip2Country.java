package com.kfels.shorturl.ip2country;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ip2Country {
    public static final Logger LOG = Logger.getLogger(Ip2Country.class.getName());

    public static boolean isAccessAllowed(String ip) {
        String country = getCountryByIp(ip);
        if (country == null || country.isEmpty()) {
            LOG.warning("Country is null or empty for IP: " + ip);
            return true; // Return true if country cannot be determined
        }
        return isCountryAllowed(country);
    }

    public static boolean isCountryAllowed(String country) {
        String allowedCountries = System.getenv("ALLOWED_COUNTRIES");
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            LOG.warning("ALLOWED_COUNTRIES environment variable is not set.");
            return true; // Default to true if not set
        } else if (allowedCountries.equalsIgnoreCase("all")) {
            LOG.info("All countries are allowed.");
            return true; // Allow all countries if set to "all"
        }

        if (country.equalsIgnoreCase("-") || country.equalsIgnoreCase("unknown")) {
            LOG.warning("Country not found");
            return true; // Return true if country cannot be determined or is internal IP address
        }

        // Check if the country is in the allowed list
        LOG.info("Checking if country is allowed: " + country);
        String[] countries = allowedCountries.split(",");
        for (String c : countries) {
            if (c.trim().equalsIgnoreCase(country)) {
                LOG.info("Country allowed: " + country);
                return true;
            }
        }
        LOG.warning("Country not allowed: " + country);
        return false;
    }

    public static String getCountryByIp(String ip) {
        String ip2cUrl = System.getenv("IP2C_URL");
        if (ip2cUrl == null || ip2cUrl.isEmpty()) {
            LOG.warning("IP2C_URL environment variable is not set.");
            return null;
        }

        String page = getPage(ip2cUrl + ip);
        if (page == null || page.isEmpty()) {
            LOG.warning("No data received for IP: " + ip);
            return null;
        }

        try {
            Pattern pattern = Pattern.compile("\"(ip|dec|country|confidence)\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(page);

            Map<String, String> result = new LinkedHashMap<>();
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                result.put(key, value);
            }

            float confidence = Float.parseFloat(result.getOrDefault("confidence", "0").replace("%", ""));
            if (confidence < 20) {
                LOG.warning("Low confidence for IP: " + ip + ", confidence: " + confidence);
                return null;
            }
            LOG.info("Country found for IP: " + ip + ", Country: " + result.get("country") +
                    ", Confidence: " + confidence);
            return result.get("country");
        } catch (Exception e) {
            LOG.severe("Error parsing IP2C response: " + e.getMessage());
        }
        return null;
    }

    public static String getPage(String url) {
        try {
            URL urlObj = URI.create(url).toURL();
            if (urlObj == null) {
                LOG.warning("Invalid URL: " + url);
                return null;
            }

            LOG.info("Fetching data from: " + urlObj.toString());
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            String response = new String(conn.getInputStream().readAllBytes());
            LOG.info("Response received: \n" + response);
            return response;
        } catch (Exception e) {
            LOG.severe("Error fetching page: " + e.getMessage());
            return null;
        }
    }
}
