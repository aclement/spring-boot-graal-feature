package org.springframework.boot.graal.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.springframework.boot.graal.domain.proxies.ProxiesDescriptor;
import org.springframework.boot.graal.domain.resources.ResourcesDescriptor;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jdk.Resources;
import com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry;
import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl;
import com.oracle.svm.hosted.FeatureImpl.DuringSetupAccessImpl;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.ResourcesFeature;
import com.oracle.svm.hosted.ResourcesFeature.ResourcesRegistry;
import com.oracle.svm.hosted.config.ConfigurationDirectories;
import com.oracle.svm.hosted.config.ConfigurationParser;
import com.oracle.svm.hosted.config.ResourceConfigurationParser;
import com.oracle.svm.reflect.proxy.hosted.DynamicProxyFeature;
import com.oracle.svm.reflect.proxy.hosted.DynamicProxyFeature.Options;

@AutomaticFeature // indicates it will be added just by being found on the classpath
public class SpringFeature implements Feature {

	public SpringFeature() {
		System.out.println(
				"███████╗██████╗ ██████╗ ██╗███╗   ██╗ ██████╗     ██████╗  ██████╗  ██████╗ ████████╗     ██████╗ ██████╗  █████╗  █████╗ ██╗     \n" + 
				"██╔════╝██╔══██╗██╔══██╗██║████╗  ██║██╔════╝     ██╔══██╗██╔═══██╗██╔═══██╗╚══██╔══╝    ██╔════╝ ██╔══██╗██╔══██╗██╔══██╗██║     \n" + 
				"███████╗██████╔╝██████╔╝██║██╔██╗ ██║██║  ███╗    ██████╔╝██║   ██║██║   ██║   ██║       ██║  ███╗██████╔╝███████║███████║██║     \n" + 
				"╚════██║██╔═══╝ ██╔══██╗██║██║╚██╗██║██║   ██║    ██╔══██╗██║   ██║██║   ██║   ██║       ██║   ██║██╔══██╗██╔══██║██╔══██║██║     \n" + 
				"███████║██║     ██║  ██║██║██║ ╚████║╚██████╔╝    ██████╔╝╚██████╔╝╚██████╔╝   ██║       ╚██████╔╝██║  ██║██║  ██║██║  ██║███████╗\n" + 
				"╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝     ╚═════╝  ╚═════╝  ╚═════╝    ╚═╝        ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝\n" + 
				"                                                                                                                                  ");
	}
    /**
     * This method is called immediately after the constructor, to check whether the feature is part
     * of the configuration or not. If this method returns false, the feature is not included in the
     * list of features and no other methods are called (in particular, the
     * {@link #getRequiredFeatures required features} are not processed).
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return true;
    }

    /**
     * Returns the list of features that this feature depends on. As long as the dependency chain is
     * non-cyclic, all required features are processed before this feature.
     *
     * @since 1.0
     */
    public List<Class<? extends Feature>> getRequiredFeatures() {
    	List<Class<? extends Feature>> fs = new ArrayList<>();
    	fs.add(DynamicProxyFeature.class);
    	fs.add(ResourcesFeature.class);
    	return fs;
    }

    /**
     * Handler for initializations after all features have been registered and all options have been
     * parsed; but before any initializations for the static analysis have happened.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */

    public void afterRegistration(AfterRegistrationAccess access) {
//        ImageSingletons.add(ResourcesSupport.class, new ResourcesSupport());
    }

//    /**
//     * Handler for initializations at startup time. It allows customization of the static analysis
//     * setup.
//     *
//     * @param access The supported operations that the feature can perform at this time
//     *
//     * @since 1.0
//     */
//    public void duringSetup(DuringSetupAccess access) {
//    }
//    

    @Override
    public void duringSetup(DuringSetupAccess a) {
    	ProxiesDescriptor pd = new DynamicProxies().compute();
    	System.out.println("SBG: Proxy registration: #"+pd.getProxyDescriptors().size()+" proxies");
    	DuringSetupAccessImpl access = (DuringSetupAccessImpl) a;
    	ImageClassLoader imageClassLoader = access.getImageClassLoader();
    	// Should have been registered by DynamicProxyFeature already
    	DynamicProxyRegistry dynamicProxySupport = ImageSingletons.lookup(DynamicProxyRegistry.class);
//      DynamicProxySupport dynamicProxySupport = new DynamicProxySupport(imageClassLoader.getClassLoader());
//      ImageSingletons.add(DynamicProxyRegistry.class, dynamicProxySupport);
    	Consumer<List<String>> proxyRegisteringConsumer = interfaceNames -> {
    		System.out.println(interfaceNames);
            Class<?>[] interfaces = new Class<?>[interfaceNames.size()];
            for (int i = 0; i < interfaceNames.size(); i++) {
                String className = interfaceNames.get(i);
                Class<?> clazz = imageClassLoader.findClassByName(className, false);
                if (clazz == null) {
                    throw new RuntimeException("Class " + className + " not found");
                }
                if (!clazz.isInterface()) {
                    throw new RuntimeException("The class \"" + className + "\" is not an interface.");
                }
                interfaces[i] = clazz;
            }
            /* The interfaces array can be empty. The java.lang.reflect.Proxy API allows it. */
            dynamicProxySupport.addProxyClass(interfaces);
    	};
    	pd.consume(proxyRegisteringConsumer);
//        DuringSetupAccessImpl access = (DuringSetupAccessImpl) a;
//
//        ImageClassLoader imageClassLoader = access.getImageClassLoader();
//        DynamicProxySupport dynamicProxySupport = new DynamicProxySupport(imageClassLoader.getClassLoader());
//        ImageSingletons.add(DynamicProxyRegistry.class, dynamicProxySupport);
//
//        Consumer<String[]> adapter = interfaceNames -> {
//            Class<?>[] interfaces = new Class<?>[interfaceNames.length];
//            for (int i = 0; i < interfaceNames.length; i++) {
//                String className = interfaceNames[i];
//                Class<?> clazz = imageClassLoader.findClassByName(className, false);
//                if (clazz == null) {
//                    throw new RuntimeException("Class " + className + " not found");
//                }
//                if (!clazz.isInterface()) {
//                    throw new RuntimeException("The class \"" + className + "\" is not an interface.");
//                }
//                interfaces[i] = clazz;
//            }
//            /* The interfaces array can be empty. The java.lang.reflect.Proxy API allows it. */
//            dynamicProxySupport.addProxyClass(interfaces);
//        };
//        ProxyConfigurationParser parser = new ProxyConfigurationParser(adapter);
//        ConfigurationParser.parseAndRegisterConfigurations(parser, imageClassLoader, "dynamic proxy",
//                        Options.DynamicProxyConfigurationFiles, Options.DynamicProxyConfigurationResources, ConfigurationDirectories.FileNames.DYNAMIC_PROXY_NAME);
    }

    /**
     * Handler for initializations before the static analysis.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public void beforeAnalysis(BeforeAnalysisAccess access) {
//        ImageClassLoader imageClassLoader = ((BeforeAnalysisAccessImpl) access).getImageClassLoader();
//        ResourcesDescriptor rd = new Resources().load();
//        ResourcesRegistry rr = ImageSingletons.lookup(ResourcesRegistry.class);
//        rr.addResource
//        
//        Resources.registerResource(relativePath, is);
//        ResourceConfigurationParser parser = new ResourceConfigurationParser(ImageSingletons.lookup(ResourcesRegistry.class));
//        ConfigurationParser.parseAndRegisterConfigurations(parser, imageClassLoader, "resource",
//                        Options.ResourceConfigurationFiles, Options.ResourceConfigurationResources, ConfigurationDirectories.FileNames.RESOURCES_NAME);
//
//        newResources.addAll(Arrays.asList(Options.IncludeResources.getValue()));
    }

    /**
     * Handler for performing operations during the static analysis. This handler is called after
     * analysis is complete. So all analysis meta data is available. If the handler performs
     * changes, e.g., makes new types or methods reachable, it needs to call
     * {@link DuringAnalysisAccess#requireAnalysisIteration()}. This triggers a new iteration:
     * analysis is performed again and the handler is called again.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public void duringAnalysis(DuringAnalysisAccess access) {
    }

    /**
     * Handler for initializations after analysis and before universe creation.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public void afterAnalysis(AfterAnalysisAccess access) {
    }

    /**
     * Handler for code that needs to run after the analysis, even if an error has occured, e.g.,
     * like reporting code.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public void onAnalysisExit(OnAnalysisExitAccess access) {
    }

    /**
     * Handler for initializations before compilation.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public void beforeCompilation(BeforeCompilationAccess access) {
    }

    /**
     * Handler for initializations after compilation, i.e., before the native image is written.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public void afterCompilation(AfterCompilationAccess access) {
    }

    /**
     * Handler for initializations after the native image heap and code layout. Objects and methods
     * have their offsets assigned. At this point, no additional objects must be added to the native
     * image heap, i.e., modifying object fields of native image objects that are part of the native
     * image heap is not allowed at this point.
     *
     * @param access The supported operations that the feature can perform at this time
     *
     * @since 1.0
     */
    public void afterHeapLayout(AfterHeapLayoutAccess access) {
    }

    /**
     * Handler for altering the linker command after the native image has been built and before it
     * is written.
     *
     * @param access The supported operations that the feature can perform at this time.
     *
     * @since 1.0
     */
    public void beforeImageWrite(BeforeImageWriteAccess access) {
    }

    /**
     * Handler for altering the image (or shared object) that the linker command produced.
     *
     * @param access The supported operations that the feature can perform at this time.
     *
     * @since 1.0
     */
    public void afterImageWrite(AfterImageWriteAccess access) {
    }

    /**
     * Handler for cleanup. Can be used to cleanup static data. This can avoid memory leaks if
     * native image generation is done many times, e.g. during unit tests.
     * <p>
     * Usually, overriding this method can be avoided by putting a configuration object into the
     * {@link ImageSingletons}.
     *
     * @since 1.0
     */
    public void cleanup() {
    }
}
