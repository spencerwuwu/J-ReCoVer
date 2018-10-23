// https://searchcode.com/api/result/11920214/

/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/OldBufferAllocator.java#5 $
*/

package ariba.util.core;

/**
    package private
*/
final class OldBufferAllocator implements BufferAllocator
{
    /*
        Keep around a buffer to reduce stoage allocation

        JP:  Netscape 3 doesn't support locking TheBuffer.  Locking array
        objects appear to cause class verifier errors with Netscape 3.  I
        reverted the code to use BufLock instead.
    */
    private final Object       BufLock      = new Object();
    private boolean      BufInUse     = false;
    private final FormatBuffer TheBuffer    = new FormatBuffer(BufSize);
    /**
        @aribaapi private
    */
    public FormatBuffer getBuffer ()
    {
            // Todo: this code should be changed to handle the
            // case where TheBuffer is lost due to an exception

        synchronized (BufLock) {
            if (! BufInUse) {
                BufInUse = true;
                return TheBuffer;
            }
        }
        return new FormatBuffer();
    }

    /**
        @aribaapi private

    */
    public void freeBuffer (FormatBuffer buf)
    {
            // Todo: this code should be changed to handle the
            // case where TheBuffer is lost due to an exception
        if (buf == TheBuffer) {
            buf.truncateToLength(0);
            if (buf.getBuffer().length > BufSizeLimit) {
                buf.setCapacity(BufSize);
            }
            synchronized (BufLock) {
                BufInUse = false;
            }
        }
    }

}

