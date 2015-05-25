import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer
import java.nio.file.Path

/**
 * Created by objectx on 2015/05/24.
 */
@Slf4j
@Immutable
@CompileStatic
class SourceTextValidator {
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

    final boolean normalize (final Path path) {
        false
    }

    final boolean normalize (final OutputStream output, final byte [] input) {
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
        cntConversion == 0
    }

    final boolean normalizeLine (final OutputStream output, final byte [] input, final int start, final int end) {
        int cntConversion = 0
        int pos = 0
        int col = 0

        while (pos < (end - start)) {
            int ch = input [start + pos]
            ++pos

            if (ch == 0x09) {
                int next = nextTabStop (col, 8)
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
        return (cntConversion == 0)
    }

    final int nextTabStop (int col, int tabWidth) {
        Math.floorDiv (col + tabWidth, tabWidth) * tabWidth
    }
}

