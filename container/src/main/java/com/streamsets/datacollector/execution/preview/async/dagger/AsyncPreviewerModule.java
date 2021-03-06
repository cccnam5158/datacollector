/**
 * Copyright 2015 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.datacollector.execution.preview.async.dagger;

import com.streamsets.datacollector.execution.Previewer;
import com.streamsets.datacollector.execution.PreviewerListener;
import com.streamsets.datacollector.execution.preview.async.AsyncPreviewer;
import com.streamsets.datacollector.execution.preview.sync.SyncPreviewer;
import com.streamsets.pipeline.lib.executor.SafeScheduledExecutorService;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Named;

/**
 * Provides instances of SyncPreviewer.
 */
@Module(injects = Previewer.class, library = true, complete = false)
public class AsyncPreviewerModule {

  private final String id;
  private final String name;
  private final String rev;
  private final ObjectGraph objectGraph;
  private final PreviewerListener previewerListener;

  public AsyncPreviewerModule(String id, String name, String rev, PreviewerListener previewerListener,
                              ObjectGraph objectGraph) {
    this.id = id;
    this.name = name;
    this.rev = rev;
    this.objectGraph = objectGraph;
    this.previewerListener = previewerListener;
  }


  @Provides
  public SyncPreviewer providePreviewer() {
    return new SyncPreviewer(id, name, rev, previewerListener, objectGraph);
  }

  @Provides
  public Previewer provideAsyncPreviewer(SyncPreviewer previewer,
                                         @Named("previewExecutor") SafeScheduledExecutorService asyncExecutor) {
    return new AsyncPreviewer(previewer, asyncExecutor);
  }
}
