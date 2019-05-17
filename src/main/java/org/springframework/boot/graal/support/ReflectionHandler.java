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

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature.DuringSetupAccess;
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport;
import org.springframework.boot.graal.domain.reflect.ClassDescriptor;
import org.springframework.boot.graal.domain.reflect.ClassDescriptor.Flag;
import org.springframework.boot.graal.domain.reflect.FieldDescriptor;
import org.springframework.boot.graal.domain.reflect.JsonMarshaller;
import org.springframework.boot.graal.domain.reflect.MethodDescriptor;
import org.springframework.boot.graal.domain.reflect.ReflectionDescriptor;

import com.oracle.svm.hosted.FeatureImpl.DuringSetupAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.config.ReflectionRegistryAdapter;

public class ReflectionHandler {
	
	ReflectionRegistryAdapter rra;
	ReflectionDescriptor loadedDescriptor;

	public ReflectionDescriptor compute() {
		if (loadedDescriptor == null) {
			try {
				InputStream s = this.getClass().getResourceAsStream("/reflect.json");
				loadedDescriptor = JsonMarshaller.read(s);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return loadedDescriptor;
	}
	
	public void addAccess(String typename, Flag...flags) {
//		System.out.println("Registering reflective access to "+typename);
		Class<?> type = rra.resolveType(typename);
		if (type == null) {
			System.out.println("ERROR: CANNOT RESOLVE "+typename+" ???");
			return;
		}
		if (loadedDescriptor.hasClassDescriptor(typename)) {
			System.out.println("JSON FILE CONTAINS THIS ALREADY "+typename);
		}
		rra.registerType(type);
		for (Flag flag: flags) {
			if (flag==Flag.allDeclaredClasses) {
				rra.registerDeclaredClasses(type);
			}
			if (flag==Flag.allDeclaredFields) {
				rra.registerDeclaredFields(type);
			}
			if (flag==Flag.allPublicFields) {
				rra.registerPublicFields(type);
			}
			if (flag==Flag.allDeclaredConstructors) {
				rra.registerDeclaredConstructors(type);
			}
			if (flag==Flag.allPublicConstructors) {
				rra.registerPublicConstructors(type);
			}
			if (flag==Flag.allDeclaredMethods) {
				rra.registerDeclaredMethods(type);
			}
			if (flag==Flag.allPublicMethods) {
				rra.registerPublicMethods(type);
			}
			if (flag==Flag.allPublicClasses) {
				rra.registerPublicClasses(type);
			}
		}
		
	}

	public void register(DuringSetupAccess a) {
		DuringSetupAccessImpl access = (DuringSetupAccessImpl) a;
		RuntimeReflectionSupport rrs = ImageSingletons.lookup(RuntimeReflectionSupport.class);
		ImageClassLoader cl = access.getImageClassLoader();
		rra = new ReflectionRegistryAdapter(rrs, cl);
		ReflectionDescriptor reflectionDescriptor = compute();
		System.out.println("SBG: reflection registering #"+reflectionDescriptor.getClassDescriptors().size()+" entries");
		for (ClassDescriptor classDescriptor : reflectionDescriptor.getClassDescriptors()) {
			Class<?> type = rra.resolveType(classDescriptor.getName());
			if (type == null) {
				System.out.println("reflect.json included "+classDescriptor.getName()+" but it doesn't exist in this code, skipping...");
				continue;
			}
	        rra.registerType(type);
			Set<Flag> flags = classDescriptor.getFlags();
			try {
				if (flags != null) {
					if (flags.contains(Flag.allDeclaredClasses)) {
	//					System.out.println("DeclaredClasses: " + type);
						rra.registerDeclaredClasses(type);
					}
					if (flags.contains(Flag.allDeclaredFields)) {
	//					System.out.println("DeclaredFields: " + type);
						rra.registerDeclaredFields(type);
					}
					if (flags.contains(Flag.allPublicFields)) {
	//					System.out.println("PublicFields: " + type);
						rra.registerPublicFields(type);
					}
					if (flags.contains(Flag.allDeclaredConstructors)) {
	//					System.out.println("DeclaredConstructors: " + type);
						rra.registerDeclaredConstructors(type);
					}
					if (flags.contains(Flag.allPublicConstructors)) {
	//					System.out.println("PublicConstructors: " + type);
						rra.registerPublicConstructors(type);
					}
					if (flags.contains(Flag.allDeclaredMethods)) {
	//					System.out.println("DeclaredMethods: " + type);
						rra.registerDeclaredMethods(type);
					}
					if (flags.contains(Flag.allPublicMethods)) {
	//					System.out.println("PublicMethods: " + type);
						rra.registerPublicMethods(type);
					}
					if (flags.contains(Flag.allPublicClasses)) {
	//					System.out.println("PublicClasses: " + type);
						rra.registerPublicClasses(type);
					}
				}
			} catch (NoClassDefFoundError ncdfe) {
				System.out.println("Problem handling flags for "+type.getName()+" because of missing "+ncdfe.getMessage());
			}
			List<MethodDescriptor> methods = classDescriptor.getMethods();
			if (methods != null) {
				for (MethodDescriptor methodDescriptor : methods) {
					String n = methodDescriptor.getName();
					List<String> parameterTypes = methodDescriptor.getParameterTypes();
					if (parameterTypes == null) {
						if (n.equals("<init>")) {
							rra.registerAllConstructors(type);
						} else {
							rra.registerAllMethodsWithName(type, n);
						}
					} else {
						List<Class<?>> collect = parameterTypes.stream().map(pname -> rra.resolveType(pname))
								.collect(Collectors.toList());
						try {
							if (n.equals("<init>")) {
								rra.registerConstructor(type, collect);
							} else {
								rra.registerMethod(type, n, collect);
							}
						} catch (NoSuchMethodException nsme) {
							throw new IllegalStateException("Couldn't find: " + methodDescriptor.toString(), nsme);
						}
					}
				}
			}
			List<FieldDescriptor> fields = classDescriptor.getFields();
			if (fields != null) {
				for (FieldDescriptor fieldDescriptor : fields) {
					throw new IllegalStateException(fieldDescriptor.toString());
				}
			}
		}
		registerLogback();
	}

	// TODO this is horrible
	// from PatternLayout
	private String logBackPatterns[] = new String[] { "ch.qos.logback.core.pattern.IdentityCompositeConverter", "ch.qos.logback.core.pattern.ReplacingCompositeConverter",
			"DateConverter", "RelativeTimeConverter", "LevelConverter", "ThreadConverter", "LoggerConverter",
			"MessageConverter", "ClassOfCallerConverter", "MethodOfCallerConverter", "LineOfCallerConverter",
			"FileOfCallerConverter", "MDCConverter", "ThrowableProxyConverter", "RootCauseFirstThrowableProxyConverter",
			"ExtendedThrowableProxyConverter", "NopThrowableInformationConverter", "ContextNameConverter",
			"CallerDataConverter", "MarkerConverter", "PropertyConverter", "LineSeparatorConverter",
			"color.BlackCompositeConverter", "color.RedCompositeConverter", "color.GreenCompositeConverter",
			"color.YellowCompositeConverter", "color.BlueCompositeConverter", "color.MagentaCompositeConverter",
			"color.CyanCompositeConverter", "color.WhiteCompositeConverter", "color.GrayCompositeConverter",
			"color.BoldRedCompositeConverter", "color.BoldGreenCompositeConverter",
			"color.BoldYellowCompositeConverter", "color.BoldBlueCompositeConverter",
			"color.BoldMagentaCompositeConverter", "color.BoldCyanCompositeConverter",
			"color.BoldWhiteCompositeConverter", "ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter",
			"LocalSequenceNumberConverter", "org.springframework.boot.logging.logback.ColorConverter",
			"org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter",
	"org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter"};
// what would a reflection hint look like here? Would it specify maven coords for logback as a requirement on the classpath?
// does logback have a feature? or meta data files for graal?
	private void registerLogback() {
		addAccess("ch.qos.logback.core.Appender", Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
		addAccess("org.springframework.boot.logging.logback.LogbackLoggingSystem", Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
		for (String p: logBackPatterns) {
			if (p.startsWith("org")) {
				addAccess(p, Flag.allDeclaredConstructors,Flag.allDeclaredMethods);
			} else if (p.startsWith("ch.")) {
					addAccess(p, Flag.allDeclaredConstructors,Flag.allDeclaredMethods);
			} else if (p.startsWith("color.")) {
				addAccess("ch.qos.logback.core.pattern."+p,Flag.allDeclaredConstructors,Flag.allDeclaredMethods);
			} else {
				addAccess("ch.qos.logback.classic.pattern."+p,Flag.allDeclaredConstructors,Flag.allDeclaredMethods);
			}
		}
	}

}
