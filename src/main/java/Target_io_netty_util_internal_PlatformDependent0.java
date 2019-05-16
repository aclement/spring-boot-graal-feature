import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.TargetClass;

//@TargetClass(className = "io.netty.util.internal.PlatformDependent0")
//final class Target_io_netty_util_internal_PlatformDependent0 {
//     @Alias @RecomputeFieldValue(kind = Kind.FieldOffset, declClassName = "java.nio.Buffer", name = "address")
//     private static long ADDRESS_FIELD_OFFSET;
//}
// 
//@TargetClass(className = "io.netty.util.internal.CleanerJava6")
//final class Target_io_netty_util_internal_CleanerJava6 {
//     @Alias @RecomputeFieldValue(kind = Kind.FieldOffset, declClassName = "java.nio.DirectByteBuffer", name = "cleaner")
//     private static long CLEANER_FIELD_OFFSET;
//}
// 
//@TargetClass(className = "io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess")
//final class Target_io_netty_util_internal_shaded_org_jctools_util_UnsafeRefArrayAccess {
//     @Alias @RecomputeFieldValue(kind = Kind.ArrayIndexShift, declClass = Object[].class)
//     public static int REF_ELEMENT_SHIFT;
//}