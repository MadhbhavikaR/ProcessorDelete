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

import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class ProcessingQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingQueue.class);
    private static final Map<QueueType, ProcessingQueue> qMap = new EnumMap<>(QueueType.class);
    private final Queue<Data> q;
    private final Object writeSemaphore = new Object();
    private final Object readSemaphore = new Object();
    private final int capacity;
    private final QueuePoller queuePoller;

    private ProcessingQueue(QueueType queueType, int capacity) {
        this.q = new LinkedBlockingQueue<>(capacity);
        this.capacity = capacity;
        this.queuePoller = new QueuePoller(queueType);
        LOGGER.info("Initialized [{}] queue with capacity [{}]", queueType, capacity);
    }

    private Queue<Data> getQ() {
        return q;
    }

    private Object getWriteSemaphore() {
        return writeSemaphore;
    }

    private Object getReadSemaphore() {
        return readSemaphore;
    }

    private int getCapacity() {
        return capacity;
    }

    private QueuePoller getQueuePoller() {
        return queuePoller;
    }

    private static void throwExceptionIfRequired(final QueueType queueType) throws QueueNotFoundException {
        if (!qMap.containsKey(queueType)) {
            throw new QueueNotFoundException("Queue with queueType [" + queueType + "] does not exist!");
        }
    }

    public static synchronized void createQueue(final QueueType type, final int capacity) {
        if (!qMap.containsKey(type)) {
            qMap.put(type, new ProcessingQueue(type, capacity));
        } else {
            LOGGER.debug("Skipped creating a queue with type [{}] and capacity [{}] as it is already exists with current capacity [{}]", type, capacity, qMap.get(type).getCapacity());
        }
    }

    static boolean offer(final QueueType queueType, Data data) throws QueueNotFoundException {
        throwExceptionIfRequired(queueType);
        if (null == data) {
            throw new NullPointerException("Provided Data is null");
        }
        Queue<Data> queue = qMap.get(queueType).getQ();
        boolean contains = queue.contains(data);
        if (contains) {
            LOGGER.debug("Ignored duplicate data [{}] offered to queue [{}]", data, queueType);
        }
        boolean offered = !contains && queue.offer(data);
        if (offered) {
            synchronized (ProcessingQueue.getReadSemaphore(queueType)) {
                ProcessingQueue.getReadSemaphore(queueType).notifyAll();
                LOGGER.debug("Enqueued Data [{}] in queue [{}]", data, queueType);
            }
        }
        return contains || offered;
    }

    static Data poll(final QueueType queueType) throws QueueNotFoundException {
        throwExceptionIfRequired(queueType);
        synchronized (qMap.get(queueType).getWriteSemaphore()) {
            Data data = qMap.get(queueType).getQ().poll();
            qMap.get(queueType).getWriteSemaphore().notifyAll();
            return data;
        }
    }

    public static Data peek(final QueueType queueType) throws QueueNotFoundException {
        throwExceptionIfRequired(queueType);
        return qMap.get(queueType).getQ().peek();
    }

    static int size(final QueueType queueType) throws QueueNotFoundException {
        throwExceptionIfRequired(queueType);
        return qMap.get(queueType).getQ().size();
    }

    static int capacity(final QueueType queueType) throws QueueNotFoundException {
        throwExceptionIfRequired(queueType);
        return qMap.get(queueType).getCapacity();
    }

    static Object getWriteSemaphore(final QueueType queueType) throws QueueNotFoundException {
        throwExceptionIfRequired(queueType);
        return qMap.get(queueType).getWriteSemaphore();
    }

    static Object getReadSemaphore(final QueueType queueType) throws QueueNotFoundException {
        throwExceptionIfRequired(queueType);
        return qMap.get(queueType).getReadSemaphore();
    }

    static void registerAsConsumer(final QueueType queueType, final Consumer consumer) throws ProcessingException {
        try {
            throwExceptionIfRequired(queueType);
        } catch (QueueNotFoundException e) {
            throw new ProcessingException(e);
        }
        qMap.get(queueType).getQueuePoller().registerConsumer(consumer.getUniqueId(), consumer);
    }

    static void unRegisterConsumer(final QueueType queueType, final Consumer consumer) throws ProcessingException {
        try {
            throwExceptionIfRequired(queueType);
        } catch (QueueNotFoundException e) {
            throw new ProcessingException(e);
        }
        qMap.get(queueType).getQueuePoller().unRegisterConsumer(consumer.getUniqueId());
    }

    static void unRegisterAll() {
        for (QueueType queueType : QueueType.values()) {
            qMap.get(queueType).getQueuePoller().getRegisteredConsumers().forEach((consumer) -> {
                try {
                    synchronized (ProcessingQueue.getReadSemaphore(queueType)) {
                        unRegisterConsumer(queueType, consumer);
                        ProcessingQueue.getReadSemaphore(queueType).notifyAll();
                    }
                } catch (QueueNotFoundException | ProcessingException e) {
                    LOGGER.error("", e);
                }
            });
        }
    }

    public static void startPoller() {
        for (QueueType queueType : QueueType.values()) {
            if (qMap.get(queueType).getQueuePoller().isStartable() && qMap.get(queueType).getQueuePoller().size() > 0) {
                qMap.get(queueType).getQueuePoller().start();
            }
        }
    }
}

