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

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;
import org.springframework.boot.graal.domain.resources.ResourcesDescriptor;
import org.springframework.boot.graal.domain.resources.ResourcesJsonMarshaller;

import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.ResourcesFeature.ResourcesRegistry;

public class ResourcesHandler extends Support {

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
      ImageClassLoader imageClassLoader = ((BeforeAnalysisAccessImpl) access).getImageClassLoader();
      ResourcesDescriptor rd = compute();
      ResourcesRegistry resourcesRegistry = ImageSingletons.lookup(ResourcesRegistry.class);
      // Patterns can be added to the registry, resources can be directly registered against Resources
      // resourcesRegistry.addResources("*");
      // Resources.registerResource(relativePath, inputstream);
      System.out.println("SBG: adding resources, patterns: #"+rd.getPatterns().size());
      for (String pattern: rd.getPatterns()) {
//    	  if (pattern.equals("META-INF/spring.factories")) {
//    		  // voodoo
//    	  }
    	  resourcesRegistry.addResources(pattern);
      }
	}
}
