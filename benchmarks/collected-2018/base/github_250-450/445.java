// https://searchcode.com/api/result/65832803/

/*
 * JBoss, by Red Hat.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.forge.shell.command.fshparser;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * @author Mike Brock .
 */
public class AutoReducingQueue implements Queue<String>
{
   private Node currNode;

   private String reduceCache;

   private final FSHRuntime runtime;

   public AutoReducingQueue(final Node startNode, final FSHRuntime runtime)
   {
      this.currNode = startNode;
      this.runtime = runtime;
   }

   @Override
   public boolean add(final String o)
   {
      throw new RuntimeException("add() not supported");
   }

   @Override
   public boolean offer(final String o)
   {
      throw new RuntimeException("offer() not supported");
   }

   @Override
   public String remove()
   {
      if (currNode == null)
      {
         throw new RuntimeException("empty queue");
      }

      reduce();
      advance();

      return reduceCache;
   }

   @Override
   public String poll()
   {
      reduce();
      advance();
      return reduceCache;
   }

   @Override
   public String element()
   {
      return null;
   }

   @Override
   public String peek()
   {
      if (reduceCache == null)
      {
         reduce();
      }
      return reduceCache;
   }

   private void reduce()
   {
      if (currNode instanceof ScriptNode)
      {
         reduceCache = Parse.executeScript((ScriptNode) currNode, runtime);

         if (((ScriptNode) currNode).isNocommand())
         {
            reduceCache = "";
         }
      }
      else if (currNode instanceof TokenNode)
      {
         reduceCache = ((TokenNode) currNode).getValue();
      }
      else if (currNode instanceof LogicalStatement)
      {
         StringBuilder sb = new StringBuilder("(");

         Iterator<String> iter = new AutoReducingQueue(((LogicalStatement) currNode).getNest(), runtime)
                  .iterator();

         while (iter.hasNext())
         {
            sb.append(iter.next());
            if (iter.hasNext())
            {
               sb.append(" ");
            }
         }

         reduceCache = sb.append(")").toString();
      }
      else
      {
         throw new RuntimeException("cannot reduce: " + currNode);
      }

      if (reduceCache == null)
      {
         reduceCache = "";
      }
   }

   private void advance()
   {
      reduceCache = null;
      if ((currNode != null) && (currNode.next != null))
      {
         currNode = currNode.next;
      }
      else
      {
         currNode = null;
      }
   }

   @Override
   public int size()
   {
      return 0;
   }

   @Override
   public boolean isEmpty()
   {
      return currNode == null;
   }

   @Override
   public boolean contains(final Object o)
   {
      return false;
   }

   @Override
   public Iterator<String> iterator()
   {
      return new Iterator<String>()
      {
         @Override
         public boolean hasNext()
         {
            return currNode != null;
         }

         @Override
         public String next()
         {
            try
            {
               return AutoReducingQueue.this.peek();
            }
            finally
            {
               advance();
            }
         }

         @Override
         public void remove()
         {
         }
      };
   }

   @Override
   public Object[] toArray()
   {
      return new Object[0];
   }

   @Override
   public <T> T[] toArray(final T[] a)
   {
      return null;
   }

   @Override
   public boolean remove(final Object o)
   {
      return false;
   }

   @Override
   public boolean containsAll(final Collection<?> c)
   {
      throw new RuntimeException("containsAll() not supported");
   }

   @Override
   @SuppressWarnings("rawtypes")
   public boolean addAll(final Collection c)
   {
      throw new RuntimeException("addAll() not supported");
   }

   @Override
   public boolean removeAll(final Collection<?> c)
   {
      throw new RuntimeException("removeAll() not supported");
   }

   @Override
   public boolean retainAll(final Collection<?> c)
   {
      throw new RuntimeException("retainAll() not supported");
   }

   @Override
   public void clear()
   {
      throw new RuntimeException("clear() not supported");
   }
}

