/*
 * Copyright (c) 2018 MadhbhavikaR
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package online.madhbhavikar.processor.plugins.io.input;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import online.madhbhavikar.processor.core.domain.CustomHeaders;
import online.madhbhavikar.processor.core.domain.Data;
import online.madhbhavikar.processor.core.domain.LogicFiles;
import online.madhbhavikar.processor.core.domain.MetaData;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.io.Reader;
import online.madhbhavikar.processor.core.queue.Producer;
import online.madhbhavikar.processor.exception.ProcessingException;
import online.madhbhavikar.processor.plugins.constants.Excel;
import online.madhbhavikar.processor.plugins.constants.FileTypes;
import online.madhbhavikar.processor.plugins.domain.XLSX;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class XLSReader implements Reader<Boolean> {

    private static final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private static final Logger LOGGER = LoggerFactory.getLogger(XLSReader.class);
    private static final Producer PRODUCER = new Producer(QueueType.INPUT);

    private final Set<Integer> sheets;
    private final String srcFileName;
    private final String logicFileName;
    private final String classifier;

    public XLSReader(final String srcFileName) {
        this.srcFileName = srcFileName;
        this.logicFileName = LogicFiles.getLogicFilePath(srcFileName);
        this.classifier = LogicFiles.getInstance().getPropertyList(logicFileName, "processor_class_list").get(0);
        this.sheets = LogicFiles.getInstance().getIntSet(logicFileName, "sheets_to_process", "0");
        Thread.currentThread().setName("XLSReader-" + srcFileName.substring(srcFileName.lastIndexOf(File.separator) + 1));
    }


    private boolean process() throws ProcessingException {
        FileInputStream excelFile = null;
        Workbook workbook = null;

        try {
            excelFile = new FileInputStream(new File(srcFileName));
            LOGGER.info("File [{}] opened for reading", srcFileName);
            String extension = ".*" + FileTypes.EXCEL.toString() + "$";
            if (srcFileName.toLowerCase().matches(extension)) {
                workbook = new HSSFWorkbook(excelFile);
            } else {
                workbook = new XSSFWorkbook(excelFile);
            }
            int sheetCounter = 0;
            for (int sheetNo : sheets) {
                processSheet(workbook, sheetNo, sheets.size() == ++sheetCounter);
                LOGGER.info("Reading of file [{}]:[{}], was successful!", srcFileName, sheetNo);
            }
            LOGGER.info("Reading of file [{}], was successful!", srcFileName);
        } catch (FileNotFoundException e) {
            LOGGER.error("", e);
            return false;
        } catch (IOException e) {
            throw new ProcessingException(e);
        } finally {
            try {
                closeWorkbook(excelFile, workbook);
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
        return true;
    }

    private List<String> getHeaders(final Sheet sheet) {
        List<String> headerMap = new ArrayList<>();
        if (sheet != null) {
            Row headerRow = sheet.getRow(0);
            int headerIndexCounter = 0;
            for (Cell currentCell : headerRow) {
                String cellValue = currentCell.getStringCellValue().trim();
                headerMap.add(headerIndexCounter, cellValue);
                headerIndexCounter++;
            }
        }
        LOGGER.info("Got headers [{}]", headerMap);
        return headerMap;
    }

    private void processSheet(final Workbook workbook, final int sheetNo, final boolean isLastSheet) throws ProcessingException {
        final Sheet workSheet = workbook.getSheetAt(sheetNo);
        ObjectNode jsonData;
        LOGGER.info("Reading [{}] of [{}] workbook", sheetNo, srcFileName);

        List<String> headers = getHeaders(workSheet);
        String sheetName = workSheet.getSheetName();
        int lastRowNumber = workSheet.getLastRowNum();
        for (Row currentRow : workSheet) {
            int rowNumber = currentRow.getRowNum();
            if (rowNumber > 0) {
                jsonData = jsonNodeFactory.objectNode();
                for (Cell currentCell : currentRow) {
                    extractData(currentCell, headers.get(currentCell.getColumnIndex()), jsonData);
                }
                for (String header : headers) {
                    if(null == jsonData.get(header)) {
                        jsonData.put(header, "");
                    }
                }
                Data data = new Data(jsonData, writeMetaData(sheetName, sheetNo, rowNumber, lastRowNumber, isLastSheet && lastRowNumber == rowNumber));
                dataListener(data);
            }
        }
    }

    private MetaData writeMetaData(final String sheetName, final int sheetNo, final int rowNo, final int rowCount, final boolean isEOF) {
        CustomHeaders customHeaders = new XLSX(sheetName, sheetNo, rowNo, rowCount, sheets.size());
        return new MetaData(srcFileName, logicFileName, classifier, customHeaders, isEOF);
    }

    private void extractData(final Cell currentCell, String header, ObjectNode jsonNode) {
        header = null != header && !header.isEmpty() ? header : String.valueOf(currentCell.getColumnIndex());
        switch (currentCell.getCellTypeEnum()) {
            case NUMERIC:
                Object o = currentCell.getNumericCellValue();
                // XSL tends to prefer scientific notation, so we consider this value as text
                jsonNode.put(header, new BigDecimal(o.toString()).toPlainString());
                break;
            case _NONE:
            case BLANK:
            case STRING:
                jsonNode.put(header, currentCell.getStringCellValue().replaceAll("\\s+", ""));
                break;
            case BOOLEAN:
                jsonNode.put(header, currentCell.getBooleanCellValue());
                break;
            case FORMULA:
                ObjectNode formulaNode = jsonNodeFactory.objectNode();
                formulaNode.put(header, currentCell.getCellFormula());
                jsonNode.set(Excel.FORMULA.toString(), formulaNode);
                break;
            case ERROR:
                ObjectNode errorNode = jsonNodeFactory.objectNode();
                errorNode.put(header, currentCell.getErrorCellValue());
                jsonNode.set(Excel.ERROR.toString(), errorNode);
                break;
        }
    }

    private void closeWorkbook(final FileInputStream excelFile, final Workbook workbook) throws IOException {
        if (null != excelFile) {
            excelFile.close();
        }
        if (null != workbook) {
            workbook.close();
        }
    }

    @Override
    public Boolean call() throws ProcessingException {
        return process();
    }

    @Override
    public void dataListener(final Data data) throws ProcessingException {
        LOGGER.info("Received row [{}]", data);
        PRODUCER.produce(data);
    }

    @Override
    public String getPluginName() {
        return "Excel File Reader";
    }

    @Override
    public String getPluginVersion() {
        return "v0.0.1";
    }
}
