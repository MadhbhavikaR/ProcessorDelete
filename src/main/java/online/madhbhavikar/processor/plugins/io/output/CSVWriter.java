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

package online.madhbhavikar.processor.plugins.io.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import online.madhbhavikar.processor.core.domain.Data;
import online.madhbhavikar.processor.core.domain.LogicFiles;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.queue.AbstractConsumer;
import online.madhbhavikar.processor.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: Handle nested Json Files

public class CSVWriter extends AbstractConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CSVWriter.class);

    private static final String CRLF = "\r\n";
    private final String sourceFile;
    private final String separator;
    private boolean isHeaderWritten = false;
    private List<String> headers;
    private final BufferedWriter writer;

    public CSVWriter(String sourceFile) throws IOException {
        super(QueueType.OUTPUT);
        this.sourceFile = sourceFile;
        LogicFiles logicFile = LogicFiles.getInstance();
        final String logicFilePath = LogicFiles.getLogicFilePath(sourceFile);
        this.separator = logicFile.getStringProperty(logicFilePath, "separator", ",");
        this.headers = new ArrayList<>();

        String targetFilePath = logicFile.getStringProperty(logicFilePath, "output_directory", sourceFile);
        if (".".equals(targetFilePath)) {
            targetFilePath = sourceFile;
        }
        File file = new File(sourceFile + ".csv");
        writer = new BufferedWriter(new FileWriter(file));

    }

    @Override
    public boolean onMessage(Data data) {

        final ObjectNode processedData = data.getProcessedData();
        final Iterator<String> header = processedData.fieldNames();
        final Iterator<JsonNode> column = processedData.elements();
        final List<String> element = new ArrayList<>();
        while (header.hasNext()) {
            String headerName = header.next();
            final JsonNode columnValue = column.next();
            if (!headers.contains(headerName)) {
                headers.add(headerName);
            }
            element.add(columnValue.asText());
        }
        try {
            if (!isHeaderWritten) {
                writer.write(createCSVLine(headers));
                isHeaderWritten = true;
            }
            writer.write(createCSVLine(element));
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            if (data.getMetaData().isLastRecord()) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                } finally {
                    try {
                        unRegisterConsumer(this.sourceFile);
                    } catch (ProcessingException e) {
                        LOGGER.error("", e);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String getPluginName() {
        return "CSV Writer";
    }

    @Override
    public String getPluginVersion() {
        return "v0.0.1";
    }


    private String createCSVLine(List<String> data) {
        StringBuilder stringBuilder = new StringBuilder();
        int index = 0;
        //FIXME Handle missing data gracefully
        for (String column : data) {
            column = column.replace("\"", "\"\"");
            column = "\"" + column + "\"";
            stringBuilder.append(column);
            if (index + 1 < data.size()) {
                stringBuilder.append(separator);
            }
            index++;
        }
        stringBuilder.append(CRLF);
        return stringBuilder.toString();
    }
}


