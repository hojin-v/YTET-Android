package com.ytet.android.extract;

import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.core.MediaType;
import com.ytet.android.core.VideoQuality;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ExtractionOutputsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void collectsOnlyExpectedOutputsAndSkipsTransientFiles() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");
        write(workspace, "video.mp4");
        write(workspace, "audio.m4a");
        write(workspace, "download.part");
        write(workspace, "metadata.info.json");
        File nested = new File(workspace, "nested");
        assertTrue(nested.mkdirs());
        write(nested, "subtitle.ko.srt");

        List<File> outputs = ExtractionOutputs.collectOutputFiles(workspace);

        assertEquals(
                Arrays.asList("audio.m4a", "subtitle.ko.srt", "video.mp4"),
                names(outputs)
        );
    }

    @Test
    public void doesNotFallbackToArbitraryFiles() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");
        write(workspace, "metadata.info.json");
        write(workspace, "thumbnail.jpg");

        ExtractionException exception = assertThrows(
                ExtractionException.class,
                () -> ExtractionOutputs.collectOutputFiles(workspace)
        );

        assertTrue(exception.getMessage().contains("예상한 형식의 추출 결과 파일을 찾지 못했습니다."));
        assertTrue(exception.getMessage().contains("metadata.info.json"));
        assertTrue(exception.getMessage().contains("thumbnail.jpg"));
    }

    @Test
    public void reportsEmptyWorkspaceClearly() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");

        ExtractionException exception = assertThrows(
                ExtractionException.class,
                () -> ExtractionOutputs.collectOutputFiles(workspace)
        );

        assertEquals("추출 결과 파일을 찾지 못했습니다.", exception.getMessage());
    }

    @Test
    public void summarySeparatesRequestFromVerifiedFiles() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtu.be/abc123",
                "content://tree/output",
                MediaType.VIDEO,
                VideoQuality.P720.value(),
                true,
                false
        );
        StorageWriter.CopiedFile copiedFile = new StorageWriter.CopiedFile(
                "channel - title.mp4",
                "video/mp4",
                1536,
                "content://tree/output/video"
        );

        String summary = ExtractionOutputs.buildSummary(request, List.of(copiedFile));

        assertTrue(summary.contains("저장 완료"));
        assertTrue(summary.contains("검증: Android 저장소에 복사된 파일 확인"));
        assertTrue(summary.contains("요청: 영상 / 720p MP4"));
        assertTrue(summary.contains("자막 요청: 예"));
        assertTrue(summary.contains("다중 오디오 요청: 아니오"));
        assertTrue(summary.contains("파일: channel - title.mp4 (1.5 KB)"));
        assertTrue(summary.contains("위치: content://tree/output/video"));
    }

    private void write(File dir, String name) throws IOException {
        Files.write(new File(dir, name).toPath(), "test".getBytes());
    }

    private List<String> names(List<File> files) {
        return files.stream().map(File::getName).toList();
    }
}
