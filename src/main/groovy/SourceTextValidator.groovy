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
     * Checks INPUT [0..position () - 1]
     * @param input
     * @return true if valid, otherwise return false
     */
    final validateToPosition (final ByteBuffer input) {
        int size = input.position ()
        if (input.get (0) == 0x09) {
            return false
        }
        if (input.get (0) == 0x20) {
            // Checks \t between input [0..<1st non space byte>]
            for (int i in 1..<size) {
                int ch = input.get (i)
                if (ch == 0x09) {
                    return false
                }
                if (ch != 0x20) {
                    break
                }
            }
        }
        if (2 <= size && input.get (size - 2) == 0x0D && input.get (size - 1) == 0x0A) {
            // \r\n detected
            return false
        }
        true
    }
}

