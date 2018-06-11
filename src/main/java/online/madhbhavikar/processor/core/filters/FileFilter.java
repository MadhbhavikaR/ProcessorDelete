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

package online.madhbhavikar.processor.core.filters;

import online.madhbhavikar.processor.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class FileFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileFilter.class);
//    public static final String LOGIC_FILE_EXTENSION_NAME = "logic";
    public static final String LOGIC_FILE_EXTENSION = ".logic";

    private FileFilter() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    public static Map<ApplicationFile, Set<String>> filter(ApplicationFile fileType, File path) {

        switch (fileType) {
            case LOGIC_FILES:
                final Map<ApplicationFile, Set<String>> logicFiles = getLogicFiles(path);
                logicFiles.remove(ApplicationFile._LOGIC_FILE_NAME);
                LOGGER.debug("Detecting Logic files from path [{}]", path);
                return logicFiles;

            case SOURCE_FILES:
            default:
                LOGGER.debug("Detecting application files from path [{}]", path);
                return getApplicationFiles(path);
        }
    }

    private static Map<ApplicationFile, Set<String>> getApplicationFiles(final File path) {
        final Set<String> sourceFileSet = new HashSet<>();
        Map<ApplicationFile, Set<String>> applicationFileMap = new EnumMap<>(ApplicationFile.class);
        if (path.isDirectory()) {
            final String[] allFileList = path.list();
            applicationFileMap = getLogicFiles(path);
            final Set<String> logicFileNameSet = applicationFileMap.get(ApplicationFile._LOGIC_FILE_NAME);
            String[] logicFileNameArray = logicFileNameSet.toArray(new String[0]);
            int index;
            for (String file : allFileList) {
                index = 0;
                for (String logicFileName : logicFileNameSet) {
                    try {
                        logicFileNameArray[index] = getFileName(logicFileName);
                        if(logicFileName.substring(0, logicFileName.lastIndexOf(LOGIC_FILE_EXTENSION)).equalsIgnoreCase(file.toLowerCase())) {
                            LOGGER.debug("Adding source file [{}{}{}]", path.getAbsolutePath(), File.separator, file);
                            sourceFileSet.add(path.getAbsolutePath() + File.separator + file);
                        }
                    } catch (ProcessingException e) {
                        continue;
                    }
                    index++;
                }
            }
        } else {
            applicationFileMap.put(ApplicationFile.LOGIC_FILES, new HashSet<>());
        }
        applicationFileMap.put(ApplicationFile.SOURCE_FILES, sourceFileSet);
        applicationFileMap.remove(ApplicationFile._LOGIC_FILE_NAME);
        return applicationFileMap;
    }

    private static String getFileName(final String logicFilePath) throws ProcessingException {
        String logicFilePathLower = logicFilePath.toLowerCase();
        if (logicFilePathLower.endsWith(LOGIC_FILE_EXTENSION)) {
            int dot = logicFilePathLower.lastIndexOf(LOGIC_FILE_EXTENSION);
            return logicFilePath.substring(0, dot);
        }
        throw new ProcessingException("Logic File not found");
    }

    private static Map<ApplicationFile, Set<String>> getLogicFiles(File path) {
        Map<ApplicationFile, Set<String>> logicMap = new EnumMap<>(ApplicationFile.class);
        String[] files = path.list((dir, name) -> name.toLowerCase().endsWith(LOGIC_FILE_EXTENSION));

        String[] _files = files.clone();
        for (int i = 0; i < files.length; i++) {
            files[i] = path.getAbsolutePath() + File.separator + files[i];
        }
        logicMap.put(ApplicationFile.LOGIC_FILES, new HashSet<>(Arrays.asList(files)));
        logicMap.put(ApplicationFile._LOGIC_FILE_NAME, new HashSet<>(Arrays.asList(_files)));
        return logicMap;
    }
}
