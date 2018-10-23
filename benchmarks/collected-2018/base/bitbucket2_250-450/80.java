// https://searchcode.com/api/result/102777153/

/* ------------------------------------------------------------------------
 * Copyright 2012 Tim Vernum
 * ------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ------------------------------------------------------------------------
 */

package org.adjective.syntactic.parser.node;

import org.adjective.syntactic.parser.ast.ASTArraySuffixList;
import org.adjective.syntactic.parser.ast.ASTClassOrInterfaceType;
import org.adjective.syntactic.parser.ast.ASTIdentifier;
import org.adjective.syntactic.parser.ast.ASTPrimitiveType;
import org.adjective.syntactic.parser.ast.ASTReferenceType;
import org.adjective.syntactic.parser.ast.ASTVoidType;
import org.adjective.syntactic.parser.ast.Node;
import org.adjective.syntactic.parser.ast.SimpleNode;
import org.adjective.syntactic.parser.jj.Token;
import org.adjective.syntactic.parser.type.ArrayType;
import org.adjective.syntactic.parser.type.JavaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author <a href="http://blog.adjective.org/">Tim Vernum</a>
 */
public class BaseNode extends SimpleNode implements NodeType
{
    protected BaseNode(int i)
    {
        super(i);
    }

    protected <T extends Node> T child(Class<T> type)
    {
        return child(type, false);
    }

    protected <T extends Node> T optionalChild(Class<T> type)
    {
        return child(type, true);
    }

    protected BaseNode child(final int i)
    {
        return (BaseNode) jjtGetChild(i);
    }

    private <T extends Node> T child(Class<T> type, boolean optional)
    {
        for (NodeType node : children())
        {
            if (type.isInstance(node))
            {
                return type.cast(node);
            }
        }
        if (optional)
        {
            return null;
        }
        else
        {
            throw new IllegalStateException("No child matching type " + type.getSimpleName());
        }
    }

    protected Nodes<? extends BaseNode> children()
    {
        return allChildren(BaseNode.class);
    }

    protected <T extends Node> Nodes<T> allChildren(final Class<T> type)
    {
        return new TypeCheckNodeList<T>(this, type, false);
    }

    protected <T extends Node> Nodes<T> findChildren(final Class<T> type)
    {
        return new TypeCheckNodeList<T>(this, type, true);
    }

    protected int countChildren(Class<? extends Node> nodeType)
    {
        int count = 0;
        for (int i = 0; i < jjtGetNumChildren(); i++)
        {
            Node node = jjtGetChild(i);
            if (nodeType.isInstance(node))
            {
                count++;
            }
        }
        return count;
    }

    public <T extends Node> Nodes<T> children(final Class<T> type, final int start)
    {
        return this.children(type, start, jjtGetNumChildren());
    }

    public <T extends Node> Nodes<T> children(final Class<T> type, final int start, final int end)
    {
        return new TypeCheckNodeList<T>(this, type, start, end, false);
    }

    public boolean isEmpty()
    {
        return jjtGetNumChildren() == 0;
    }

    protected BaseNode getFirstChild()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Node " + this + " has no children");
        }
        return (BaseNode) jjtGetChild(0);
    }

    protected BaseNode getLastChild()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Node " + this + " has no children");
        }
        return (BaseNode) jjtGetChild(jjtGetNumChildren() - 1);
    }

    protected void appendChild(final Node node)
    {
        jjtAddChild(node, jjtGetNumChildren());
    }

    protected void appendChildren(final Node... nodes)
    {
        for (Node node : nodes)
        {
            appendChild(node);
        }
    }

    protected void appendChildren(final Iterable<? extends Node> nodes)
    {
        for (Node node : nodes)
        {
            appendChild(node);
        }
    }

    @Override
    public boolean is(Class<? extends Node> nodeType)
    {
        return nodeType.isInstance(this);
    }

    @Override
    public <T extends Node> T as(Class<T> nodeType)
    {
        if (is(nodeType))
        {
            return nodeType.cast(this);
        }
        else
        {
            throw new IllegalStateException("The node " + this + " is not " + nodeType.getSimpleName());
        }
    }

    public static JavaType makeType(JavaType type, ASTArraySuffixList arraySuffix)
    {
        return makeArrayType(type, arraySuffix == null ? 0 : arraySuffix.getArrayDepth());
    }

    protected static JavaType makeArrayType(final JavaType type, final int arrayDepth)
    {
        if (arrayDepth == 0)
        {
            return type;
        }
        else
        {
            return new ArrayType(type, arrayDepth);
        }
    }

    protected String getIdentifierName(final Iterable<ASTIdentifier> identifiers)
    {
        StringBuilder builder = new StringBuilder();
        for (ASTIdentifier identifier : identifiers)
        {
            builder.append(identifier.getIdentifier());
            builder.append(".");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    protected int countTokens(final int kind)
    {
        int count = 0;
        for (Token token : tokens())
        {
            if (token.kind == kind)
            {
                count++;
            }
        }
        return count;

    }

    private Iterable<? extends Token> tokens()
    {
        return new Iterable<Token>()
        {
            @Override
            public Iterator<Token> iterator()
            {
                return new TokenIterator(jjtGetFirstToken(), jjtGetLastToken());
            }
        };
    }

    protected void setChildren(final Node... elements)
    {
        // We run in reverse order to reduce the number of array allocations
        for (int i = elements.length - 1; i >= 0; i--)
        {
            jjtAddChild(elements[i], i);
        }
    }

    protected void setChildren(final Iterable<? extends Node> elements)
    {
        int i = 0;
        for (Node element : elements)
        {
            jjtAddChild(element, i);
            i++;
        }
    }

    protected TypeNode makeTypeNode(final JavaType type)
    {
        TypeNode componentType;
        switch (type.getBaseType())
        {
            case OBJECT:
            case STRING_LITERAL:
                componentType = new ASTClassOrInterfaceType(type.getParameterizedTypeName());
                break;
            case LAMBDA_LITERAL:
                throw new IllegalArgumentException("Cannot create a type node for a lambda");
            case VOID:
                return new ASTVoidType();
            default:
                componentType = new ASTPrimitiveType(type.getBaseType());
                break;
        }
        if (type.getArrayDepth() == 0)
        {
            return componentType;
        }
        else
        {
            return new ASTReferenceType(componentType, type.getArrayDepth());
        }
    }

    protected Token makeToken(final int kind, final String image)
    {
        final Token token = new Token();
        token.image = image;
        token.kind = kind;
        return token;
    }

    public <T extends Node> T findAncestor(final Class<T> type)
    {
        for (Node node = jjtGetParent(); node != null; node = node.jjtGetParent())
        {
            if (type.isInstance(node))
            {
                return type.cast(node);
            }
        }
        return null;
    }

    public void replace(final Node node, final Collection<? extends Node> replacement)
    {
        List<Node> nodes = new ArrayList<Node>();
        for (BaseNode child : children())
        {
            if (child == node)
            {
                nodes.addAll(replacement);
            }
            else
            {
                nodes.add(child);
            }
        }
        jjtClearChildren();
        setChildren(nodes);
    }

    public CharSequence getDebugInfo()
    {
        StringBuilder builder = new StringBuilder(toString());
        builder.append("(@");
        final Token token = jjtGetFirstToken();
        if (token == null)
        {
            builder.append("?)");
            return builder;
        }
        builder.append(token.beginLine).append(':').append(token.beginColumn).append(' ').append(token.image);
        builder.append(")");
        return builder;
    }

    private class TokenIterator implements Iterator<Token>
    {
        private Token _next;
        private final Token _last;

        public TokenIterator(final Token first, final Token last)
        {
            _next = first;
            _last = last;
        }

        @Override
        public boolean hasNext()
        {
            return _next != null;
        }

        @Override
        public Token next()
        {
            Token tok = _next;
            if (tok == null)
            {
                throw new NoSuchElementException();
            }
            if (_next == _last)
            {
                _next = null;
            }
            else
            {
                _next = _next.next;
            }
            return tok;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("TokenIterator.remove - not implemented");
        }
    }
}

