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

	public ReflectionDescriptor compute() {
		try {
			InputStream s = this.getClass().getResourceAsStream("/reflect.json");
			return JsonMarshaller.read(s);
		} catch (Exception e) {
			return null;
		}
	}

	public void register(DuringSetupAccess a) {
		DuringSetupAccessImpl access = (DuringSetupAccessImpl) a;
		RuntimeReflectionSupport rrs = ImageSingletons.lookup(RuntimeReflectionSupport.class);
		ImageClassLoader cl = access.getImageClassLoader();
		ReflectionRegistryAdapter rra = new ReflectionRegistryAdapter(rrs, cl);
		ReflectionDescriptor reflectionDescriptor = compute();
		System.out.println("SBG: reflection registering #"+reflectionDescriptor.getClassDescriptors().size()+" entries");
		for (ClassDescriptor classDescriptor : reflectionDescriptor.getClassDescriptors()) {
			Class<?> type = rra.resolveType(classDescriptor.getName());
	        rra.registerType(type);
			Set<Flag> flags = classDescriptor.getFlags();
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
	}

}
