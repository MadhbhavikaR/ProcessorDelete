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
import online.madhbhavikar.processor.core.domain.MetaData;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.queue.AbstractConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOperation extends AbstractConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoOperation.class);
    private static final LogicFiles logicFile = LogicFiles.getInstance();

    public NoOperation(String sourceFile) {
        super(QueueType.OUTPUT);
    }

    @Override
    public boolean onMessage(Data data) {
        LOGGER.warn("NOOP[{}]", data);
        return true;
    }

    private MetaData writeMetaData(final Data data, final String classifier) {
        final MetaData metaData = data.getMetaData();
        return new MetaData(metaData.getSourceFile(), metaData.getLogicFile(), classifier, metaData.getCustomHeaders(), metaData.isLastRecord());
    }

    @Override
    public String getPluginName() {
        return "No Operation";
    }

    @Override
    public String getPluginVersion() {
        return "v0.0.1";
    }
}
