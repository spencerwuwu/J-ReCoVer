// https://searchcode.com/api/result/15025720/

// Copyright 2010 Vijay Mathew Pandyalakal. All rights reserved.

// Redistribution and use in source and binary forms, with or 
// without modification, are permitted provided that the following 
// conditions are met:

//    1. Redistributions of source code must retain the above copyright 
//       notice, this list of conditions and the following disclaimer.

//    2. Redistributions in binary form must reproduce the above copyright 
//       notice, this list of conditions and the following disclaimer in the 
//       documentation and/or other materials provided with the distribution.

// THIS SOFTWARE IS PROVIDED BY VIJAY MATHEW PANDYALAKAL ``AS IS'' AND ANY 
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
// DISCLAIMED. IN NO EVENT SHALL VIJAY MATHEW PANDYALAKAL OR CONTRIBUTORS BE 
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
// THE POSSIBILITY OF SUCH DAMAGE.

package org.niue.vm.operation.fp;

import java.util.ArrayList;
import org.niue.vm.IVmOperation;
import org.niue.vm.Vm;
import org.niue.vm.VmException;
import org.niue.vm.DataStackElement;
import org.niue.vm.ByteCode;

// Implements reduce.
// See: http://www.thocp.net/biographies/papers/backus_turingaward_lecture.pdf. 

public final class Reduce implements IVmOperation {

    // E.g:
    // 1 2 3 4 5 6 '+ reduce
    // => 21
    public void execute (Vm vm) throws VmException {
	DataStackElement block = vm.pop ();
	ByteCode.Type type = block.getType ();
	if (type != ByteCode.Type.VM && type != ByteCode.Type.STRING) {
	    VmException.raiseUnexpectedValueOnStack ();
	}
        int id = block.getElement ();
        try {
            while (vm.getDataStackSize () > 1) {
                if (type == ByteCode.Type.VM) {
                    vm.runChildVm (id, false);
                } else {
                    vm.executeWord (id);        
                }
            }
        } catch (VmException ex) {
            throw ex;
        } finally {
            if (type == ByteCode.Type.VM) 
                vm.discardChildVm (id);
        }
    }
}

