// https://searchcode.com/api/result/280727/

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

import static java.lang.System.*;
import java.util.Arrays;

/**
 * http://projecteuler.net/index.php?section=problems&id=63
 *
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class Problem063 {
    public static void main(String[] args) throws Exception {
        final long time = currentTimeMillis();

        double[] logs = new double[10];
        for (int i = 1; i < logs.length; i++)
            logs[i] = Math.log10(i);

        int count = 0;
        for (double n = 1.0; n < 22.0; n++) {
            double limit = (n - 1.0) / n;
            int pos = Arrays.binarySearch(logs, limit);
            count += pos > 0 ? 10 - pos : 11 + pos;
        }

        System.out.println(count + " in " + (System.currentTimeMillis() - time) + "ms");
    }
}
/*

10^n has n+1 digits. Before, there are n digits or less for a^n with 0<a<=9.
So we only need to test a^n with 1<a<10 and n>=1

By searching the limit, we try:
9^10=3486784401 (10 digits)
9^20=12 157665 459056 928801 (20 digits)
9^40=147 808829 414345 923316 083210 206383 297601 (39 digits)

So there exist a value n for which 9^n becomes to have less than n digits.

We know that the length of a number n can be obtain by: len(n) = floor(log(n))+1. We want to have:
len(a^n)=n
floor(log(a^n))+1=n
floor(n*log(a))=n-1

So we just need to iterate over all n numbers starting at 1, and for each n we check the equality.
Also, we can even reduce the search: we can see that for n = 3 it is obvious that 2^3, 3^3 won't give 3 digits. This is true for all a^n with a<=n.

With the log, we can generalize:

floor(n*log(a))=n-1

We want to know the limits for log(a), so that floor(n*log(a)) can produce n-1.

n*log(a)=n-1 is when log(a)=(n-1)/n

We know that 1<=a<=9. So log(a) is always below 1.

So if log(a) < (n-1)/n we won't consider a since the number of digits will be too small (less than or n-1)

Let's take an example.

For n = 3: 4^3=64 (we reject) and 5^3=125 (we accept)
The limit is (n-1)/n=2/3=0.66666
log(4)=0.6020... < limit, so we reject
log(5)=0.6989... > limit, so we accept
All a>=5 will product 3 digits.

So for our algorithm, we will precompute all values of log(a) for 1<=a<=9 in an array.
For each n, we will compute the limit (n-1)/n
Then we will find in the table the first value that is greater than the limit, and increment the counter of the remaining log in the array.
In example, for n=3, we found that array[5] is greater than log(3). So the counter is incremented by 10-5=5.

But when to stop ? We stop if there is no log values in the array greater than the limit (n-1)/n.
This occurs when log(9)=(n-1)/n, so when n=22

// outputs 49

*/
