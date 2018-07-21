package com.wy.sofix.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

/**
 * @author cantalou
 * @date 2018-07-08 16:34
 */
public class IoUtilTest {

    private File tempFile;

    private InputStream inputStream;

    @Before
    public void setUp() throws Exception {

        tempFile = new File("tempFile");
        if (tempFile.exists()) {
            tempFile.delete();
        }

        inputStream = new ByteArrayInputStream("test".getBytes("UTF-8"));
    }

    @After
    public void tearDown() throws Exception {
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    @Test
    public void copy() throws IOException {
        IoUtil.copy(inputStream, tempFile);
        assertEquals("test", getFileContent());
    }

    private String getFileContent() throws IOException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(tempFile), "UTF-8"));
            return bufferedReader.readLine();
        } finally {
            IoUtil.closeSilent(bufferedReader);
        }
    }
}