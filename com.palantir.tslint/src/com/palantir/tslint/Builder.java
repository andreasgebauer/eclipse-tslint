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
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

public final class Builder extends IncrementalProjectBuilder {

    private static final ILog LOGGER = TSLintPlugin.getDefault().getLog();

    public static final String BUILDER_ID = "com.palantir.tslint.tslintBuilder";

    private Linter linter;

    public Builder() {
        super();
        this.linter = new Linter();
    }

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        if (kind == FULL_BUILD) {
            fullBuild(monitor);
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                fullBuild(monitor);
            } else {
                incrementalBuild(delta, monitor);
            }
        }

        return null;
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        getProject().deleteMarkers(Linter.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
        monitor.done();
    }

    protected void fullBuild(IProgressMonitor monitor) throws CoreException {
        getProject().accept(new ResourceVisitor(monitor));
    }

    protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
        delta.accept(new DeltaVisitor(monitor));
    }

    private void lint(final IResource resource, final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }

        IProject project = this.getProject();
        IScopeContext projectScope = new ProjectScope(project);
        IEclipsePreferences prefs = projectScope.getNode(TSLintPlugin.ID);
        String configurationPath = prefs.get("configPath", null);
        if (configurationPath != null && !configurationPath.equals("")) {
            File configFile = new File(configurationPath);
            // if we're given a relative path get the absolute path for it
            if (!configFile.isAbsolute()) {
                IPath projectLocation = project.getRawLocation();
                String projectLocationPath = projectLocation.toOSString();
                File projectFile = new File(projectLocationPath, configurationPath);
                configurationPath = projectFile.getAbsolutePath();
            }
        } else {
            configurationPath = project.getFile("tslint.json").getRawLocation().toOSString();
        }

        final String config = configurationPath;

        this.linter.lint(resource, config);
    }

    private class ResourceVisitor implements IResourceVisitor {
        private final IProgressMonitor monitor;

        public ResourceVisitor(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public boolean visit(IResource resource) {

            lint(resource, this.monitor);

            return true;
        }
    }

    private class DeltaVisitor implements IResourceDeltaVisitor {
        private final IProgressMonitor monitor;

        public DeltaVisitor(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();

            switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                case IResourceDelta.CHANGED:
                    lint(resource, this.monitor);
                    break;
            }

            return true;
        }
    }

}
