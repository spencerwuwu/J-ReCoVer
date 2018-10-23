// https://searchcode.com/api/result/62980116/

// Copyright 2010 NexJ Systems Inc. This software is licensed under the terms of the Eclipse Public License 1.0
package nexj.core.integration.format.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nexj.core.integration.Input;
import nexj.core.integration.IntegrationException;
import nexj.core.integration.MessageParser;
import nexj.core.meta.Attribute;
import nexj.core.meta.Metaclass;
import nexj.core.meta.MetadataException;
import nexj.core.meta.integration.CompositeMessagePart;
import nexj.core.meta.integration.CompositeMessagePartRef;
import nexj.core.meta.integration.Message;
import nexj.core.meta.integration.MessagePart;
import nexj.core.meta.integration.MessageTable;
import nexj.core.meta.integration.PrimitiveMessagePart;
import nexj.core.meta.integration.format.object.ObjectMessagePartMapping;
import nexj.core.rpc.GenericServer;
import nexj.core.rpc.RPCUtil;
import nexj.core.rpc.TransferObject;
import nexj.core.runtime.Instance;
import nexj.core.runtime.InvocationContext;
import nexj.core.runtime.InvocationContextAware;
import nexj.core.util.HashTab;
import nexj.core.util.HashTab2D;
import nexj.core.util.Logger;
import nexj.core.util.Lookup;
import nexj.core.util.Lookup2D;

/**
 * Object message parser.
 */
public class ObjectMessageParser implements MessageParser, InvocationContextAware
{
   // attributes

   /**
    * The flag whether an instance should be fetched when parsing a TransferObject.
    */
   protected boolean m_bFetchInstance = false;

   // associations

   /**
    * The invocation context.
    */
   protected InvocationContext m_context;
   
   /**
    * Instance map: Instance[TransferObject].
    */
   protected Lookup m_instanceMap;

   /**
    * Processed pairs (Instance, MessagePart) used to avoid circular dependencies
    */ 
   protected Lookup2D m_processedSet;
   
   /**
    * The class logger.
    */
   protected final static Logger s_logger = Logger.getLogger(ObjectMessageParser.class);
   
   // operations

   /**
    * @see nexj.core.runtime.InvocationContextAware#setInvocationContext(nexj.core.runtime.InvocationContext)
    */
   public void setInvocationContext(InvocationContext context)
   {
      m_context = context;
   }

   /**
    * Sets the flag whether an instance should be fetched when parsing a TransferObject.
    * @param bFetchInstance The flag whether an instance should be fetched when parsing a TransferObject to set.
    */
   public void setFetchInstance(boolean bFetchInstance)
   {
      m_bFetchInstance = bFetchInstance;
   }

   /**
    * @return The flag whether an instance should be fetched when parsing a TransferObject.
    */
   public boolean isFetchInstance()
   {
      return m_bFetchInstance;
   }

   /**
    * @see nexj.core.integration.MessageParser#parse(nexj.core.integration.Input, nexj.core.meta.integration.Message)
    */
   public TransferObject parse(Input in, Message message) throws IntegrationException
   {
      try
      {
         if (s_logger.isDebugEnabled())
         {
            s_logger.debug("Parsing object message \"" + message.getName() + "\"");
            s_logger.dump(in);
         }

         m_instanceMap = new HashTab();
         m_processedSet = new HashTab2D();

         TransferObject tobj = (TransferObject)transfer(in.getObject(), message.getRoot());

         GenericServer.auditResponse(m_instanceMap, m_context);

         if (s_logger.isDumpEnabled())
         {
            s_logger.dump("Parse result");
            s_logger.dump(tobj);
         }

         return tobj;
      }
      catch (Exception e)
      {
         throw new IntegrationException("err.integration.object.graph",
            new Object[]{message.getName()}, e);
      }
   }

   /**
    * @see nexj.core.integration.MessageParser#parse(nexj.core.integration.Input, nexj.core.meta.integration.MessageTable)
    */
   public TransferObject parse(Input in, MessageTable table) throws IntegrationException
   {
      if (s_logger.isDebugEnabled())
      {
         s_logger.debug("Identifying and parsing an object message");
         s_logger.dump(in);
      }

      Object obj = in.getObject();
      Message message = null;
      
      if (obj instanceof Instance)
      {
         Metaclass metaclass = ((Instance)obj).getMetaclass();
         List messageList = (List)table.getParserTable();

         for (int i = 0, n = messageList.size(); i != n; ++i)
         {
            Message msg = (Message)messageList.get(i);

            if (((ObjectMessagePartMapping)msg.getRoot().getMapping()).getMetaclass().isUpcast(metaclass))
            {
               message = msg;

               break;
            }
         }
      }

      if (message == null)
      {
         throw new IntegrationException("err.integration.object.unsupportedMessage",
            new Object[]{(obj == null) ? "null" : (obj instanceof Instance) ?
               ((Instance)obj).getLazyMetaclass().getName() : obj.getClass().getName()});
      }

      if (s_logger.isDebugEnabled())
      {
         s_logger.debug("Identified the object message as \"" + message.getName() + "\"");
      }
      
      try
      {
         m_instanceMap = new HashTab();
         m_processedSet = new HashTab2D();
         
         TransferObject tobj = (TransferObject)transfer(in.getObject(), message.getRoot());

         GenericServer.auditResponse(m_instanceMap, m_context);

         if (s_logger.isDumpEnabled())
         {
            s_logger.dump("Parse result");
            s_logger.dump(tobj);
         }

         return tobj;
      }
      catch (Exception e)
      {
         throw new IntegrationException("err.integration.object.graph",
            new Object[]{message.getName()}, e);
      }
   }

   /**
    * Builds the parser table.
    * Disallows two messages if they have the same class mapping. Orders the table
    * starting with derived messages and ending with base messages.
    * 
    * @see nexj.core.integration.MessageParser#initializeMessageTable(nexj.core.meta.integration.MessageTable)
    */
   public void initializeMessageTable(MessageTable table) throws MetadataException
   {
      ArrayList messageList = new ArrayList(table.getMessageCount());

      for (int i = 0, n = table.getMessageCount(); i < n; i++)
      {
         boolean bFound = false;
         Message candidate = table.getMessage(i);

         for (int k = 0, m = messageList.size(); k < m && !bFound; k++)
         {
            Message msg = (Message)messageList.get(k);
            Metaclass candidateClass = ((ObjectMessagePartMapping)candidate.getRoot().getMapping()).getMetaclass();
            Metaclass msgClass = ((ObjectMessagePartMapping)msg.getRoot().getMapping()).getMetaclass();

            if (msgClass == candidateClass)
            {
               throw new IntegrationException("err.integration.object.ambiguousTable",
                  new Object[]{candidate.getName(), msg.getName()});
            }

            // Optimization: reduce table size when a base message is added
            if (msg.isUpcast(candidate) && msg.getDerivation() != Message.DERIVATION_FINAL)
            {
               bFound = true;
            }
            else if (candidate.isUpcast(msg) && candidate.getDerivation() != Message.DERIVATION_FINAL)
            {
               bFound = true;
               messageList.set(k, candidate);
            }
         }

         if (!bFound)
         {
            messageList.add(candidate);
         }
      }

      // Table should be sorted with derived messages first
      Collections.sort(messageList, new Comparator()
      {
         public int compare(Object obj1, Object obj2)
         {
            Message msg1 = (Message)obj1;
            Message msg2 = (Message)obj2;
            Metaclass claz1 = ((ObjectMessagePartMapping)msg1.getRoot().getMapping()).getMetaclass();
            Metaclass claz2 = ((ObjectMessagePartMapping)msg2.getRoot().getMapping()).getMetaclass();

            if (claz1 == claz2)
            {
               return 0;
            }

            return (claz1.isUpcast(claz2)) ? 1 : -1;
         }
      });

      table.setParserTable(messageList);
   }

   /**
    * Converts an object to canonical format.
    * @param obj The object to convert.
    * @param msg The message part metadata.
    * @throws IntegrationException if an error occurs. 
    */
   protected Object transfer(Object obj, MessagePart msg) throws IntegrationException
   {
      if (msg.isCollection())
      {
         if (obj == null)
         {
            if (msg.isRequired())
            {
               throw new IntegrationException("err.integration.minPartCount", new Object[]{msg.getFullPath()});
            }

            return new ArrayList(0);
         }
         
         List instanceList = (List)obj;
         int nCount = instanceList.size();
         
         if (nCount < msg.getMinCount())
         {
            throw new IntegrationException("err.integration.minPartCount", new Object[]{msg.getFullPath()});
         }
         
         if (nCount > msg.getMaxCount())
         {
            throw new IntegrationException("err.integration.maxPartCount", new Object[]{msg.getFullPath()});
         }
         
         List list = new ArrayList(instanceList.size());
         
         for (int i = 0; i < nCount; ++i)
         {
            Object tobj = transfer1(instanceList.get(i), msg);

            if (tobj != null)
            {
               list.add(tobj);
            }
         }
         
         return list;
      }

      return transfer1(obj, msg);
   }
   
   /**
    * Converts one object to canonical format.
    * @param obj The object to convert. Cannot be a collection.
    * @param msg The message part metadata.
    * @throws IntegrationException if an error occurs.
    */
   protected Object transfer1(Object obj, MessagePart msg) throws IntegrationException
   {
      MessagePart processedPart = msg;
      String sMessageName = null;
      Metaclass metaclass;
      Instance instance = (obj instanceof Instance) ? (Instance)obj : null;
      TransferObject srcTobj = (obj instanceof TransferObject) ? (TransferObject)obj : null;
      Message message = null;

      if (instance != null || srcTobj != null)
      {
         if (msg instanceof CompositeMessagePartRef)
         {
            CompositeMessagePartRef ref = (CompositeMessagePartRef)msg;
            CompositeMessagePart referencedPart = ref.getRefPart();

            message = ((ObjectMessagePartMapping)referencedPart.getMapping()).getMessage();
         }
         else if (msg.getParent() == null)
         {
            message = ((ObjectMessagePartMapping)msg.getRoot().getMapping()).getMessage();
         }
      }

      // Process inheritance
      if (message != null)
      {
         metaclass = (instance != null) ? instance.getMetaclass() :
            m_context.getMetadata().getMetaclass(srcTobj.getClassName());

         Message derivedMessage = findDerivedMessage(message, metaclass);

         if (derivedMessage == null)
         {
            throw new IntegrationException("err.integration.object.missingMapping",
               new Object[]{metaclass.getName(), message.getName(), msg.getFullPath()});
         }

         Message.validatePolymorphism(message, derivedMessage, msg);
         msg = derivedMessage.getRoot();
         message = derivedMessage;
         sMessageName = message.getName();
      }

      ObjectMessagePartMapping mapping = (ObjectMessagePartMapping)msg.getMapping();

      if (m_context.isSecure())
      {
         if (mapping.getAttribute() != null)
         {
            mapping.getAttribute().checkReadAccess(m_context.getPrivilegeSet());
         }
         else
         {
            mapping.getMetaclass().checkReadAccess(m_context.getPrivilegeSet());
         }
      }

      if (obj == null)
      {
         if (msg.isRequired())
         {
            throw new IntegrationException("err.integration.minPartCount", new Object[]{msg.getFullPath()});
         }
         
         return null;
      }
      
      if (msg instanceof PrimitiveMessagePart)
      {
         return ((PrimitiveMessagePart)msg).convertValue(obj);
      }

      CompositeMessagePart composite = (CompositeMessagePart)msg;
      int nCount = composite.getPartCount();
      
      if (obj instanceof TransferObject)
      {
         if (isFetchInstance())
         {
            instance = (Instance)RPCUtil.instantiateClean(srcTobj, new HashTab(), m_context);
         }
         else
         {
            TransferObject tobj = new TransferObject(sMessageName, composite.getPartCount());

            tobj.setEventName(srcTobj.getEventName());
            tobj.setOID(srcTobj.getOID());
   
            for (int i = 0; i < nCount; ++i)
            {
               MessagePart part = composite.getPart(i);
               ObjectMessagePartMapping objectMessagePartMapping = (ObjectMessagePartMapping)part.getMapping();
               Attribute attribute = objectMessagePartMapping.getAttribute();
               Object value = null;

               if (attribute != null)
               {
                  value = transfer(srcTobj.findValue(attribute.getName()), part);
               }
               else
               {
                  switch (objectMessagePartMapping.getSystemAttribute())
                  {
                     case ObjectMessagePartMapping.ATTR_OID:
                        value = srcTobj.getOID().toBinary();
                        break;

                     case ObjectMessagePartMapping.ATTR_CLASS:
                        value = srcTobj.getClassName();
                        break;

                     case ObjectMessagePartMapping.ATTR_EVENT:
                        value = srcTobj.getEventName();
                        break;
                  }
               }

               tobj.setValue(part.getName(), value);
            }
   
            return tobj;
         }
      }

      if (m_context.isSecure() && !instance.isReadable())
      {
         return null;
      }

      metaclass = instance.getMetaclass();

      TransferObject tobj = new TransferObject(sMessageName, composite.getPartCount());
      
      if (!mapping.getMetaclass().isUpcast(metaclass))
      {
         throw new IntegrationException("err.integration.object.partClass",
            new Object[]{mapping.getMetaclass().getName(), composite.getFullPath()});
      }

      String sEvent = null;
      boolean bProcessed = m_processedSet.put(instance, processedPart, Boolean.TRUE) != null;
      
      m_instanceMap.put(instance, tobj);
      tobj.setOID(instance.getOID());

      if (srcTobj != null)
      {
         sEvent = srcTobj.getEventName();
      } 
      else
      {
         switch (instance.getState())
         {
            case Instance.NEW:
               sEvent = "create";
               break;

            case Instance.DIRTY:
               sEvent = "update";
               break;

            case Instance.DELETED:
               sEvent = "delete";
               break;
         }
      }

      tobj.setEventName(sEvent);

      for (int i = 0; i < nCount; ++i)
      {
         MessagePart part = composite.getPart(i);
         ObjectMessagePartMapping objectMessagePartMapping = (ObjectMessagePartMapping)part.getMapping();
         Attribute attribute = objectMessagePartMapping.getAttribute();
         Object value = null;

         if (attribute != null)
         {
            if (bProcessed && objectMessagePartMapping.isKey() && !(part instanceof PrimitiveMessagePart))
            {
               throw new IntegrationException("err.integration.object.recursive", new Object[]{msg.getName()});
            }
            
            if (!bProcessed || objectMessagePartMapping.isKey())
            {
               value = transfer(instance.getValue(attribute.getOrdinal()), part);
               tobj.setValue(part.getName(), value);
            }
         }
         else
         {
            switch (objectMessagePartMapping.getSystemAttribute())
            {
               case ObjectMessagePartMapping.ATTR_OID:
                   value = (instance.getOID() == null) ? null : instance.getOID().toBinary();
                   break;

               case ObjectMessagePartMapping.ATTR_CLASS:
                  value = instance.getMetaclass().getName();
                  break;

               case ObjectMessagePartMapping.ATTR_EVENT:
                  value = sEvent;
                  break;
            }

            tobj.setValue(part.getName(), value);
         }
      }

      if (!bProcessed)
      {
         m_processedSet.remove(instance, processedPart);
      }

      return tobj;
   }

   /**
    * Selects a derived message "message" for parsing the given class.
    * @param message The base message to use for parsing the class.
    * @param clazz The class to parse.
    * @return A derived message of "message", or "message" itself.
    */
   public static Message findDerivedMessage(Message message, Metaclass clazz)
   {
      if (clazz != null && message.getDerivation() != Message.DERIVATION_FINAL)
      {
         ObjectMessagePartMapping mapping = (ObjectMessagePartMapping)message.getRoot().getMapping();
         Message result = mapping.findMessage(clazz);

         if (result == null)
         {
            return findDerivedMessage(message, clazz.getBase());
         }

         return (message.isUpcast(result)) ? result : null;
      }

      if (((ObjectMessagePartMapping)message.getRoot().getMapping()).getMetaclass().isUpcast(clazz))
      {
         return message;
      }

      return null;
   }
}

