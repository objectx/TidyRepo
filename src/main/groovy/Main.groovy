import ch.qos.logback.classic.Level
import groovy.transform.CompileStatic
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths

import static java.nio.file.Files.exists


def build_option_parser () {
    def scriptname = (new File (getClass().protectionDomain.codeSource.location.file)).name
    def cli = new CliBuilder (usage: "${scriptname} [options] [<repository>...]", stopAtNonOption: false)
    cli.with {
        H (longOpt: 'help', "Show this.")
        v (longOpt: 'verbose', "Be verbose.")
        N (longOpt: 'dry-run', "Don't modify anything.")
    }
    cli
}

def cli = build_option_parser ()

def options = cli.parse args

if (! options) {
    System.exit 1
}

if (options.'help') {
    cli.usage ()
    System.exit 1
}

@Field Logger rootLogger = LoggerFactory.getLogger Logger.ROOT_LOGGER_NAME

if (options.'verbose') {
    rootLogger.level = Level.INFO
}

@CompileStatic
def eachRepositoryFiles (Path repo, Closure<Path> closure) {
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

    files.in.eachLine { String l ->
        Path target = repo.resolve l
        closure.delegate = target
        closure target
    }
}

@CompileStatic
def eachRepositoryFiles (String path, Closure<Path> closure) {
    eachRepositoryFiles (Paths.get (path), closure)
}

eachRepositoryFiles ('.') { Path p ->
    println p.fileName
}

System.exit 0
