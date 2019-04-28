package org.springframework.boot.graal.support;

import java.io.InputStream;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;
import org.springframework.boot.graal.domain.resources.ResourcesDescriptor;
import org.springframework.boot.graal.domain.resources.ResourcesJsonMarshaller;

import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.ResourcesFeature.ResourcesRegistry;

public class Resources extends Support {

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
