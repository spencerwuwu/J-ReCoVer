// https://searchcode.com/api/result/11841892/

/**
 * Copyright (C) 2008, 2009 Ivan S. Dubrov
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

package com.google.code.nanorm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.code.nanorm.internal.BoundFragment;
import com.google.code.nanorm.internal.TextFragment;
import com.google.code.nanorm.internal.introspect.IntrospectionFactory;
import com.google.code.nanorm.internal.mapping.parameter.ParameterMapper;
import com.google.code.nanorm.internal.util.ToStringBuilder;

/**
 * Base class for dynamic SQL generators.
 * 
 * @author Ivan Dubrov
 */
public abstract class SQLSource implements BoundFragment {

    /**
     * Name of the method extender should implement for SQL generation.
     * 
     * The parameters of the method should exactly match the parameters of the
     * query method this SQL source is applied to.
     */
    public static final String GENERATOR_METHOD = "sql";

    /**
     * Stack of list of bound fragments. Each basic "append" operation adds
     * bound fragment to the list on stack top.
     * 
     * Used for collecting the generated fragments, bound to the parameters.
     */
    private final List<List<BoundFragment>> stack = new ArrayList<List<BoundFragment>>();

    private IntrospectionFactory reflFactory;

    /**
     * Helper class for generating different text fragment joins (not SQL
     * joins).
     * 
     * @author Ivan Dubrov
     */
    public static class Join implements BoundFragment {
        private String open;

        private String close;

        private String with;

        private final List<List<BoundFragment>> clauses = new ArrayList<List<BoundFragment>>();

        /**
         * Set prepending fragment of join (prepended to the join body).
         * 
         * @param o open fragment.
         * @return this
         */
        public Join open(String o) {
            this.open = o;
            return this;
        }

        /**
         * Set appending fragment of join (appended to the join body).
         * 
         * @param c close fragment
         * @return this
         */
        public Join close(String c) {
            this.close = c;
            return this;
        }

        /**
         * Set fragment used for joining the clauses.
         * 
         * @param w fragment used for joining the clauses.
         * @return this
         */
        public Join with(String w) {
            this.with = w;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void generate(StringBuilder builder, List<ParameterMapper> parameters) {
            if (clauses.isEmpty()) {
                return;
            }
            if (open != null) {
                builder.append(open);
            }

            // Join elements in the clauses list
            boolean first = true;
            for (List<BoundFragment> items : clauses) {
                if (!first && with != null) {
                    builder.append(with);
                }
                for (BoundFragment item : items) {
                    item.generate(builder, parameters);
                }
                first = false;
            }

            if (close != null) {
                builder.append(close);
            }
        }
    }

    /**
     * Get last bound fragment on the stack.
     * 
     * @return last bound fragment
     */
    private List<BoundFragment> last() {
        return stack.get(stack.size() - 1);
    }

    /**
     * Constructor.
     */
    protected SQLSource() {
        stack.add(new ArrayList<BoundFragment>());
    }

    /** @param reflFactory The reflFactory to set. */
    public void setReflFactory(IntrospectionFactory reflFactory) {
        this.reflFactory = reflFactory;
    }

    /**
     * Append given SQL fragment with parameters to the current dynamic
     * statement.
     * 
     * @param fragment SQL fragment
     * @param params clause parameters
     */
    public void append(String fragment, Object... params) {
        BoundFragment f = new TextFragment(fragment, reflFactory).bindParameters(params);
        last().add(f);
    }

    /**
     * Append given SQL fragment with given parameter if parameter is not null.
     * 
     * @param fragment SQL fragment
     * @param param parameter
     */
    public void appendNotNull(String fragment, Object param) {
        if (param != null) {
            BoundFragment f = new TextFragment(fragment, reflFactory)
                    .bindParameters(new Object[] {param });
            last().add(f);
        }
    }

    /**
     * Iterate the parameters and apply block to each of them, generating the
     * string join as a result.
     * 
     * From the functional programming point of view, this is kind of map+reduce
     * operation. {@link ParamBlock} is the map function, {@link Join} defines
     * the reduce rules (separator between fragments, opening character, closing
     * character).
     * 
     * Using the methods of the {@link Join} object, one can modify the rules of
     * joining the text fragments.
     * 
     * @param <T> parameters type
     * @param block block to apply to each parameter
     * @param params parameters
     * @return join object which could be used to configure join parameters.
     */
    public <T> Join join(ParamBlock<T> block, T... params) {
        return join(block, params == null ? null : Arrays.asList(params));
    }

    /**
     * Iterate the parameters and apply block to each of them, generating the
     * string join as a result.
     * 
     * From the functional programming point of view, this is kind of map+reduce
     * operation. {@link ParamBlock} is the map function, {@link Join} defines
     * the reduce rules (separator between fragments, opening character, closing
     * character).
     * 
     * Using the methods of the {@link Join} object, one can modify the rules of
     * joining the text fragments.
     * 
     * @param <T> parameters type
     * @param block block to apply to each parameter
     * @param params parameters, null collection is treated as empty one
     * @return join object which could be used to configure join parameters.
     */
    public <T> Join join(ParamBlock<T> block, Collection<T> params) {
        Join join = new Join();

        if (params != null && !params.isEmpty()) {
            List<BoundFragment> items = new ArrayList<BoundFragment>();
            for (T param : params) {
                // Create new list if previous one is not empty, otherwise reuse
                if (!items.isEmpty()) {
                    items = new ArrayList<BoundFragment>();
                }
                stack.add(items);
                try {
                    block.generate(param);
                } finally {
                    assert last() == items;
                    stack.remove(stack.size() - 1);
                }
                if (!items.isEmpty()) {
                    join.clauses.add(items);
                }
            }
            last().add(join);
        }
        return join;
    }

    /**
     * Join given generator blocks together.
     * 
     * From the functional programming point of view, this is kind of reduce
     * operation. {@link Block} is the text fragment generator, {@link Join}
     * defines the reduce rules (separator between fragments, opening character,
     * closing character).
     * 
     * Using the methods of the {@link Join} object, one can modify the rules of
     * joining the text fragments.
     * 
     * @param blocks generator blocks to join
     * @return join configuration method.
     */
    public Join join(Block... blocks) {
        Join join = new Join();

        List<BoundFragment> items = new ArrayList<BoundFragment>();
        for (Block block : blocks) {
            if (!items.isEmpty()) {
                // Create new list if previous one is not empty, otherwise reuse
                items = new ArrayList<BoundFragment>();
            }
            stack.add(items);
            try {
                block.generate();
            } finally {
                if (last() != items) {
                    throw new IllegalStateException(
                            "Wrong stack state, last element must be the one generated by 'join' call!");
                }
                stack.remove(stack.size() - 1);
            }
            if (!items.isEmpty()) {
                join.clauses.add(items);
            }
        }
        last().add(join);
        return join;
    }

    /**
     * Generate the final SQL fragemnt with parameter and types.
     * 
     * @param builder SQL fragment builder
     * @param parameters parameters
     */
    public void generate(StringBuilder builder, List<ParameterMapper> parameters) {
        if (stack.size() != 1) {
            throw new IllegalStateException("Stack must contain exactly one element!");
        }
        for (BoundFragment obj : last()) {
            obj.generate(builder, parameters);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).toString();
    }
}

