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
import online.madhbhavikar.processor.core.domain.MetaData;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.strategy.ErrorHandlingStrategy;
import online.madhbhavikar.processor.core.strategy.Log;
import online.madhbhavikar.processor.exception.ProcessingException;
import online.madhbhavikar.processor.exception.QueueNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class QueuePoller extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuePoller.class);
    private final QueueType queueType;
    private final Map<String, Consumer> consumerMap = new ConcurrentHashMap<>();
    private boolean hasStarted = false;

    QueuePoller(final QueueType queueType) {
        this.queueType = queueType;
        this.setName("queue-poller-" + queueType);
    }

    void registerConsumer(final String classifierId, final Consumer consumer) {
        if (!consumerMap.containsKey(classifierId)) {
            consumerMap.put(classifierId, consumer);
            LOGGER.debug("Registered Consumer for processors [{}]", classifierId);
        } else {
            LOGGER.debug("Skipped registering Consumer for processors [{}]", classifierId);
        }
    }

    void unRegisterConsumer(final String classifierId) {
        this.consumerMap.remove(classifierId);
        LOGGER.debug("unregistered Consumer for processors [{}]", classifierId);
    }

    boolean isStartable() {
        return !hasStarted;
    }

    int size() {
        return consumerMap.size();
    }

    List<Consumer> getRegisteredConsumers() {
        return new ArrayList<>(consumerMap.values());
    }

    @Override
    public void run() {
        Data data;
        LOGGER.info("Starting Queue poller for [{}]", queueType);
        hasStarted = true;
        while (!consumerMap.isEmpty()) {
            try {
                if (ProcessingQueue.size(queueType) == 0) {
                    synchronized (ProcessingQueue.getReadSemaphore(queueType)) {
                        ProcessingQueue.getReadSemaphore(queueType).wait();
                    }
                } else {
                    data = ProcessingQueue.poll(queueType);
                    notifyListeners(data);
                }
            } catch (QueueNotFoundException | InterruptedException | ProcessingException e) {
                LOGGER.error("", e);
            }
        }
        if (QueueType.OUTPUT.equals(queueType) && consumerMap.isEmpty()) {
            ProcessingQueue.unRegisterAll();
        }
        LOGGER.info("No consumers registered shutting down poller for [{}]", queueType);
    }

    private void notifyListeners(final Data data) throws ProcessingException {
        MetaData metaData = data.getMetaData();
        String classifierId = metaData.getClassifier();
        if (consumerMap.containsKey(classifierId)) {
            consumerMap.get(classifierId).onMessage(data);
        } else {
            handleError(data, classifierId);
        }
    }

    private void handleError(final Data data, final String classifierId) throws ProcessingException {
        ErrorHandlingStrategy errorHandlingStrategy = consumerMap.get(classifierId).getErrorHandlingStrategy();
        if (null == errorHandlingStrategy) {
            errorHandlingStrategy = new Log();
        }
        errorHandlingStrategy.process(data);
    }
}
