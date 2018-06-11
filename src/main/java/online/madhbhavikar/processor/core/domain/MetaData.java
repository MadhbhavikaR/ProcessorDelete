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

public class MetaData {

    private final String sourceFile;
    private final String logicFile;
    private final String classifier;
    private final CustomHeaders customHeaders;
    private final boolean isLastRecord;

    public MetaData(final String sourceFile, final String logicFile, final String classifier, final boolean isLastRecord) {
        this.sourceFile = sourceFile;
        this.logicFile = logicFile;
        this.classifier = classifier;
        this.customHeaders = null;
        this.isLastRecord = isLastRecord;
    }

    public MetaData(final String sourceFile, final String logicFile, final String classifier, final CustomHeaders customHeaders, final boolean isLastRecord) {
        this.sourceFile = sourceFile;
        this.logicFile = logicFile;
        this.classifier = classifier;
        this.customHeaders = customHeaders;
        this.isLastRecord = isLastRecord;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getLogicFile() {
        return logicFile;
    }

    public String getClassifier() {
        return classifier;
    }

    public CustomHeaders getCustomHeaders() {
        return customHeaders;
    }

    public boolean isLastRecord() {
        return isLastRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaData metaData = (MetaData) o;

        if (isLastRecord != metaData.isLastRecord) return false;
        if (!sourceFile.equals(metaData.sourceFile)) return false;
        if (!logicFile.equals(metaData.logicFile)) return false;
        if (!classifier.equals(metaData.classifier)) return false;
        return customHeaders != null ? customHeaders.equals(metaData.customHeaders) : metaData.customHeaders == null;
    }

    @Override
    public int hashCode() {
        int result = sourceFile.hashCode();
        result = 31 * result + logicFile.hashCode();
        result = 31 * result + classifier.hashCode();
        result = 31 * result + (customHeaders != null ? customHeaders.hashCode() : 0);
        result = 31 * result + (isLastRecord ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "sourceFile='" + sourceFile + '\'' +
                ", logicFile='" + logicFile + '\'' +
                ", classifier='" + classifier + '\'' +
                ", customHeaders=" + customHeaders +
                ", isLastRecord=" + isLastRecord +
                '}';
    }
}