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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.palantir.tslint.TSLintPlugin;

/**
 * This handles all requests for TSLint.
 *
 * @author aramaswamy
 */
public final class Bridge {

    private static final String OS_NAME = System.getProperty("os.name");
    private static final Splitter PATH_SPLITTER = Splitter.on(File.pathSeparatorChar);

    private Process nodeProcess;

    private LinterClient client;

    public Bridge() {
        // start the node process
        this.start();
    }

    public void dispose() {
        this.nodeProcess.destroy();

        this.nodeProcess = null;
    }

    private void start() {
        File nodeFile = Bridge.findNode();
        String nodePath = nodeFile.getAbsolutePath();

        // get the path to the bridge.js file
        File bundleFile;
        try {
            bundleFile = FileLocator.getBundleFile(TSLintPlugin.getDefault().getBundle());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File bridgeFile = new File(bundleFile, "bin/main.js");
        String bridgePath = bridgeFile.getAbsolutePath();

        // construct the arguments
        ImmutableList.Builder<String> argsBuilder = ImmutableList.builder();
        argsBuilder.add(nodePath);
        argsBuilder.add(bridgePath);

        // start the node process and create a reader/writer for its stdin/stdout
        List<String> args = argsBuilder.build();
        ProcessBuilder processBuilder = new ProcessBuilder(args.toArray(new String[args.size()]));
        try {
            this.nodeProcess = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // add a shutdown hook to destroy the node process in case its not properly disposed
        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread());

        int port = 12345;

        this.client = new LinterSocketClient(port);
    }

    private static File findNode() {
        String nodeFileName = getNodeFileName();
        String path = System.getenv("PATH");
        List<String> directories = Lists.newArrayList(PATH_SPLITTER.split(path));

        // ensure /usr/local/bin is included for OS X
        if (OS_NAME.startsWith("Mac OS X")) {
            directories.add("/usr/local/bin");
        }

        // search for Node.js in the PATH directories
        for (String directory : directories) {
            File nodeFile = new File(directory, nodeFileName);

            if (nodeFile.exists()) {
                return nodeFile;
            }
        }

        throw new IllegalStateException("Could not find Node.js.");
    }

    private static String getNodeFileName() {
        if (OS_NAME.startsWith("Windows")) {
            return "node.exe";
        }

        return "node";
    }

    private class ShutdownHookThread extends Thread {
        @Override
        public void run() {
            Process process = Bridge.this.nodeProcess;

            if (process != null) {
                process.destroy();
            }
        }
    }

    public <T> T call(Request request, Class<T> resultType) {
        return this.client.call(request, resultType);
    }
}
