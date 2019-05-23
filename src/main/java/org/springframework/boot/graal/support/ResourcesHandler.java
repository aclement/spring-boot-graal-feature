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
package org.springframework.boot.graal.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;
import org.springframework.boot.graal.domain.reflect.ClassDescriptor.Flag;
import org.springframework.boot.graal.domain.resources.ResourcesDescriptor;
import org.springframework.boot.graal.domain.resources.ResourcesJsonMarshaller;
import org.springframework.boot.graal.type.Method;
import org.springframework.boot.graal.type.MissingTypeException;
import org.springframework.boot.graal.type.Type;
import org.springframework.boot.graal.type.TypeSystem;

import com.oracle.svm.core.jdk.Resources;
import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.ResourcesFeature.ResourcesRegistry;

public class ResourcesHandler {

	private TypeSystem ts;
	
	private ImageClassLoader cl;
	
	private ReflectionHandler reflectionHandler;

	public ResourcesHandler(ReflectionHandler reflectionHandler) {
		this.reflectionHandler = reflectionHandler;
	}

	public ResourcesDescriptor compute() {
		try {
			InputStream s = this.getClass().getResourceAsStream("/resources.json");
			ResourcesDescriptor read = ResourcesJsonMarshaller.read(s);
			return read;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void register(BeforeAnalysisAccess access) {
		cl = ((BeforeAnalysisAccessImpl) access).getImageClassLoader();
		ResourcesDescriptor rd = compute();
		ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
		// Patterns can be added to the registry, resources can be directly registered
		// against Resources
		// resourcesRegistry.addResources("*");
		// Resources.registerResource(relativePath, inputstream);
		System.out.println("SBG: adding resources - #" + rd.getPatterns().size()+" patterns");
//		logging();

		for (String pattern : rd.getPatterns()) {
			if (pattern.equals("META-INF/spring.factories")) {
				continue; // leave to special handling...
			}
//			if (pattern.contains("logging.properties")) {
//				URL resource = cl.getClassLoader().getResource(pattern);
//				System.out.println("Can I find "+pattern+"?  "+resource);
//			}
			resourcesRegistry.addResources(pattern);
		}
		processSpringFactories();
		processSpringComponents();
	}
//	
//	public void logging() {
//		try {
//			InputStream s = this.getClass().getResourceAsStream("/abc.txt");
//			byte[] bs = new byte[100000];
//			int i = -1;
//			byte[] buf = new byte[10000];
//			int offset=0;
//			while ((i=s.read(buf))!=-1) {
//				System.arraycopy(buf, 0, bs, offset, i);
//				offset+=i;
//			}
//			System.out.println("read "+offset+" bytes");
//			ByteArrayInputStream bais = new ByteArrayInputStream(bs);
//			Resources.registerResource("org/springframework/boot/logging/java/logging.properties", bais);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public void processSpringComponents() {
		ts = TypeSystem.get(cl.getClasspath());
		Enumeration<URL> springComponents = fetchResources("META-INF/spring.components");
		if (springComponents.hasMoreElements()) {
			log("Processing META-INF/spring.components files...");
			while (springComponents.hasMoreElements()) {
				URL springFactory = springComponents.nextElement();
				processSpringComponents(ts, springFactory);
			}
		} else {
//			System.out.println("No META-INF/spring.components found");
			System.out.println("Found no META-INF/spring.components -> fall back to custom scan...");
			List<Entry<String, String>> components = scanClasspathForIndexedStereotypes();
			List<Entry<String,String>> filteredComponents = filterComponents(components);
			Properties p = new Properties();
			for (Entry<String,String> filteredComponent: filteredComponents) {
				String k = filteredComponent.getKey();
				System.out.println("- "+k);
				p.put(k, filteredComponent.getValue());
				reflectionHandler.addAccess(k,Flag.allDeclaredConstructors, Flag.allDeclaredMethods, Flag.allDeclaredClasses);
				ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
				resourcesRegistry.addResources(k.replace(".", "/")+".class");
			}
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				p.store(baos,"");
				baos.close();
				byte[] bs = baos.toByteArray();
				ByteArrayInputStream bais = new ByteArrayInputStream(bs);
				Resources.registerResource("META-INF/spring.components", bais);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void processSpringComponents(TypeSystem ts, URL springComponentsFile) {	
		Properties p = new Properties();
		loadSpringFactoryFile(springComponentsFile, p);
		// Example:
		// com.example.demo.Foobar=org.springframework.stereotype.Component
		// com.example.demo.DemoApplication=org.springframework.stereotype.Component
		Enumeration<Object> keys = p.keys();
		ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
		while (keys.hasMoreElements()) {
			String k = (String)keys.nextElement();
			System.out.println("Registering Spring Component: "+k);
			reflectionHandler.addAccess(k,Flag.allDeclaredConstructors, Flag.allDeclaredMethods, Flag.allDeclaredClasses);
			resourcesRegistry.addResources(k.replace(".", "/")+".class");
			// Register nested types of the component
			Type baseType = ts.resolveDotted(k);
			for (Type t: baseType.getNestedTypes()) {
				String n = t.getName().replace("/", ".");
				reflectionHandler.addAccess(n,Flag.allDeclaredConstructors, Flag.allDeclaredMethods, Flag.allDeclaredClasses);
				resourcesRegistry.addResources(t.getName()+".class");
			}
		}
	}

	/**
	 * Find all META-INF/spring.factories - for any configurations listed in each, check if those configurations use ConditionalOnClass.
	 * If the classes listed in ConditionalOnClass can't be found, discard the configuration from spring.factories. Register either
	 * the unchanged or modified spring.factories files with the system.
	 */
	public void processSpringFactories() {
		log("Processing META-INF/spring.factories files...");
		TypeSystem ts = TypeSystem.get(cl.getClasspath());
		Enumeration<URL> springFactories = fetchResources("META-INF/spring.factories");
		while (springFactories.hasMoreElements()) {
			URL springFactory = springFactories.nextElement();
			processSpringFactory(ts, springFactory);
		}
		List<String> classpath = ts.getClasspath();
	}
	
	
	private List<Entry<String, String>> filterComponents(List<Entry<String, String>> as) {
		List<Entry<String,String>> filtered = new ArrayList<>();
		List<Entry<String,String>> subtypesToRemove = new ArrayList<>();
		for (Entry<String,String> a: as) {
			String type = a.getKey();
			subtypesToRemove.addAll(as.stream().filter(e -> e.getKey().startsWith(type+"$")).collect(Collectors.toList()));
		}
		filtered.addAll(as);
		filtered.removeAll(subtypesToRemove);
		return filtered;
	}

	private List<Entry<String,String>> scanClasspathForIndexedStereotypes() {
		return findDirectories(ts.getClasspath())
			.flatMap(this::findClasses)
			.map(this::typenameOfClass)
			.map(this::isIndexed)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private Entry<String,String> isIndexed(String slashedClassname) {
		Entry<String,String> entry = ts.resolveSlashed(slashedClassname).isIndexed();
		return entry;
	}
	
	private String typenameOfClass(File f) {
		return Utils.scanClass(f).getClassname();
	}

	private Stream<File> findClasses(File dir) {
		ArrayList<File> classfiles = new ArrayList<>();
		walk(dir,classfiles);
		return classfiles.stream();
	}

	private void walk(File dir, ArrayList<File> classfiles) {
		File[] fs = dir.listFiles();
		for (File f: fs) {
			if (f.isDirectory()) {
				walk(f,classfiles);
			} else if (f.getName().endsWith(".class")) {
				classfiles.add(f);
			}
		}
	}

	private Stream<File> findDirectories(List<String> classpath) {
		List<File> directories = new ArrayList<>();
		for (String classpathEntry: classpath) {
			File f = new File(classpathEntry);
			if (f.isDirectory()) {
				directories.add(f);
			}
		}
		return directories.stream();
	}

	private void processSpringFactory(TypeSystem ts, URL springFactory) {
		List<String> forRemoval = new ArrayList<>();
		Properties p = new Properties();
		loadSpringFactoryFile(springFactory, p);
		
		Enumeration<Object> keyz = p.keys();
		// Handle all keys except EnableAutoConfiguration
		while (keyz.hasMoreElements()) {
			String k = (String)keyz.nextElement();
			if (!k.equals("org.springframework.boot.autoconfigure.EnableAutoConfiguration")) {
				String classesList = p.getProperty(k);
				for (String s: classesList.split(",")) {
					try {
						reflectionHandler.addAccess(s,Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
					} catch (NoClassDefFoundError ncdfe) {
						System.out.println("SBG: WARNING: Whilst processing "+k+" problem adding access for type: "+s+" because of missing "+ncdfe.getMessage());
					}
				}				
			}
		}
		
		String configsString = (String) p.get("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
		if (configsString != null) {
			List<String> configs = new ArrayList<>();
			for (String s: configsString.split(",")) {
				configs.add(s);
			}
			System.out.println(
					"Spring.factories processing: looking at #" + configs.size() + " configuration references");
			for (Iterator<String> iterator = configs.iterator(); iterator.hasNext();) {
				String config = iterator.next();
				if (!passesConditionalOnClassTest(ts, config, new HashSet<>())) {
					System.out.println("Excluding auto-configuration " + config);
					forRemoval.add(config);
				}
			}
			configs.removeAll(forRemoval);
			p.put("org.springframework.boot.autoconfigure.EnableAutoConfiguration", String.join(",", configs));
		}
		try {
			if (forRemoval.size() == 0) {
				Resources.registerResource("META-INF/spring.factories", springFactory.openStream());
			} else {
				System.out.println("  removed " + forRemoval.size() + " configurations");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				p.store(baos,"");
				baos.close();
				byte[] bs = baos.toByteArray();
				ByteArrayInputStream bais = new ByteArrayInputStream(bs);
				Resources.registerResource("META-INF/spring.factories", bais);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void loadSpringFactoryFile(URL springFactory, Properties p) {
		try (InputStream is = springFactory.openStream()) {
			p.load(is);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load spring.factories", e);
		}
	}

	private boolean passesConditionalOnClassTest(TypeSystem ts, String config, Set<String> visited) {
		return passesConditionalOnClassTest(ts, ts.resolveDotted(config), visited);
	}

	private boolean passesConditionalOnClassTest(TypeSystem ts, Type configType, Set<String> visited) {
		List<String> conditionalTypes = configType.findConditionalOnClassValue();
		if (conditionalTypes != null) {
			for (String lDescriptor : conditionalTypes) {
				Type t = ts.Lresolve(lDescriptor, true);
				boolean exists = (t != null);
				if (!exists) {
					return false;
				} else {
					try {
						reflectionHandler.addAccess(lDescriptor.substring(1,lDescriptor.length()-1).replace("/", "."),Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
					} catch (NoClassDefFoundError e) {
						System.out.println("Conditional type "+fromLtoDotted(lDescriptor)+" not found for configuration "+configType.getName());
					}
					
				}
			}
		}
		try {
			String configNameDotted = configType.getName().replace("/",".");
			System.out.println("Including auto-configuration "+configNameDotted);
			visited.add(configType.getName());
			reflectionHandler.addAccess(configNameDotted,Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
		} catch (NoClassDefFoundError e) {
			// Example:
			// PROBLEM? Can't register Type:org/springframework/boot/autoconfigure/web/servlet/HttpEncodingAutoConfiguration because cannot find javax/servlet/Filter
			// java.lang.NoClassDefFoundError: javax/servlet/Filter
			// ... at com.oracle.svm.hosted.config.ReflectionRegistryAdapter.registerDeclaredConstructors(ReflectionRegistryAdapter.java:97)
			System.out.println("PROBLEM? Can't register "+configType.getName()+" because cannot find "+e.getMessage());
		}
		
		List<String> imports = configType.findImports();
		if (imports != null) {
			System.out.println("Imports found on "+configType.getName()+" are "+imports);
			for (String imported: imports) {
				String importedName = fromLtoDotted(imported);
				try {
				Type t = ts.resolveDotted(importedName);
				passesConditionalOnClassTest(ts, t, visited);
				} catch (MissingTypeException mte) {
					System.out.println("Cannot find imported "+importedName+" so skipping processing that");
				}
			}
		}
		
		// Without this code, error at:
		// java.lang.ClassNotFoundException cannot be cast to java.lang.Class[]
		// at org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelector$ConfigurationPropertiesBeanRegistrar.lambda$collectClasses$1(EnableConfigurationPropertiesImportSelector.java:80)
		List<String> ecProperties = configType.findEnableConfigurationPropertiesValue();
		if (ecProperties != null) {
			for (String ecPropertyDescriptor: ecProperties) {
				String ecPropertyName = fromLtoDotted(ecPropertyDescriptor);
				try {
					reflectionHandler.addAccess(ecPropertyName,Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
				} catch (NoClassDefFoundError e) {
					System.out.println("Not found for registration: "+ecPropertyName);
				}
			}
		}
		
		// Find @Bean methods and add them
//		List<Method> methodsWithAtBean = configType.getMethodsWithAtBean();
//		if (methodsWithAtBean.size() != 0) {
//			System.out.println(configType+" here they are: "+
//			methodsWithAtBean.stream().map(m -> m.getName()+m.getDesc()).collect(Collectors.toList()));
//			for (Method m: methodsWithAtBean) {
//				String desc = m.getDesc();
//				String retType = desc.substring(desc.lastIndexOf(")")+1); //Lorg/springframework/boot/task/TaskExecutorBuilder;
//				System.out.println("@Bean return type "+retType);
//				reflectionHandler.addAccess(fromLtoDotted(retType), Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
//			}
//		}
		
		List<Type> nestedTypes = configType.getNestedTypes();
		for (Type t: nestedTypes) {
			if (visited.add(t.getName())) {
				passesConditionalOnClassTest(ts, t, visited);
			}
		}
		return true;
	}
	
	String fromLtoDotted(String lDescriptor) {
		return lDescriptor.substring(1,lDescriptor.length()-1).replace("/", ".");
	}

	private Enumeration<URL> fetchResources(String resource) {
		try {
			Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(resource);
			return resources;
		} catch (IOException e1) {
			return Collections.enumeration(Collections.emptyList());
		}
	}

	private void log(String msg) {
		System.out.println(msg);
	}
}
