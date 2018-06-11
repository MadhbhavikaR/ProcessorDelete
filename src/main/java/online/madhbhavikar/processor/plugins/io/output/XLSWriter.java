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

import online.madhbhavikar.processor.core.domain.Data;
import online.madhbhavikar.processor.core.domain.LogicFiles;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.queue.AbstractConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XLSWriter extends AbstractConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(XLSWriter.class);
    private static final LogicFiles logicFile = LogicFiles.getInstance();
    private final String srcFileName;

    public XLSWriter(String sourceFile) {
        super(QueueType.OUTPUT);
        this.srcFileName = sourceFile;
    }

    @Override
    public String getUniqueId() {
        return super.getUniqueId();
    }

    @Override
    public boolean onMessage(Data data) {
        LOGGER.info("[{}]", data);
        return true;
    }

    @Override
    public String getPluginName() {
        return "Excel File Writer";
    }

    @Override
    public String getPluginVersion() {
        return "v0.0.1";
    }
}

