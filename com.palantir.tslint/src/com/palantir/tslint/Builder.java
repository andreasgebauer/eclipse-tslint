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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

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
      final AtomicInteger cnt = new AtomicInteger();
      // count the resources
      getProject().accept(new IResourceVisitor() {
        @Override
        public boolean visit(IResource resource) throws CoreException {
          if (linter.isLintable(resource)) {
            cnt.incrementAndGet();
          }
          return true;
        }
      });
      SubMonitor subMonitor = SubMonitor.convert(monitor, "TSLint", cnt.get());
      fullBuild(subMonitor);
      subMonitor.done();
    } else {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
        fullBuild(subMonitor);
        subMonitor.done();
      } else {
        SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
        incrementalBuild(delta, subMonitor);
        subMonitor.done();
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

    if (this.linter.isLintable(resource)) {
      this.linter.lint((IFile) resource, monitor);
    }
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
