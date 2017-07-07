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

package com.palantir.tslint.failure;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andreas Gebauer
 */
public class LintResult {
    private int errorCount;
    private RuleFailure[] failures;
    private RuleFailure[] fixes;
    private String format;
    private String output;
    private int warningCount;

    public LintResult(@JsonProperty("errorCount") int errorCount,
            @JsonProperty("failures") RuleFailure[] failures,
            @JsonProperty("fixes") RuleFailure[] fixes,
            @JsonProperty("format") String format,
            @JsonProperty("output") String output,
            @JsonProperty("warningCount") int warningCount) {
        this.errorCount = errorCount;
        this.failures = failures;
        this.fixes = fixes;
        this.format = format;
        this.output = output;
        this.warningCount = warningCount;

    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public RuleFailure[] getFailures() {
        return this.failures;
    }

    public RuleFailure[] getFixes() {
        return this.fixes;
    }

    public String getFormat() {
        return this.format;
    }

    public String getOutput() {
        return this.output;
    }

    public int getWarningCount() {
        return this.warningCount;
    }

}
