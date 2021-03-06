/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.lib.io.fileref;

import com.codahale.metrics.Counter;
import com.streamsets.pipeline.api.FileRef;
import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.sdk.ContextInfoCreator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

public class TestMetricsEnabledWrapperStream {
  private File testDir;
  private Stage.Context context;

  @Before
  public void setup() throws Exception {
    testDir = new File("target", UUID.randomUUID().toString());
    testDir.mkdirs();
    FileRefTestUtil.writePredefinedTextToFile(testDir);
    context = ContextInfoCreator.createTargetContext("", false, OnRecordError.TO_ERROR);
  }

  @After
  public void tearDown() throws Exception {
    testDir.delete();
  }

  private <T extends  AutoCloseable> long getRemainingBytes(T stream) {
    return ((Counter)Whitebox.getInternalState(stream, "remainingBytesCounter")).getCount();
  }

  private <T extends  AutoCloseable> long getCopiedBytes(T stream) {
    return ((Counter)Whitebox.getInternalState(stream, "copiedBytesCounter")).getCount();
  }

  private <T extends AutoCloseable> void checkStateDuringReads(long fileSize, long remainingFileSize, T is) {
    Assert.assertEquals(remainingFileSize, getRemainingBytes(is));
    Assert.assertEquals(fileSize - remainingFileSize, getCopiedBytes(is));
    Assert.assertEquals(fileSize, getRemainingBytes(is) + getCopiedBytes(is));
  }


  private <T extends AutoCloseable>  void checkStateAfterReadCompletion(long fileSize, long remainingFileSize, T is) {
    Assert.assertEquals(0, remainingFileSize);
    Assert.assertEquals(fileSize, getCopiedBytes(is));
    Assert.assertEquals(remainingFileSize, getRemainingBytes(is));
  }

  @Test
  public void testIOReadParameterLess() throws Exception {
    FileRef fileRef = FileRefTestUtil.getLocalFileRef(testDir, true, null, null);
    long fileSize = Files.size(Paths.get(FileRefTestUtil.getSourceFilePath(testDir)));
    long remainingFileSize = fileSize;
    try (InputStream is = fileRef.createInputStream(context, InputStream.class)) {
      Assert.assertEquals(remainingFileSize, getRemainingBytes(is));
      while(is.read() != -1) {
        remainingFileSize--;
        checkStateDuringReads(fileSize, remainingFileSize, is);
      }
      checkStateAfterReadCompletion(fileSize, remainingFileSize, is);
    }
  }

  @Test
  public void testIOReadParameterized() throws Exception {
    FileRef fileRef = FileRefTestUtil.getLocalFileRef(testDir, true, null, null);
    long fileSize = Files.size(Paths.get(FileRefTestUtil.getSourceFilePath(testDir)));
    long remainingFileSize = fileSize;
    try (InputStream is = fileRef.createInputStream(context, InputStream.class)) {
      Assert.assertEquals(remainingFileSize, getRemainingBytes(is));
      int bytesRead;
      byte[] b = new byte[10];
      while( (bytesRead = is.read(b, 0, b.length)) > 0) {
        remainingFileSize -= bytesRead;
        checkStateDuringReads(fileSize, remainingFileSize, is);
      }
      checkStateAfterReadCompletion(fileSize, remainingFileSize, is);
    }
  }

  @Test
  public void testIOReadsMixed() throws Exception {
    FileRef fileRef = FileRefTestUtil.getLocalFileRef(testDir, true, null, null);
    long fileSize = Files.size(Paths.get(FileRefTestUtil.getSourceFilePath(testDir)));
    long remainingFileSize = fileSize;
    try (InputStream is = fileRef.createInputStream(context, InputStream.class)) {
      Assert.assertEquals(remainingFileSize, getRemainingBytes(is));
      int bytesRead;
      while ((bytesRead= FileRefTestUtil.randomReadMethodsWithInputStream(is)) > 0) {
        remainingFileSize -= bytesRead;
        checkStateDuringReads(fileSize, remainingFileSize, is);
      }
      checkStateAfterReadCompletion(fileSize, remainingFileSize, is);
    }
  }

  @Test
  public void testNIOReadWithDirectBuffer() throws Exception {
    FileRef fileRef = FileRefTestUtil.getLocalFileRef(testDir, true, null, null);
    long fileSize = Files.size(Paths.get(FileRefTestUtil.getSourceFilePath(testDir)));
    long remainingFileSize = fileSize;
    try (ReadableByteChannel is = fileRef.createInputStream(context, ReadableByteChannel.class)) {
      Assert.assertEquals(remainingFileSize, getRemainingBytes(is));
      int bytesRead;
      ByteBuffer b = ByteBuffer.allocateDirect(10);
      while((bytesRead = is.read(b)) > 0) {
        remainingFileSize -= bytesRead;
        checkStateDuringReads(fileSize, remainingFileSize, is);
        b.compact();
      }
      checkStateAfterReadCompletion(fileSize, remainingFileSize, is);
    }
  }

  @Test
  public void testNIOReadWithHeapByteBuffer() throws Exception {
    FileRef fileRef = FileRefTestUtil.getLocalFileRef(testDir, true, null, null);
    long fileSize = Files.size(Paths.get(FileRefTestUtil.getSourceFilePath(testDir)));
    long remainingFileSize = fileSize;
    try (ReadableByteChannel is = fileRef.createInputStream(context, ReadableByteChannel.class)) {
      Assert.assertEquals(remainingFileSize, getRemainingBytes(is));
      int bytesRead;
      ByteBuffer b = ByteBuffer.allocate(10);
      while((bytesRead = is.read(b)) > 0) {
        remainingFileSize -= bytesRead;
        checkStateDuringReads(fileSize, remainingFileSize, is);
        b.compact();
      }
      checkStateAfterReadCompletion(fileSize, remainingFileSize, is);
    }
  }
}
