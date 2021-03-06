/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hyracks.dataflow.common.io;

import java.nio.ByteBuffer;

import org.apache.hyracks.api.comm.IFrameWriter;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.api.io.FileReference;
import org.apache.hyracks.api.io.IFileHandle;
import org.apache.hyracks.api.io.IIOManager;

public class RunFileWriter implements IFrameWriter {
    private final FileReference file;
    private final IIOManager ioManager;
    private boolean failed;

    private IFileHandle handle;
    private long size;

    public RunFileWriter(FileReference file, IIOManager ioManager) {
        this.file = file;
        this.ioManager = ioManager;
    }

    @Override
    public void open() throws HyracksDataException {
        handle = ioManager.open(file, IIOManager.FileReadWriteMode.READ_WRITE,
                IIOManager.FileSyncMode.METADATA_ASYNC_DATA_ASYNC);
        size = 0;
        failed = false;
    }

    @Override
    public void fail() throws HyracksDataException {
        ioManager.close(handle);
        failed = true;
    }

    @Override
    public void nextFrame(ByteBuffer buffer) throws HyracksDataException {
        size += ioManager.syncWrite(handle, size, buffer);
    }

    @Override
    public void close() throws HyracksDataException {
        if (!failed) {
            ioManager.close(handle);
        }
    }

    public FileReference getFileReference() {
        return file;
    }

    public long getFileSize() {
        return size;
    }

    public RunFileReader createReader() throws HyracksDataException {
        if (failed) {
            throw new HyracksDataException("createReader() called on a failed RunFileWriter");
        }
        return new RunFileReader(file, ioManager, size, false);
    }

    public RunFileReader createDeleteOnCloseReader() throws HyracksDataException {
        if (failed) {
            throw new HyracksDataException("createReader() called on a failed RunFileWriter");
        }
        return new RunFileReader(file, ioManager, size, true);
    }
}