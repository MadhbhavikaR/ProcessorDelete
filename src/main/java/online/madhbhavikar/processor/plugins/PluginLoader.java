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

package online.madhbhavikar.processor.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginLoader.class);

    private String packagePrefix;
    private boolean includeAbstractClasses;
    private boolean includeInterface;

    private Set<Class> tempClassSet;

    public PluginLoader() {
        this("", false, false);
    }

    public PluginLoader(String packagePrefix) {
        this(packagePrefix, false, false);
    }

    public PluginLoader(String packagePrefix, boolean includeAbstractClasses, boolean includeInteface) {
        this.packagePrefix = packagePrefix == null ? "" : packagePrefix;
        this.includeAbstractClasses = includeAbstractClasses;
        this.includeInterface = includeInteface;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix == null ? "" : packagePrefix;
    }

    public void setIncludeAbstractClasses(boolean includeAbstractClasses) {
        this.includeAbstractClasses = includeAbstractClasses;
    }

    public void setIncludeInterface(boolean includeInterface) {
        this.includeInterface = includeInterface;
    }

    public Map<String, Class<?>> getClasses() {
        return getSubClassesOf(null);
    }
    public <T> Map<String, Class<?>> getSubClassesOf(final Class<T>[] clazz) {
        long start = System.currentTimeMillis();
        final Map<String, Class<?>> subClazzes = new HashMap<>();
        if (packagePrefix.isEmpty()) {
            LOGGER.warn("{} will load all the classes meeting the criteria from all the jar files in the classpath, this may take huge time", this.getClass().getCanonicalName());
        }

        Set<Class> classes;
        try {
            classes = getClassesUnderPackagePrefix(packagePrefix);
            for (Class clz : classes) {
                if (null != clazz && clazz.length > 0) {
                    for (Class<T> aClazz : clazz) {
                        if (aClazz.isAssignableFrom(clz)) {
                            assignAsApplicable(clz, subClazzes);
                        }
                    }
                } else {
                    assignAsApplicable(clz, subClazzes);
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("", e);
        }

        long elapsed = System.currentTimeMillis() - start;
        if(null != clazz) {
            LOGGER.debug("Subclass lookup of [{}] took [{}] milliseconds for package [{}.*]", clazz, elapsed, packagePrefix);
        } else {
            LOGGER.debug("Subclass lookup took [{}] milliseconds for package [{}.*]", elapsed, packagePrefix);
        }
        return subClazzes;
    }

    private void assignAsApplicable(Class clz, Map<String, Class<?>> subClazzes) {
        int modifiers = clz.getModifiers();
        if ((!Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers))
                || (includeAbstractClasses && Modifier.isAbstract(modifiers))
                || (includeInterface && Modifier.isInterface(modifiers))) {
            subClazzes.put(clz.getName(), clz);
        } else {
            LOGGER.debug("Ignoring Class [{}] as it is not concrete implementation", clz.getName());

        }
    }

    private Set<Class> getClassesUnderPackagePrefix(final String packagePrefix) throws ClassNotFoundException, IOException {
        String packagePath = packagePrefix.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        tempClassSet = new HashSet<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (isJarFile(resource)) {
                addClassesFromJarFile(resource, packagePrefix);
            } else {
                File file = toFile(resource);
                if (file.exists() && file.isDirectory()) {
                    addClassesFromDirectory(file, packagePrefix);
                }
            }
        }

        if (packagePrefix.isEmpty()) {
            String[] classPaths = System.getProperty("java.class.path").split(";");
            Set<String> classPathSet = new HashSet<>(Arrays.asList(classPaths));
            for (String string : classPathSet) {
                File file = new File(string);
                if (file.exists() && file.isFile() && file.getName().endsWith(".jar")) {
                    URL jarUrl = toURL(file);
                    addClassesFromJarFile(jarUrl, packagePath);
                }
            }
        }

        return tempClassSet;
    }

    private void addClassesFromDirectory(File directory, String packageName) throws ClassNotFoundException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                String filename = file.getName();
                if (file.isDirectory()) {
                    addClassesFromDirectory(file, packageName + (packageName.isEmpty() ? "" : ".") + filename);
                } else if (filename.endsWith(".class")) {
                    tempClassSet.add(Class.forName(packageName + (packageName.isEmpty() ? "" : ".") + filename.substring(0, filename.length() - 6)));
                }
            }
        }
    }

    private void addClassesFromJarFile(URL url, String packageName) throws IOException {
        String packagePathFilter = packageName.replace('.', '/');
        Enumeration<JarEntry> allEntries = Objects.requireNonNull(toJarFile(url)).entries();
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        while (allEntries.hasMoreElements()) {
            JarEntry jarEntry = allEntries.nextElement();
            String entryName = jarEntry.getName();
            if (!jarEntry.isDirectory() && entryName.startsWith(packagePathFilter) && entryName.endsWith(".class")) {
                String className = jarEntry.getName().replaceFirst("\\.class$", "").replace("/", ".");
                try {
                    tempClassSet.add(Class.forName(className, false, contextClassLoader));
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Class not found for [{}]", jarEntry.getName(), e);
                }
            }
        }
    }

    private static boolean isJarFile(URL url) {
        return url.toString().startsWith("jar:file:");
    }

    private static File toFile(URL url) {
        try {
            String filepath = URLDecoder.decode(url.getFile(), "UTF-8");
            return new File(filepath);
        } catch (UnsupportedEncodingException ex) {
            String filePath = url.toString();
            LOGGER.error("Decoding of File [{}] failed, Loading directly", filePath);
            return new File(filePath);
        }
    }

    private static JarFile toJarFile(final URL url) throws IOException {
        String rawJarPath = URLDecoder.decode(url.getFile(), "UTF-8");
        String jarPath = rawJarPath.replaceFirst("!.*$", "").replaceFirst("^file:", "");
        return new JarFile(jarPath);
    }

    private static URL toURL(File file) throws MalformedURLException {
        return file.toURI().toURL();
    }
}
