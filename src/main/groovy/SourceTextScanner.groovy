import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Created by objectx on 2015/05/24.
 */
@Slf4j
@Immutable
@CompileStatic
class SourceTextScanner {
    boolean dryrun = false
    boolean expandAllTabs = false
    int tabStop = 8

    final boolean validate (Path path) {
        false
    }

    final boolean validate (final ByteBuffer input) {
        ByteBuffer buf = input.asReadOnlyBuffer ()
        int pos = buf.position ()
        while (buf.position () < buf.limit ()) {
            int ch = buf.get ()
            if (ch == 0x0A) {
                // Line formed
                if (! validateToPosition (buf, pos)) {
                    return false
                }
                pos = buf.position ()
            }
        }
        if (pos < buf.limit ()) {
            return validateToPosition (buf, pos)
        }
        true
    }

    /**
     * Checks INPUT [start..start + position () - 1]
     *
     * @param input
     * @return true if valid, otherwise return false
     */
    final boolean validateToPosition (final ByteBuffer input, int start) {
        int size = input.position () - start
        if (input.get (start + 0) == 0x09) {
            return false
        }
        if (input.get (start + 0) == 0x20) {
            // Checks \t between input [0..<1st non space byte>]
            for (int i in 1..<size) {
                int ch = input.get (start + i)
                if (ch == 0x09) {
                    return false
                }
                if (ch != 0x20) {
                    break
                }
            }
        }
        if (2 <= size && input.get (start + size - 2) == (byte)0x0D && input.get (start + size - 1) == (byte)0x0A) {
            // \r\n detected
            return false
        }
        true
    }

    /**
     * Normalize supplied PATH file
     *
     * @param path File to normalize
     * @return true when normalization occur
     */
    final boolean normalize (final Path path) {
        UUID uuid = UUID.randomUUID ()
        Path tmp

        if (path.parent) {
            tmp = path.parent.resolve "tidy-${uuid}.tmp"
        }
        else {
            tmp = Paths.get "tidy-${uuid}.tmp"
        }
        final byte [] contents = Files.readAllBytes path

        def normalizer = { ByteArrayOutputStream out, final byte [] bytes ->
            if (expandAllTabs) {
                normalizeAll (out, bytes)
            }
            else {
                normalizeFirstIndent (out, bytes)
            }
        }
        new ByteArrayOutputStream ().withStream { ByteArrayOutputStream output ->
            if (normalizer (output, contents)) {
                log.info "{} ({} -> {} bytes)", path.toString (), contents.size (), output.size ()
                if (! dryrun) {
                    log.debug "Write to: {}", tmp.toString ()
                    tmp.withOutputStream { OutputStream o ->
                        output.writeTo o
                    }
                    Files.move tmp, path, StandardCopyOption.REPLACE_EXISTING
                }
            }
        }
    }

    /**
     * Normalize INPUT
     *
     * Expands all TABs in INPUT
     * @param output
     * @param input
     * @return true when normalization occur
     */
    boolean normalizeAll (OutputStream output, final byte[] input) {
        int end = input.size ()
        int pos = 0
        int col = 0
        int cntConversion = 0

        while (pos < end) {
            int ch = input [pos]
            ++pos

            if (ch == 0x09) {
                int next = nextTabStop col, tabStop
                while (col < next) {
                    output.write 0x20
                    ++col
                }
                ++cntConversion
            }
            else if (ch == 0x0D) {
                if (pos < end) {
                    int ch2 = input [pos]
                    ++pos
                    if (ch2 == 0x0A) {
                        output.write 0x0A
                        ++cntConversion
                        col = 0
                    }
                    else {
                        output.write ch
                        output.write ch2
                        col += 2
                    }
                }
                else {
                    // Already reached to the end...
                    output.write ch
                    ++col
                }
            }
            else if (ch == 0x0A) {
                output.write 0x0A
                col = 0
            }
            else {
                output.write ch
                ++col
            }
        }
        cntConversion != 0
    }

    /**
     * Normalize INPUT
     *
     * Only expand TABs in 1st level of indent
     * @param output
     * @param input
     * @return true when normalization occur
     */
    final boolean normalizeFirstIndent (final OutputStream output, final byte [] input) {
        int start = 0
        int cntConversion = 0
        input.eachWithIndex{ byte ch, int i ->
            if (ch == (byte)0x0A) {
                if (normalizeLine (output, input, start, i + 1)) {
                    ++cntConversion
                }
                start = i + 1
            }
        }
        if (start < input.size ()) {
            if (normalizeLine (output, input, start, input.size ())) {
                ++cntConversion
            }
        }
        cntConversion != 0
    }

    /**
     * Normalize INPUT [START..<END]
     *
     * @param output
     * @param input
     * @param start
     * @param end
     * @return true when normalization occur
     */
    final boolean normalizeLine (final OutputStream output, final byte [] input, final int start, final int end) {
        int cntConversion = 0
        int pos = 0
        int col = 0

        while (pos < (end - start)) {
            int ch = input [start + pos]
            ++pos

            if (ch == 0x09) {
                int next = nextTabStop (col, tabStop)
                while (col < next) {
                    output.write 0x20
                    ++col
                }
                ++cntConversion
            }
            else if (ch == 0x20) {
                output.write 0x20
                ++col
            }
            else if (ch == 0x0D) {
                --pos
                break
            }
            else {
                output.write ch
                ++col
                break
            }
        }
        while (pos < (end - start)) {
            int ch = input [start + pos]
            ++pos
            if (ch == 0x0D && (start + pos) < end) {
                int ch2 = input [start + pos]
                ++pos
                if (ch2 == 0x0A) {
                    output.write 0x0A
                    ++cntConversion
                }
                else {
                    output.write ch
                    output.write ch2
                }
            }
            else {
                output.write ch
            }
        }
        return cntConversion != 0
    }

    /**
     * Compute next tab-stop position
     * @param col
     * @param tabWidth
     * @return Next tab stop column
     */
    static final int nextTabStop (int col, int tabWidth) {
        Math.floorDiv (col + tabWidth, tabWidth) * tabWidth
    }
}
