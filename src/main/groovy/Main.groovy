import ch.qos.logback.classic.Level
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by objectx on 2015/05/23.
 */

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
