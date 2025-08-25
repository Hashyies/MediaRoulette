package me.hash.mediaroulette.plugins;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Try to load from parent first (this allows plugins to use main app classes)
        try {
            Class<?> clazz = getParent().loadClass(name);
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            // If not found in parent, try to load from this classloader
            return super.loadClass(name, resolve);
        }
    }
}