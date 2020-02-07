package edu.hm.hafner.analysis;

import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.digester3.Digester;
import org.xml.sax.InputSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A secure {@link Digester} implementation that does not resolve external entities.
 *
 * @author Ullrich Hafner
 */
public final class SecureDigester extends Digester {
    /**
     * Creates a new {@link Digester} instance that does not resolve external entities.
     *
     * @param classWithClassLoader
     *         the class to get the class loader from
     */
    public SecureDigester(final Class<?> classWithClassLoader) {
        super();

        setClassLoader(classWithClassLoader.getClassLoader());

        SAXParserFactory factory = getFactory(); // Since there is no way to set the factory we need to modify the existing one
        SecureXmlParserFactory parserFactory = new SecureXmlParserFactory();
        parserFactory.configureSaxParserFactory(factory);
        setValidating(false);
        setEntityResolver((publicId, systemId) -> new InputSource());
    }

    @SuppressFBWarnings
    @SuppressWarnings("illegalcatch")
    private void setFeature(final SAXParserFactory factory, final String prefix, final String feature,
            final boolean value) {
        try {
            factory.setFeature(prefix + feature, value);
        }
        catch (Exception ignored) {
            // ignore and continue
        }
    }
}
