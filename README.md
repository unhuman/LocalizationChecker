# Localization Checker

This tool will check a specified localization file against other localizations in the same directory.  This will look for inconsistencies between all localizations.  Inconsistencies will be reported in the program output.  
You will see messages like:  
`Localization Inconsistency: fr-FR.json missing key: 'localization-key-001'`  
`Localization Inconsistency: fr-FR.json extra key: 'localization-key-002'`

This tool will also check a provided source code tree to look for usages of the localization keys in the code.  It does this by checking for usages surrounded by quotes (`'`, `"`, and `` ` ``).    
You will see messages like:  
`Deprecated key: 'localization-key-001'`

## Execution
`groovy /path/to/LocalizzationChecker.groovy parameters...`

### Parameters
1. `-l`, `localization` - this is the primary localization file.  Must be `.json`.
1. `-p`, `path` - this is the path to project location that will be scanned for localizations.
1. `-e`, `extensions` - (optional) this is the file types to be scanned in the project path.  Comma separated or multiple instances permitted.

### Example Execution
`groovy src/LocalizationChecker.groovy -p /path/to/project/packages/ -l /path/to/project/packages/src/locales/en-US.json -e .js,.ts,.tsx`

## Notes
1. alternate localization files must be in the same directory as the primary localization file.
1. `.json` files are always excluded while processing the project scan, even if this is explicitly specified in the project scan.

## Developer Setup
### IntelliJ must add Ivy
1. Project Structure / Project Settings / Libraries
1. Add (From Maven) apache-ivy