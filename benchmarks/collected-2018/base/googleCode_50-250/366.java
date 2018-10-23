// https://searchcode.com/api/result/280752/

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

import com.mycila.math.Digits;
import com.mycila.math.prime.PrimaltyTest;

import static java.lang.System.*;

import static org.junit.Assert.*;

/**
 * http://projecteuler.net/index.php?section=problems&id=41
 *
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class Problem041 {
    public static void main(String[] args) throws Exception {
        long time = currentTimeMillis();
        Digits pandigital = Digits.base(10);
        for (int i = 7654321; i >= 1234567; i -= 2) {
            if (pandigital.isPandigital(i, 1, 7) && PrimaltyTest.millerRabin(i)) {
                out.println(i + " in " + (currentTimeMillis() - time) + "ms");
                assertEquals(7652413, i);
                break;
            }
        }
    }
}
/*

Problem 41 requires little thinking since testing all primes up to 987654321 is too expensive in Java. So we must try to reduce the quantity of prime numbers to generate for testing against our pandigital test.

A number is divisible by 3 if its digit sum is a multil[le of 3. We try to find the maximum pandigital number not divisible:

9+8+7+6+5+4+3+2+1=45
8+7+6+5+4+3+2+1=36
6+5+4+3+2+1=21
5+4+3+2+1=15
3+2+1=6
2+1=3

Divisible by 3 => There can't be any 1-9, 1-8, 1-6, 1-5 pandigital prime.

7+6+5+4+3+2+1=28
4+3+2+1=10

Not divisible by 3. So we will check primes from 7654321 to 1234567 and then if not found from 4321 to 1234.  

*/
