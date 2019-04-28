# spring-boot-graal-feature

Pass on the classpath to `native-image` and it will process your Spring Boot application
as the native image is built:

```
native-image \
  -Dorg.springframework.boot.logging.LoggingSystem=none \
  -Dio.netty.noUnsafe=true \
  --no-server -H:Name=clr -H:ReflectionConfigurationFiles=../../../reflect.json \
  -H:IncludeResources='META-INF/spring.components|META-INF/spring.factories|application.properties|logging.properties|org/springframework/boot/context/properties/EnableConfigurationPropertiesImportSelector.class|org/springframework/boot/context/properties/EnableConfigurationPropertiesImportSelector\$ConfigurationPropertiesBeanRegistrar.class|org/springframework/boot/context/properties/ConfigurationPropertiesBindingPostProcessorRegistrar.class|org/springframework/boot/autoconfigure/.*/.*Configuration.class|com/example/commandlinerunner/CLR.class|com/example/commandlinerunner/CommandlinerunnerApplication.class|org/springframework/boot/CommandLineRunner.class|org/springframework/boot/autoconfigure/condition/SearchStrategy.class|org/springframework/context/EnvironmentAware.class|org/springframework/beans/factory/BeanFactoryAware.class|org/springframework/beans/factory/Aware.class' \
  -H:+ReportExceptionStackTraces \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --delay-class-initialization-to-runtime=org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener\$LiquibasePresent,\
org.springframework.boot.json.JacksonJsonParser,\
org.springframework.boot.autoconfigure.jdbc.HikariDriverConfigurationFailureAnalyzer,\
org.springframework.boot.diagnostics.analyzer.ValidationExceptionFailureAnalyzer,\
org.springframework.core.type.filter.AspectJTypeFilter,\
org.springframework.boot.autoconfigure.cache.JCacheCacheConfiguration,\
org.springframework.boot.autoconfigure.cache.RedisCacheConfiguration,\
org.springframework.boot.autoconfigure.cache.InfinispanCacheConfiguration,\
org.springframework.boot.json.GsonJsonParser,\
org.springframework.core.io.VfsUtils \
  -cp $CP com.example.commandlinerunner.CommandlinerunnerApplication
```

```
[clr:34465]    classlist:   4,522.89 ms
███████╗██████╗ ██████╗ ██╗███╗   ██╗ ██████╗     ██████╗  ██████╗  ██████╗ ████████╗     ██████╗ ██████╗  █████╗  █████╗ ██╗
██╔════╝██╔══██╗██╔══██╗██║████╗  ██║██╔════╝     ██╔══██╗██╔═══██╗██╔═══██╗╚══██╔══╝    ██╔════╝ ██╔══██╗██╔══██╗██╔══██╗██║
███████╗██████╔╝██████╔╝██║██╔██╗ ██║██║  ███╗    ██████╔╝██║   ██║██║   ██║   ██║       ██║  ███╗██████╔╝███████║███████║██║
╚════██║██╔═══╝ ██╔══██╗██║██║╚██╗██║██║   ██║    ██╔══██╗██║   ██║██║   ██║   ██║       ██║   ██║██╔══██╗██╔══██║██╔══██║██║
███████║██║     ██║  ██║██║██║ ╚████║╚██████╔╝    ██████╔╝╚██████╔╝╚██████╔╝   ██║       ╚██████╔╝██║  ██║██║  ██║██║  ██║███████╗
╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝     ╚═════╝  ╚═════╝  ╚═════╝    ╚═╝        ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝

[clr:34465]        (cap):   2,123.35 ms
SBG: Proxy registration: #3 proxies
[org.springframework.boot.context.properties.ConfigurationProperties, org.springframework.core.annotation.SynthesizedAnnotation]
[org.springframework.stereotype.Component]
[org.springframework.boot.context.properties.ConfigurationProperties]
[clr:34465]        setup:   3,854.49 ms
[clr:34465]   (typeflow):  13,798.89 ms
[clr:34465]    (objects):  16,477.79 ms
[clr:34465]   (features):   1,124.83 ms
[clr:34465]     analysis:  32,444.96 ms
[clr:34465]     universe:     822.19 ms
[clr:34465]      (parse):   1,786.89 ms
[clr:34465]     (inline):   3,649.50 ms
[clr:34465]    (compile):  19,231.44 ms
[clr:34465]      compile:  26,480.49 ms
[clr:34465]        image:   3,304.71 ms
[clr:34465]        write:     903.14 ms
[clr:34465]      [total]:  72,530.85 ms
```

Work in progress - currently handles dynamic proxy registration but when done there
will be no need to supply resources/reflection/delay-class-initialization options.
