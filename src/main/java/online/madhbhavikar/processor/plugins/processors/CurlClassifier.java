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

package online.madhbhavikar.processor.plugins.processors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import online.madhbhavikar.processor.core.domain.Data;
import online.madhbhavikar.processor.core.domain.LogicFiles;
import online.madhbhavikar.processor.core.domain.MetaData;
import online.madhbhavikar.processor.core.domain.constants.QueueType;
import online.madhbhavikar.processor.core.queue.AbstractConsumer;
import online.madhbhavikar.processor.core.queue.Producer;
import online.madhbhavikar.processor.exception.ProcessingException;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.toilelibre.libe.curl.Curl.curl;

public class CurlClassifier extends AbstractConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurlClassifier.class);
    private static final LogicFiles logicFile = LogicFiles.getInstance();
    private static final Producer PRODUCER = new Producer(QueueType.OUTPUT);     // TODO: create producer for input and error too

    public CurlClassifier() {
        super(QueueType.INPUT);
    }

    @Override
    public boolean onMessage(Data data) {
        try {
            Data newData = search(data);
            PRODUCER.produce(newData);
        } catch (ProcessingException e) {
            LOGGER.error("", e);
        }
        return true;
    }

    @Override
    public String getPluginName() {
        return "CurlClassifier Classifier";
    }

    @Override
    public String getPluginVersion() {
        return "v0.0.1";
    }

    private MetaData writeMetaData(final Data receivedData, final String classifier) {
        final MetaData metaData = receivedData.getMetaData();
        return new MetaData(metaData.getSourceFile(), metaData.getLogicFile(), classifier, metaData.getCustomHeaders(), metaData.isLastRecord());
    }

    private Data search(Data receivedData) {
        // TODO: add the classifier dynamically so that we can make use of the multi classifier
        String classifier = logicFile.getStringProperty(receivedData.getMetaData().getLogicFile(), "writer_class", "online.madhbhavikar.processor.plugins.io.output.NoOperation");
        ObjectNode dataNode = receivedData.getProcessedData();
        String logicFileName = receivedData.getMetaData().getLogicFile();
        String defaultGroup = logicFile.getStringProperty(logicFileName, "classifier_default_group", "Not Found");
        String command = logicFile.getStringProperty(logicFileName, "curl");
        if (null == command || command.isEmpty()) {
            dataNode.put("classifier", defaultGroup);
            return new Data(dataNode, writeMetaData(receivedData, classifier));
        }
        String placeHolder = logicFile.getStringProperty(logicFileName, "place_holder", "\\{(\\w*)\\}");
        Map<String, String> classifiers = loadClassifiers(logicFileName);


        Pattern pattern = Pattern.compile(placeHolder);
        Matcher matcher = pattern.matcher(command);
        for (int i=1; matcher.find(); i++) {
            String group = matcher.group(i);
            if (null != group && !group.isEmpty()) {
                String query = dataNode.get(group).asText();
                command = command.replace("{" + group + "}", query);
            } else {
                dataNode.put("classifier", defaultGroup);
                return new Data(dataNode, writeMetaData(receivedData, classifier));
            }
            // Don't know what is the time complexity penalty we have here, need to come up with an optimum algo
            matcher.reset();
            matcher = pattern.matcher(command);
        }
        LOGGER.info("Invoking command [{}]", command);

        Set<String> matchedClassifiers = new HashSet<>();
        try {
            HttpResponse response = curl(command);
            if (response.getStatusLine().getStatusCode() == 200) {
                BufferedReader rd = null;
                rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                result.trimToSize();
                LOGGER.debug("{}", result);

                for (Map.Entry<String, String> possibleClassifier : classifiers.entrySet()) {
                    if (result.toString().matches(possibleClassifier.getKey())) {
                        matchedClassifiers.add(possibleClassifier.getValue());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        if (matchedClassifiers.isEmpty()) {
            dataNode.put("classifier", defaultGroup);
        } else {
            dataNode.put("classifier", String.join(",", matchedClassifiers));
        }

        return new

                Data(dataNode, writeMetaData(receivedData, classifier));
    }

    private Map<String, String> loadClassifiers(String logicFileName) {
        Map<String, String> classifiers = new HashMap<>();
        for (int i = 1; ; i++) {
            String classifier = logicFile.getStringProperty(logicFileName, "classifier_" + i);
            String group = logicFile.getStringProperty(logicFileName, "classifier_group_" + i);

            if (null == classifier || classifier.isEmpty() || null == group || group.isEmpty()) {
                break;
            }
            classifiers.put(classifier, group);
        }
        return classifiers;
    }
}
