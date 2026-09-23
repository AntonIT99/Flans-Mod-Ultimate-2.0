package com.flansmodultimate.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.OptionalInt;

/**
 * Reads the playback length of an Ogg Vorbis file without decoding any audio.
 * <p>
 * The sample rate comes from the Vorbis identification header in the first Ogg page, and the total
 * sample count is the granule position of the last page, so only the head and the tail of the file
 * are ever read.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OggDurationReader
{
    /** Capture pattern every Ogg page starts with. */
    private static final byte[] CAPTURE_PATTERN = {'O', 'g', 'g', 'S'};
    /** Identification header of the first Vorbis packet: packet type 1 followed by the codec name. */
    private static final byte[] VORBIS_IDENTIFICATION = {0x01, 'v', 'o', 'r', 'b', 'i', 's'};

    /** Fixed part of an Ogg page header, before the segment table. */
    private static final int PAGE_HEADER_SIZE = 27;
    /** Largest possible Ogg page: the header, a full segment table and 255 segments of 255 bytes. */
    private static final int MAX_PAGE_SIZE = PAGE_HEADER_SIZE + 255 + 255 * 255;
    /** Enough to cover the first page header, its segment table and the identification header. */
    private static final int HEAD_PROBE_SIZE = 512;

    private static final int OFFSET_GRANULE_POSITION = 6;
    private static final int OFFSET_SEGMENT_COUNT = 26;
    private static final int OFFSET_IDENTIFICATION_SAMPLE_RATE = 12;

    /** Granule position written on pages that no packet finishes on. */
    private static final long GRANULE_POSITION_NONE = -1L;

    private static final int TICKS_PER_SECOND = 20;

    private static final Logger log = com.mojang.logging.LogUtils.getLogger();

    /**
     * Reads how many whole ticks the given Ogg Vorbis file plays for, truncated rather than rounded.
     *
     * @param file the {@code .ogg} file to measure
     * @return the length in ticks, at least {@code 1}, or empty when the file is not readable Ogg Vorbis
     */
    public static OptionalInt readDurationTicks(Path file)
    {
        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ))
        {
            long fileSize = channel.size();
            if (fileSize < PAGE_HEADER_SIZE)
                return OptionalInt.empty();

            int sampleRate = readSampleRate(channel);
            if (sampleRate <= 0)
                return OptionalInt.empty();

            long totalSamples = readLastGranulePosition(channel, fileSize);
            if (totalSamples <= 0)
                return OptionalInt.empty();

            // Integer division truncates, which is what a sound timer wants: a sound repeated on the
            // last tick it is still playing overlaps itself inaudibly, while one repeated a tick late
            // leaves a gap that is clearly audible in a continuous sound such as an engine.
            long ticks = totalSamples * TICKS_PER_SECOND / sampleRate;
            return OptionalInt.of((int)Math.max(1L, Math.min(Integer.MAX_VALUE, ticks)));
        }
        catch (IOException e)
        {
            log.warn("Could not read the sound length of {}: {}", file, e.toString());
            return OptionalInt.empty();
        }
    }

    /** Reads the sample rate from the Vorbis identification header carried by the first page. */
    private static int readSampleRate(SeekableByteChannel channel) throws IOException
    {
        ByteBuffer head = read(channel, 0, HEAD_PROBE_SIZE);
        if (!hasCapturePattern(head, 0))
            return 0;

        int segmentCount = Byte.toUnsignedInt(head.get(OFFSET_SEGMENT_COUNT));
        int payloadStart = PAGE_HEADER_SIZE + segmentCount;
        if (head.limit() < payloadStart + OFFSET_IDENTIFICATION_SAMPLE_RATE + Integer.BYTES)
            return 0;

        for (int i = 0; i < VORBIS_IDENTIFICATION.length; i++)
        {
            if (head.get(payloadStart + i) != VORBIS_IDENTIFICATION[i])
                return 0;
        }

        return head.getInt(payloadStart + OFFSET_IDENTIFICATION_SAMPLE_RATE);
    }

    /**
     * Scans backwards from the end of the file for the last page that finishes a packet. Its granule
     * position is the total number of samples in the stream.
     */
    private static long readLastGranulePosition(SeekableByteChannel channel, long fileSize) throws IOException
    {
        int tailSize = (int)Math.min(fileSize, MAX_PAGE_SIZE);
        ByteBuffer tail = read(channel, fileSize - tailSize, tailSize);

        for (int offset = tail.limit() - PAGE_HEADER_SIZE; offset >= 0; offset--)
        {
            if (!hasCapturePattern(tail, offset))
                continue;

            long granulePosition = tail.getLong(offset + OFFSET_GRANULE_POSITION);
            if (granulePosition != GRANULE_POSITION_NONE)
                return granulePosition;
        }

        return GRANULE_POSITION_NONE;
    }

    private static boolean hasCapturePattern(ByteBuffer buffer, int offset)
    {
        if (offset < 0 || offset + CAPTURE_PATTERN.length > buffer.limit())
            return false;

        for (int i = 0; i < CAPTURE_PATTERN.length; i++)
        {
            if (buffer.get(offset + i) != CAPTURE_PATTERN[i])
                return false;
        }
        return true;
    }

    /** Reads up to {@code length} bytes from {@code position}, little endian as all Ogg fields are. */
    private static ByteBuffer read(SeekableByteChannel channel, long position, int length) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        channel.position(position);
        while (buffer.hasRemaining() && channel.read(buffer) > 0)
        {
            // Reading until the buffer is full or the channel is exhausted.
        }
        return buffer.flip();
    }
}
