/*
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Linter, LintResult, ILinterOptions, Configuration } from 'tslint';
import { sync, GlobSync, IOptions } from 'glob';
import * as fs from 'fs';
import * as path from 'path';
import * as ts from 'typescript';

const options = {
  fix: false,
  formatter: "json"
};

export class LinterEndpoint {
  private projectDirectory: string;

  private reuseProgram = true;

  private pathToLinter = [];
  private pathToProgram = [];
  private lintConfigToProgram = {};
  private lintConfigToLinter = {};

  public setProjectDirectory(projectDirectory: string) {
    this.projectDirectory = projectDirectory;

    var angularCliJsonPath = projectDirectory + "/.angular-cli.json";
    var angularCliJson = JSON.parse(fs.readFileSync(angularCliJsonPath, "utf8"));

    for (let lintDir of angularCliJson.lint) {
      var configFile = projectDirectory + "/" + lintDir.project;
      var program = Linter.createProgram(configFile, projectDirectory);
      var linter = new Linter(options, program);
      this.pathToLinter.push({
        files: lintDir.files,
        linter: linter
      });
      this.pathToProgram.push({
        files: lintDir.files,
        program: program
      });
    }
  }

  public lint(path: string) {

    var linter = this.getLinter(path);

    if (!linter) {
      return;
    }

    var contents = fs.readFileSync(path, "utf8");

    var loadResult = Configuration.findConfiguration(null, path);

    linter.lint(path, contents, loadResult.results);

    var result = linter.getResult();

    var ruleFailures = [];
    for (let failure of result.failures) {
      ruleFailures.push(failure.toJson());
    }
    result.failures = ruleFailures;

    return result;
  }

  private getLinter(inputFile: string): Linter {
    if (this.reuseProgram) {
      const tsConfigFilePath = this.getTsConfigFilePath(inputFile);

      if(!tsConfigFilePath) {
        return undefined;
      }
      
      let program;
      if (this.lintConfigToProgram[tsConfigFilePath] === undefined) {
        program = Linter.createProgram(tsConfigFilePath, this.projectDirectory);
      } else {
        const oldProgram = this.lintConfigToProgram[tsConfigFilePath];
        program = this.createProgram(tsConfigFilePath, this.projectDirectory, oldProgram);
      }

      this.lintConfigToProgram[tsConfigFilePath] = program;
      return new Linter(options, program);

    } else {
      var program = Linter.createProgram(this.getTsConfigFilePath(inputFile), this.projectDirectory);
      return new Linter(options, program);
    }
  }

  private getTsConfigFilePath(inputFile: string): string {
    const lintSettings = this.getLintSettings(inputFile);

    if (lintSettings) {
      return this.projectDirectory + '/' + lintSettings.project;
    }
    
    return undefined;
  }

  private getLintSettings(inputFile: string) {
    var globOptions: IOptions = {};
    globOptions.cwd = this.projectDirectory;

    for (let lintSettings of this.readAngularCliJson().lint) {
      const files: string[] = sync(lintSettings.files, globOptions);
      for (let file of files) {
        var match = this.projectDirectory + '/' + file;
        if (inputFile == match) {
          return lintSettings;
        }
      }
    }
  }

  private readAngularCliJson() {
    const angularCliJsonPath = this.projectDirectory + '/.angular-cli.json';
    return JSON.parse(fs.readFileSync(angularCliJsonPath, 'utf8'));
  }

  private createProgram = function(configFile, projectDirectory, oldProgram) {
    if (projectDirectory === undefined) {
      projectDirectory = path.dirname(configFile);
    }
    var config = ts.readConfigFile(configFile, ts.sys.readFile).config;
    var parseConfigHost = {
      fileExists: fs.existsSync,
      readDirectory: ts.sys.readDirectory,
      readFile: function(file) { return fs.readFileSync(file, "utf8"); },
      useCaseSensitiveFileNames: true,
    };
    var parsed = ts.parseJsonConfigFileContent(config, parseConfigHost, projectDirectory);
    var host = ts.createCompilerHost(parsed.options, true);
    var program = ts.createProgram(parsed.fileNames, parsed.options, host, oldProgram);
    return program;
  };
}
