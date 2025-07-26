package logger;

import ch.qos.logback.core.rolling.TriggeringPolicyBase;
import ch.qos.logback.core.util.Duration;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.InvocationGate;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
public class LimitedFileSizeAppender<E> extends TriggeringPolicyBase<E> {
    private boolean isCopied = false;
    private static final long DEFAULT_MAX_FILE_SIZE = 10_485_760L; // 10 MB

    @Setter
    @Getter
    FileSize maxFileSize = new FileSize(DEFAULT_MAX_FILE_SIZE);

    InvocationGate invocationGate = new SimpleInvocationGate(new Duration(1000));

    @Setter
    @Getter
    Duration checkIncrement = null;

    public LimitedFileSizeAppender() {
    }

    @Override
    public void start() {
        if (checkIncrement != null) {
            invocationGate = new SimpleInvocationGate(checkIncrement);
        }
        super.start();
    }

    private void copyFile(File sourceFile) {
        try {
            Path sourcePath = Paths.get(sourceFile.getAbsolutePath());
            String baseName = sourceFile.getName();
            int lastDotIndex = baseName.lastIndexOf('.');
            String baseNameWithoutExtension = lastDotIndex == -1 ? baseName : baseName.substring(0, lastDotIndex);
            String extension = lastDotIndex == -1 ? "" : baseName.substring(lastDotIndex);

            Path targetPath = Paths.get(sourceFile.getParent(), baseNameWithoutExtension + "_#1" + extension);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(sourcePath, "");
            log.info("File Copied: {}", sourceFile.getName());
        } catch (IOException e) {
            log.error("Error Copying File: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isTriggeringEvent(final File activeFile, final E event) {
        long now = System.currentTimeMillis();
        if (invocationGate.isTooSoon(now)) {
            return false;
        }

        long fileSize = activeFile.length();
        if (!isCopied && fileSize >= maxFileSize.getSize()) {
            copyFile(activeFile);
            isCopied = true;
            return false;
        }

        return (fileSize >= maxFileSize.getSize());
    }

    private static class SimpleInvocationGate implements InvocationGate {
        private long last = 0;
        private final long minInterval;

        public SimpleInvocationGate(Duration minInterval) {
            this.minInterval = minInterval.getMilliseconds();
        }

        @Override
        public boolean isTooSoon(long now) {
            if (now - last < minInterval) {
                return true;
            }
            last = now;
            return false;
        }
    }
}
