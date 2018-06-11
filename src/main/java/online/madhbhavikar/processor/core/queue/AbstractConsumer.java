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

package online.madhbhavikar.processor.core.queue;

import online.madhbhavikar.processor.core.domain.LogicFiles;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.strategy.ErrorHandlingStrategy;
import online.madhbhavikar.processor.core.strategy.Log;
import online.madhbhavikar.processor.exception.ProcessingException;

public abstract class AbstractConsumer implements Consumer {
    private ErrorHandlingStrategy errorHandlingStrategy = new Log();
    private final QueueType queueType;
    private final String id;

    public AbstractConsumer(QueueType queueType) {
        this.queueType = queueType;
        this.id = this.getClass().getName();
    }

    @Override
    public ErrorHandlingStrategy getErrorHandlingStrategy() {
        return errorHandlingStrategy;
    }

    public void setErrorHandlingStrategy(ErrorHandlingStrategy errorHandlingStrategy) {
        this.errorHandlingStrategy = errorHandlingStrategy;
    }

    @Override
    public String getUniqueId() {
        return id;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    @Override
    public void registerConsumer() throws ProcessingException {
        ProcessingQueue.registerAsConsumer(queueType, this);
    }

    @Override
    public void unRegisterConsumer(String srcFile) throws ProcessingException {
        ProcessingQueue.unRegisterConsumer(queueType, this);
        closeLogicFile(srcFile);
    }

    private void closeLogicFile(String srcFile) {
        if (QueueType.OUTPUT.equals(queueType)) {
            LogicFiles.getInstance().removeSourceFile(srcFile);
        }
    }
}
