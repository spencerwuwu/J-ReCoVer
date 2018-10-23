// https://searchcode.com/api/result/68682413/

package com.spbsu.hadoop;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * User: svasilinets
 * Date: 02.05.12
 * Time: 18:39
 */
class CountryReducer extends Reducer<CountryTimeKey, LogRecord, Text, FloatWritable> {

    private static final long MAX_SESSION_TIME = 30 * 60 * 1000;


    private class Session {
        final String ip;
        final String userAgent;
        int pageCounter;
        long lastUsage;

        private Session(String ip, String userAgent) {
            this.ip = ip;
            this.userAgent = userAgent;
        }

        @Override
        public int hashCode() {
            return ip.hashCode() * 157 + userAgent.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Session) {
                Session s = (Session) obj;
                boolean res = s.ip.equals(ip) && s.userAgent.equals(s.userAgent);

                return res;
            }
            return false;
        }
    }


    private static boolean isPage(String request) {
        if (request == null || !request.startsWith("GET ")) {
            return false;
        }
        String rest = request.substring(4);
        StringTokenizer tokenizer = new StringTokenizer(rest);
        if (!tokenizer.hasMoreTokens()) {
            return false;
        }
        String path = tokenizer.nextToken();
        return !path.contains(".");

    }


    @Override
        protected void reduce(CountryTimeKey key, Iterable<LogRecord> values, Context context) throws IOException, InterruptedException {

        LinkedList<Session> toRemove = new LinkedList<Session>();

        int sessionCounter = 0;
        int pageCounter = 0;
        for (LogRecord l : values) {

            long current = l.getTimestamp();
            while (!toRemove.isEmpty() && toRemove.getFirst().lastUsage + MAX_SESSION_TIME <= current) {
                Session session = toRemove.removeFirst();
                sessionCounter++;
                pageCounter += session.pageCounter;
            }


            Session session = new Session(l.getIp(), l.getUserAgent());
            System.out.println(l.getIp() + " " + l.getUserAgent());
            int ind = toRemove.indexOf(session);
            if (ind != -1) {
                session = toRemove.remove(ind);
                toRemove.addLast(session);
            }
            session.lastUsage = current;

            if (isPage(l.getRequest())) {
                session.pageCounter++;
                if (ind == -1) {
                    toRemove.addLast(session);
                }
                continue;
            }

            if (ind == -1) {
                Session sameIp = null;
                for (Iterator<Session> iter = toRemove.descendingIterator(); iter.hasNext(); ) {
                    Session s = iter.next();
                    if (s.ip.equals(l.getIp())) {
                        sameIp = s;
                        iter.remove();
                        break;
                    }
                }

                if (sameIp != null) {
                    sameIp.lastUsage = current;
                    toRemove.addLast(sameIp);
                }


            }

        }

        for (Session session : toRemove) {
            sessionCounter++;
            pageCounter += session.pageCounter;
        }

        System.out.println(pageCounter + "  " + sessionCounter );
        FloatWritable pagePerSession = new FloatWritable(((float) pageCounter) / sessionCounter);
        context.write(new Text(key.getCountry()), pagePerSession);
    }

}

