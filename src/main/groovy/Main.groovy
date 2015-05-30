import ch.qos.logback.classic.Level
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

import static java.nio.file.Files.exists

@Slf4j
final class Main {
    static final Logger rootLogger = LoggerFactory.getLogger Logger.ROOT_LOGGER_NAME
    static int cntThread = 0
    static Pattern rxSource = Pattern.compile (/.+\.(?:h|hh|hpp|hxx|h\+\+|c|cc|cpp|cxx|c\+\+|py|pl|java|groovy)$/)

    static void main (String [] args) {
        CliBuilder cli = build_option_parser ()

        def options = cli.parse args

        if (! options) {
            System.exit 1
        }

        if (options.'help') {
            cli.usage ()
            System.exit 1
        }

        if (options.'verbose') {
            rootLogger.level = Level.INFO
        }

        if (options.'parallel') {
            try {
                cntThread = Integer.parseInt (options.'parallel')
            }
            catch (NumberFormatException e) {
                log.error "Bad # of threads specified: \"{}\"", options.'parallel'
                System.exit 1
            }
        }

        try {
            def repos = options.arguments ()
            def scanner = new SourceTextScanner (dryrun: options.'dry-run', expandAllTabs: options.'expand-all-tabs')

            if (! repos) {
                repos = ['.']
            }
            for (String r in repos) {
                rootLogger.info "Processing repository: {}", r
                eachRepositoryFiles (Paths.get (r), Main.&isTarget) {
                    scanner.normalize it
                }
            }
        }
        catch (Exception e) {
            rootLogger.error "Something wrong happen! ({})", e.message
            System.exit 1
        }

        System.exit 0
    }

    static final CliBuilder build_option_parser () {
        def scriptname = (new File (Main.protectionDomain.codeSource.location.file)).name
        def cli = new CliBuilder (usage: "${scriptname} [options] [<repository>...]", stopAtNonOption: false)
        cli.with {
            H (longOpt: 'help', "Show this.")
            v (longOpt: 'verbose', "Be verbose.")
            N (longOpt: 'dry-run', "Don't modify anything.")
            A (longOpt: 'expand-all-tabs', "Expand all tabs.")
            _ (longOpt: 'parallel', "Do parallel conversion", optionalArg: true, args: 1, argName: 'threads')
        }
        cli
    }

    static final void eachRepositoryFiles (Path repo,
                                           @ClosureParams (value=SimpleType, options=['java.nio.file.Path']) Closure<Boolean> predicate,
                                           @ClosureParams (value=SimpleType, options=['java.nio.file.Path']) Closure closure) {
        Process files

        if (exists (repo.resolve ('.hg'))) {
            // log.info ".hg/ found"
            files = ["hg", "files"].execute ([], repo.toFile ())
        }
        else if (exists (repo.resolve ('.git'))) {
            // log.info ".git/ found"
            files = ["git", "ls-files"].execute ([], repo.toFile ())
        }
        else {
            throw new IOException ("${repo} is neither git nor mercurial repository.")
        }

        if (1 < cntThread) {
            rootLogger.info "Processing with {} threads", this.cntThread
            String [] lines = files.in.readLines ().toArray ()
            groovyx.gpars.GParsPool.withPool (this.cntThread) {
                lines.eachParallel { String l ->
                    Path target = repo.resolve l
                    predicate.delegate = target
                    if (predicate (target)) {
                        log.debug "Process: {}", target.toString ()
                        closure.delegate = target
                        closure target
                    }
                }
            }
        }
        else {
            files.in.eachLine { String l ->
                Path target = repo.resolve l
                predicate.delegate = target
                if (predicate (target)) {
                    log.debug "Process: {}", target.toString ()
                    closure.delegate = target
                    closure target
                }
            }
        }
    }

    @CompileStatic
    static final boolean isTarget (Path path) {
        rxSource.matcher (path.fileName.toString ())?.matches ()
    }
}
