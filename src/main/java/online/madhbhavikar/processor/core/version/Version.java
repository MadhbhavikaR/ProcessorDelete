
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
package online.madhbhavikar.processor.core.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
    A class for version information of the application.
 */
public final class Version implements VersionData {
    private static final Logger LOGGER = LoggerFactory.getLogger(Version.class);

    private Version() {
    }


    public static void printVersion() {
        try (
                InputStream inputStream = Version.class.getClass().getResourceAsStream("/logo");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            StringBuilder stringBuilder = new StringBuilder();
            int length = readLogoFile(bufferedReader, stringBuilder);
            appendVersionInfo(stringBuilder, length);
            stringBuilder.append("\n\n");

            LOGGER.info(stringBuilder.toString());
        } catch (IOException e) {
            // ignore
        }
    }

    private static int readLogoFile(BufferedReader bufferedReader, StringBuilder stringBuilder) throws IOException {
        int length = 0;
        String line;
        stringBuilder.append("\n\n\n");

        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
            if (length < line.length()) {
                length = line.length();
            }
        }
        return length;
    }

    private static void appendVersionInfo(StringBuilder stringBuilder, int length) {
        final int headerLength = TITLE.length() + VERSION.length() + BUILD_HASH.length() + 3;
        final int urlLength = URL.length() + ORG_URL.length() + 1;

        stringBuilder.append("\n");
        stringBuilder.append(applyPadding(TITLE + " v" + VERSION + "." + BUILD_HASH, getDifference(length, headerLength)));
        stringBuilder.append("\n");
        stringBuilder.append(applyPadding(URL + " " + ORG_URL, getDifference(length, urlLength)));
    }

    private static int getDifference(int bufferLength, int stringLength) {
        int difference = 0;

        if (stringLength < bufferLength) {
            difference = (bufferLength - stringLength) / 2;
        }
        return difference;
    }

    private static String applyPadding(String string, int difference) {
        StringBuilder leftPadding = new StringBuilder();

        if (difference > 1) {
            for (int i = 0; i < difference; i++) {
                leftPadding.append(" ");
            }
        }

        leftPadding.append(string);
        return leftPadding.toString();
    }

}

