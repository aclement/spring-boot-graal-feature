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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature.DuringSetupAccess;
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport;
import org.graalvm.util.GuardedAnnotationAccess;
import org.springframework.boot.graal.domain.reflect.ClassDescriptor;
import org.springframework.boot.graal.domain.reflect.ClassDescriptor.Flag;
import org.springframework.boot.graal.domain.reflect.FieldDescriptor;
import org.springframework.boot.graal.domain.reflect.JsonMarshaller;
import org.springframework.boot.graal.domain.reflect.MethodDescriptor;
import org.springframework.boot.graal.domain.reflect.ReflectionDescriptor;

import com.oracle.svm.hosted.FeatureImpl.DuringSetupAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.config.ReflectionRegistryAdapter;

/**
 * Loads up the constant data defined in resource file and registers reflective access being
 * necessary with the image build. Also provides an method (<tt>addAccess(String typename, Flag... flags)</tt>}
 * usable from elsewhere when needing to register reflective access to a type (e.g. used when resource
 * processing).
 * 
 * @author Andy Clement
 */
public class ReflectionHandler {
	
	private final static String RESOURCE_FILE = "/reflect.json";
	
	ReflectionRegistryAdapter rra;

	ReflectionDescriptor constantReflectionDescriptor;

	public ReflectionDescriptor getConstantData() {
		if (constantReflectionDescriptor == null) {
			try {
				InputStream s = this.getClass().getResourceAsStream(RESOURCE_FILE);
				constantReflectionDescriptor = JsonMarshaller.read(s);
			} catch (Exception e) {
				throw new IllegalStateException("Unexpectedly can't load "+RESOURCE_FILE, e);
			}
		}
		return constantReflectionDescriptor;
	}
	
	public void register(DuringSetupAccess a) {
		DuringSetupAccessImpl access = (DuringSetupAccessImpl) a;
		RuntimeReflectionSupport rrs = ImageSingletons.lookup(RuntimeReflectionSupport.class);
		ImageClassLoader cl = access.getImageClassLoader();
		rra = new ReflectionRegistryAdapter(rrs, cl);
		ReflectionDescriptor reflectionDescriptor = getConstantData();

		System.out.println("SBG: reflection registering #"+reflectionDescriptor.getClassDescriptors().size()+" entries");
		for (ClassDescriptor classDescriptor : reflectionDescriptor.getClassDescriptors()) {
			Class<?> type = rra.resolveType(classDescriptor.getName());
			if (type == null) {
				System.out.println("SBG: WARNING: "+RESOURCE_FILE+" included "+classDescriptor.getName()+" but it doesn't exist on the classpath, skipping...");
				continue;
			}
	        rra.registerType(type);
			Set<Flag> flags = classDescriptor.getFlags();
			if (flags != null) {
				for (Flag flag: flags) {
					try {
						switch (flag) {
						case allDeclaredClasses:
							rra.registerDeclaredClasses(type);
							break;
						case allDeclaredFields:
							rra.registerDeclaredFields(type);
							break;
						case allPublicFields:
							rra.registerPublicFields(type);
							break;
						case allDeclaredConstructors:
							rra.registerDeclaredConstructors(type);
							break;
						case allPublicConstructors:
							rra.registerPublicConstructors(type);
							break;
						case allDeclaredMethods:
							rra.registerDeclaredMethods(type);
							break;
						case allPublicMethods:
							rra.registerPublicMethods(type);
							break;
						case allPublicClasses:
							rra.registerPublicClasses(type);
							break;						
						}
					} catch (NoClassDefFoundError ncdfe) {
						System.out.println("SBG: ERROR: problem handling flag: "+flag+" for "+type.getName()+" because of missing "+ncdfe.getMessage());
					}
				}
			}
			
			// Process all specific methods defined in the input class descriptor (including constructors)
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
			
			// Process all specific fields defined in the input class descriptor
			List<FieldDescriptor> fields = classDescriptor.getFields();
			if (fields != null) {
				for (FieldDescriptor fieldDescriptor : fields) {
					try {
						rra.registerField(type, fieldDescriptor.getName(), fieldDescriptor.isAllowWrite());
					} catch (NoSuchFieldException nsfe) {
						System.out.println("Skipping reflection registration of field "+type.getName()+"."+fieldDescriptor.getName()+": field not found");
					}
				}
			}
		}
		registerLogback();
	}

	public void addAccess(String typename, Flag...flags) {
//		System.out.println("Registering reflective access to "+typename);
		Class<?> type = rra.resolveType(typename);
		if (type == null) {
			System.out.println("ERROR: CANNOT RESOLVE "+typename+" ???");
			return;
		}
		if (constantReflectionDescriptor.hasClassDescriptor(typename)) {
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
	
	private boolean verify(Object[] things) {
			for (Object o: things) {
				try {
			        if (o instanceof Method) {
			            ((Method)o).getGenericReturnType();
			        }
			        if (o instanceof Field) {
			            ((Field)o).getGenericType();
			        }
			        if (o instanceof AccessibleObject) {
			            AccessibleObject accessibleObject = (AccessibleObject) o;
			            GuardedAnnotationAccess.getDeclaredAnnotations(accessibleObject);
			        }
	
			        if (o instanceof Parameter) {
			            Parameter parameter = (Parameter) o;
			            parameter.getType();
			        }
					if (o instanceof Executable) {
						Executable e = (Executable)o;
						e.getGenericParameterTypes();
						e.getGenericExceptionTypes();
						e.getParameters();
					}
				} catch (Exception e) {
					System.out.println("REFLECTION PROBLEM LATER due to reference from "+o+" to "+e.getMessage());
					return false;
				}
			}
			return true;
	}



	// TODO this is horrible, it should be packaged with logback
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
		try {
			addAccess("ch.qos.logback.core.Appender", Flag.allDeclaredConstructors, Flag.allDeclaredMethods);
		} catch (NoClassDefFoundError e) {
			System.out.println("Logback not found, skipping registration logback types");
			return;
		}
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
