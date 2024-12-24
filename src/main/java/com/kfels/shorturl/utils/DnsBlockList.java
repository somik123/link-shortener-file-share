package com.kfels.shorturl.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class DnsBlockList {

    private static final Logger LOG = Logger.getLogger(DnsBlockList.class.getName());

    private static final String url = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/pro.txt";
    private static final String file = "./db/block.list";

    public static void updateBlockList() {
        String tmpFile = String.format("%s.tmp", file);

        deleteFile(tmpFile);
        deleteFile(file);

        if (downloadFileFromUrl(url, tmpFile)) {
            removeDuplicates(tmpFile, file);
            deleteFile(tmpFile);
        }
    }

    public static boolean checkBlockList(String url) {
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String domain = getDomainFromUrl(url);
            while ((line = br.readLine()) != null) {
                if (line.equals(domain))
                    return true;
            }
        } catch (Exception e) {
            CommonUtils.logErrors(LOG, e);
        }
        return false;
    }

    private static String getDomainFromUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost().toLowerCase();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private static boolean downloadFileFromUrl(String urlStr, String savePath) {
        try {
            URL url = new URI(urlStr).toURL();
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            FileOutputStream fis = new FileOutputStream(savePath);
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                fis.write(buffer, 0, count);
            }
            fis.close();
            bis.close();
            return true;
        } catch (Exception e) {
            CommonUtils.logErrors(LOG, e);
            return false;
        }
    }

    private static boolean removeDuplicates(String inputFile, String outputFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            Set<String> uniqueLines = new HashSet<>();
            String line;
            String domain;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;

                domain = line.startsWith("www.") ? line.substring(4) : line;
                if (uniqueLines.add(domain)) {
                    bw.write(domain);
                    bw.newLine();
                }
            }
            return true;

        } catch (Exception e) {
            CommonUtils.logErrors(LOG, e);
            return false;
        }
    }

    private static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists() && !file.isDirectory()) {
            file.delete();
        }
    }
}
