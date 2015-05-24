import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteBuffer

/**
 * Created by objectx on 2015/05/24.
 */
class SourceTextValidatorTest extends Specification {
    @Unroll ("helper (#a) == #b ?")
    def "Check: SouceTextValidator.valudate" () {
    given:
        def validator = new SourceTextValidator ()
        def helper = { final String s ->
            def b = s.getBytes 'UTF-8'
            def bb = ByteBuffer.wrap (b).slice ()
            bb.position (b.size ())
            validator.validateToPosition (bb.asReadOnlyBuffer ())
        }
    expect:
        helper (a) == b
    where:
        a || b
        "TEST" | true
        "TEST\n" | true
        "TEST\rTEST\n" | true
        "TEST\r\n" | false
        " TEST" | true
        " \tTEST" | false
        "\tTEST" | false
    }
}
