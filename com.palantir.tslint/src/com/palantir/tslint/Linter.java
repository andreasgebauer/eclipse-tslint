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

package com.palantir.tslint;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.texteditor.MarkerUtilities;

import com.google.common.collect.Maps;
import com.palantir.tslint.failure.LintResult;
import com.palantir.tslint.failure.RuleFailure;
import com.palantir.tslint.failure.RuleFailurePosition;
import com.palantir.tslint.failure.RuleSeverity;
import com.palantir.tslint.services.Bridge;
import com.palantir.tslint.services.NodeFuckedUpException;
import com.palantir.tslint.services.Request;

final class Linter {

    public static final String MARKER_TYPE = "com.palantir.tslint.tslintProblem";

    private Bridge bridge;

    public Linter() {
        this.bridge = null;
    }

    public void lint(IResource resource, String configurationPath) {
        String resourceName = resource.getName();
        if (resource instanceof IFile &&
                (resourceName.endsWith(".ts") || resourceName.endsWith(".tsx")) &&
                !resourceName.endsWith(".d.ts")) {
            IFile file = (IFile) resource;
            String resourcePath = resource.getRawLocation().toOSString();

            Logger.getLogger("Linter").info("Analyzing " + resourcePath);

            // remove any pre-existing markers for the given file
            deleteMarkers(file);

            int tries = 0;
            boolean error = false;
            do {
                try {
                    // get a bridge
                    if (this.bridge == null) {
                        IPath projectLocation = resource.getProject().getRawLocation();
                        String projectLocationPath = projectLocation.toOSString();
                        File projectFile = new File(projectLocationPath);
                        Request projectDirectoryRequest = new Request("setProjectDirectory", projectFile);

                        this.bridge = new Bridge();
                        this.bridge.call(projectDirectoryRequest, Void.class);
                    }

                    Request request = new Request("lint", resourcePath);
                    LintResult response = this.bridge.call(request, LintResult.class);
                    if (response != null) {
                        Logger.getLogger("Linter").info(resourceName + " failures: " + Arrays.asList(response.getFailures()));
                        for (RuleFailure ruleFailure : response.getFailures()) {
                            addMarker(ruleFailure);
                        }
                    }
                    error = false;
                } catch (NodeFuckedUpException e) {
                    this.bridge.dispose();
                    error = true;
                }
            } while (error && tries++ <= 3);

        }
    }

    private void addMarker(RuleFailure ruleViolation) {
        try {
            Path path = new Path(ruleViolation.getName());
            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);

            RuleFailurePosition startPosition = ruleViolation.getStartPosition();
            RuleFailurePosition endPosition = ruleViolation.getEndPosition();

            RuleSeverity severity = ruleViolation.getRuleSeverity();

            int markerSeverity = IMarker.SEVERITY_WARNING;
            switch (severity) {
                case ERROR:
                    markerSeverity = IMarker.SEVERITY_ERROR;
            }

            Map<String, Object> attributes = Maps.newHashMap();
            attributes.put(IMarker.LINE_NUMBER, startPosition.getLine() + 1);
            attributes.put(IMarker.CHAR_START, startPosition.getPosition());
            attributes.put(IMarker.CHAR_END, endPosition.getPosition());
            attributes.put(IMarker.MESSAGE, ruleViolation.getFailure());
            attributes.put(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
            attributes.put(IMarker.SEVERITY, markerSeverity);

            MarkerUtilities.createMarker(file, attributes, MARKER_TYPE);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteMarkers(IFile file) {
        try {
            file.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

}
