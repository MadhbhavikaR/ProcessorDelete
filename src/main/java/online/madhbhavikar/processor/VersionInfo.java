
/*
 * MIT License
 *
 * Copyright (c) 2018. MadhbhavikaR <connected.madhbhavikar@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * THIS IS A DYNAMICALLY GENERATED CLASS, DO NOT EDIT, WOULD BE OVERWRITTEN
 */
package online.madhbhavikar.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class VersionInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionInfo.class);

    private VersionInfo() {
    }

    public static final String VERSION = "0.0.1";
    public static final String BUILD_HASH = "developer";
    public static final String VENDOR = "MadhbhavikaR";
    public static final String TITLE = "Processor";
    public static final String ORG_URL = "http://www.madhbhavikar.online";
    public static final String URL = "http://processor.madhbhavikar.online";

    public static void printVersion() {
        java.net.URL fileUrl = VersionInfo.class.getResource("/logo");
        final File file = new File(fileUrl.getFile());
        try (
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
            LOGGER.info("{} version {}-{}", VersionInfo.TITLE, VersionInfo.VERSION, VersionInfo.BUILD_HASH);
            LOGGER.info("Project Page: {}", VersionInfo.URL);
            LOGGER.info("Powered by: {}", VersionInfo.ORG_URL);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

}

