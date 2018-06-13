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

package online.madhbhavikar.processor.core;

import online.madhbhavikar.processor.core.version.Version;
import online.madhbhavikar.processor.core.domain.LogicFiles;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.filters.ApplicationFile;
import online.madhbhavikar.processor.core.io.Reader;
import online.madhbhavikar.processor.core.queue.AbstractConsumer;
import online.madhbhavikar.processor.core.queue.Consumer;
import online.madhbhavikar.processor.core.queue.ProcessingQueue;
import online.madhbhavikar.processor.exception.ProcessingException;
import online.madhbhavikar.processor.plugins.PluginLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootStrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootStrap.class);
    private final PluginLoader pluginLoader;
    private final Map<QueueType, Map<String, Class<?>>> ioModules;
    private Map<String, Class<? extends AbstractConsumer>> classifiers;
    private static ExecutorService executorService = Executors.newFixedThreadPool(32);
    private final LogicFiles logic = LogicFiles.getInstance();

    public BootStrap() {
        this.pluginLoader = new PluginLoader();
        this.ioModules = new EnumMap<>(QueueType.class);
    }

    private void showBanner() {
        Version.printVersion();
    }

    private void loadDirectories(final String[] directoryPaths) {
        final int acceptedPaths = LogicFiles.getInstance().addPaths(directoryPaths);
        if (directoryPaths.length != acceptedPaths) {
            LOGGER.warn("Discarding invalid paths");
            LOGGER.debug("Input directories [{}], Valid file paths [{}]", directoryPaths, LogicFiles.getInstance().getInputPathList());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadKnownModules() {
        LOGGER.info("Scanning for available Plugins");
        pluginLoader.setPackagePrefix("online.madhbhavikar.processor.plugins.processors");
        this.classifiers = pluginLoader.getSubClassesOf(new Class[]{AbstractConsumer.class});
        pluginLoader.setPackagePrefix("online.madhbhavikar.processor.plugins.io.input");
        this.ioModules.put(QueueType.INPUT, pluginLoader.getClasses());
        pluginLoader.setPackagePrefix("online.madhbhavikar.processor.plugins.io.output");
        this.ioModules.put(QueueType.OUTPUT, pluginLoader.getSubClassesOf(new Class[]{AbstractConsumer.class}));
    }

    private void createQueues(final int workerThreads) {
        if (LogicFiles.getInstance().getSourceFilesCount() > 0) {
            ProcessingQueue.createQueue(QueueType.INPUT, workerThreads);
            ProcessingQueue.createQueue(QueueType.OUTPUT, workerThreads);
            ProcessingQueue.createQueue(QueueType.ERROR, workerThreads * 2);
        }
    }

    private int computeAndValidateRequirements() {
        int workerThreads = 16;
        int identifierThreads = 0;
        LOGGER.info("Validating Logic Files");
        for (String sourceFile : logic.getInputPathList().get(ApplicationFile.SOURCE_FILES)) {
            String logicFile = LogicFiles.getLogicFilePath(sourceFile);
            String reader = logic.getStringProperty(logicFile, "reader_class");
            boolean continueLoop = false;
            for (String classifier : logic.getPropertyList(logicFile, "processor_class_list")) {
                if (null == classifier || null == reader || classifier.isEmpty() || reader.isEmpty() || !classifiers.containsKey(classifier) || !ioModules.get(QueueType.INPUT).containsKey(reader)) {
                    LOGGER.warn("Skipping file [{}] due to invalid Logic parameters", sourceFile);
                    logic.removeSourceFile(sourceFile);
                    continueLoop = true;
                    break;
                }
            }
            if (continueLoop) {
                continue;
            }
            String workerProperty = logic.getStringProperty(logicFile, "worker_threads");
            if (null != workerProperty && !workerProperty.isEmpty()) {
                identifierThreads = Integer.parseInt(workerProperty);
            }
            if (identifierThreads > workerThreads) {
                workerThreads = identifierThreads;
            }
        }
        if (logic.getInputPathList().get(ApplicationFile.SOURCE_FILES).isEmpty()) {
            LOGGER.error("Terminating Application, Too many skips detected for source files, make sure the Logic files are correctly configured");
            System.exit(1);
        }

        return workerThreads;
    }

    private void initializePlugins() {
        for (String sourceFile : logic.getInputPathList().get(ApplicationFile.SOURCE_FILES)) {
            String logicFile = LogicFiles.getLogicFilePath(sourceFile);
            if(initializeProcessorPlugins(sourceFile, logicFile) && initializeReaderPlugin(sourceFile, logicFile)) {
                if(!initializeWriterPlugin(sourceFile, logicFile)){
                    LOGGER.error("Some issue occurred during initialization of the Writer plugins");
                    System.exit(1);
                }
            }
        }
    }

    private boolean initializeProcessorPlugins(String sourceFile, String logicFile) {
        for (String processorClassName : logic.getPropertyList(logicFile, "processor_class_list")) {
            Class<? extends AbstractConsumer> processorClass = classifiers.get(processorClassName);
            Consumer processor;
            try {
                processor = processorClass.getConstructor().newInstance();
                processor.registerConsumer();
                LOGGER.info("Initialized Processor Plugin [{}] [{}]", processor.getPluginName(), processor.getPluginVersion());
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | ProcessingException e) {
                LOGGER.error("Error occurred while trying to initialize Processor Plugin [{}] for source file [{}], {}", processorClass, sourceFile, e.getCause().getMessage(), e);
                return false;
            }
        }
        return true;
    }

    private boolean initializeReaderPlugin(String sourceFile, String logicFile) {
        Class<?> readerClass = ioModules.get(QueueType.INPUT).get(logic.getStringProperty(logicFile, "reader_class"));
        try {
            Reader reader = (Reader) readerClass.getConstructor(String.class).newInstance(sourceFile);
            executorService.submit((Callable<?>) reader);
            LOGGER.info("Initialized Reader Plugin [{}] [{}]", reader.getPluginName(), reader.getPluginVersion());
            return true;
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("Error occurred while trying to initialize Reader Plugin [{}] for source file [{}]", readerClass, sourceFile, e);
            return false;
        }
    }

    private boolean initializeWriterPlugin(String sourceFile, String logicFile) {
        Class<?> writerClass = ioModules.get(QueueType.OUTPUT).get(logic.getStringProperty(logicFile, "writer_class", "online.madhbhavikar.processor.plugins.io.output.NoOperation"));
        try {
            Consumer writer = (Consumer) writerClass.getConstructor(String.class).newInstance(sourceFile);
            writer.registerConsumer();
            LOGGER.info("Initialized Writer Plugin [{}] [{}]", writer.getPluginName(), writer.getPluginVersion());
            return true;
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | ProcessingException e) {
            LOGGER.error("Error occurred while trying to initialize Writer Plugin [{}] for source file [{}]", writerClass, sourceFile, e);
            return false;
        }
    }

    public void run(String[] directories) {
        showBanner();
        LOGGER.info("Boot Strapping Application");
        LOGGER.info("Scanning Directories for Source and Logic files");
        loadDirectories(directories);
        loadKnownModules();
        LOGGER.info("Initializing Queue Subsystem");
        createQueues(computeAndValidateRequirements());
        LOGGER.info("Initializing Plugin Subsystem");
        initializePlugins();
        LOGGER.info("Initializing Polling Subsystem");
        ProcessingQueue.startPoller();
        executorService.shutdown();
    }
}
