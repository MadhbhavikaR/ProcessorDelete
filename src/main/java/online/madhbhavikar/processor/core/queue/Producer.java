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

import online.madhbhavikar.processor.core.domain.Data;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.exception.ProcessingException;
import online.madhbhavikar.processor.exception.QueueNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Producer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Producer.class);
    private final QueueType queueType;

    public Producer(QueueType queueType) {
        this.queueType = queueType;
    }

    public void produce(Data data) throws ProcessingException {
        LOGGER.debug("Producing Data [{}] for Queue [{}]", data, this.queueType);
        boolean retry;
        try {
            do {
                retry = (ProcessingQueue.size(queueType) == ProcessingQueue.capacity(queueType))
                        || !ProcessingQueue.offer(queueType, data);
                if (retry) {
                    synchronized (ProcessingQueue.getWriteSemaphore(queueType)) {
                        LOGGER.warn("Processing Queue [{}] is full waiting for a slot", queueType);
                        ProcessingQueue.getWriteSemaphore(queueType).wait();
                    }
                }
            } while (retry);
        } catch (QueueNotFoundException | InterruptedException e) {
            throw new ProcessingException(e);
        }
    }
}
