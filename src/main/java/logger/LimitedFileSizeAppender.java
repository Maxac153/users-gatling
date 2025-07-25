//package logger;
//
//import ch.qos.logback.core.rolling.TriggeringPolicyBase;
//import ch.qos.logback.core.util.Duration;
//import ch.qos.logback.core.util.FileSize;
//import ch.qos.logback.core.util.InvocationGate;
//import ch.qos.logback.core.util.SimpleInvocationGate;
//import lombok.Getter;
//import lombok.Setter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//
//public class LimitedFileSizeAppender<E> extends TriggeringPolicyBase<E> {
//    private static final Logger log = LoggerFactory.getLogger(LimitedFileSizeAppender.class);
//    private boolean isCopied = false;
//    private static final long DEFAULT_MAX_FILE_SIZE = 10_485_760L;
//    @Setter
//    @Getter
//    FileSize maxFileSize = new FileSize(DEFAULT_MAX_FILE_SIZE);
//    InvocationGate invocationGate = new SimpleInvocationGate();
//    @Setter
//    @Getter
//    Duration checkIncrement = null;
//
//    public LimitedFileSizeAppender() {
//    }
//
//    public void start() {
//        if (checkIncrement != null) {
//            invocationGate = new SimpleInvocationGate(checkIncrement);
//        }
//        super.start();
//    }
//
//    private void copyFile(File sourceFile) {
//        try {
//            Path sourcePath = Paths.get(sourceFile.getAbsolutePath());
//            String baseName = sourceFile.getName();
//            int lastDotIndex = baseName.lastIndexOf('.');
//            String baseNameWithoutExtension = baseName.substring(0, lastDotIndex);
//            String extension = baseName.substring(lastDotIndex);
//
//            Path targetPath = Paths.get(sourceFile.getParent(), baseNameWithoutExtension + "_#1" + extension);
//            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
//            Files.writeString(sourcePath, "");
//            log.info("File Copied: {}", sourceFile.getName());
//        } catch (IOException e) {
//            log.error("Error Copying File: {}", e.getMessage(), e);
//        }
//    }
//
//    public boolean isTriggeringEvent(final File activeFile, final E event) {
//        long now = System.currentTimeMillis();
//        if (invocationGate.isTooSoon(now)) {
//            return false;
//        }
//
//        long fileSize = activeFile.length();
//        if (!isCopied && fileSize >= maxFileSize.getSize()) {
//            copyFile(activeFile);
//            isCopied = true;
//            return false;
//        }
//
//        return (fileSize >= maxFileSize.getSize());
//    }
//}
