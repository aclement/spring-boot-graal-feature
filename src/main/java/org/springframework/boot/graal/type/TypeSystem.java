/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.graal.type;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Simple ASM backed type system.
 */
public class TypeSystem {

	private List<String> classpath;

	private Map<String, Type> typeCache = new HashMap<>();

	private Map<String, File> packageCache = new HashMap<>();

	private Map<String, List<File>> appPackages = new HashMap<>();

	public static TypeSystem get(List<String> classpath) {
		return new TypeSystem(classpath);
	}

	public TypeSystem(List<String> classpath) {
		this.classpath = classpath;
		index();
	}

	public Type resolveDotted(String dottedTypeName) {
		String slashedTypeName = toSlashedName(dottedTypeName);
		return resolveSlashed(slashedTypeName);
	}

	public boolean canResolveSlashed(String slashedTypeName) {
		try {
			return resolveSlashed(slashedTypeName) != null;
		} catch (RuntimeException re) {
			if (re.getMessage().startsWith("Unable to find class file for")) {
				return false;
			}
			throw re;
		}
	}

	public Type resolveSlashed(String slashedTypeName) {
		Type type = typeCache.get(slashedTypeName);
		if (type == Type.MISSING) {
			throw new MissingTypeException(slashedTypeName);
		}
		if (type != null) {
			return type;
		}
		byte[] bytes = find(slashedTypeName);
		if (bytes == null) {
			// System class?
			InputStream resourceAsStream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(slashedTypeName + ".class");
			if (resourceAsStream == null) {
				// cache a missingtype so we don't go looking again!
				typeCache.put(slashedTypeName, Type.MISSING);
				throw new MissingTypeException(slashedTypeName);
			}
			try {
				bytes = loadFromStream(resourceAsStream);
			} catch (RuntimeException e) {
				throw new RuntimeException("Problems loading class from resource stream: " + slashedTypeName, e);
			}
		}
		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(bytes);
		reader.accept(node, ClassReader.SKIP_DEBUG);
		type = Type.forClassNode(this, node);
		typeCache.put(slashedTypeName, type);
		return type;
	}

	private String toSlashedName(String dottedTypeName) {
		return dottedTypeName.replace(".", "/");
	}

	public boolean canResolve(String classname) {
		if (classname.contains(".")) {
			throw new RuntimeException("Dont pass dotted names to resolve() :" + classname);
		}
		return canResolveSlashed(classname);
	}

	public Type resolve(String classname) {
		if (classname.contains(".")) {
			throw new RuntimeException("Dont pass dotted names to resolve() :" + classname);
		}
		return resolveSlashed(classname);
	}

	public Type Lresolve(String desc) {
		return resolve(desc.substring(1, desc.length() - 1));
	}

	public Type Lresolve(String desc, boolean silent) {
		try {
		return resolve(desc.substring(1, desc.length() - 1));
		} catch (MissingTypeException mte) {
			if (silent) return null;
			else throw mte;
		}
	}

//	public File getJarpath() {
//		return jarpath;
//	}

//	ClasspathScanner(List<String> classpath) {
//		System.out.println("Initializing type system based on classpath: "+classpath);
//		index(classpath);
//		System.out.println( "#"+appClasses.size()+" application classes");
//		System.out.println("#" + packageCache.values().stream().distinct().count() + " dependencies containing #"
//				+ packageCache.keySet().size() + " packages");
//		System.out.println();
//	}

	public void index() {
		for (String s : classpath) {
			File f = new File(s);
			if (f.isDirectory()) {
				indexDir(f);
			} else {
				indexJar(f);
			}
		}
	}

	public void indexDir(File dir) {
		Path root = Paths.get(dir.toURI());
		try {
			Files.walk(root).filter(f -> f.toString().endsWith(".class")).map(f -> {
				String name = f.toString().substring(root.toString().length() + 1);
				int lastSlash = name.lastIndexOf("/");
				if (lastSlash != -1 && name.endsWith(".class")) {
					return name.substring(0, lastSlash);
				}
				return null;
			}).forEach(n -> {
				if (n != null) {
					List<File> dirs = appPackages.get(n);
					if (dirs == null) {
						dirs = new ArrayList<>();
						appPackages.put(n, dirs);
					}
					dirs.add(dir);
				}
			});
		} catch (IOException ioe) {
			throw new IllegalStateException("Unable to walk " + dir, ioe);
		}
	}

	public void indexJar(File jar) {
		// Walk the jar, index entries and cache package > this jar
		try {
			try (ZipFile zf = new ZipFile(jar)) {
				Enumeration<? extends ZipEntry> entries = zf.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.endsWith(".class")) {
						int lastSlash = name.lastIndexOf("/");
						if (lastSlash != -1 && name.endsWith(".class")) {
							packageCache.put(name.substring(0, lastSlash), jar);
						}
					}
				}
			}
		} catch (IOException ioe) {
			throw new RuntimeException("Problem during scan of " + jar, ioe);
		}
	}

	public byte[] find(String slashedTypeName) {
		String search = slashedTypeName + ".class";
		try {
			String packageName = slashedTypeName.substring(0, slashedTypeName.lastIndexOf("/"));

			if (appPackages.containsKey(packageName)) {
				List<File> list = appPackages.get(packageName);
				for (File f : list) {
					File toTry = new File(f, search);
					if (toTry.exists()) {
						return loadFromStream(new FileInputStream(toTry));
					}
				}
			} else {
				File jarfile = packageCache.get(packageName);
//				System.out.println("Trying " + jarfile);
//				try {
				if (jarfile != null) {
				try (ZipFile zf = new ZipFile(jarfile)) {
					Enumeration<? extends ZipEntry> entries = zf.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						String name = entry.getName();
						if (name.equals(search)) {
							return loadFromStream(zf.getInputStream(entry));
						}
//							if (name.endsWith(".class")) {
//								int lastSlash = name.lastIndexOf("/");
//								if (lastSlash != -1 && name.endsWith(".class")) {
////									packageCache.put(name.substring(0, lastSlash), jar);
//								}
//							}
					}
				}
				}
//				} catch (IOException ioe) {
//					throw new RuntimeException("Problem during load of class "+slashedTypeName+" from " + jarfile, ioe);
//				}
			}

//			ZipEntry dependencyEntry = packageCache.get(packageName);
//			if (dependencyEntry != null) {
//				return loadFromDepEntry(dependencyEntry, slashedTypeName);
//			}
//			ZipEntry appEntry = appClasses.get(slashedTypeName);
//			if (appEntry != null) {
//				return loadFromAppEntry(appEntry, slashedTypeName);
//			}
			return null;
		} catch (IOException ioe) {
			throw new RuntimeException("Problem finding " + slashedTypeName, ioe);
		}
	}

//	private byte[] loadFromAppEntry(ZipEntry dependencyEntry, String slashedTypeName) throws IOException {
//		try (ZipFile zf = new ZipFile(bootJarPath)) {
//			try (InputStream is = zf.getInputStream(dependencyEntry)) {
//				return loadFromStream(is);
//			}
//		}
//	}
//
//	private byte[] loadFromDepEntry(ZipEntry dependencyEntry, String slashedTypeName) throws IOException {
//		String searchName = slashedTypeName + ".class";
////		System.out.println("Loading "+searchName+" from "+dependencyEntry.getName());
//		try (ZipFile zf = new ZipFile(bootJarPath)) {
//			try (InputStream is = zf.getInputStream(dependencyEntry)) {
//				ZipInputStream zis = new ZipInputStream(is);
//				ZipEntry ze = zis.getNextEntry();
//				while (ze != null) {
//					String name = ze.getName();
//					if (name.equals(searchName)) {
//						return loadFromStream(zis);
//					}
//					ze = zis.getNextEntry();
//				}
//			}
//		}
//		return null;
//	}

	public static byte[] loadFromStream(InputStream stream) {
		try {
			BufferedInputStream bis = new BufferedInputStream(stream);
			int size = 2048;
			byte[] theData = new byte[size];
			int dataReadSoFar = 0;
			byte[] buffer = new byte[size / 2];
			int read = 0;
			while ((read = bis.read(buffer)) != -1) {
				if ((read + dataReadSoFar) > theData.length) {
					// need to make more room
					byte[] newTheData = new byte[theData.length * 2];
					// System.out.println("doubled to " + newTheData.length);
					System.arraycopy(theData, 0, newTheData, 0, dataReadSoFar);
					theData = newTheData;
				}
				System.arraycopy(buffer, 0, theData, dataReadSoFar, read);
				dataReadSoFar += read;
			}
			bis.close();
			// Resize to actual data read
			byte[] returnData = new byte[dataReadSoFar];
			System.arraycopy(theData, 0, returnData, 0, dataReadSoFar);
			return returnData;
		} catch (IOException e) {
			throw new RuntimeException("Unexpectedly unable to load bytedata from input stream", e);
		} finally {
			try {
				stream.close();
			} catch (IOException ioe) {
			}
		}
	}

	public String toString() {
		return "TypeSystem for cp(" + classpath + ")  jarPackages=#" + packageCache.size() + " appPackages="
				+ appPackages;
	}

}