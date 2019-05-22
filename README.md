# spring-boot-graal-feature

Pass on the classpath to `native-image` and it will process your Spring Boot application
as the native image is built:

```
native-image \
  -Dorg.springframework.boot.logging.LoggingSystem=none \
  -Dio.netty.noUnsafe=true \
  --no-server -H:Name=clr \
  -H:+ReportExceptionStackTraces \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  -cp $CP com.example.commandlinerunner.CommandlinerunnerApplication
```

Latest notes:
17-May-2019: 
- now works with Graal 19 release
- now no longer 'patches' spring jars (using a different workaround for Graal #1196 issue)
- now computing much more based on spring.factories, not hard coded just for one app
- added more samples using the same graal feature for both
- added sample showing maven native-image invocation

```

[clr:36189]    classlist:   3,381.06 ms
███████╗██████╗ ██████╗ ██╗███╗   ██╗ ██████╗     ██████╗  ██████╗  ██████╗ ████████╗     ██████╗ ██████╗  █████╗  █████╗ ██╗
██╔════╝██╔══██╗██╔══██╗██║████╗  ██║██╔════╝     ██╔══██╗██╔═══██╗██╔═══██╗╚══██╔══╝    ██╔════╝ ██╔══██╗██╔══██╗██╔══██╗██║
███████╗██████╔╝██████╔╝██║██╔██╗ ██║██║  ███╗    ██████╔╝██║   ██║██║   ██║   ██║       ██║  ███╗██████╔╝███████║███████║██║
╚════██║██╔═══╝ ██╔══██╗██║██║╚██╗██║██║   ██║    ██╔══██╗██║   ██║██║   ██║   ██║       ██║   ██║██╔══██╗██╔══██║██╔══██║██║
███████║██║     ██║  ██║██║██║ ╚████║╚██████╔╝    ██████╔╝╚██████╔╝╚██████╔╝   ██║       ╚██████╔╝██║  ██║██║  ██║██║  ██║███████╗
╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝     ╚═════╝  ╚═════╝  ╚═════╝    ╚═╝        ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝

[clr:36189]        (cap):   1,084.62 ms
SBG: reflection registering #144 entries
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

The samples/demo project is a webflux based app using annotation based configuration for the controllers:
```
cd samples/demo
./compile.sh

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::

2019-05-11 22:44:56.722  INFO 17672 --- [           main] com.example.demo.DemoApplication         :
  Starting DemoApplication on Andys-MacBook-Pro-2018.local with PID 17672 (started by aclement in /Users/aclement/gits/spring-boot-graal-feature/samples/demo)
2019-05-11 22:44:56.722  INFO 17672 --- [           main] com.example.demo.DemoApplication         :
  No active profile set, falling back to default profiles: default
2019-05-11 22:44:56.770  INFO 17672 --- [           main] o.s.b.web.embedded.netty.NettyWebServer  : 
 Netty started on port(s): 8080
2019-05-11 22:44:56.770  INFO 17672 --- [           main] com.example.demo.DemoApplication         : 
 Started DemoApplication in 0.062 seconds (JVM running for 0.063)
```

The `compile.sh` script:

- mvn compiles the project
- unpacks the boot packaged jar
- runs native-image to compile the app
- runs the compiled app


TODO

- tidy it all up!
- review where the split would be framework vs boot
- use the netty feature rather than having the netty config in here
- generate more of what is in the json files rather than hard coding it
- try other workarounds for the graal issue
