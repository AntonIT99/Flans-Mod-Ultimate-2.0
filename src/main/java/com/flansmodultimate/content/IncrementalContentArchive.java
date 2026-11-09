package com.flansmodultimate.content;

import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Regenerates a content pack archive without moving the files generation does not touch: only the
 * selected entries reach the disk, and every entry that comes back unchanged is copied over in its
 * compressed form. Archives outside the plain ZIP subset read here get {@code null} from
 * {@link #open}, so the caller can take the full extract-and-repack route.
 */
final class IncrementalContentArchive
{
    private static final int LOCAL_SIGNATURE = 0x04034b50;
    private static final int CENTRAL_SIGNATURE = 0x02014b50;
    private static final int END_SIGNATURE = 0x06054b50;
    private static final int LOCAL_HEADER_SIZE = 30;
    private static final int CENTRAL_HEADER_SIZE = 46;
    private static final int END_RECORD_SIZE = 22;
    private static final int MAX_COMMENT_SIZE = 0xFFFF;
    private static final int METHOD_STORED = 0;
    private static final int METHOD_DEFLATED = 8;
    private static final int FLAG_ENCRYPTED = 0x1;
    private static final int FLAG_UTF8 = 0x800;
    private static final int ZIP_VERSION = 20;
    private static final long MAX_UINT32 = 0xFFFFFFFFL;
    private static final int MAX_UINT16 = 0xFFFF;
    // Generation can roughly double a pack: these sources stay clear of the ZIP64 limits of the output.
    private static final int MAX_SOURCE_ENTRIES = 30_000;
    private static final long MAX_SOURCE_BYTES = 3L << 29;
    private static final long COMPRESSION_BATCH_BYTES = 32L << 20;
    private static final int WRITE_BUFFER_BYTES = 1 << 20;

    private record Entry(String name, boolean directory, int method, long crc, long compressedSize, long size, int dosTime, int dosDate, long dataOffset)
    {}

    private record ExtractedFile(Entry entry, ContentFileCache.Stamp stamp)
    {}

    private record PendingFile(Entry entry, Path output)
    {}

    // Only ever read through its accessors: equals and hashCode are never used, so the array component is harmless.
    @SuppressWarnings("java:S6218")
    private record Compressed(String name, int method, long crc, long size, byte[] data)
    {}

    private final Path archive;
    private final List<Entry> entries;
    private final Set<String> selectedNames = new HashSet<>();
    private final Map<String, ExtractedFile> extractedFiles = new HashMap<>();

    private IncrementalContentArchive(Path archive, List<Entry> entries)
    {
        this.archive = archive;
        this.entries = entries;
    }

    /** Returns {@code null} when the archive needs the full extract-and-repack route. */
    @Nullable
    static IncrementalContentArchive open(Path archive)
    {
        try
        {
            List<Entry> entries = readEntries(archive);
            return entries == null ? null : new IncrementalContentArchive(archive, entries);
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.debug("Using full repacking for '{}': {}", archive, e.toString());
            return null;
        }
    }

    /**
     * Extracts the entries accepted by the filter, which receives lower-case entry names.
     *
     * @return {@code true} when extraction completed successfully
     */
    boolean extract(Path outputDir, Predicate<String> filter, ContentLoadingWorkers workers)
    {
        try (FileChannel input = FileChannel.open(archive, StandardOpenOption.READ))
        {
            Path root = outputDir.toAbsolutePath().normalize();
            Files.createDirectories(root);

            // Java model sources are compiled against the classes of the whole pack.
            boolean everything = entries.stream().anyMatch(entry -> !entry.directory() && entry.name().toLowerCase(Locale.ROOT).endsWith(".java"));
            // Keyed by path: names that the platform treats as one file are also extracted as one, the last one winning.
            Map<Path, Entry> selected = new LinkedHashMap<>();
            Set<Path> directories = new LinkedHashSet<>();
            for (Entry entry : entries)
            {
                Path output = root.resolve(entry.name()).normalize();
                if (!output.startsWith(root))
                    throw new IOException("Blocked zip entry (zip slip): " + entry.name());
                if (!everything && !filter.test(entry.name().toLowerCase(Locale.ROOT)))
                    continue;
                selectedNames.add(entry.name());
                selected.put(output, entry);
                directories.add(entry.directory() ? output : output.getParent());
            }
            for (Path directory : directories)
                Files.createDirectories(directory);

            List<PendingFile> files = selected.entrySet().stream().filter(file -> !file.getValue().directory()).map(file -> new PendingFile(file.getValue(), file.getKey())).toList();
            for (ExtractedFile file : workers.map(files, file -> extractFile(input, file)))
                extractedFiles.put(file.entry().name(), file);
            FlansLog.log.debug("{}: Extracted {} of {} archive entries", archive.getFileName(), files.size(), entries.size());
            return true;
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.error("Failed to extract archive for content pack {}", archive, e);
            return false;
        }
    }

    /** Writes the original archive with the extracted directory's changes applied. */
    void write(Path target, Path extractedRoot, ContentLoadingWorkers workers) throws IOException
    {
        try
        {
            Map<String, Path> disk = listFiles(extractedRoot);
            Set<String> written = new HashSet<>();
            try (FileChannel input = FileChannel.open(archive, StandardOpenOption.READ); ArchiveWriter writer = new ArchiveWriter(target))
            {
                for (Entry entry : latestEntries())
                {
                    if (isRetained(entry, disk))
                    {
                        writer.copy(input, entry);
                        written.add(entry.name());
                    }
                }
                writeFromDisk(disk, written, writer, workers);
                writer.finish();
            }
        }
        catch (UncheckedIOException e)
        {
            throw e.getCause();
        }
    }

    private boolean isRetained(Entry entry, Map<String, Path> disk) throws IOException
    {
        if (!selectedNames.contains(entry.name()))
            return !disk.containsKey(entry.name());
        ExtractedFile extracted = extractedFiles.get(entry.name());
        Path file = disk.get(entry.name());
        return extracted != null && extracted.entry().equals(entry) && file != null && Files.isRegularFile(file) && ContentFileCache.stamp(file).equals(extracted.stamp());
    }

    /** A name stored more than once resolves to its last entry, as it does when extracted in order. */
    private Iterable<Entry> latestEntries()
    {
        Map<String, Entry> latest = new LinkedHashMap<>();
        for (Entry entry : entries)
            latest.put(entry.name(), entry);
        return latest.values();
    }

    private static void writeFromDisk(Map<String, Path> disk, Set<String> written, ArchiveWriter writer, ContentLoadingWorkers workers) throws IOException
    {
        List<Map.Entry<String, Path>> batch = new ArrayList<>();
        long batchBytes = 0;
        for (Map.Entry<String, Path> file : disk.entrySet())
        {
            if (written.contains(file.getKey()))
                continue;
            if (Files.isDirectory(file.getValue()))
            {
                writer.directory(file.getKey(), writer.currentDosTime(), writer.currentDosDate());
            }
            else
            {
                batch.add(file);
                batchBytes += Files.size(file.getValue());
                // Bounds the compressed data held in memory while staying in a deterministic order.
                if (batchBytes >= COMPRESSION_BATCH_BYTES)
                {
                    writeBatch(batch, writer, workers);
                    batch.clear();
                    batchBytes = 0;
                }
            }
        }
        writeBatch(batch, writer, workers);
    }

    private static void writeBatch(List<Map.Entry<String, Path>> batch, ArchiveWriter writer, ContentLoadingWorkers workers) throws IOException
    {
        for (Compressed file : workers.map(batch, IncrementalContentArchive::compress))
            writer.compressed(file);
    }

    private static Compressed compress(Map.Entry<String, Path> file)
    {
        try
        {
            byte[] bytes = Files.readAllBytes(file.getValue());
            CRC32 crc = new CRC32();
            crc.update(bytes);
            byte[] deflated = deflate(bytes);
            return deflated.length < bytes.length ? new Compressed(file.getKey(), METHOD_DEFLATED, crc.getValue(), bytes.length, deflated)
                : new Compressed(file.getKey(), METHOD_STORED, crc.getValue(), bytes.length, bytes);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    // Deflater is not AutoCloseable on Java 17: end() releases its native memory.
    @SuppressWarnings("java:S2093")
    private static byte[] deflate(byte[] bytes)
    {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try
        {
            deflater.setInput(bytes);
            deflater.finish();
            ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(32, bytes.length / 2));
            byte[] chunk = new byte[1 << 16];
            while (!deflater.finished())
                output.write(chunk, 0, deflater.deflate(chunk));
            return output.toByteArray();
        }
        finally
        {
            deflater.end();
        }
    }

    private static Map<String, Path> listFiles(Path extractedRoot) throws IOException
    {
        Map<String, Path> files = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(extractedRoot))
        {
            walk.filter(path -> !path.equals(extractedRoot)).forEach(path -> files.put(extractedRoot.relativize(path).toString().replace('\\', '/'), path));
        }
        return files;
    }

    private static ExtractedFile extractFile(FileChannel input, PendingFile file)
    {
        Entry entry = file.entry();
        Inflater inflater = new Inflater(true);
        try (InputStream range = new RangeInputStream(input, entry.dataOffset(), entry.compressedSize());
            InputStream data = entry.method() == METHOD_DEFLATED ? new InflaterInputStream(range, inflater, 1 << 14) : range;
            CheckedInputStream checked = new CheckedInputStream(data, new CRC32());
            OutputStream output = Files.newOutputStream(file.output()))
        {
            checked.transferTo(output);
            if (checked.getChecksum().getValue() != entry.crc())
                throw new IOException("CRC mismatch in archive entry " + entry.name());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            inflater.end();
        }

        try
        {
            return new ExtractedFile(entry, ContentFileCache.stamp(file.output()));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Nullable
    private static List<Entry> readEntries(Path archive) throws IOException
    {
        try (FileChannel input = FileChannel.open(archive, StandardOpenOption.READ))
        {
            long archiveSize = input.size();
            if (archiveSize < END_RECORD_SIZE || archiveSize > MAX_SOURCE_BYTES)
                return null;

            int tailSize = (int) Math.min(archiveSize, (long) END_RECORD_SIZE + MAX_COMMENT_SIZE);
            long tailStart = archiveSize - tailSize;
            ByteBuffer tail = read(input, tailStart, tailSize);
            int end = findEndRecord(tail);
            if (end < 0 || uint16(tail, end + 4) != 0 || uint16(tail, end + 6) != 0)
                return null;

            int count = uint16(tail, end + 10);
            long centralSize = uint32(tail, end + 12);
            long centralOffset = uint32(tail, end + 16);
            // The offsets of an archive with data in front of it, or of a ZIP64 archive, do not add up.
            if (count != uint16(tail, end + 8) || count > MAX_SOURCE_ENTRIES || centralOffset + centralSize != tailStart + end)
                return null;

            return readCentralDirectory(input, read(input, centralOffset, (int) centralSize), count, centralOffset);
        }
    }

    private static int findEndRecord(ByteBuffer tail)
    {
        for (int offset = tail.capacity() - END_RECORD_SIZE; offset >= 0; offset--)
        {
            if (tail.getInt(offset) == END_SIGNATURE && offset + END_RECORD_SIZE + uint16(tail, offset + 20) == tail.capacity())
                return offset;
        }
        return -1;
    }

    @Nullable
    private static List<Entry> readCentralDirectory(FileChannel input, ByteBuffer central, int count, long centralOffset) throws IOException
    {
        List<Entry> result = new ArrayList<>(count);
        int position = 0;
        for (int i = 0; i < count; i++)
        {
            if (position + CENTRAL_HEADER_SIZE > central.capacity() || central.getInt(position) != CENTRAL_SIGNATURE)
                return null;

            int flags = uint16(central, position + 8);
            int method = uint16(central, position + 10);
            long compressedSize = uint32(central, position + 20);
            long size = uint32(central, position + 24);
            long localOffset = uint32(central, position + 42);
            int nameLength = uint16(central, position + 28);
            String rawName = decodeName(central, position + CENTRAL_HEADER_SIZE, nameLength);
            if (rawName == null || isUnsupported(flags, method, compressedSize, size, localOffset))
                return null;

            boolean directory = rawName.endsWith("/");
            String name = FileUtils.safeArchiveEntryName(rawName, directory);
            if (name != null)
            {
                long dataOffset = dataOffset(input, localOffset);
                if (dataOffset < 0 || dataOffset + compressedSize > centralOffset)
                    return null;
                result.add(new Entry(name, directory, method, uint32(central, position + 16), compressedSize, size, uint16(central, position + 12), uint16(central, position + 14), dataOffset));
            }
            position += CENTRAL_HEADER_SIZE + nameLength + uint16(central, position + 30) + uint16(central, position + 32);
        }
        return result;
    }

    private static boolean isUnsupported(int flags, int method, long compressedSize, long size, long localOffset)
    {
        boolean unsupportedMethod = method != METHOD_STORED && method != METHOD_DEFLATED || method == METHOD_STORED && compressedSize != size;
        return (flags & FLAG_ENCRYPTED) != 0 || unsupportedMethod || compressedSize == MAX_UINT32 || size == MAX_UINT32 || localOffset == MAX_UINT32;
    }

    /** Returns the offset of the entry data, or -1 when the local header is not valid. */
    private static long dataOffset(FileChannel input, long localOffset) throws IOException
    {
        ByteBuffer local = read(input, localOffset, LOCAL_HEADER_SIZE);
        if (local.getInt(0) != LOCAL_SIGNATURE)
            return -1;
        return localOffset + LOCAL_HEADER_SIZE + uint16(local, 26) + uint16(local, 28);
    }

    /** Names that are not UTF-8 stay with the full route, which has always decoded them as UTF-8. */
    @Nullable
    private static String decodeName(ByteBuffer central, int offset, int length)
    {
        if (offset + length > central.capacity())
            return null;
        try
        {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(central.duplicate().position(offset).limit(offset + length))
                .toString();
        }
        catch (CharacterCodingException e)
        {
            return null;
        }
    }

    private static ByteBuffer read(FileChannel input, long offset, int length) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        long position = offset;
        while (buffer.hasRemaining())
        {
            int read = input.read(buffer, position);
            if (read < 0)
                throw new EOFException("Unexpected end of archive");
            position += read;
        }
        buffer.flip();
        return buffer;
    }

    private static int uint16(ByteBuffer buffer, int offset)
    {
        return Short.toUnsignedInt(buffer.getShort(offset));
    }

    private static long uint32(ByteBuffer buffer, int offset)
    {
        return Integer.toUnsignedLong(buffer.getInt(offset));
    }

    /** Reads a byte range through positional reads, so that workers can share one channel. */
    private static final class RangeInputStream extends InputStream
    {
        private final FileChannel channel;
        private long position;
        private long remaining;

        private RangeInputStream(FileChannel channel, long position, long length)
        {
            this.channel = channel;
            this.position = position;
            this.remaining = length;
        }

        @Override
        public int read() throws IOException
        {
            byte[] single = new byte[1];
            return read(single, 0, 1) < 0 ? -1 : Byte.toUnsignedInt(single[0]);
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException
        {
            if (remaining <= 0)
                return -1;
            int read = channel.read(ByteBuffer.wrap(bytes, offset, (int) Math.min(length, remaining)), position);
            if (read < 0)
                throw new EOFException("Unexpected end of archive");
            position += read;
            remaining -= read;
            return read;
        }
    }

    /** Writes a plain ZIP file whose local headers carry the sizes, so no entry needs a data descriptor. */
    private static final class ArchiveWriter implements AutoCloseable
    {
        private final FileChannel output;
        private final ByteBuffer buffer = ByteBuffer.allocate(WRITE_BUFFER_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        private final ByteArrayOutputStream central = new ByteArrayOutputStream();
        private final int dosTime;
        private final int dosDate;
        private long position;
        private int count;

        private ArchiveWriter(Path target) throws IOException
        {
            output = FileChannel.open(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault()); // ZIP DOS timestamps are local time
            dosTime = now.getHour() << 11 | now.getMinute() << 5 | now.getSecond() / 2;
            dosDate = (now.getYear() - 1980) << 9 | now.getMonthValue() << 5 | now.getDayOfMonth();
        }

        private int currentDosTime()
        {
            return dosTime;
        }

        private int currentDosDate()
        {
            return dosDate;
        }

        private void copy(FileChannel input, Entry entry) throws IOException
        {
            if (entry.directory())
            {
                directory(entry.name(), entry.dosTime(), entry.dosDate());
                return;
            }
            header(entry.name(), entry.method(), entry.crc(), entry.compressedSize(), entry.size(), entry.dosTime(), entry.dosDate());
            copyData(input, entry.dataOffset(), entry.compressedSize());
        }

        private void directory(String name, int time, int date) throws IOException
        {
            header(name + "/", METHOD_STORED, 0, 0, 0, time, date);
        }

        private void compressed(Compressed file) throws IOException
        {
            header(file.name(), file.method(), file.crc(), file.data().length, file.size(), dosTime, dosDate);
            ByteBuffer data = ByteBuffer.wrap(file.data());
            flush();
            writeFully(data);
        }

        private void header(String name, int method, long crc, long compressedSize, long size, int time, int date) throws IOException
        {
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            if (count >= MAX_UINT16 || position + buffer.position() >= MAX_UINT32 || compressedSize >= MAX_UINT32 || size >= MAX_UINT32 || nameBytes.length > MAX_UINT16)
                throw new IOException("Content pack is too large for a plain ZIP archive at entry " + name);

            long offset = position + buffer.position();
            ByteBuffer centralHeader = ByteBuffer.allocate(CENTRAL_HEADER_SIZE + nameBytes.length).order(ByteOrder.LITTLE_ENDIAN);
            centralHeader.putInt(CENTRAL_SIGNATURE).putShort((short) ZIP_VERSION);
            putCommonFields(centralHeader, method, crc, compressedSize, size, time, date, nameBytes.length);
            centralHeader.putShort((short) 0).putShort((short) 0).putShort((short) 0).putInt(0).putInt((int) offset).put(nameBytes);
            central.write(centralHeader.array());
            count++;

            reserve(LOCAL_HEADER_SIZE + nameBytes.length);
            buffer.putInt(LOCAL_SIGNATURE);
            putCommonFields(buffer, method, crc, compressedSize, size, time, date, nameBytes.length);
            buffer.put(nameBytes);
        }

        // The ZIP local and central headers share these fields, so each one has to be passed in.
        @SuppressWarnings("java:S107")
        private static void putCommonFields(ByteBuffer target, int method, long crc, long compressedSize, long size, int time, int date, int nameLength)
        {
            target.putShort((short) ZIP_VERSION).putShort((short) FLAG_UTF8).putShort((short) method).putShort((short) time).putShort((short) date).putInt((int) crc).putInt((int) compressedSize)
                .putInt((int) size).putShort((short) nameLength).putShort((short) 0);
        }

        private void copyData(FileChannel input, long offset, long length) throws IOException
        {
            if (length <= buffer.capacity())
            {
                reserve((int) length);
                int limit = buffer.limit();
                buffer.limit(buffer.position() + (int) length);
                long source = offset;
                while (buffer.hasRemaining())
                {
                    int read = input.read(buffer, source);
                    if (read < 0)
                        throw new EOFException("Unexpected end of archive");
                    source += read;
                }
                buffer.limit(limit);
                return;
            }

            flush();
            long transferred = 0;
            while (transferred < length)
            {
                long moved = input.transferTo(offset + transferred, length - transferred, output);
                if (moved <= 0)
                    throw new EOFException("Unexpected end of archive");
                transferred += moved;
            }
            position += length;
        }

        private void reserve(int bytes) throws IOException
        {
            if (buffer.remaining() < bytes)
                flush();
        }

        private void flush() throws IOException
        {
            buffer.flip();
            writeFully(buffer);
            buffer.clear();
        }

        private void writeFully(ByteBuffer data) throws IOException
        {
            while (data.hasRemaining())
                position += output.write(data);
        }

        private void finish() throws IOException
        {
            flush();
            long centralOffset = position;
            byte[] centralBytes = central.toByteArray();
            writeFully(ByteBuffer.wrap(centralBytes));
            if (centralOffset >= MAX_UINT32)
                throw new IOException("Content pack is too large for a plain ZIP archive");

            ByteBuffer end = ByteBuffer.allocate(END_RECORD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            end.putInt(END_SIGNATURE).putShort((short) 0).putShort((short) 0).putShort((short) count).putShort((short) count).putInt(centralBytes.length).putInt((int) centralOffset).putShort((short) 0);
            end.flip();
            writeFully(end);
        }

        @Override
        public void close() throws IOException
        {
            output.close();
        }
    }
}
