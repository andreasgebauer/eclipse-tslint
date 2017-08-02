import * as fs from 'fs';
import * as path from 'path';
import * as ts from 'typescript';
import { Linter, LintResult, ILinterOptions, Configuration } from 'tslint';
import { IOptions, sync } from "glob";

import { Request } from './linterEndpoint';
import { Logger } from './Logger';

const options = {
  fix: false,
  formatter: "json"
};

interface lint {
  files: any,
  project: string
}

interface AngularCli {
  lint: lint[]
}

export class LinterManager {
  private projectDirectory: string = null;

  private reuseProgram = true;
  private reuseCreatingNewProgram = true;

  private pathToLinter = [];
  private pathToProgram = [];
  private lintConfigToProgram = {};
  private lintConfigToLinter = {};

  private logger = new Logger(LinterManager);

  processRequest(requestJson: string) {
    this.logger.log("processing", requestJson);

    var request: Request = JSON.parse(requestJson);
    // invoke the endpoint method with the supplied arguments
    var method = this[ request.method ];
    var result = method.apply(this, request.arguments);

    // convert undefined to null (its basically the Java equivalent of void)
    if (result === undefined) {
      result = null;
    }

    // convert the result to JSON and write it to stdout
    return JSON.stringify(result);
  }

  lint(path: string, projectDir: string) {
    this.logger.log('getting linter');
    var linter = this.getLinter(path, projectDir);
    if (!linter) {
      return;
    }

    this.logger.log('reading file');
    var contents = fs.readFileSync(path, "utf8");

    this.logger.log('finding config');
    var loadResult = Configuration.findConfiguration(null, path);

    this.logger.log('linting');
    var result = this.doLint(linter, path, contents, loadResult.results);
    this.logger.log('linted');

    var ruleFailures = [];
    for (let failure of result.failures) {
      ruleFailures.push(failure.toJson());
    }
    result.failures = ruleFailures;

    return result;
  }

  doLint(
    linter: Linter,
    path: string,
    contents: string,
    config: Configuration.IConfigurationFile): any {

    linter.lint(path, contents, config);
    return linter.getResult();
  }

  private getLinter(inputFile: string, projectDir: string): Linter {

    this.logger.log('getting tsConfig');

    const tsConfigFilePath = this.getTsConfigFilePath(inputFile, projectDir);

    if (this.reuseProgram) {

      if (!tsConfigFilePath) {
        return undefined;
      }

      let program;
      if (this.lintConfigToProgram[ tsConfigFilePath ] === undefined) {
        this.logger.log('creating fresh program');
        program = Linter.createProgram(tsConfigFilePath, projectDir);
      } else {
        this.logger.log('reusing program');
        const oldProgram = this.lintConfigToProgram[ tsConfigFilePath ];
        if (this.reuseCreatingNewProgram) {
          program = this.createProgram(inputFile, tsConfigFilePath, projectDir, oldProgram);
        } else {
          program = oldProgram;
        }
      }

      this.lintConfigToProgram[ tsConfigFilePath ] = program;

      this.logger.log('creating linter');
      return new Linter(options, program);

    } else {
      this.logger.log('creating fresh program');
      var program = Linter.createProgram(tsConfigFilePath, projectDir);
      this.logger.log('creating linter');
      return new Linter(options, program);
    }
  }

  private getTsConfigFilePath(inputFile: string, projectDir: string): string {
    const lintSettings = this.getLintSettings(inputFile, projectDir);

    if (lintSettings) {
      return lintSettings.project;
    }

    return undefined;
  }

  private getLintSettings(inputFile: string, projectDir: string): lint {
    for (let lintSettings of this.readAngularCliJson(inputFile, projectDir).lint) {
      const files: string[] = sync(lintSettings.files);
      for (let file of files) {
        if (inputFile == file) {
          return lintSettings;
        }
      }
    }
  }

  private readAngularCliJson(inputFile: string, projectDir: string): AngularCli {
    const angularConfigFileName = '.angular-cli.json';
    const angularCliJsonPath = path.join(projectDir, angularConfigFileName);

    const angularCli: AngularCli = JSON.parse(fs.readFileSync(angularCliJsonPath, 'utf8'));

    angularCli.lint.forEach((value, index) => {
      angularCli.lint[ index ].project = path.join(projectDir, value.project);
      angularCli.lint[ index ].files = path.join(projectDir, value.files);
    });

    return angularCli;
  }

  private createProgram(inputFile, configFile, projectDirectory, oldProgram): ts.Program {
    if (projectDirectory === undefined) {
      projectDirectory = path.dirname(configFile);
    }
    this.logger.log('reading config file');
    var config = ts.readConfigFile(configFile, ts.sys.readFile).config;
    var parseConfigHost = {
      fileExists: fs.existsSync,
      readDirectory: ts.sys.readDirectory,
      readFile: (file: string) => fs.readFileSync(file, "utf8"),
      useCaseSensitiveFileNames: true,
    };
    this.logger.log('parsing json config file');
    var parsed = ts.parseJsonConfigFileContent(config, parseConfigHost, projectDirectory);
    this.logger.log('creating compiler host');
    var host = ts.createCompilerHost(parsed.options, true);
    this.logger.log('creating program with old program');

    let fileNames: string[] = [];
    fileNames.push(inputFile);
    //    fileNames = parsed.fileNames;

    return ts.createProgram(fileNames, parsed.options, host, oldProgram);
  };

}