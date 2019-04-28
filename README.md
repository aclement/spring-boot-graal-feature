# spring-boot-graal-feature

Pass on the classpath to `native-image` and it will process your Spring Boot application
as the native image is built:

```
native-image \
  -Dorg.springframework.boot.logging.LoggingSystem=none \
  -Dio.netty.noUnsafe=true \
  --no-server -H:Name=clr -H:ReflectionConfigurationFiles=../../../reflect.json \
  -H:+ReportExceptionStackTraces \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  -cp $CP com.example.commandlinerunner.CommandlinerunnerApplication
```

```

[clr:36189]    classlist:   3,381.06 ms
███████╗██████╗ ██████╗ ██╗███╗   ██╗ ██████╗     ██████╗  ██████╗  ██████╗ ████████╗     ██████╗ ██████╗  █████╗  █████╗ ██╗
██╔════╝██╔══██╗██╔══██╗██║████╗  ██║██╔════╝     ██╔══██╗██╔═══██╗██╔═══██╗╚══██╔══╝    ██╔════╝ ██╔══██╗██╔══██╗██╔══██╗██║
███████╗██████╔╝██████╔╝██║██╔██╗ ██║██║  ███╗    ██████╔╝██║   ██║██║   ██║   ██║       ██║  ███╗██████╔╝███████║███████║██║
╚════██║██╔═══╝ ██╔══██╗██║██║╚██╗██║██║   ██║    ██╔══██╗██║   ██║██║   ██║   ██║       ██║   ██║██╔══██╗██╔══██║██╔══██║██║
███████║██║     ██║  ██║██║██║ ╚████║╚██████╔╝    ██████╔╝╚██████╔╝╚██████╔╝   ██║       ╚██████╔╝██║  ██║██║  ██║██║  ██║███████╗
╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝     ╚═════╝  ╚═════╝  ╚═════╝    ╚═╝        ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝

[clr:36189]        (cap):   1,084.62 ms
SBG: Proxy registration: #3 proxies
[org.springframework.boot.context.properties.ConfigurationProperties, org.springframework.core.annotation.SynthesizedAnnotation]
[org.springframework.stereotype.Component]
[org.springframework.boot.context.properties.ConfigurationProperties]
[clr:36189]        setup:   2,225.69 ms
SBG: adding resources, patterns: #15
SBG: delaying initialization of #10 classes
[clr:36189]   (typeflow):  12,400.81 ms
[clr:36189]    (objects):  14,827.05 ms
[clr:36189]   (features):   1,069.89 ms
[clr:36189]     analysis:  29,255.59 ms
[clr:36189]     universe:     847.85 ms
[clr:36189]      (parse):   1,924.93 ms
[clr:36189]     (inline):   4,442.09 ms
[clr:36189]    (compile):  22,225.32 ms
[clr:36189]      compile:  30,564.01 ms
[clr:36189]        image:   3,765.08 ms
[clr:36189]        write:   1,005.77 ms
[clr:36189]      [total]:  71,211.78 ms
```

Work in progress
