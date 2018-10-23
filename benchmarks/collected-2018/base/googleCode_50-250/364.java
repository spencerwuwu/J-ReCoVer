// https://searchcode.com/api/result/280718/

/**
 * Copyright (C) 2009 Mathieu Carbou <mathieu.carbou@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package euler;

import com.mycila.math.number.BigInt;

import static java.lang.System.*;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * http://projecteuler.net/index.php?section=problems&id=56
 *
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class Problem056 {

    public static void main(String[] args) throws Exception {
        long time = currentTimeMillis();

        System.out.println("Maximum digits: " + BigInt.big(99).pow(99).toString().length());
        System.out.println("Sum for 99^99:" + BigInt.big(99).pow(99).digitsSum());
        System.out.println("Sum for 90^90:" + BigInt.big(90).pow(90).digitsSum());

        final TreeSet<String> results = new TreeSet<String>();
        for (int a = 91; a <= 99; a++)
            for (int b = 91; b <= 99; b++)
                results.add(BigInt.big(a).pow(b).digitsSum() + " for " + a + "^" + b);
        //for (String result : results)
        //    System.out.println(result);
        out.println(results.last() + " in " + (currentTimeMillis() - time) + "ms");
        assertEquals("972 for 99^95", results.last());
    }

}

/*

a^b, a and b < 100.

The maximum number of digits is 198.
The bigger a and b will be, the higher the sum will be since there are more digits. So we can start with a=99 and b=99.
If we print the sum of the digits of 99^99 we obtain 936.

As we see in the example, multiplication by 10 seems to reduce the count since it introduces 0:
90 x 90 = 9*9 * 100 = 8100
We can test, 90^90 gives a digit sum of 360 only. So we will stop for a and b > 90 

*/
