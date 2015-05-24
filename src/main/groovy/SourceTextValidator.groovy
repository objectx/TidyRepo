import groovy.transform.Immutable
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer
import java.nio.file.Path

/**
 * Created by objectx on 2015/05/24.
 */
@Slf4j
@Immutable
class SourceTextValidator {
    final boolean validate (Path path) {
        false
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
        if (2 <= size && input.get (start + size - 2) == 0x0D && input.get (start + size - 1) == 0x0A) {
            // \r\n detected
            return false
        }
        true
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
}

