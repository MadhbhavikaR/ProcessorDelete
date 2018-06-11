/*
 * Copyright (c) 2018 MadhbhavikaR
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package online.madhbhavikar.processor.plugins.domain;

import online.madhbhavikar.processor.core.domain.CustomHeaders;

public class XLSX implements CustomHeaders {
    private final String sheetName;
    private final int sheetNo;
    private final int sheetCount;
    private final int rowNumber;
    private final int rowCount;

    public XLSX(String sheetName, int sheetNo, int rowNumber, int rowCount, int sheetCount) {
        this.sheetName = sheetName;
        this.sheetNo = sheetNo;
        this.rowNumber = rowNumber;
        this.rowCount = rowCount;
        this.sheetCount = sheetCount;
    }

    public String getSheetName() {
        return sheetName;
    }

    public int getSheetNo() {
        return sheetNo;
    }

    public int getSheetCount() {
        return sheetCount;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public int getRowCount() {
        return rowCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XLSX xlsx = (XLSX) o;

        if (sheetNo != xlsx.sheetNo) return false;
        if (sheetCount != xlsx.sheetCount) return false;
        if (rowNumber != xlsx.rowNumber) return false;
        if (rowCount != xlsx.rowCount) return false;
        return sheetName.equals(xlsx.sheetName);
    }

    @Override
    public int hashCode() {
        int result = sheetName.hashCode();
        result = 31 * result + sheetNo;
        result = 31 * result + sheetCount;
        result = 31 * result + rowNumber;
        result = 31 * result + rowCount;
        return result;
    }

    @Override
    public String toString() {
        return "XLSX{" +
                "sheetName='" + sheetName + '\'' +
                ", sheetNo=" + sheetNo +
                ", sheetCount=" + sheetCount +
                ", rowNumber=" + rowNumber +
                ", rowCount=" + rowCount +
                '}';
    }
}
