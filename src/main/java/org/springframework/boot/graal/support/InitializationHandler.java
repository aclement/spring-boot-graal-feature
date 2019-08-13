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
import java.util.Objects;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;
import org.springframework.boot.graal.domain.buildtimeinit.InitializationDescriptor;
import org.springframework.boot.graal.domain.buildtimeinit.InitializationJsonMarshaller;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;

/**
 * 
 * @author Andy Clement
 */
public class InitializationHandler {

	public InitializationDescriptor compute() {
		try {
			InputStream s = this.getClass().getResourceAsStream("/initialization.json");
			return InitializationJsonMarshaller.read(s);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void register(BeforeAnalysisAccess access) {
		InitializationDescriptor id = compute();
		System.out.println("SBG: forcing explicit class initialization at build or runtime:");
		System.out.println(id.toString());
		List<Class> collect = id.getBuildtimeClasses().stream()
				.map(access::findClassByName).filter(Objects::nonNull).collect(Collectors.toList());
		RuntimeClassInitialization.initializeAtBuildTime(collect.toArray(new Class[] {}));
		id.getRuntimeClasses().stream()
				.map(access::findClassByName).filter(Objects::nonNull)
				.forEach(RuntimeClassInitialization::initializeAtRunTime);
		System.out.println("Registering these packages for buildtime initialization: \n"+id.getBuildtimePackages());
		RuntimeClassInitialization.initializeAtBuildTime(id.getBuildtimePackages().toArray(new String[] {}));
		System.out.println("Registering these packages for runtime initialization: \n"+id.getRuntimePackages());
		RuntimeClassInitialization.initializeAtRunTime(id.getRuntimePackages().toArray(new String[] {}));
	}

}
