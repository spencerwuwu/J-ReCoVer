// https://searchcode.com/api/result/280723/

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
import com.mycila.math.concurrent.ConcurrentOperation;
import static org.junit.Assert.*;

import static java.lang.System.*;

/**
 * http://projecteuler.net/index.php?section=problems&id=53
 *
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class Problem053 {
    public static void main(String[] args) throws Exception {
        long time = currentTimeMillis();
        int count = 0;
        final BigInt LIMIT = BigInt.big(1000000);
        for (int n = 23; n <= 100; n++) {
            for (int p = 2, max = n >>> 1; p <= max; p++) {
                final BigInt c = BigInt.big(n).binomial(p);
                if (c.compareTo(LIMIT) > 0)
                    count += p << 1 == n ? 1 : 2;
            }
        }
        out.println(count + " in " + (currentTimeMillis() - time) + "ms");
        assertEquals(4075, count);
    }

}

/*

We need to compute all C(n,p) with 23 <= n <= 100 and 1 <= p =< n
We know that C(n,1)=n and C(n,n)=1 So we can reduce to 1 < p < n.
We can reduce the search knowing that C(n, p) = C(n, n-p). Thus: 1 < p <= n/2.
If C(n,p) > 1000000, we double count for p and n-p, except if n=2p, where we count only one.

*/
