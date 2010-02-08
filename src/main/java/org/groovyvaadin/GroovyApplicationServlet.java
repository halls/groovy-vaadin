package org.groovyvaadin;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.ApplicationServlet;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyResourceLoader;
import org.codehaus.groovy.control.CompilerConfiguration;

import javax.servlet.ServletException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Created 24.01.2010 23:33:33
 *
 * @author Andrey Khalzov
 */
public class GroovyApplicationServlet extends ApplicationServlet {

    private GroovyClassLoader cl = null;

    @Override
    protected ClassLoader getClassLoader() throws ServletException {
        if (cl == null) {
            final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
            compilerConfiguration.setRecompileGroovySource(true);
            cl = new GroovyClassLoader(super.getClassLoader(), compilerConfiguration);
            final String scriptsPath = getServletConfig().getInitParameter("scriptsPath");
            if (scriptsPath != null) {
                cl.setResourceLoader(new GroovyResourceLoader() {
                    public URL loadGroovySource(final String name) throws MalformedURLException {
                        return (URL) AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                String filename = name.replace('.', '/') 
                                        + compilerConfiguration.getDefaultScriptExtension();
                                try {
                                    final File file = new File(scriptsPath + "/" + filename);
                                    if (!file.exists() || !file.isFile()) {
                                        return null;
                                    }
                                    return new URL("file:///" + scriptsPath + "/" + filename);
                                } catch (MalformedURLException e) {
                                    return null;
                                }
                            }
                        });
                    }
                });
            }
        }
        return cl;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<? extends Application> getApplicationClass() {
        final String applicationClassName = getServletConfig().getInitParameter("application");
        if (applicationClassName == null) {
            throw new RuntimeException("Application not specified in servlet parameters");
        }
        try {
            return (Class<? extends Application> ) cl.loadClass(applicationClassName, true, false);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Failed to load application class: " + applicationClassName);
        }
    }
}
