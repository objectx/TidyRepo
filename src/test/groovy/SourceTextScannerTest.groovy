import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteBuffer

/**
 * Created by objectx on 2015/05/24.
 */
class SourceTextScannerTest extends Specification {
    @Unroll ("helper (#a) == #b ?")
    def "Test single line validator" () {
    given:
        def validator = new SourceTextScanner ()
        def helper = { final String s ->
            def b = s.getBytes 'UTF-8'
            def bb = ByteBuffer.wrap (b).slice ()
            bb.position b.size ()
            validator.validateToPosition bb.asReadOnlyBuffer (), 0
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

    @Unroll ('helper (#a) == #b ?')
    def "Test multiline validator" () {
    given:
        def validator = new SourceTextScanner ()
        def helper = { final String s ->
            def b = s.getBytes 'UTF-8'
            validator.validate (ByteBuffer.wrap (b))
        }
    expect:
        helper (a) == b
    where:
        a << ['''abc
def
ghi
'''
        ,     '''abc\r
def\r
ghi\r
'''
        ,     '''abc
\tdef'''
        ]
        b << [true, false, false]
    }

    @Unroll ('helper (#a) == #b')
    def "Test line normalizer" () {
    given:
        def validator = new SourceTextScanner ()
        def helper = { final String s ->
            def b = s.getBytes 'UTF-8'
            ByteArrayOutputStream o = new ByteArrayOutputStream ()
            validator.normalizeLine (o, b, 0, b.size ())
            o.toString ()
        }
    expect:
        helper (a) == b
    where:
        a || b
        '' || ''
        '\r\n'|| '\n'
        '\t'||'        '
        ' \t'||'        '
        '  \tabc\td\r'||'        abc\td\r'
        ' \t \t abc\r\n'||'                 abc\n'
    }

    @Unroll ('helper (#a) == #b')
    def "Test normalizer" () {
    given:
        def validator = new SourceTextScanner ()
        def helper = { final String s ->
            def sb = s.getBytes ('UTF-8')
            validator.normalizeFirstIndent (sb) { o, changed ->
                o.toString ()
            }
        }
    expect:
        helper (a) == b
    where:
        a << [ '''abc\r
def'''
             , '''\tabc\t\r
def
ghi\r'''
        ]
        b << [ '''abc
def'''
             , '''        abc\t
def
ghi\r'''
        ]
    }
    @Unroll ('helper (#a) == #b')
    def "Test normalizeAll" () {
    given:
        def validator = new SourceTextScanner ()
        def helper = { final String s ->
            def sb = s.getBytes ('UTF-8')
            validator.normalizeAll (sb) { o, changed ->
                o.toString ()
            }
        }
    expect:
        helper (a) == b
    where:
        a << [ '''abc\r
def'''
               , '''\tabc\t?\r
def
ghi\r'''
        ]
        b << [ '''abc
def'''
               , '''        abc     ?
def
ghi\r'''
        ]
    }
}
