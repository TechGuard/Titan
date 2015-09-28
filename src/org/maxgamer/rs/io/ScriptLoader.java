package org.maxgamer.rs.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.maxgamer.rs.io.classparse.ClassReference;
import org.maxgamer.rs.lib.log.Log;

/**
 * ScriptLoader class allows us to load class files from a directory and returns
 * a list of classes that were loaded
 * @author netherfoam
 * @param <T>
 */
public class ScriptLoader<T> {
	/**
	 * Class that allows us to load a script from a given file
	 * @author netherfoam
	 */
	private class ScriptClassLoader extends ClassLoader {
		
		public ScriptClassLoader(ClassLoader parent) {
			super(parent);
		}
		
		public Class<?> defineClass(String name, byte[] data) throws IOException{
			Class<?> c;
			try {
				//We want to find the class if it is already loaded. This searches
				//all available classloaders and may not be desirable (If class names
				//conflict, then this may result in a different file being loaded
				//by the ClassLoader.)
				c = Class.forName(name);
			}
			catch (ClassNotFoundException e) {
				//We have no existing class, thus we define this new class here.
				//This allows us to define a class without the class being placed
				//in the correct package, which is a handy feature for new users.
				if (transformer != null) {
					data = transformer.transform(name, data);
				}
				
				c = this.defineClass(null, data, 0, data.length, (ProtectionDomain) null);
			}
			
			return c;
		}
		
		public Class<?> defineClass(File file) throws IOException {
			FileInputStream in = new FileInputStream(file);
			
			try {
				byte[] data = new byte[in.available()];
				in.read(data);
				ClassReference cr = ClassReference.decode(data);
				
				Class<?> c = this.defineClass(cr.getClassName(), data);
				
				for (File f : file.getParentFile().listFiles()) {
					if (f.getName().startsWith(c.getCanonicalName() + '$') && f.getName().endsWith(".class")) {
						//This is an inner class
						Log.debug("Loading inner class " + f.getName());
						this.defineClass(f);
					}
				}
				
				return c;
			}
			finally {
				in.close();
			}
		}
	}
	
	public static interface ClassTransformer {
		public abstract byte[] transform(String clazz, byte[] src);
	}
	
	/**
	 * The type of class we're looking for
	 */
	private Class<T> type;
	private ClassTransformer transformer;
	private ScriptClassLoader loader;
	
	/**
	 * Constructs a new ScriptLoader class which will load the given type of
	 * classes
	 * @param types the type of classes to load
	 */
	public ScriptLoader(Class<T> types, ClassTransformer transformer) {
		this.type = types;
		this.loader = new ScriptClassLoader(Thread.currentThread().getContextClassLoader());
		this.transformer = transformer;
	}
	
	public ScriptLoader(Class<T> types) {
		this(types, null);
	}
	
	/**
	 * Loads all classes from the given folder, and returns them as a map of
	 * their file to the class that was loaded.
	 * @param from the folder to load from
	 * @param listAnonymous true to load anonymous classes (inner classes) as
	 *        well
	 * @return the classes loaded, never null
	 */
	public ArrayList<Class<T>> load(File from, boolean listAnonymous) {
		ArrayList<Class<T>> scripts = new ArrayList<Class<T>>();
		
		if (!from.exists()) {
			return scripts;
		}
		
		if (from.isDirectory()) {
			LinkedList<File> files = ScriptLoader.getFiles(from);
			
			URL[] urls = new URL[files.size()];
			for (int i = 0; i < files.size(); i++) {
				try {
					urls[i] = files.get(i).toURI().toURL();
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			
			for (File file : files) {
				try {
					
					@SuppressWarnings("unchecked")
					Class<T> clazz = (Class<T>) loader.defineClass(file);
					
					if (this.type.isInterface()) {
						//It's an interface.
						if (isInterface(this.type, clazz) == false) {
							continue; //We're not looking for you.
						}
					}
					else {
						//It's a class
						if (isSuperClass(clazz, this.type) == false) {
							continue; //We're not looking for you.
						}
					}
					
					if (listAnonymous || !clazz.isAnonymousClass()) {
						scripts.add(clazz);
					}
				}
				catch (Exception e) {
					System.out.println(e.getMessage() + " Failed to load - " + e.getClass().getSimpleName());
				}
				catch (Error e) {
					e.printStackTrace();
					System.out.println("[Severe] Something went wrong loading " + file.toString());
				}
			}
		}
		else if (from.isFile() && from.getName().endsWith(".jar")) {
			try {
				ClassLoader cl = new URLClassLoader(new URL[]{from.toURI().toURL()});
				JarFile jarFile = new JarFile(from);
				Enumeration<JarEntry> entry = jarFile.entries();
				
				while (entry.hasMoreElements()) {
					JarEntry je = (JarEntry) entry.nextElement();
					if (je.isDirectory() || !je.getName().endsWith(".class")) {
						continue;
					}
					// -6 because of .class
					String className = je.getName().substring(0, je.getName().length() - 6);
					className = className.replace('/', '.');
					try {
						InputStream in = cl.getResourceAsStream(je.getName());
						
						byte[] data = new byte[in.available()];
						in.read(data);
						String name = je.getName().replaceAll("/", ".").substring(0, je.getName().length() - 6);
						
						@SuppressWarnings("unchecked")
						Class<T> clazz = (Class<T>) this.loader.defineClass(name, data);
						in.close();
						
						if (this.type.isInterface()) {
							//It's an interface.
							if (isInterface(this.type, clazz) == false) {
								continue; //We're not looking for you.
							}
						}
						else {
							//It's a class
							if (isSuperClass(clazz, this.type) == false) {
								continue; //We're not looking for you.
							}
						}
						
						if (listAnonymous || !clazz.isAnonymousClass()) {
							scripts.add(clazz);
						}
					}
					catch (Exception e) {
						Log.warning(e.getMessage() + " Failed to load - " + entry.getClass().getSimpleName());
						e.printStackTrace();
					}
					catch (Error e) {
						e.printStackTrace();
						Log.warning("[Severe] Something went wrong loading " + je.getName());
					}
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			throw new RuntimeException("Classes could not be loaded from the file " + from);
		}
		return scripts;
	}
	
	/**
	 * Returns true if the given base class is a subclass of the given
	 * superclass.
	 * @param base The base class
	 * @param superClazz The class which might be a super class
	 * @return true if the given base class is a subclass of the given
	 *         superclass.
	 */
	private static boolean isSuperClass(Class<?> base, Class<?> superClazz) {
		while (base != null) {
			if (base.equals(superClazz)) {
				return true;
			}
			base = base.getSuperclass();
		}
		return false;
	}
	
	/**
	 * Returns true if the given base class implements the given interface
	 * @param interfac the interface class
	 * @param child the base class
	 * @return true if the base class implements the interface class, else false
	 */
	private static boolean isInterface(Class<?> interfac, Class<?> child) {
		LinkedList<Class<?>> interfaces = new LinkedList<Class<?>>();
		Class<?>[] starters = child.getInterfaces();
		
		for (int i = 0; i < starters.length; i++) {
			interfaces.add(starters[i]);
		}
		
		while (interfaces.isEmpty() == false) {
			Class<?> face = interfaces.pop();
			if (face.getName().equals(interfac.getName())) {
				return true;
			}
			else {
				starters = face.getInterfaces();
				for (int i = 0; i < starters.length; i++) {
					interfaces.add(starters[i]);
				}
			}
		}
		return false;
	}
	
	/**
	 * Loads the scripts from the given folder which correspond to this
	 * ScriptLoader's type and then returns them. This does not load
	 * inner/anonymous classes.
	 * @param from the file to load from
	 * @return a map of file to class, where class is the class that was loaded
	 *         from the matching file. Never null
	 */
	public ArrayList<Class<T>> getScripts(File from) {
		return load(from, false);
	}
	
	/**
	 * Fetches all files in the given folder that can be accepted by the given
	 * filter. This method is recursive.
	 * 
	 * @param dir The directory to search
	 * @param filter The filter. Must not be null.
	 * @return The list of files, never null.
	 */
	private static LinkedList<File> getFiles(File dir) {
		LinkedList<File> files = new LinkedList<File>();
		if (dir == null || dir.isDirectory() == false) {
			return files;
		}
		
		for (File f : dir.listFiles()) {
			if (f.getName().startsWith(".")) {
				continue;
			}
			if (f.isDirectory()) {
				files.addAll(getFiles(f));
			}
			else {
				if (f.getName().endsWith(".class")) {
					files.add(f);
				}
			}
		}
		
		return files;
	}
}