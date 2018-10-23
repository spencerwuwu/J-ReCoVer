// https://searchcode.com/api/result/124047209/

/**
 * Copyright (c) 2007, Markus Jevring <markus@jevring.net>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The names of the contributors may not be used to endorse or promote
 *    products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */
package cu.ftpd.commands.transfer;

import cu.ftpd.logging.Logging;

/**
 * @author Markus Jevring <markus@jevring.net>
 * @since 2008-jan-29 - 20:58:31
 * @version $Id: SpeedLimitedTransferThread.java 258 2008-10-26 12:47:23Z jevring $
 */
public abstract class SpeedLimitedTransferThread extends TransferThread {
    protected final long speed;
    protected int pollsPerSecond;
    protected long loopsPerPoll;
    protected long bytesPerPoll;
    protected long timeTaken;
    protected long wait;
    protected long start;
    protected int loops;
    protected int timePerPoll;

    public SpeedLimitedTransferThread(TransferController controller, long speed) {
        super(controller);
        this.speed = speed;
        /*
        According to http://www.codinghorror.com/blog/archives/000339.html (and here http://www.yale.edu/fastcamac/gigabit/gbit_linux.html)
        We should use 64k packets for gbit speeds
        Thus we need to give people a way to set the packet size
        Do we do this for all connections? (i.e. the sender and receiver should
        preferrably have the same packet size)

        polling 10 times per second: (each poll should take 100ms)(AHH, this is per 100ms, which makes it a piece-of-cake to implement even with 10ms resolution)!!!!!!!!
        speed   bytes-per-poll  bufsize loops-per-poll
        125M    12.5M           8192    1600
        100M    10M             8192    1200
        10M     1M              8192    120
        1M      100k            8192    12
        500k    50k             8192    5
        250k    25k             8192    3
        100k    10k             4096    2-3
        50k     5k              2048    2
        10k     1k              512     2
        5k      512b            256     2
        1k      100b            50      2

        polling 4 times per second (every 125ms)
        speed   bytes-per-poll  bufsize loops-per-poll
        125M    30M             8192    3750
        100M    25M             8192    3125
        10M     2.5M            8192    312
        1M      250k            8192    31
        500k    125k            8192    16
        250k    65k             8192    8
        100k    25k             8192    3
        50k     12.5k           4096    3
        10k     2.5k            1024    2
        5k      1250b           512     2-3
        1k      250b            128     2

        we should increase the poll rate as the speed goes up.
        but no, we can't do that, because we still have the timing resolution problem
        but we can lower it to 2 times per second if the speed limit is low enough (but then they probably aren't using ethernet anyway)
        also, ethernet has an MTU, which is max. I wonder if it can send smaller packets
        ahh, yes, ethernet packets can be smaller than that, that's nice

        is there even a point to using buffers smaller than 1500 bytes, since that is the MTU of ethernet?
        we can just say that the lower the limit is, the more ineffective transmission will be

        maybe we should set a poll interval, and then change the buffer size. at least up to a certain speed, then we reduce the polling interval

        we should have a lowest buffer size of 1500 ( minus TCP and IP headers )

        Ethernet MTU: 1500
        IP header: 20
        TCP header:
        TCP/IP-over-ethernet-MTU: 1500 - 20 -20 = 1480 bytes
        thus, our lowest buffer size for ethernet is 1480 bytes
        but then again, if someone is doing low limits, they might not be using ethernet

         */
        // calculate and set buffer size depending on the speed
        // ideally we want more than 10 loops per poll

        // loops = (speed / pps) / bufsz
        // we want to keep loops around atleast 25, so that the time is measurable
        // 25 = (8192 / 10) / X
        // 25 = 819 / X
        // 25 * X = 819
        // X = 819 / 25
        // x = 32 => buffersize should be 32 bytes
        // x = (speed / pps) / loops
        // thus, try to use this formula with a set loops=25, and if that gives us a buffer size bigger than 8192, set the buffer size to 8192 and create the other numbers instead

        pollsPerSecond = 10;
        loopsPerPoll = 25;
//        System.out.println("speed: " + speed + " bytes per second");
        bytesPerPoll = speed / pollsPerSecond;
        if (bytesPerPoll <= bufferSize * 2) { // only go for a lower buffer if we are aiming for a lower speed
            bufferSize = (int)(((double)speed / (double)pollsPerSecond) / (double)loopsPerPoll);
//            System.out.println("attempted buffer size: " + bufferSize);
            if (bufferSize > 8192) {
                bufferSize = 8192;
            }
            // we only have to set the bufferSize, and the buffer will be created for us
            //buf = new byte[bufferSize];
        }


//        System.out.println("bytes per poll: " + bytesPerPoll);
        loopsPerPoll = bytesPerPoll / bufferSize;
//        System.out.println("bufsize: " + bufferSize);
//        System.out.println("loops per poll: " + loopsPerPoll);
        timePerPoll = 1000 / pollsPerSecond;
//        System.out.println("time per poll: " + timePerPoll);
    }

    protected void limit() {
        if (loops == loopsPerPoll) {
            timeTaken = (System.nanoTime() - start) / 1000000;
            wait = timePerPoll - timeTaken;
//            System.out.println("loop took " + timeTaken + " milliseconds, sleeping for " + wait + " milliseconds (which should add up to " + timePerPoll + ")");
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Logging.getErrorLog().reportException("Thread was interupted in speed limiter", e);
                    //e.printStackTrace();
                }
            } else {
//                System.out.println("wait was lower than 0, we're going slower than expected");
            }
            loops = 0;
            start = System.nanoTime();
        }
        loops++;
    }

}

