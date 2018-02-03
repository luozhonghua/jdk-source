/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

// XMLReaderFactory.java - factory for creating a new reader.
// http://www.saxproject.org
// Written by David Megginson
// and by David Brownell
// NO WARRANTY!  This class is in the Public Domain.
// $Id: XMLReaderFactory.java,v 1.2.2.1 2005/07/31 22:48:08 jeffsuttor Exp $

package org.xml.sax.helpers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


/**
 * Factory for creating an XML reader.
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 *
 * <p>This class contains static methods for creating an XML reader
 * from an explicit class name, or based on runtime defaults:
 *
 * <pre>
 * try {
 *   XMLReader myReader = XMLReaderFactory.createXMLReader();
 * } catch (SAXException e) {
 *   System.err.println(e.getMessage());
 * }
 * </pre>
 *
 * <p><strong>Note to Distributions bundled with parsers:</strong>
 * You should modify the implementation of the no-arguments
 * <em>createXMLReader</em> to handle cases where the external
 * configuration mechanisms aren't set up.  That method should do its
 * best to return a parser when one is in the class path, even when
 * nothing bound its class name to {@code org.xml.sax.driver} so
 * those configuration mechanisms would see it.
 *
 * @since 1.4, SAX 2.0
 * @author David Megginson, David Brownell
 * @version 2.0.1 (sax2r2)
 *
 * @deprecated It is recommended to use {@link javax.xml.parsers.SAXParserFactory}
 * instead.
 */
@Deprecated(since="9")
final public class XMLReaderFactory
{
    /**
     * Private constructor.
     *
     * <p>This constructor prevents the class from being instantiated.
     */
    private XMLReaderFactory ()
    {
    }

    private static final String property = "org.xml.sax.driver";
    private static final SecuritySupport ss = new SecuritySupport();

    /**
     * Obtains a new instance of a {@link org.xml.sax.XMLReader}.
     * This method uses the following ordered lookup procedure to find and load
     * the {@link org.xml.sax.XMLReader} implementation class:
     * <p>
     * <ol>
     * <li>If the system property {@code org.xml.sax.driver}
     * has a value, that is used as an XMLReader class name. </li>
     * <li>
     * Use the service-provider loading facility, defined by the
     * {@link java.util.ServiceLoader} class, to attempt to locate and load an
     * implementation of the service {@link org.xml.sax.XMLReader} by using the
     * {@linkplain java.lang.Thread#getContextClassLoader() current thread's context class loader}.
     * If the context class loader is null, the
     * {@linkplain ClassLoader#getSystemClassLoader() system class loader} will
     * be used.
     * </li>
     * <li>
     * Deprecated. Look for a class name in the {@code META-INF/services/org.xml.sax.driver}
     * file in a jar file available to the runtime.</li>
     * <li>
     * <p>
     * Otherwise, the system-default implementation is returned.
     * </li>
     * </ol>
     *
     * @apiNote
     * The process that looks for a class name in the
     * {@code META-INF/services/org.xml.sax.driver} file in a jar file does not
     * conform to the specification of the service-provider loading facility
     * as defined in {@link java.util.ServiceLoader} and therefore does not
     * support modularization. It is deprecated as of Java SE 9 and subject to
     * removal in a future release.
     *
     * @return a new XMLReader.
     * @exception org.xml.sax.SAXException If no default XMLReader class
     *            can be identified and instantiated.
     * @see #createXMLReader(java.lang.String)
     */
    public static XMLReader createXMLReader ()
        throws SAXException
    {
        String          className = null;
        ClassLoader     cl = ss.getClassLoader();

        // 1. try the JVM-instance-wide system property
        try {
            className = ss.getSystemProperty(property);
        }
        catch (RuntimeException e) { /* continue searching */ }

        // 2. try the ServiceLoader
        if (className == null) {
            final XMLReader provider = findServiceProvider(XMLReader.class, cl);
            if (provider != null) {
                return provider;
            }
        }

        // 3. try META-INF/services/org.xml.sax.driver. This old process allows
        // legacy providers to be found
        if (className == null) {
            className = jarLookup(cl);
        }

        // 4. Distro-specific fallback
        if (className == null) {
            return new com.sun.org.apache.xerces.internal.parsers.SAXParser();
        }

        return loadClass (cl, className);
    }


    /**
     * Attempt to create an XML reader from a class name.
     *
     * <p>Given a class name, this method attempts to load
     * and instantiate the class as an XML reader.
     *
     * <p>Note that this method will not be usable in environments where
     * the caller (perhaps an applet) is not permitted to load classes
     * dynamically.
     *
     * @return A new XML reader.
     * @exception org.xml.sax.SAXException If the class cannot be
     *            loaded, instantiated, and cast to XMLReader.
     * @see #createXMLReader()
     */
    public static XMLReader createXMLReader (String className)
        throws SAXException
    {
        return loadClass (ss.getClassLoader(), className);
    }

    private static XMLReader loadClass (ClassLoader loader, String className)
    throws SAXException
    {
        try {
            return NewInstance.newInstance (XMLReader.class, loader, className);
        } catch (ClassNotFoundException e1) {
            throw new SAXException("SAX2 driver class " + className +
                                   " not found", e1);
        } catch (IllegalAccessException e2) {
            throw new SAXException("SAX2 driver class " + className +
                                   " found but cannot be loaded", e2);
        } catch (InstantiationException e3) {
            throw new SAXException("SAX2 driver class " + className +
           " loaded but cannot be instantiated (no empty public constructor?)",
                                   e3);
        } catch (ClassCastException e4) {
            throw new SAXException("SAX2 driver class " + className +
                                   " does not implement XMLReader", e4);
        }
    }

    /**
     * Locates a provider by directly reading the jar service file.
     * @param loader the ClassLoader to be used to read the service file
     * @return the name of the provider, or null if nothing is found
     */
    private static String jarLookup(final ClassLoader loader) {
        final ClassLoader cl = Objects.requireNonNull(loader);
        String clsFromJar = null;
        String service = "META-INF/services/" + property;
        InputStream in;
        BufferedReader      reader;

        try {
            in = ss.getResourceAsStream(cl, service);

            // If no provider found then try the current ClassLoader
            if (in == null) {
                in = ss.getResourceAsStream(null, service);
            }

            if (in != null) {
                reader = new BufferedReader (new InputStreamReader (in, "UTF8"));
                clsFromJar = reader.readLine ();
                in.close ();
            }
        } catch (IOException e) {
        }
        return clsFromJar;
    }

    /*
     * Try to find provider using the ServiceLoader API
     *
     * @param type Base class / Service interface of the factory to find.
     *
     * @return instance of provider class if found or null
     */
    private static <T> T findServiceProvider(final Class<T> type, final ClassLoader loader)
            throws SAXException {
        ClassLoader cl = Objects.requireNonNull(loader);
        try {
            return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
                final ServiceLoader<T> serviceLoader;
                serviceLoader = ServiceLoader.load(type, cl);
                final Iterator<T> iterator = serviceLoader.iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                } else {
                    return null;
                }
            });
        } catch(ServiceConfigurationError e) {
            final RuntimeException x = new RuntimeException(
                    "Provider for " + type + " cannot be created", e);
            throw new SAXException("Provider for " + type + " cannot be created", x);

          }
      }

}