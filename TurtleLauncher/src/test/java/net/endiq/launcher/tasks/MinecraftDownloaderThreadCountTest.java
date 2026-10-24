package net.endiq.launcher.tasks;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MinecraftDownloaderThreadCountTest {
    @Test
    public void clampsLegacyValuesToTwenty() {
        assertEquals(20, MinecraftDownloader.resolveMaxDownloadThreads(128, 100));
    }

    @Test
    public void neverCreatesMoreWorkersThanScheduledFiles() {
        assertEquals(3, MinecraftDownloader.resolveMaxDownloadThreads(20, 3));
    }

    @Test
    public void respectsUserSettingBelowTheMaximum() {
        assertEquals(8, MinecraftDownloader.resolveMaxDownloadThreads(8, 100));
    }

    @Test
    public void usesAtLeastOneWorkerForEmptyOrInvalidCounts() {
        assertEquals(1, MinecraftDownloader.resolveMaxDownloadThreads(0, 100));
        assertEquals(1, MinecraftDownloader.resolveMaxDownloadThreads(20, 0));
    }
}
