package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OggDurationReaderTest
{
    private static final int SAMPLE_RATE = 44100;

    @TempDir
    Path tempDir;

    @Test
    void readsTheLengthOfAWholeNumberOfTicks() throws IOException
    {
        Path file = writeOggFile("three_seconds.ogg", SAMPLE_RATE, SAMPLE_RATE * 3L);

        assertEquals(OptionalInt.of(60), OggDurationReader.readDurationTicks(file));
    }

    /**
     * A sound repeated on the last tick it is still playing overlaps itself inaudibly, while one
     * repeated a tick late leaves an audible gap, so a part tick is always dropped.
     */
    @Test
    void truncatesPartTicksRatherThanRoundingUp() throws IOException
    {
        Path justOver = writeOggFile("just_over.ogg", SAMPLE_RATE, (long)(SAMPLE_RATE * 2.51));
        Path justUnder = writeOggFile("just_under.ogg", SAMPLE_RATE, (long)(SAMPLE_RATE * 2.99));

        assertEquals(OptionalInt.of(50), OggDurationReader.readDurationTicks(justOver));
        assertEquals(OptionalInt.of(59), OggDurationReader.readDurationTicks(justUnder));
    }

    @Test
    void neverReportsLessThanOneTick() throws IOException
    {
        Path file = writeOggFile("very_short.ogg", SAMPLE_RATE, 100L);

        assertEquals(OptionalInt.of(1), OggDurationReader.readDurationTicks(file));
    }

    @Test
    void honoursTheSampleRate() throws IOException
    {
        Path file = writeOggFile("low_rate.ogg", 22050, 22050L * 2);

        assertEquals(OptionalInt.of(40), OggDurationReader.readDurationTicks(file));
    }

    @Test
    void skipsTrailingPagesThatFinishNoPacket() throws IOException
    {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.writeBytes(identificationPage(SAMPLE_RATE));
        content.writeBytes(page(SAMPLE_RATE * 2L, new byte[0]));
        content.writeBytes(page(-1L, new byte[0]));

        Path file = tempDir.resolve("unfinished_tail.ogg");
        Files.write(file, content.toByteArray());

        assertEquals(OptionalInt.of(40), OggDurationReader.readDurationTicks(file));
    }

    @Test
    void reportsNothingForFilesThatAreNotOggVorbis() throws IOException
    {
        Path notOgg = tempDir.resolve("not_ogg.ogg");
        Files.write(notOgg, "this is not a sound file".getBytes(StandardCharsets.UTF_8));

        Path empty = tempDir.resolve("empty.ogg");
        Files.write(empty, new byte[0]);

        assertTrue(OggDurationReader.readDurationTicks(notOgg).isEmpty());
        assertTrue(OggDurationReader.readDurationTicks(empty).isEmpty());
        assertTrue(OggDurationReader.readDurationTicks(tempDir.resolve("missing.ogg")).isEmpty());
    }

    private Path writeOggFile(String fileName, int sampleRate, long totalSamples) throws IOException
    {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.writeBytes(identificationPage(sampleRate));
        content.writeBytes(page(totalSamples, new byte[0]));

        Path file = tempDir.resolve(fileName);
        Files.write(file, content.toByteArray());
        return file;
    }

    /** First page of the stream, carrying the Vorbis identification header. */
    private static byte[] identificationPage(int sampleRate)
    {
        ByteBuffer header = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
        header.put((byte)0x01);
        header.put("vorbis".getBytes(StandardCharsets.UTF_8));
        header.putInt(0); // Vorbis version
        header.put((byte)2); // channels
        header.putInt(sampleRate);
        header.putInt(0); // maximum bitrate
        header.putInt(128000); // nominal bitrate
        header.putInt(0); // minimum bitrate
        header.put((byte)0xB8); // block sizes
        header.put((byte)0x01); // framing flag

        return page(0L, header.array());
    }

    /** Builds one Ogg page. The checksum is left blank because the reader never validates it. */
    private static byte[] page(long granulePosition, byte[] payload)
    {
        int segmentCount = Math.max(1, (payload.length + 254) / 255);

        ByteBuffer page = ByteBuffer.allocate(27 + segmentCount + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        page.put("OggS".getBytes(StandardCharsets.UTF_8));
        page.put((byte)0); // stream structure version
        page.put((byte)0); // header type
        page.putLong(granulePosition);
        page.putInt(1); // bitstream serial number
        page.putInt(0); // page sequence number
        page.putInt(0); // checksum
        page.put((byte)segmentCount);

        int remaining = payload.length;
        for (int segment = 0; segment < segmentCount; segment++)
        {
            page.put((byte)Math.min(255, remaining));
            remaining -= Math.min(255, remaining);
        }
        page.put(payload);

        return page.array();
    }
}
