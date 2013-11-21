package edu.nclab.swimudpserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: taiwoo
 * Date: 2013. 11. 20.
 * Time: 오후 3:25
 * To change this template use File | Settings | File Templates.
 */
public class FileLogger {

    public static final String ROOT_PATH = ".";
    public static final String OUTPUT_PATH = "/log";

    private File outputFile;
    FileWriter fileWriter;
    BufferedWriter out;


    private String globalPrefix = "";

    public FileLogger(String globalPrefix) {
        this.globalPrefix = globalPrefix;
        openFile("");
    }

    public void refreshLog(String prefix) {
        closeFile();
        openFile(prefix);
    }

    private void openFile(String prefix) {
        String filename = String.format("%s-%s-%s.txt", globalPrefix, prefix, new Date());
        String path = ROOT_PATH + OUTPUT_PATH;
        File resultPath = new File(path);

        Logger.d(path);

        if (!resultPath.exists()) {
            resultPath.mkdirs();
        }
        try {
            outputFile = new File(path, filename);
            fileWriter = new FileWriter(outputFile);
            out = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeFile() {
        try {
            out.close();
            out = null;

            fileWriter.close();
            fileWriter = null;

            outputFile = null;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String data) {
        try {
            out.write(data + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
