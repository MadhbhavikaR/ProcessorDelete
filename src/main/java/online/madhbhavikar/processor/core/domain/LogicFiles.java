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

package online.madhbhavikar.processor.core.domain;

import online.madhbhavikar.processor.core.filters.ApplicationFile;
import online.madhbhavikar.processor.core.filters.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LogicFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogicFiles.class);
    private static final String EMPTY = "";
    private static final LogicFiles instance = new LogicFiles();
    private final Map<ApplicationFile, Set<String>> inputPathList;
    //TODO: Optimize this storage for lists and sets from the properties file
    private final Map<String, Properties> logicMap;

    private LogicFiles() {
        this.inputPathList = new EnumMap<>(ApplicationFile.class);
        this.logicMap = new ConcurrentHashMap<>();
        this.inputPathList.put(ApplicationFile.LOGIC_FILES, new HashSet<>());
        this.inputPathList.put(ApplicationFile.SOURCE_FILES, new HashSet<>());
    }

    public boolean addPath(String path) {
        File directory = new File(path);
        if (directory.isDirectory()) {
            final Map<ApplicationFile, Set<String>> fileList = FileFilter.filter(ApplicationFile.SOURCE_FILES, directory);
            Set<String> logicSet = this.inputPathList.get(ApplicationFile.LOGIC_FILES);
            logicSet.addAll(fileList.get(ApplicationFile.LOGIC_FILES));
            this.inputPathList.put(ApplicationFile.LOGIC_FILES, logicSet);

            final Set<String> sourceSet = this.inputPathList.get(ApplicationFile.SOURCE_FILES);
            sourceSet.addAll(fileList.get(ApplicationFile.SOURCE_FILES));
            this.inputPathList.put(ApplicationFile.SOURCE_FILES, sourceSet);
            for (String logicFilepath : this.inputPathList.get(ApplicationFile.LOGIC_FILES)) {
                try (InputStream input = new FileInputStream(logicFilepath)) {
                    Properties properties = new Properties();
                    if (!logicMap.containsKey(logicFilepath)) {
                        properties.load(input);
                        LOGGER.debug("Loaded Logic for File [{}]", logicFilepath);
                        logicMap.put(logicFilepath, properties);
                    }
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
            return true;
        }
        LOGGER.error("[{}] is not a valid directory ignoring", directory);
        return false;
    }

    public boolean removeSourceFile(String path) {
        if (this.inputPathList.get(ApplicationFile.SOURCE_FILES).contains(path)) {
            String logicFilePath = getLogicFilePath(path);
            this.inputPathList.get(ApplicationFile.SOURCE_FILES).remove(path);
            this.inputPathList.get(ApplicationFile.LOGIC_FILES).remove(logicFilePath);
            Properties properties = this.logicMap.remove(logicFilePath);
            properties.clear();
            LOGGER.info("Cleared Logic for Source [{}]", path);
            return true;
        }
        return false;
    }

    public static String getLogicFilePath(String path) {
        if (path.isEmpty()) {
            return "";
        }
        return path.concat(".logic");
    }

    public int addPaths(String[] paths) {
        int addedCount = 0;
        if (paths.length > 0) {
            for (String path : paths) {
                if (addPath(path)) {
                    addedCount++;
                }
            }
        }
        if (this.inputPathList.get(ApplicationFile.LOGIC_FILES).isEmpty() || this.inputPathList.get(ApplicationFile.SOURCE_FILES).isEmpty()) {
            LOGGER.error("Terminating Application, No processable {} found in the path" , this.inputPathList.get(ApplicationFile.LOGIC_FILES).isEmpty() ? "Logic File" : this.inputPathList.get(ApplicationFile.SOURCE_FILES).isEmpty() ? "Source File" : "Files");
            System.exit(1);
        }
        return addedCount;
    }

    public Map<ApplicationFile, Set<String>> getInputPathList() {
        return new EnumMap<>(inputPathList);
    }

    /*public Properties getLogic(final String logicPath) {
        final Properties properties = new Properties();
        properties.putAll(logicMap.get(logicPath));
        return properties;
    }*/

    public String getStringProperty(String logicFile, String property) {
        return logicMap.get(logicFile).getProperty(property);
    }

    public String getStringProperty(String logicFile, String property, String defaultValue) {
        return logicMap.get(logicFile).getProperty(property, defaultValue);
    }

    public int getIntProperty(String logicFile, String property, String defaultValue) {
        return Integer.parseInt(logicMap.get(logicFile).getProperty(property, defaultValue));
    }

    public long getLongProperty(String logicFile, String property, String defaultValue) {
        return Long.parseLong(logicMap.get(logicFile).getProperty(property, defaultValue));
    }


    //TODO Generalize this for all sets
    public Set<Integer> getIntSet(String logicFile, String property, String defaultValue) {
        Set<Integer> integerList = new LinkedHashSet<>();
        String propertyString = logicMap.get(logicFile).getProperty(property, defaultValue);
        StringTokenizer stringTokenizer = new StringTokenizer(propertyString, ",");
        while (stringTokenizer.hasMoreTokens()) {
            try {
                String sheet = stringTokenizer.nextToken();
                if (null != sheet && !sheet.isEmpty()) {
                    integerList.add(Integer.parseInt(sheet.trim()));
                }
            } catch (NumberFormatException e) {
                LOGGER.error("", e);
            }
        }
        return integerList;
    }

    public List<String> getPropertyList(String logicFile, String property){
        return getPropertyList(logicFile, property, null);
    }

    public List<String> getPropertyList(String logicFile, String property, String defaultValue) {
        List<String> stringList = new ArrayList<>();
        String propertyString = logicMap.get(logicFile).getProperty(property);
        StringTokenizer stringTokenizer = new StringTokenizer(propertyString, ",");
        while (stringTokenizer.hasMoreTokens()) {
            String entry = stringTokenizer.nextToken();
            if (null != entry && !entry.isEmpty()) {
                stringList.add(entry.trim());
            }
        }
        if(stringList.isEmpty() && null !=defaultValue && !defaultValue.isEmpty()){
            stringList.add(defaultValue);
        }
        return stringList;
    }


    public int getSourceFilesCount() {
        return this.inputPathList.get(ApplicationFile.SOURCE_FILES).size();
    }

    public static LogicFiles getInstance() {
        return instance;
    }


}
