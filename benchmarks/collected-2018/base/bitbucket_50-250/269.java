// https://searchcode.com/api/result/103037968/

package com.shuaqiu.reactor;

import reactor.Fn;
import reactor.core.Composable.Reduce;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Function;

/**
 * @author qiushaohua 2013-5-22 a l5:33:17
 * 
 */
public class SimpleActor {

    public void act() {
        String on = "on";
        String send = "send";
        String receive = "receive";

        Reactor reactor = new Reactor();

        reactor.on(Fn.$(on), new Consumer<Event<String>>() {

            public void accept(Event<String> t) {
                System.out.println("on: " + t.getId() + ":" + t.getData());
            }
        });

        reactor.map(Fn.$(send), new Function<Event<String>, String>() {
            public String apply(Event<String> t) {
                System.out.println("map: " + t.getId() + ":" + t.getData());
                return t.getData();
            }
        }).map(new Function<String, Integer>() {
            public Integer apply(String t) {
                System.out.println("map: " + t);
                return t.hashCode();
            }
        }).reduce(new Function<Reduce<Integer, String>, String>() {
            public String apply(Reduce<Integer, String> t) {
                System.out.println("reduce: " + t.getNextValue() + ":"
                        + t.getLastValue());
                return t.toString();
            }
        });
        reactor.receive(Fn.$(send), new Function<Event<String>, String>() {
            public String apply(Event<String> t) {
                System.out.println("receive: " + t.getId() + ":" + t.getData());
                return t.getData();
            }
        });

        reactor.notify(on, Fn.event("test"));
        reactor.send(send, Fn.event("aaaaa", on));
        System.out.println("f");
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                System.err.println("b");
                SimpleActor a = new SimpleActor();
                a.act();
                System.err.println("s");
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}

