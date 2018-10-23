// https://searchcode.com/api/result/125476722/

/*
 * Copyright 2011-2013 Kevin A. Burton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package peregrine.app.benchmark;

import peregrine.Batch;
import peregrine.Job;
import peregrine.io.Input;
import peregrine.io.Output;

public class Benchmark extends Batch {

    public static final String DEFAULT_INPUT = "/test/benchmark.in";

    public static final String DEFAULT_OUTPUT = "/test/benchmark.out";

    public static int DEFAULT_WIDTH   = 1024;

    public static int DEFAULT_MAX     = 10000;

    private int width = DEFAULT_WIDTH;

    private int max = DEFAULT_MAX;

    private boolean extract = true;

    private boolean map = true;

    private boolean reduce = true;

    private String input = DEFAULT_INPUT;

    private String output = DEFAULT_OUTPUT;

    public Benchmark() {
        setName( getClass().getName() );
        setDescription( getName() );
    }

    public void init() {

        if ( extract ) {
            map( new Job().setDelegate( BenchmarkExtracter.class )
                   .setInput( "blackhole:" )
                   .setOutput( input )
                   .setParameters( "max", max,
                                   "width", width ) );
        }

        if ( map ) {
            map( BenchmarkMapper.class,
                 new Input( input ),
                 new Output( "shuffle:default" ) );
        }

        if ( reduce ) {
            reduce( BenchmarkReducer.class,
                    new Input( "shuffle:default" ),
                    new Output( output ) );
        }

    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public boolean isExtract() {
        return extract;
    }

    public void setExtract(boolean extract) {
        this.extract = extract;
    }

    public boolean isMap() {
        return map;
    }

    public void setMap(boolean map) {
        this.map = map;
    }

    public boolean isReduce() {
        return reduce;
    }

    public void setReduce(boolean reduce) {
        this.reduce = reduce;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}

