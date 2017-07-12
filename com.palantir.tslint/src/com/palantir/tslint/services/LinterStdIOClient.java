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

package com.palantir.tslint.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.google.common.base.Charsets;

/**
 * @author andreas
 */
public class LinterStdIOClient extends JsonBasedLinterClient {

    private BufferedReader nodeStdout;
    private BufferedReader nodeErrout;
    private PrintWriter nodeStdin;

    public LinterStdIOClient(Process nodeProcess) {
        super(new BufferedReader(new InputStreamReader(nodeProcess.getInputStream(), Charsets.UTF_8)),
            new PrintWriter(new OutputStreamWriter(nodeProcess.getOutputStream(), Charsets.UTF_8), true));
        this.nodeErrout = new BufferedReader(new InputStreamReader(nodeProcess.getErrorStream(), Charsets.UTF_8));
    }

    @Override
    public void dispose() {
        this.nodeStdin.close();

        try {
            this.nodeStdout.close();
            this.nodeErrout.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
