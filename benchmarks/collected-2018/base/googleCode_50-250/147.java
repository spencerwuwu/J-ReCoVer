// https://searchcode.com/api/result/3863898/

/*
 * CoreStringTest.java
 * 
 * Copyright (c) 2010, Ralf Biedert All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the author nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package junit;

import static net.jcores.jre.CoreKeeper.$;

import java.io.File;

import net.jcores.jre.cores.CoreFile;
import net.jcores.jre.cores.CoreString;
import net.jcores.jre.interfaces.functions.F1;
import net.jcores.jre.interfaces.functions.F2ReduceObjects;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ralf Biedert
 */
public class CoreFileTest {

    /** */
    @SuppressWarnings("boxing")
    @Test
    public void testComplexReadWrite() {

        // For n times we randomly either append or remove a line to a given file while tracking how
        // its content must look like afterwards.
        int sum = 667;

        final File testFile = $.sys.tempfile();
        $(testFile).append(sum + "\n"); // This line must always be present in the file

        for (int i = 0; i < 1000; i++) {
            // In this case we add
            if (sum < 1000 || $.random().nextBoolean()) {
                sum += i;
                $(testFile).append(i + "\n");
            } else {
                String text = null;

                // Add a bit of diversity
                if ($.random().nextBoolean()) {
                    text = $(testFile).text().get(0);
                } else {
                    text = $(testFile).input().text().get(0);
                }

                int removed = Integer.parseInt($(text).split("\n").get(-1));
                String rest = $(text).split("\n").slice(0, -2).as(CoreString.class).as(CoreString.class).join("\n").concat("\n");

                $(testFile).delete().append(rest);

                sum -= removed;
            }
        }
        
        // pdfjoin $@
        // $("a", "b", "c") ---> pdfjo. a b c 
        // $("pdfjoin " + $))
        

        // Now compute the value printed in the file
        int filesum = $(testFile).text().split("\n").map(new F1<String, Integer>() {
            @Override
            public Integer f(String x) {
                return Integer.valueOf(x);
            }
        }).reduce(new F2ReduceObjects<Integer>() {
            @Override
            public Integer f(Integer left, Integer right) {
                return left + right;
            }
        }).get(0);

        Assert.assertEquals(sum, filesum);
    }

    /** */
    @Test
    public void testZip() {
        final String path = $.sys.tempfile().getAbsolutePath();
        
        final CoreFile source = $("documentation").file();
        final int srcsize = source.dir().size();
        
        // Zip by path
        source.zip(path);
        Assert.assertEquals(srcsize, $(path).file().input().zipstream().dir().size());
        $(path).file().delete();
    }
    
    
    /** */
    @Test
    public void testFilesize() {
        Assert.assertTrue($(".").file().dir().filesize().sum() > 1000000);
    }
    
    
    /** */
    @Test
    public void testCopy() {
        final File file = $.sys.tempfile();
        final File dir = $.sys.tempdir();
        
        $("core/tests/junit/data/ranges.zip").file().copy(file.getAbsolutePath());
        Assert.assertEquals($("core/tests/junit/data/ranges.zip").file().get(0).length(), file.length());

        $("core/tests/junit/data/ranges.zip").file().copy(dir.getAbsolutePath());
        Assert.assertEquals($("core/tests/junit/data/ranges.zip").file().get(0).length(), new File(dir.getAbsolutePath() + "/ranges.zip").length());
    }

}

