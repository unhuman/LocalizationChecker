import groovy.cli.commons.CliBuilder
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.commons.cli.Option

@Grapes([
        @Grab(group='commons-cli', module='commons-cli', version='1.5.0')
])

static void main(String[] args) {
  def cli = new CliBuilder(usage: 'localizationChecker [options]', header: 'Options:');
  cli.width = 120
  cli.h(longOpt: 'help', 'Shows useful information')
  cli.l(longOpt: 'localization', required: true, args: 1, argName: 'localizationFile', 'Main localization file (must be .json)')
  cli.p(longOpt: 'path', required: true, args: 1, argName: 'path', 'Path to search for localization entities')
  cli.e(longOpt: 'extensions', 'required': false, args: Option.UNLIMITED_VALUES, argName: 'ext,...', valueSeparator: ',', '(Optional) Scan file extensions (comma separated) to process (json files always excluded)')

  def options = cli.parse(this.args)
  if (!options) {
    return
  }

  if (options.h) {
    cli.usage()
    return
  }

  // validates localizationFile and returns it as a file
  def validateLocalizationFile = { localizationFile ->
    if (!localizationFile.matches(/.+\.json$/)) {
      throw new IllegalArgumentException("Invalid localizationFile name: $localizationFile")
    }
    def file = new File(localizationFile)
    if (!file.exists()) {
      throw new IllegalArgumentException("File DNE: localizationFile: $localizationFile")
    }
    return file
  }

  // validates a path and returns it as a file
  def validatePath = { path ->
    File check = new File(path)
    if (!check.exists()) {
      throw new IllegalArgumentException("Path DNE: $path")
    }
    if (!check.isDirectory()) {
      throw new IllegalArgumentException("Path is not a directory: $path")
    }
    return check
  }

  // Read in localizations
  File localizationFile = validateLocalizationFile(options.l)
  File path = validatePath(options.p)
  List<String> extensions = (options.es)
          ? options.es.collect { extension -> extension.startsWith('.')
                  ? extension.toLowerCase()
                  : String.format(".%s", extension.toLowerCase()) // ".${extension.toLowerCase()}" doesn't work
          }
          : null

  System.out.println("EXTENSIONS: ${extensions}")

  def jsonSlurper = new JsonSlurper()
  Set<String> localizationKeys = jsonSlurper.parse(localizationFile).keySet()

  // Match all files in the localization directory to check for inconsistent keys
  File localizationDirectory = localizationFile.getParentFile()
  localizationDirectory.listFiles().each {altFile ->
    if (!altFile.isFile() || !altFile.name.toLowerCase().endsWith(".json")) {
      return
    }
    Set<String> altLocalizationKeys = jsonSlurper.parse(altFile).keySet()

    // Report key inconsistencies
    Set<String> missingLocalizationKeys = localizationKeys.minus(altLocalizationKeys)
    missingLocalizationKeys.each {localizationKey -> System.out.println("Localization Inconsistency: ${altFile.name} missing key: '${localizationKey}'")}
    Set<String> extraLocalizationKeys = altLocalizationKeys.minus(localizationKeys)
    extraLocalizationKeys.each {localizationKey -> System.out.println("Localization Inconsistency: ${altFile.name} extra key: '${localizationKey}'")}
  }

  // Now find all the files in the directory that may be using the localization keys to find misses
  // (must quote the string - '', ``, and ""
  path.eachFileRecurse(FileType.FILES) {file ->
    String scanFilename = file.name.toLowerCase()
    String extension = scanFilename.lastIndexOf('.') > 0 ? scanFilename.substring(scanFilename.lastIndexOf('.')) : null
    // Don't process files w/o extension, .json files, or, if specified, non-matching extensions
//    System.out.println("Extensions value: '${extensions.get(0)}', Extension Value: '${extension}'")
    if (extension == null || extension == ".json" || (extensions != null && !extensions.contains(extension))) {
      return
    }
    String contents = file.text

    // Copy the set of keys, so we can remove things we find
    Set<String> iterateKeys = new HashSet<>(localizationKeys)
    // Check for usages of these strings
    iterateKeys.each {key -> {
      for (char quote in ['\'', '"', '`']) {
        String check = "${quote}${key}${quote}"
        if (contents.contains(check)) {
          // System.out.println("${key} found in ${file.name}")
          localizationKeys.remove(key)
          break
        }
      }
    }}
  }

  // Output the keys that have not been found
  localizationKeys.each{ key -> System.out.println("Deprecated key: '${key}'")}
}