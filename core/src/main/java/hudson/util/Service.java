/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Level.WARNING;

/**
 * Load classes by looking up <tt>META-INF/services</tt>.
 *
 * @author Kohsuke Kawaguchi
 */
public class Service {
    /**
     * Poorman's clone of JDK6 ServiceLoader.
     */
    public static <T> List<T> loadInstances(ClassLoader classLoader, Class<T> type) throws IOException {
        List<T> result = new ArrayList<T>();

        final Enumeration<URL> e = classLoader.getResources("META-INF/services/"+type.getName());
        while (e.hasMoreElements()) {
            URL url = e.nextElement();
            BufferedReader configFile = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
            try {
                String line;
                while ((line = configFile.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#") || line.length()==0)   continue;

                    try {
                        Class<?> t = classLoader.loadClass(line);
                        if (!type.isAssignableFrom(t))   continue;      // invalid type

                        result.add(type.cast(t.newInstance()));
                    } catch (ClassNotFoundException x) {
                        LOGGER.log(WARNING,"Failed to load "+line,x);
                    } catch (InstantiationException x) {
                        LOGGER.log(WARNING,"Failed to load "+line,x);
                    } catch (IllegalAccessException x) {
                        LOGGER.log(WARNING,"Failed to load "+line,x);
                    }
                }
            } finally {
                configFile.close();
            }
        }

        return result;
    }

    /**
     * Look up <tt>META-INF/service/<i>SPICLASSNAME</i></tt> from the classloader
     * and all the discovered classes into the given collection.
     */
    public static <T> void load(Class<T> spi, ClassLoader cl, Collection<Class<? extends T>> result) {
        try {
            Enumeration<URL> e = cl.getResources("META-INF/services/" + spi.getName());
            while(e.hasMoreElements()) {
                final URL url = e.nextElement();
                final BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
                try {
                    String line;
                    while((line=r.readLine())!=null) {
                        if(line.startsWith("#"))
                            continue;   // comment line
                        line = line.trim();
                        if(line.length()==0)
                            continue;   // empty line. ignore.

                        try {
                            result.add(cl.loadClass(line).asSubclass(spi));
                        } catch (ClassNotFoundException x) {
                            LOGGER.log(Level.WARNING, "Failed to load "+line, x);
                        }
                    }
                } catch (IOException x) {
                    LOGGER.log(Level.WARNING, "Failed to load "+url, x);
                } finally {
                    r.close();
                }
            }
        } catch (IOException x) {
            LOGGER.log(Level.WARNING, "Failed to look up service providers for "+spi, x);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Service.class.getName());
}
