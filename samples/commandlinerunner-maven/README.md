This is a variant of the commandlinerunner sample that uses maven to drive the native-image construction.

Ensure you have:
- `mvn install`ed the root feature project
- have the graal JDK installed and JAVA_HOME set appropriately (eg. /Users/aclement/installs/graalvm-ce-19.0.0/Contents/Home)

Then you can:

`mvn clean package`

This will compile the project then drive it through native-image, producing an executable called `clr`.

Notes:
- without the compile script we need to pass the options to the native-image command. This is done via the file in `src/main/resources/META-INF/com.example/commandlinerunner/native-image.properties`:
```
ImageName=clr
Args= -H:+ReportExceptionStackTraces --no-fallback --allow-incomplete-classpath --report-unsupported-elements-at-runtime
```
- the script also used to pass the initial class to the command. That is now done by setting `<start-class>` in the properties section of the pom.


.\clr
