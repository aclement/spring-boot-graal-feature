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
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
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
	
	private ReflectionRegistryAdapter rra;

	private ReflectionDescriptor constantReflectionDescriptor;

	private ImageClassLoader cl;

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
		cl = access.getImageClassLoader();
		rra = new ReflectionRegistryAdapter(rrs, cl);
		ReflectionDescriptor reflectionDescriptor = getConstantData();

		System.out.println("SBG: reflection registering #"+reflectionDescriptor.getClassDescriptors().size()+" entries");
		for (ClassDescriptor classDescriptor : reflectionDescriptor.getClassDescriptors()) {
			Class<?> type = null;
			String n2 = classDescriptor.getName();
			if (n2.endsWith("[]")) {
				System.out.println("ARRAY: "+n2.substring(0,n2.length()-2));
				type = rra.resolveType(n2.substring(0,n2.length()-2));
				System.out.println("Array base type resolved as "+type.getName());
				Object o = Array.newInstance(type, 1);
				type = o.getClass();
				System.out.println("Class of array is "+type.getName());
			} else {
				type = rra.resolveType(classDescriptor.getName());
			}
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
						rra.registerField(type, fieldDescriptor.getName(), fieldDescriptor.isAllowWrite(),fieldDescriptor.isAllowUnsafeAccess());
					} catch (NoSuchFieldException nsfe) {
						throw new IllegalStateException("Couldn't find field: " + type.getName()+"."+fieldDescriptor.getName(), nsfe);
//						System.out.println("SBG: WARNING: skipping reflection registration of field "+type.getName()+"."+fieldDescriptor.getName()+": field not found");
					}
				}
			}
		}
		registerLogback();
	}

	// TODO review - not strictly correct as they may ask with different flags (but right now they don't)
	public static final Set<String> added = new HashSet<>();

	/**
	 * Record that reflective access to a type (and a selection of its members based on the flags) should
	 * be possible at runtime. This method will pre-emptively check all type references to ensure later
	 * native-image processing will not fail if, for example, it trips up over a type reference in a
	 * generic type that isn't on the image building classpath. NOTE: it is assumed that if elements are
	 * not accessible that the runtime doesn't need them (this is done under the spring model where
	 * conditional checks on auto configuration would cause no attempts to be made to types/members that
	 * aren't added here).
	 * 
	 * @param typename the dotted type name for which to add reflective access
	 * @param flags any members that should be accessible via reflection
	 * @return the class, if the type was successfully registered for reflective access, otherwise null
	 */
	public Class<?> addAccess(String typename, Flag...flags) {
		if (!added.add(typename)) {
			return null;
		}
		System.out.println("SBG: INFO: Registering reflective access to "+typename);
		// This can return null if, for example, the supertype of the specified type is not
		// on the classpath. In a simple app there may be a number of types coming in from
		// spring-boot-autoconfigure but they extend types not on the classpath.
		Class<?> type = rra.resolveType(typename);
		if (type == null) {
			System.out.println("SBG: ERROR: CANNOT RESOLVE "+typename+" ???");
			return null;
		}
		if (constantReflectionDescriptor.hasClassDescriptor(typename)) {
			System.out.println("SBG: WARNING: type "+typename+" being added dynamically whilst "+RESOURCE_FILE+
					" already contains it - does it need to be in the file? ");
		}
		rra.registerType(type);
		for (Flag flag: flags) {
			try {
				switch (flag) {
				case allDeclaredClasses:
					if (verify(type.getDeclaredClasses())) {
						rra.registerDeclaredClasses(type);
					}
					break;
				case allDeclaredFields:
					if (verify(type.getDeclaredFields())) {
						rra.registerDeclaredFields(type);
					}
					break;
				case allPublicFields:
					if (verify(type.getFields())) {
						rra.registerPublicFields(type);
					}
					break;
				case allDeclaredConstructors:
					if (verify(type.getDeclaredConstructors())) {
						rra.registerDeclaredConstructors(type);	
					}
					break;
				case allPublicConstructors:
					if (verify(type.getConstructors())) {
						rra.registerPublicConstructors(type);
					}
					break;
				case allDeclaredMethods:
					if (verify(type.getDeclaredMethods())) {
						rra.registerDeclaredMethods(type);
					}
					break;
				case allPublicMethods:
					if (verify(type.getMethods())) {
						rra.registerPublicMethods(type);
					}
					break;
				case allPublicClasses:
					if (verify(type.getClasses())) {
						rra.registerPublicClasses(type);
					}
					break;
				}
			} catch (NoClassDefFoundError ncdfe) {
				System.out.println("SBG: ERROR: problem handling flag: "+flag+" for "+type.getName()+" because of missing "+ncdfe.getMessage());
			}
		}
		return type;
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
