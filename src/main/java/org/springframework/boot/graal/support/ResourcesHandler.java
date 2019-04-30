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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;
import org.springframework.boot.graal.domain.resources.ResourcesDescriptor;
import org.springframework.boot.graal.domain.resources.ResourcesJsonMarshaller;
import org.springframework.boot.graal.type.Type;
import org.springframework.boot.graal.type.TypeSystem;

import com.oracle.svm.core.jdk.Resources;
import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.ResourcesFeature.ResourcesRegistry;

public class ResourcesHandler extends Support {

	ImageClassLoader cl;

	public ResourcesDescriptor compute() {
		try {
			InputStream s = this.getClass().getResourceAsStream("/resources.json");
			ResourcesDescriptor read = ResourcesJsonMarshaller.read(s);
			return read;
		} catch (Exception e) {
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
		System.out.println("SBG: adding resources, patterns: #" + rd.getPatterns().size());
		for (String pattern : rd.getPatterns()) {
			if (pattern.equals("META-INF/spring.factories")) {
				continue; // leave to special handling...
			}
			resourcesRegistry.addResources(pattern);
		}
		processSpringFactories();
	}

	/**
	 * Find all META-INF/spring.factories - for any configurations listed in each, check if those configurations use ConditionalOnClass.
	 * If the classes listed in ConditionalOnClass can't be found, discard the configuration from spring.factories. Register either
	 * the unchanged or modified spring.factories files with the system.
	 */
	public void processSpringFactories() {
		log("Processing META-INF/spring.factories files...");
		TypeSystem ts = TypeSystem.get(cl.getClasspath());
		Type cocType = ts.resolveDotted("org.springframework.boot.autoconfigure.condition.ConditionalOnClass");
		Enumeration<URL> springFactories = fetchResources("META-INF/spring.factories");
		while (springFactories.hasMoreElements()) {
			Properties p = new Properties();
			List<String> forRemoval = new ArrayList<>();
			URL springFactory = springFactories.nextElement();
			String name = springFactory.toString();
			name = name.substring(name.lastIndexOf("/"));
			try (InputStream is = springFactory.openStream()) {
				p.load(is);
			} catch (IOException e) {
				throw new IllegalStateException("Unable to load spring.factories", e);
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
					Type configType = ts.resolveDotted(config);
					List<String> conditionalTypes = configType.findConditionalOnClassString();
					if (conditionalTypes != null) {
//						System.out.println(">> @COC on " + config+" for "+conditionalTypes);
						boolean passesConditionalOnClassTest = true;
						for (String lDescriptor : conditionalTypes) {
							boolean exists = ts.Lresolve(lDescriptor, true) != null;
							if (!exists) {
								passesConditionalOnClassTest = false;
								break;
							}
						}
						if (!passesConditionalOnClassTest) {
							System.out.println("  @COC check failed for " + config);
							forRemoval.add(config);
						}
					}
				}
				configs.removeAll(forRemoval);
				p.put("org.springframework.boot.autoconfigure.EnableAutoConfiguration", String.join(",", configs));
			}
			try {
				if (forRemoval.size() == 0) {
					System.out.println("  that was not changed");
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
