// https://searchcode.com/api/result/74211899/

/*******************************************************************************
 * Copyright (c) 2011, 2012 SunGard CSA LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SunGard CSA LLC - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.stardust.engine.core.persistence.jdbc;

import static org.eclipse.stardust.engine.core.runtime.beans.DataValueBean.TABLE_NAME;
import static org.eclipse.stardust.engine.core.runtime.beans.ProcessInstanceBean.DEFAULT_ALIAS;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.stardust.common.Assert;
import org.eclipse.stardust.common.Pair;
import org.eclipse.stardust.common.Unknown;
import org.eclipse.stardust.common.config.Parameters;
import org.eclipse.stardust.common.error.ConcurrencyException;
import org.eclipse.stardust.common.error.InternalException;
import org.eclipse.stardust.common.error.PublicException;
import org.eclipse.stardust.common.log.LogManager;
import org.eclipse.stardust.common.log.Logger;
import org.eclipse.stardust.engine.api.query.CasePolicy;
import org.eclipse.stardust.engine.core.persistence.AndTerm;
import org.eclipse.stardust.engine.core.persistence.Column;
import org.eclipse.stardust.engine.core.persistence.ComparisonTerm;
import org.eclipse.stardust.engine.core.persistence.DefaultPersistentVector;
import org.eclipse.stardust.engine.core.persistence.DeleteDescriptor;
import org.eclipse.stardust.engine.core.persistence.FieldRef;
import org.eclipse.stardust.engine.core.persistence.FieldRefResolver;
import org.eclipse.stardust.engine.core.persistence.Function;
import org.eclipse.stardust.engine.core.persistence.Functions;
import org.eclipse.stardust.engine.core.persistence.InsertDescriptor;
import org.eclipse.stardust.engine.core.persistence.Join;
import org.eclipse.stardust.engine.core.persistence.JoinElement;
import org.eclipse.stardust.engine.core.persistence.Joins;
import org.eclipse.stardust.engine.core.persistence.MultiPartPredicateTerm;
import org.eclipse.stardust.engine.core.persistence.Operator;
import org.eclipse.stardust.engine.core.persistence.OrTerm;
import org.eclipse.stardust.engine.core.persistence.OrderCriterion;
import org.eclipse.stardust.engine.core.persistence.PersistenceController;
import org.eclipse.stardust.engine.core.persistence.Persistent;
import org.eclipse.stardust.engine.core.persistence.PersistentVector;
import org.eclipse.stardust.engine.core.persistence.PhantomException;
import org.eclipse.stardust.engine.core.persistence.PredicateTerm;
import org.eclipse.stardust.engine.core.persistence.Predicates;
import org.eclipse.stardust.engine.core.persistence.QueryDescriptor;
import org.eclipse.stardust.engine.core.persistence.QueryExtension;
import org.eclipse.stardust.engine.core.persistence.ResultIterator;
import org.eclipse.stardust.engine.core.persistence.jdbc.TypeDescriptor.CompositeKey;
import org.eclipse.stardust.engine.core.runtime.beans.removethis.KernelTweakingProperties;


/**
 * @author ubirkemeyer
 * @version $Revision$
 */
public class DmlManager
{
   private static final Logger trace = LogManager.getLogger(DmlManager.class);

   private static final String SQL_NULL = "NULL";
   private static final int DEFAULT_STMT_BUFFER_SIZE = 200;

   private final TypeDescriptor typeDescriptor;
   private final DBDescriptor dbDescriptor;
   private final SqlUtils sqlUtils;

   private String insertStmtPrefix;
   private String preparedInsertStmt;

   public DmlManager(SqlUtils sqlUtils, TypeDescriptor descriptor, DBDescriptor dbDescriptor)
   {
      this.typeDescriptor = descriptor;
      this.dbDescriptor = dbDescriptor;
      this.sqlUtils = sqlUtils;

      initializeStatementStubs();
   }

   public static String getSQLValue(Class type, Object value, DBDescriptor dbDescriptor)
   {
      if (value == null)
      {
         if (type == java.util.Date.class)
         {
            return "0";
         }
         return SQL_NULL;
      }

      if (type == Integer.class || type == Integer.TYPE || type == Long.class ||
            type == Long.TYPE)
      {
         return value.toString();
      }
      else if (type == java.sql.Date.class)
      {
         final Parameters params = Parameters.instance();
         DateFormat fmtDate = new SimpleDateFormat(params.getString(
               "Carnot.Db.DateFormat", "yyyy-MM-dd"));

         return MessageFormat.format(params.getString(
               "Carnot.Db.ToDbmsDateFuntion", "TO_DATE(''{0}'', ''YYYY-MM-DD'')"),
               new Object[] {fmtDate.format((java.sql.Date) value)});
      }
      else if (type == Date.class)
      {
         return new Long(((Date) value).getTime()).toString();
      }
      else if (type == Float.class || type == Float.TYPE)
      {
         return value.toString();
      }
      else if (type == Double.class || type == Double.TYPE)
      {
         Double doubleValue = (Double) value;
         if(doubleValue == Unknown.DOUBLE)
         {
            return SQL_NULL;
         }
         
         Pair<Double, Double> valueBorders = dbDescriptor
               .getNumericSQLTypeValueBorders(Double.class);
         if (doubleValue < valueBorders.getFirst())
         {
            doubleValue = valueBorders.getFirst();
         }
         else if (doubleValue > valueBorders.getSecond())
         {
            doubleValue = valueBorders.getSecond();
         }

         return doubleValue.toString();
      }
      else if (type == String.class)
      {
         String string = (String) value;
         int stringLength = string.length();
         if (DBMSKey.SYBASE == dbDescriptor.getDbmsKey() && isSybaseEmptyString(string))
         {
            // Better to write a null which emulates Oracles behavior.
            return SQL_NULL;
         }

         int sqPos = string.indexOf('\'');
         if ( -1 != sqPos)
         {
            StringBuffer buffer = new StringBuffer(2 * stringLength);
            buffer.append(string);
            while (sqPos < buffer.length())
            {
               if ('\'' == buffer.charAt(sqPos))
               {
                  buffer.insert(sqPos++, '\'');
               }
               ++sqPos;
            }
            return "'" + buffer.toString() + "'";
         }
         else
         {
            return "'" + string + "'";
         }
      }
      else
      {
         throw new InternalException(
               "Illegal type for SQL mapping: '" + type.getName() + "'");
      }
   }

   /**
    * Maps Java field values to valid SQL column values. Handles pseudo NULL
    * values (@see org.eclipse.stardust.common.Unknown) for literals.
    */
   public static void setSQLValue(PreparedStatement statement, int index, Class type,
         Object value, DBDescriptor dbDescriptor)
   {
      try
      {
         if (type == java.sql.Date.class)
         {
            statement.setDate(index, ((java.sql.Date) value));
            return;
         }
         else if (type == java.util.Date.class)
         {
            if (value == null)
            {
               statement.setLong(index, 0);
            }
            else
            {
               statement.setLong(index, ((Date) value).getTime());
            }
            return;
         }

         if (value == null)
         {
            statement.setNull(index, mapJavaTypeToSQLTypeID(type));
            return;
         }

         if (type == Integer.class || type == Integer.TYPE)
         {
            int intValue = ((Integer) value).intValue();

            if (intValue == Unknown.INT)
            {
               statement.setNull(index, mapJavaTypeToSQLTypeID(Integer.TYPE));
            }
            else
            {
               statement.setInt(index, intValue);
            }
         }
         else if (type == Long.class || type == Long.TYPE)
         {
            long longValue = ((Long) value).longValue();

            if (longValue == Unknown.LONG)
            {
               statement.setNull(index, mapJavaTypeToSQLTypeID(Long.TYPE));
            }
            else
            {
               statement.setLong(index, longValue);
            }
         }
         else if (type == Float.class || type == Float.TYPE)
         {
            float floatValue = ((Float) value).floatValue();

            if (floatValue == Unknown.FLOAT)
            {
               statement.setNull(index, mapJavaTypeToSQLTypeID(Float.TYPE));
            }
            else
            {
               statement.setFloat(index, floatValue);
            }
         }
         else if (type == Double.class || type == Double.TYPE)
         {
            double doubleValue = ((Double) value).doubleValue();

            if (doubleValue == Unknown.DOUBLE)
            {
               statement.setNull(index, mapJavaTypeToSQLTypeID(Double.TYPE));
            }
            else
            {
               Pair<Double, Double> valueBorders = dbDescriptor
                     .getNumericSQLTypeValueBorders(Double.class);
               if (doubleValue < valueBorders.getFirst())
               {
                  doubleValue = valueBorders.getFirst();
               }
               else if (doubleValue > valueBorders.getSecond())
               {
                  doubleValue = valueBorders.getSecond();
               }
               statement.setDouble(index, doubleValue);
            }
         }
         else if (type == String.class)
         {
            String string = (String) value;
            // TODO (ab) should look if this field is of type CLOB (?)

            int stringLength = string.length();
            if (stringLength > 2000)
            {
               StringReader stringReader = new StringReader(string.toString());
               statement.setCharacterStream(index, stringReader, stringLength);
            }
            else
            {
               if (DBMSKey.SYBASE == dbDescriptor.getDbmsKey()
                     && isSybaseEmptyString(string))
               {
                  // Better to write a null which emulates Oracles behavior.
                  statement.setNull(index, mapJavaTypeToSQLTypeID(type));
               }
               else
               {
                  statement.setString(index, (String) value);
               }
            }
         }
         else
         {
            throw new InternalException(
                  "Illegal type for SQL mapping: '" + type.getName() + "'");
         }
      }
      catch (SQLException x)
      {
         throw new InternalException("Exception during value mapping.", x);
      }
   }

   /**
    * Maps the content of a result set to the appropriate java object.
    */
   public static Object getJavaValue(Class type, int fieldLength, ResultSet resultSet,
         int index, boolean translateNullToUnknown, boolean secret)
   {
      try
      {
         final Object objectValue = resultSet.getObject(index);

         if (trace.isDebugEnabled())
         {
            trace.debug("column[" + index + "] = " + (secret ? "xxxxxxxx" : objectValue));
         }

         if ((null == objectValue) && resultSet.wasNull() && !translateNullToUnknown)
         {
            // yield null to callers knowing how to handle it

            return null;
         }

         if (type == Integer.TYPE || type == Integer.class)
         {
            if (objectValue == null)
            {
               return new Integer(Unknown.INT);
            }
            else if (objectValue instanceof Number)
            {
               return new Integer(((Number) objectValue).intValue());
            }
            return new Integer(resultSet.getInt(index));
         }
         else if (type == Long.TYPE || type == Long.class)
         {
            if (objectValue == null)
            {
               return new Long(Unknown.LONG);
            }
            else if (objectValue instanceof Number)
            {
               return new Long(((Number) objectValue).longValue());
            }
            return new Long(resultSet.getLong(index));
         }
         else if (type == Float.TYPE || type == Float.class)
         {
            if (objectValue == null)
            {
               return new Float(Unknown.FLOAT);
            }
            else if (objectValue instanceof Number)
            {
               return new Float(((Number) objectValue).floatValue());
            }
            return new Float(resultSet.getFloat(index));
         }
         else if (type == Double.TYPE || type == Double.class)
         {
            if (objectValue == null)
            {
               return new Double(Unknown.DOUBLE);
            }
            else if (objectValue instanceof Number)
            {
               return new Double(((Number) objectValue).doubleValue());
            }
            return new Double(resultSet.getDouble(index));
         }
         else if (type == String.class)
         {
            if (null != objectValue)
            {
               if (fieldLength == Integer.MAX_VALUE)
               {
                  // This is a CLOB
                  int clobReadBufferSize = Parameters.instance().getInteger(
                        KernelTweakingProperties.CLOB_READ_BUFFER_SIZE, 2000);

                  if (0 >= clobReadBufferSize)
                  {
                     Clob clob = resultSet.getClob(index);
                     return clob.getSubString(1L, (int) clob.length());
                  }
                  else
                  {
                     Reader reader = objectValue instanceof Clob
                        ? ((Clob) objectValue).getCharacterStream()
                        : resultSet.getCharacterStream(index);;
                     StringWriter stringWriter = new StringWriter(clobReadBufferSize);
                     char[] buffer = new char[clobReadBufferSize];
                     int returnedBytes;
                     while ( -1 != (returnedBytes = reader.read(buffer)))
                     {
                        stringWriter.write(buffer, 0, returnedBytes);
                     }
                     return stringWriter.getBuffer().toString();
                  }
               }
               else
               {
                  // good old string data
                  if (objectValue instanceof String)
                  {
                     return (String) objectValue;
                  }
                  return resultSet.getString(index);
               }
            }
            else
            {
               return null;
            }
         }
         else if (type == java.sql.Date.class)
         {
            return resultSet.getDate(index);
         }
         else if (type == java.util.Date.class)
         {
            long result = resultSet.getLong(index);
            if (result <= 0)
            {
               return null;
            }
            return new Date(result);
         }
      }
      catch (SQLException x)
      {
         throw new InternalException("Failed to retrieve Java value from result set.", x);
      }
      catch (IOException x)
      {
         throw new InternalException("Failed to retrieve Java value from result set.", x);
      }

      throw new InternalException("Illegal type for SQL mapping: '" + type.getName()
            + "'.");
   }

   /**
    * Maps Java types to constants of java.sql.Types.
    *
    * @todo check, whether this method is already provided by sql package
    */
   public static int mapJavaTypeToSQLTypeID(Class type)
   {
      Assert.isNotNull(type);

      if (type == Integer.TYPE)
      {
         return java.sql.Types.INTEGER;
      }
      else if (type == java.sql.Date.class)
      {
         return java.sql.Types.DATE;
      }
      else if (type == Long.TYPE || type == Long.class || type == Date.class)
      {
         return java.sql.Types.BIGINT;
      }
      else if (type == Float.TYPE)
      {
         return java.sql.Types.FLOAT;
      }
      else if (type == Double.TYPE )
      {
         return java.sql.Types.DOUBLE;
      }
      else if (type == String.class)
      {
         return java.sql.Types.VARCHAR;
      }
      else
      {
         throw new InternalException(
               "Illegal type for SQL mapping: '" + type.getName() + "'");
      }
   }

   public String getSQLValue(Class type, Object value)
   {
      return getSQLValue(type, value, dbDescriptor);
   }

   public void setSQLValue(PreparedStatement statement, int index, Class type,
         Object value)
   {
      setSQLValue(statement, index, type, value, dbDescriptor);
   }

   private void initializeStatementStubs()
   {
      // insert statement stubs
      StringBuffer stmt = new StringBuffer();

      stmt.append("INSERT INTO ");
      sqlUtils.appendTableRef(stmt, typeDescriptor, false);

      stmt.append(" (");

      int nSlots = 0;

      List<FieldDescriptor> persistentFields = typeDescriptor.getPersistentFields();
      for (int n = 0; n < persistentFields.size(); ++n)
      {
         FieldDescriptor field = persistentFields.get(n);
         if (!(dbDescriptor.supportsIdentityColumns()
               && typeDescriptor.requiresPKCreation()
               && typeDescriptor.isPkField(field.getField())))
         {
            if (nSlots > 0)
            {
               stmt.append(", ");
            }

            field.getField().setAccessible(true);
            final String fieldName = field.getField().getName();
            stmt.append(dbDescriptor.quoteIdentifier(fieldName));

            ++nSlots;
         }
      }

      List<LinkDescriptor> links = typeDescriptor.getLinks();
      for (int m = 0; m < links.size(); ++m)
      {
         LinkDescriptor link = links.get(m);

         Field fkField = link.getFkField();
         fkField.setAccessible(true);

         if (nSlots > 0)
         {
            stmt.append(", ");
         }

         final String fieldName = link.getField().getName();
         stmt.append(dbDescriptor.quoteIdentifier(fieldName));
         ++nSlots;
      }

      stmt.append(")");

      this.insertStmtPrefix = stmt.toString();

      stmt.append(" VALUES (");

      nSlots = 0;

      for (int n = 0; n < persistentFields.size(); ++n)
      {
         FieldDescriptor descriptor = (FieldDescriptor) persistentFields.get(n);
         if (!(dbDescriptor.supportsIdentityColumns()
               && typeDescriptor.requiresPKCreation()
               && typeDescriptor.isPkField(descriptor.getField())))
         {
            if (nSlots > 0)
            {
               stmt.append(", ");
            }

            if (null != descriptor.getFieldEncryptFunction())
            {
               stmt.append(descriptor.getFieldEncryptFunction());
               stmt.append("(?, '");
               stmt.append(typeDescriptor.getEncryptKey());
               stmt.append("')");
            }
            else
            {
               stmt.append("?");
            }
            ++nSlots;
         }
      }

      for (int m = 0; m < links.size(); ++m)
      {
         if (nSlots > 0)
         {
            stmt.append(", ");
         }

         stmt.append("?");
         ++nSlots;
      }

      stmt.append(")");

      this.preparedInsertStmt = stmt.toString();
   }

   private String getPKPredicateStatementString(String prefix, Persistent persistent)
   {
      return getPKPredicateStatementString(prefix, persistent, true);
   }

   private String getPKPredicateStatementString(String prefix, Persistent persistent,
         boolean useTableAlias)
   {
      Field[] pkFields = typeDescriptor.getPkFields();

      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
      buffer.append(prefix).append(" WHERE ");

      if (1 < pkFields.length)
      {
         buffer.append("(");
      }
      for (int i = 0; i < pkFields.length; i++ )
      {
         if (0 < i)
         {
            buffer.append(" AND ");
         }
         sqlUtils.appendFieldRef(buffer, typeDescriptor.fieldRef(pkFields[i].getName()),
               useTableAlias);

         try
         {
            buffer.append(" = ").append(
                  getSQLValue(pkFields[i].getType(), pkFields[i].get(persistent)));
         }
         catch (Exception e)
         {
            throw new InternalException(e);
         }
      }
      if (1 < pkFields.length)
      {
         buffer.append(")");
      }

      if (trace.isDebugEnabled())
      {
         trace.debug("Statement: " + buffer.toString());
      }

      return buffer.toString();
   }

   private String extendPKPredicateStatement(String prefix, Connection connection, int offset, boolean useTableAlias)
   {
      Field[] pkFields = typeDescriptor.getPkFields();

      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
      buffer.append(prefix).append(" WHERE ");

      if (1 < pkFields.length)
      {
         buffer.append("(");
      }
      for (int i = 0; i < pkFields.length; i++ )
      {
         if (0 < i)
         {
            buffer.append(" AND ");
         }
         sqlUtils.appendFieldRef(buffer, typeDescriptor.fieldRef(pkFields[i].getName()),
               useTableAlias);
         buffer.append(" = ?");
         }
      if (1 < pkFields.length)
      {
         buffer.append(")");
      }

      if (trace.isDebugEnabled())
      {
         trace.debug("Statement: " + buffer.toString());
      }

      return buffer.toString();
   }

   private void setPkFields(Persistent persistent, PreparedStatement statement, int offset) throws Exception
   {
      Field[] pkFields = typeDescriptor.getPkFields();
      for (int i = 0; i < pkFields.length; i++ )
      {
         setSQLValue(statement, offset + 1 + i, pkFields[i].getType(),
               pkFields[i].get(persistent));
      }
   }

   private StringBuffer buildValueExpression(Operator op, FieldRef lhsField, Object value, List<Pair<Class<?>, ?>> bindValueList,
         boolean useLiteralsWhereAppropriate)
   {    
      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      if (value instanceof QueryDescriptor)
      {
         QueryDescriptor subQuery = (QueryDescriptor) value;
         buffer.append("(")
               .append(prepareSelectStatement(subQuery, false, bindValueList, useLiteralsWhereAppropriate))
               .append(")");
      }
      else
      {
         boolean useBindValues = null != bindValueList;
         
         //performance optimization for mixed mode: if comparison value starts with '%' 
         //then value will not be bound as prepared statement parameter but set as literal in SQL
         if(useLiteralsWhereAppropriate)
         {
            if(op.equals(Operator.LIKE) && value instanceof String)
            {
               String stringValue = (String) value;
               if(stringValue.startsWith("%"))
               {  
                  useBindValues = false;
               }
            }
         }
         
         if(lhsField.isIgnorePreparedStatements())
         {
            useBindValues = false;
         }
               
         if (value instanceof FieldRef)
         {
            sqlUtils.appendFieldRef(buffer, (FieldRef) value);
         }
         else if ((useLiteralsWhereAppropriate && useBindValues && !lhsField.isLiteralField()) ||
               (useBindValues && !useLiteralsWhereAppropriate))
         {
            String concatenationToken = "";

            if (value instanceof List)
            {
               List<?> valueList = (List<?>) value;

               if (!valueList.isEmpty())
               {
                  buffer.append("(");

                  for (int n = 0; n < valueList.size(); ++n)
                  {
                     buffer.append(concatenationToken).append("?");
                     Object listItem = valueList.get(n);
                     bindValueList.add(new Pair<Class<?>, Object>(listItem.getClass(), listItem));

                     concatenationToken = ", ";
                  }

                  buffer.append(")");
               }
            }
            else if (value instanceof Integer)
            {
               buffer.append(concatenationToken).append("?");
               bindValueList.add(new Pair<Class<?>, Integer>(Integer.class, (Integer)value));
            }
            else if (value instanceof Long)
            {
               buffer.append(concatenationToken).append("?");
               bindValueList.add(new Pair<Class<?>, Long>(Long.class, (Long)value));
            }
            else if (value instanceof Float)
            {
               buffer.append(concatenationToken).append("?");
               bindValueList.add(new Pair<Class<?>, Float>(Float.class, (Float)value));
            }
            else if (value instanceof Double)
            {
               buffer.append(concatenationToken).append("?");
               bindValueList.add(new Pair<Class<?>, Double>(Double.class, (Double)value));
            }
            else if (value instanceof String)
            {
               buffer.append(concatenationToken).append("?");
               bindValueList.add(new Pair<Class<?>, String>(String.class, (String)value));
            }
         }
         else
         {
            String concatenationToken = "";

            if (value instanceof List)
            {
               List<?> valueList = (List<?>) value;

               if (!valueList.isEmpty())
               {
                  buffer.append("(");

                  for (int n = 0; n < valueList.size(); ++n)
                  {
                     Object listValue = valueList.get(n);
                     buffer.append(concatenationToken).append(
                           getSQLValue((null != listValue) ? listValue.getClass() : null,
                                 listValue));

                     concatenationToken = ", ";
                  }

                  buffer.append(")");
               }
            }
            else
            {
               buffer.append(getSQLValue((null != value) ? value.getClass() : null, value));
            }
         }
      }

      return buffer;
   }

   /**
    * Converts a predicate to a SQL-Syntax representation which can be used in a
    * SQL-WHERE-Clause.
    *
    * @param predicateTerm
    *           Tree of predicates
    * @param bindValues
    *           list of bind variables to be used with prepared statements
    * @param setDefaultAlias
    *           hint whether column names without table alias qualification should
    *           get the default alias
    *
    * @return Predicate
    */
   private StringBuffer buildWhereClause(PredicateTerm predicateTerm, List<Pair<Class<?>, ?>> bindValues,
         FieldRefResolver fieldRefResolver, boolean useLiteralsWhereAppropriate)
   {
      return buildWhereClause(predicateTerm, bindValues, fieldRefResolver, false,
            useLiteralsWhereAppropriate);
   }

   private StringBuffer buildWhereClause(PredicateTerm predicateTerm, List<Pair<Class<?>, ?>> bindValues,
         FieldRefResolver fieldRefResolver, boolean setDefaultAlias, boolean useLiteralsWhereAppropriate)
   {
      StringBuffer whereClause = null;
      if (predicateTerm instanceof ComparisonTerm)
      {
         whereClause = buildSqlFragment((ComparisonTerm) predicateTerm, bindValues,
               fieldRefResolver, setDefaultAlias, useLiteralsWhereAppropriate);
      }
      else if (predicateTerm instanceof AndTerm)
      {
         whereClause = buildSqlFragment((AndTerm) predicateTerm, bindValues, " AND ",
               fieldRefResolver, setDefaultAlias, useLiteralsWhereAppropriate);
      }
      else if (predicateTerm instanceof OrTerm)
      {
         whereClause = buildSqlFragment((OrTerm) predicateTerm, bindValues, " OR ",
               fieldRefResolver, setDefaultAlias, useLiteralsWhereAppropriate);
      }
      else if (null != predicateTerm)
      {
         throw new InternalException("Do not know how to handle type "
               + predicateTerm.getClass().toString()
               + " (extending interface PredicateTerm)");
      }

      return (null != whereClause) ? whereClause : new StringBuffer(
            DEFAULT_STMT_BUFFER_SIZE);
   }

   private StringBuffer buildSqlFragment(ComparisonTerm term, List<Pair<Class<?>, ?>> bindValues,
         FieldRefResolver fieldRefResolver, boolean setDefaultAlias, boolean useLiteralsWhereAppropriate)
   {
      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      Operator op = term.getOperator();

      // convert single element IN expressions into IS_EQUAL expressions to reduce clutter
      Object valueExpr = term.getValueExpr();
      if (Operator.IN.equals(op) && (valueExpr instanceof Collection)
            && (1 == ((Collection) valueExpr).size()))
      {
         op = Operator.IS_EQUAL;
         valueExpr = ((Collection) valueExpr).iterator().next();
      }

      if (valueExpr instanceof FieldRef)
      {
         // overwrite valueExpr with newly resolved FieldRef for correct aliases.
         final FieldRef rhsField = (FieldRef) valueExpr;
         valueExpr = (null != fieldRefResolver)
               ? fieldRefResolver.resolveFieldRef(rhsField)
               : rhsField;
      }

      FieldRef lhsField = (null != fieldRefResolver)
            ? fieldRefResolver.resolveFieldRef(term.getLhsField())
            : term.getLhsField();

      sqlUtils.appendFieldRef(buffer, lhsField);
      buffer.append(" ").append(op.toString());

      if (op.isBinary())
      {
         buffer.append(" ").append(buildValueExpression(op, lhsField, valueExpr, bindValues,
               useLiteralsWhereAppropriate));
      }
      else if (op.isTernary())
      {
         String secondOp = ((Operator.Ternary) op).getSecondOperator();
         Pair pair = (Pair) valueExpr;

         buffer.append(" ").append(buildValueExpression(op, lhsField, pair.getFirst(), bindValues,
               useLiteralsWhereAppropriate));
         buffer.append(" ").append(secondOp);
         buffer.append(" ").append(buildValueExpression(op, lhsField, pair.getSecond(), bindValues,
               useLiteralsWhereAppropriate));
      }

      return buffer;
   }

   private StringBuffer buildSqlFragment(MultiPartPredicateTerm term, List<Pair<Class<?>, ?>> bindValues,
         String partConcatToken, FieldRefResolver fieldRefResolver,
         boolean setDefaultAlias, boolean useLiteralsWhereAppropriate)
   {
      StringBuffer buffer = null;

      if (!term.getParts().isEmpty())
      {
         buffer = new StringBuffer(4 * DEFAULT_STMT_BUFFER_SIZE);
         if (1 < term.getParts().size())
         {
            buffer.append("(");
         }

         String concatToken = "";
         for (Iterator<PredicateTerm> i = term.getParts().iterator(); i.hasNext();)
         {
            StringBuffer fragment = buildWhereClause(i.next(),
                  bindValues, fieldRefResolver, setDefaultAlias, useLiteralsWhereAppropriate);

            if ((null != fragment) && (0 < fragment.length()))
            {
               buffer.append(concatToken).append(fragment);
               concatToken = partConcatToken;
            }
         }

         if (1 < term.getParts().size())
         {
            buffer.append(")");
         }
      }

      return buffer;
   }

   private void applyJoins(Joins joins, StringBuffer whereClause,
         StringBuffer fromClause, List<Pair<Class<?>, ?>> bindValueList, FieldRefResolver fieldRefResolver,
         boolean setDefaultAlias, boolean useLiteralsWhereAppropriate, String selectAlias)
   {
      boolean useAnsiJoins = ((Session) SessionFactory
            .getSession(SessionFactory.AUDIT_TRAIL)).getDBDescriptor().useAnsiJoins();

      Set<Join> appliedJoins = new HashSet<Join>();

      for (Iterator<Join> joinItr = joins.iterator(); joinItr.hasNext();)
      {
         applyJoin(joinItr.next(), whereClause, fromClause, bindValueList,
               useAnsiJoins, appliedJoins, fieldRefResolver, setDefaultAlias,
               useLiteralsWhereAppropriate, selectAlias);
      }
   }

   private void applyJoin(Join join, StringBuffer whereClause, StringBuffer fromClause,
         List<Pair<Class<?>, ?>> bindValues, boolean useAnsiJoins, Set<Join> appliedJoins,
         FieldRefResolver fieldRefResolver, boolean setDefaultAlias,
         boolean useLiteralsWhereAppropriate, String selectAlias)
   {
      if (!appliedJoins.contains(join))
      {
         appliedJoins.add(join);

         if (null != join.getDependency()
               && !appliedJoins.contains(join.getDependency()))
         {
            applyJoin(join.getDependency(), whereClause, fromClause, bindValues,
                  useAnsiJoins, appliedJoins, fieldRefResolver, setDefaultAlias,
                  useLiteralsWhereAppropriate, selectAlias);
         }

         if (useAnsiJoins)
         {
            if (join.isRequired())
            {
               fromClause.append(" INNER JOIN ");
            }
            else
            {
               fromClause.append(" LEFT OUTER JOIN ");
            }

            sqlUtils.appendTableRef(fromClause, join);

            fromClause.append(" ON (");

            boolean first = true;
            for (Iterator<JoinElement> i = join.getJoinConditions().iterator(); i.hasNext();)
            {
               final JoinElement joinElement = i.next();
               final Pair<FieldRef, ?> joinCondition = joinElement.getJoinCondition();
               final FieldRef lhsField = fieldRefResolver.resolveFieldRef((FieldRef) joinCondition.getFirst());
               final Object rhs = joinCondition.getSecond();

               if ( !first)
               {
                  switch (joinElement.getJoinConditionType())
                  {
                  case OR:
                     fromClause.append(" OR ");
                     break;
                  case AND:
                  default:
                     fromClause.append(" AND ");
                     break;
                  }
               }

               // Data_Value prefetch join needs to use the custom selectAlias instead of default alias.
               if (DEFAULT_ALIAS.equals(lhsField.getType()
                     .getTableAlias())
                     && TABLE_NAME.equals(join.getRhsTableDescriptor()
                           .getTableName()))
               {
                  sqlUtils.appendFieldRef(fromClause, lhsField, selectAlias);
               }
               else
               {
                  sqlUtils.appendFieldRef(fromClause, lhsField);
               }
               fromClause.append(" = ");
               if (rhs instanceof FieldRef)
               {
                  final FieldRef rhsField = fieldRefResolver.resolveFieldRef((FieldRef) rhs);

                  sqlUtils.appendFieldRef(fromClause, rhsField);
               }
               else if (rhs instanceof String)
               {
                  fromClause.append(rhs);
               }
               if (first)
               {
                  first = false;
               }
            }

            if (null != join.getRestriction())
            {
               AndTerm joineePredicate = join.getRestriction();
               fromClause.append(" AND ")
                     .append(buildSqlFragment(joineePredicate, bindValues, " AND ",
                           fieldRefResolver, setDefaultAlias, useLiteralsWhereAppropriate));
            }
            fromClause.append(")");
         }
         else
         {
            fromClause.append(", ");
            sqlUtils.appendTableRef(fromClause, join);

            if (0 < whereClause.length())
            {
               whereClause.append(" AND ");
            }

            String joinToken = "";
            for (Iterator<JoinElement> i = join.getJoinConditions().iterator(); i.hasNext();)
            {
               final JoinElement joinElement = i.next();
               final Pair<FieldRef, ?> joinCondition = joinElement.getJoinCondition();
               final FieldRef lhsField = (FieldRef) joinCondition.getFirst();
               final FieldRef rhsField = (FieldRef) joinCondition.getSecond();

               whereClause.append(joinToken);
               switch (joinElement.getJoinConditionType())
               {
               case OR:
                  joinToken = " OR ";
                  break;
               case AND:
               default:
                  joinToken = " AND ";
                  break;
               }

               // TODO
               sqlUtils.appendFieldRef(whereClause, lhsField);
               whereClause.append(" = ");
               sqlUtils.appendFieldRef(whereClause, rhsField);
            if (!join.isRequired())
            {
               whereClause.append(" (+)");
            }
            }

            if (null != join.getRestriction())
            {
               AndTerm joineePred = join.getRestriction();
               StringBuffer fragment;
               if (join.isRequired())
               {
                  fragment = buildSqlFragment(joineePred, bindValues, " AND ",
                        fieldRefResolver, setDefaultAlias, useLiteralsWhereAppropriate);
               }
               else
               {
                  fragment = new StringBuffer(100 * joineePred.getParts().size());
                  for (Iterator<PredicateTerm> i = joineePred.getParts().iterator(); i.hasNext();)
                  {
                     PredicateTerm part = i.next();
                     if (part instanceof ComparisonTerm)
                     {
                        ComparisonTerm compTerm = (ComparisonTerm) part;
                        if (0 < fragment.length())
                        {
                           fragment.append(" AND ");
                        }
                        fragment.append(buildSqlFragment(Predicates.orTerm(joineePred,
                              Predicates.isNull(compTerm.getLhsField())), bindValues, " OR ",
                              fieldRefResolver, setDefaultAlias, useLiteralsWhereAppropriate));
                     }
                     else
                     {
                        throw new InternalException("Too complex OUTER JOIN predicate: "
                              + joineePred);
                     }
                  }
               }

               if ((null != fragment) && (0 < fragment.length()))
               {
                  whereClause.append(" AND ").append(fragment);
               }
            }
         }
      }
   }

   public String getInsertRowStatementString(Persistent persistent)
   {
      try
      {
         StringBuffer result = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
         result.append(insertStmtPrefix);

         result.append(" VALUES (");

         int nSlots = 0;

         List<FieldDescriptor> persistentFields = typeDescriptor.getPersistentFields();
         for (int n = 0; n < persistentFields.size(); ++n)
         {
            FieldDescriptor descriptor = persistentFields.get(n);
            if (!(dbDescriptor.supportsIdentityColumns()
                  && typeDescriptor.requiresPKCreation()
                  && typeDescriptor.isPkField(descriptor.getField())))
            {
               if (nSlots > 0)
               {
                  result.append(", ");
               }

               String value = getSQLValue(descriptor.getField().getType(), descriptor
                     .getField().get(persistent));
               if (descriptor.getFieldEncryptFunction() != null)
               {
                  result.append(descriptor.getFieldEncryptFunction());
                  result.append("(").append(value).append(", '").append(
                        typeDescriptor.getEncryptKey()).append("')");
               }
               else
               {
                  result.append(value);
               }
               ++nSlots;
            }
         }

         List<LinkDescriptor> links = typeDescriptor.getLinks();
         for (int m = 0; m < links.size(); ++m)
         {
            LinkDescriptor link = links.get(m);

            Object linkedObject = link.getField().get(persistent);

            Field fkField = link.getFkField();

            fkField.setAccessible(true);

            if (nSlots > 0)
            {
               result.append(", ");
            }

            if (linkedObject != null)
            {
               result.append(getSQLValue(fkField.getType(), fkField.get(linkedObject)));
            }
            else
            {
               result.append(getSQLValue(fkField.getType(), null));
            }
            ++nSlots;
         }

         result.append(")");

         if (trace.isDebugEnabled())
         {
            trace.debug(result.toString());
         }

         return result.toString();
      }
      catch (IllegalAccessException x)
      {
         throw new InternalException(x);
      }
   }

   /**
    * @deprecated
    */
   public BatchStatementWrapper prepareInsertRowStatement(Connection connection,
         Persistent persistent)
   {
      return prepareInsertRowStatement(connection, persistent, false, true);
   }

   public BatchStatementWrapper prepareInsertRowStatement(Connection connection,
         Persistent persistent, boolean useJdbcReturnGeneratedKeys, boolean useBatchStatement)
   {
      return prepareInsertRowStatement(connection, Collections.singletonList(persistent),
            useJdbcReturnGeneratedKeys, useBatchStatement);
   }

   /**
    * @deprecated
    */
   public BatchStatementWrapper prepareInsertRowStatement(Connection connection)
   {
      return prepareInsertRowStatement(connection, false);
   }

   public BatchStatementWrapper prepareInsertRowStatement(Connection connection,
         boolean useJdbcReturnGeneratedKeys)
   {
      try
      {
         return new BatchStatementWrapper(preparedInsertStmt, //
               useJdbcReturnGeneratedKeys
                     ? connection.prepareStatement(preparedInsertStmt,
                           Statement.RETURN_GENERATED_KEYS)
                     : connection.prepareStatement(preparedInsertStmt));
      }
      catch (SQLException x)
      {
         throw new InternalException("Batch statement: " + preparedInsertStmt, x);
      }
   }

   /**
    * @deprecated
    */
   public BatchStatementWrapper prepareInsertRowStatement(Connection connection,
         List<Persistent> persistents)
   {
      return prepareInsertRowStatement(connection, persistents, false, true);
   }

   public BatchStatementWrapper prepareInsertRowStatement(Connection connection,
         List<Persistent> persistents, boolean useJdbcReturnGeneratedKeys, boolean useBatchStatement)
   {
      try
      {
         final BatchStatementWrapper insertStatement = prepareInsertRowStatement(
               connection, useJdbcReturnGeneratedKeys);

         // actually bind persistent to SQL statement

         final List<FieldDescriptor> persistentFields = typeDescriptor.getPersistentFields();
         final List<LinkDescriptor> links = typeDescriptor.getLinks();

         final PreparedStatement stmt = insertStatement.getStatement();

         for (Iterator<Persistent> persistentIterator = persistents.iterator(); persistentIterator.hasNext(); )
         {
            Persistent persistent = persistentIterator.next();

            int slot = 0;
            for (int n = 0; n < persistentFields.size(); ++n)
            {
               FieldDescriptor descriptor = persistentFields.get(n);
               if (!(dbDescriptor.supportsIdentityColumns()
                     && typeDescriptor.requiresPKCreation()
                     && typeDescriptor.isPkField(descriptor.getField())))
               {
                  setSQLValue(stmt, slot + 1, descriptor.getField().getType(),
                        descriptor.getField().get(persistent));
                  ++slot;
               }
            }

            for (int i = 0; i < links.size(); ++i)
            {
               LinkDescriptor link = links.get(i);
               Object linkedObject = link.getField().get(persistent);
               Field fkField = link.getFkField();
               fkField.setAccessible(true);
               if (linkedObject != null)
               {
                  setSQLValue(stmt, slot + 1, fkField.getType(), fkField.get(linkedObject));
               }
               else
               {
                  stmt.setNull(slot + 1, mapJavaTypeToSQLTypeID(fkField.getType()));
               }
               ++slot;
            }

            stmt.addBatch();
         }

         return insertStatement;
      }
      catch (IllegalAccessException x)
      {
         throw new InternalException(x);
      }
      catch (SQLException x)
      {
         throw new InternalException("Batch statement: " + preparedInsertStmt, x);
      }
   }

   public BatchStatementWrapper prepareUpdateRowStatement(Connection connection,
         List<Persistent> persistents, boolean useTableAlias)
   {
      boolean batchUpdate = (persistents.size() > 1);

      StringBuffer buffer = new StringBuffer();

      buffer.append("UPDATE ");
      sqlUtils.appendTableRef(buffer, typeDescriptor, useTableAlias);

      buffer.append(" SET ");

      int nFields = 0;

      List<FieldDescriptor> persistentFields = typeDescriptor.getPersistentFields();
      Set<String> modifiedFields;
      if (batchUpdate)
      {
         // update all fields in a batch statement
         modifiedFields = null;
      }
      else
      {
         modifiedFields = persistents.get(0).getPersistenceController().getModifiedFields();
      }
      if ((null != modifiedFields) && !modifiedFields.isEmpty())
      {
         // use modifiable copy
         modifiedFields = new HashSet<String>(modifiedFields);
      }
      for (int i = 0; i < persistentFields.size(); ++i)
      {
         FieldDescriptor descriptor = (FieldDescriptor) persistentFields.get(i);

         // skip update of primary keys and unmodified fields
         if ( !typeDescriptor.isPkField(descriptor.getField())
               && ((null == modifiedFields) || modifiedFields.contains(descriptor.getField()
                     .getName())))
         {
            if (nFields > 0)
            {
               buffer.append(", ");
            }

            sqlUtils.appendFieldRef(buffer, typeDescriptor.fieldRef(descriptor.getField()
                  .getName()), useTableAlias);
            buffer.append(" = ");

            if (descriptor.getFieldEncryptFunction() != null)
            {
               buffer.append(descriptor.getFieldEncryptFunction());
               buffer.append("(?, '");
               buffer.append(typeDescriptor.getEncryptKey());
               buffer.append("')");
            }
            else
            {
               buffer.append("?");
            }
            ++nFields;
         }
      }

      List<LinkDescriptor> links = typeDescriptor.getLinks();
      for (int i = 0; i < links.size(); ++i)
      {
         LinkDescriptor link = links.get(i);

         if ( !batchUpdate)
         {
            // Skip unfetched links, they cannot have been modified
            // @todo Do we need a markLinkModified() method
            DefaultPersistenceController dpc = (DefaultPersistenceController) persistents.get(0).getPersistenceController();
            if ( !dpc.isLinkFetched(i))
            {
               continue;
            }
         }
         if ((null != modifiedFields)
               && !modifiedFields.contains(link.getField().getName()))
         {
            // skip update of unmodified fields
            continue;
         }

         if (nFields > 0)
         {
            buffer.append(", ");
         }

         sqlUtils.appendFieldRef(buffer, typeDescriptor.fieldRef(link.getField()
               .getName()), useTableAlias);
         buffer.append(" = ?");
         ++nFields;
      }

      String statementString = extendPKPredicateStatement(buffer.toString(), connection, nFields, useTableAlias);
      try
      {
         PreparedStatement stmt = connection.prepareStatement(statementString);

         for (Iterator<Persistent> persistentIterator = persistents.iterator(); persistentIterator.hasNext(); )
         {
            Persistent persistent = persistentIterator.next();
            DefaultPersistenceController dpc = (DefaultPersistenceController)persistent.getPersistenceController();

            if (trace.isDebugEnabled())
            {
               trace.debug("updating: " + persistent.getClass().getName());
            }

            setPkFields(persistent, stmt, nFields);

            // Set field values
            int k = 1;

            for (int i = 0; i < persistentFields.size(); ++i)
            {
               FieldDescriptor descriptor = (FieldDescriptor) persistentFields.get(i);

               // skip update of primary keys and unmodified fields
               if ( !typeDescriptor.isPkField(descriptor.getField())
                     && ((null == modifiedFields) || modifiedFields.contains(descriptor.getField()
                           .getName())))
               {
                  setSQLValue(stmt, k, descriptor.getField().getType(), descriptor
                        .getField().get(persistent));

                  if (null != modifiedFields)
                  {
                     modifiedFields.remove(descriptor.getField().getName());
                  }
                  ++k;
               }
            }

            for (int i = 0; i < links.size(); ++i)
            {
               // Skip unfetched links, they cannot have been modified

               final LinkDescriptor link = (LinkDescriptor) links.get(i);

               if ( !batchUpdate)
               {
                  // Skip unfetched links, they cannot have been modified
                  if ( !dpc.isLinkFetched(i))
                  {
                     continue;
                  }
               }

               if ((null != modifiedFields)
                     && !modifiedFields.contains(link.getField().getName()))
               {
                  // skip update of unmodified fields
                  continue;
               }

               link.getField().setAccessible(true);

               Object linkedObject = link.getField().get(persistent);

               Field fkField = link.getFkField();

               if (linkedObject != null)
               {
                  if (trace.isDebugEnabled())
                  {
                     trace.debug("Updating link " + link.getField().getName()
                           + " with object " + linkedObject.getClass().getName() + ".");
                  }

                  setSQLValue(stmt, k, fkField.getType(), fkField.get(linkedObject));
               }
               else
               {
                  Object cachedFk = dpc.getLinkBuffer()[i];
                  if (cachedFk == null)
                  {
                     if (trace.isDebugEnabled())
                     {
                        trace.debug("Updating link " + link.getField().getName()
                              + " with null.");
                     }
                     stmt.setNull(k, mapJavaTypeToSQLTypeID(fkField.getType()));
                  }
                  else
                  {
                     if (trace.isDebugEnabled())
                     {
                        trace.debug("Updating link " + link.getField().getName()
                              + " with foreignkey " + cachedFk + ".");
                     }
                     setSQLValue(stmt, k, fkField.getType(), cachedFk);
                  }

               }

               if (null != modifiedFields)
               {
                  modifiedFields.remove(link.getField().getName());
               }
               ++k;
            }

            if ((null != modifiedFields) && !modifiedFields.isEmpty())
            {
               trace.error("Don't know how to update modified fields " + modifiedFields
                     + " for " + persistent + ".");
            }

            stmt.addBatch();
         }

         return new BatchStatementWrapper(statementString, stmt);
      }
      catch (IllegalAccessException x)
      {
         throw new InternalException(x);
      }
      catch (SQLException x)
      {
         throw new InternalException("Statement: " + statementString, x);
      }
      catch (Exception e)
      {
         throw new InternalException("Statement: " + statementString, e);
      }
   }

   public String getUpdateRowStatementString(Connection connection, Persistent persistent)
   {
      return getUpdateRowStatementString(connection, persistent, false);
   }

   public String getUpdateRowStatementString(Connection connection,
         Persistent persistent, boolean useTableAlias)
   {
      DefaultPersistenceController persistenceController = (DefaultPersistenceController)
            persistent.getPersistenceController();

      Assert.isNotNull(persistenceController, "Persistence controller is null.");

      try
      {
         StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

         buffer.append("UPDATE ");
         sqlUtils.appendTableRef(buffer, typeDescriptor, useTableAlias);

         buffer.append(" SET ");

         int nFields = 0;

         List<FieldDescriptor> persistentFields = typeDescriptor.getPersistentFields();
         Set<String> modifiedFields = persistenceController.getModifiedFields();
         if (null != modifiedFields)
         {
            // use modifiable copy
            modifiedFields = new HashSet<String>(modifiedFields);
         }
         for (int n = 0; n < persistentFields.size(); ++n)
         {
            FieldDescriptor descriptor = persistentFields.get(n);

            // skip update of primary keys and unmodified fields
            if ( !typeDescriptor.isPkField(descriptor.getField())
                  && ((null == modifiedFields) || modifiedFields.contains(descriptor.getField()
                        .getName())))
            {
               if (nFields > 0)
               {
                  buffer.append(", ");
               }

               sqlUtils.appendFieldRef(buffer,
                     typeDescriptor.fieldRef(descriptor.getField().getName()),
                     useTableAlias);

               buffer.append(" = ");

               String value = getSQLValue(descriptor.getField().getType(),
                     descriptor.getField().get(persistent));

               if (descriptor.getFieldEncryptFunction() != null)
               {
                  buffer.append(descriptor.getFieldEncryptFunction());
                  buffer.append("(").append(value).append(", '").append(
                        typeDescriptor.getEncryptKey()).append("')");
               }
               else
               {
                  buffer.append(value);
               }
               if (null != modifiedFields)
               {
                  modifiedFields.remove(descriptor.getField().getName());
               }
               ++nFields;
            }
         }

         List<LinkDescriptor> links = typeDescriptor.getLinks();
         for (int i = 0; i < links.size(); ++i)
         {
            // Skip unfetched links, they cannot have been modified
            // @todo Do we need a markLinkModified() method

            final LinkDescriptor link = links.get(i);

            if (!persistenceController.isLinkFetched(i))
            {
               continue;
            }
            if ((null != modifiedFields) && !modifiedFields.contains(link.getField().getName()))
            {
               // skip update of unmodified fields
               continue;
            }

            Object linkedObject = link.getField().get(persistent);

            Field fkField = link.getFkField();

            if (nFields > 0)
            {
               buffer.append(", ");
            }

            sqlUtils.appendFieldRef(buffer, typeDescriptor.fieldRef(link.getField()
                  .getName()), useTableAlias);

            buffer.append(" = ");

            if (linkedObject != null)
            {
               buffer.append(getSQLValue(fkField.getType(), fkField.get(linkedObject)));
            }
            else
            {
               buffer.append(getSQLValue(fkField.getType(), null));
            }

            if (null != modifiedFields)
            {
               modifiedFields.remove(link.getField().getName());
         }
            ++nFields;
         }

         if ((null != modifiedFields) && !modifiedFields.isEmpty())
         {
            trace.error("Don't know how to updated modified fields " + modifiedFields
                  + " for " + persistent + ".");
         }

         return getPKPredicateStatementString(buffer.toString(), persistent,
               useTableAlias);
      }
      catch (IllegalAccessException x)
      {
         throw new InternalException(x);
      }
   }

/*   public String prepareUpdateStatement(UpdateDescriptor update, List<Pair<Class<?>, ?>> bindValueList,
         boolean useLockTables)
   {
      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      buffer.append("INSERT INTO ");
      sqlUtils.appendTableRef(buffer, new SchemaDecorator(insert.getSchemaName(),
            useLockTables
                  ? getTypeDescriptor().getLockTableDescriptor()
                  : getTypeDescriptor()),//
            false);

      buffer.append(" (");
      sqlUtils.appendDefaultSelectList(buffer, getTypeDescriptor(), false);
      buffer.append(")");

      if (null != insert.getFullselect())
      {
         buffer.append(" ").append(
               prepareSelectStatement(insert.getFullselect(), true, bindValueList, false));
      }
      else
      {
         // must be insert of values

         throw new PublicException("INSERT ... VALUES ...is not yet implemented.");
      }

      if (trace.isDebugEnabled())
      {
         trace.debug("Insert statement: " + buffer.toString());
      }

      return buffer.toString();
   }*/

   public String prepareInsertStatement(InsertDescriptor insert, List<Pair<Class<?>, ?>> bindValueList,
         boolean useLockTables)
   {
      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      buffer.append("INSERT INTO ");
      sqlUtils.appendTableRef(buffer, new SchemaDecorator(insert.getSchemaName(),
            useLockTables
                  ? getTypeDescriptor().getLockTableDescriptor()
                  : getTypeDescriptor()),//
            false);

      buffer.append(" (");
      sqlUtils.appendDefaultSelectList(buffer, getTypeDescriptor(), false);
      buffer.append(")");

      if (null != insert.getFullselect())
      {
         buffer.append(" ").append(
               prepareSelectStatement(insert.getFullselect(), true, bindValueList, false));
      }
      else
      {
         // must be insert of values

         throw new PublicException("INSERT ... VALUES ...is not yet implemented.");
      }

      if (trace.isDebugEnabled())
      {
         trace.debug("Insert statement: " + buffer.toString());
      }

      return buffer.toString();
   }

   public String prepareDeleteStatement(final DeleteDescriptor delete, List<Pair<Class<?>, ?>> bindValueList,
         boolean useLockTables)
   {
      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      buffer.append("DELETE FROM ");

      sqlUtils.appendTableRef(buffer, new SchemaDecorator(delete.getSchemaName(),
            useLockTables
               ? typeDescriptor.getLockTableDescriptor()
               : typeDescriptor),
            false);

      if (dbDescriptor.supportsSubselects())
      {

         StringBuffer whereBuffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

         if (null != delete.getPredicateTerm())
         {
            final PredicateTerm predicateTerm;
            if (delete.getQueryExtension().getJoins().isEmpty())
            {
               predicateTerm = delete.getPredicateTerm();
            }
            else
            {
               final Field[] pkFields = getTypeDescriptor().getPkFields();

               // TODO consider reducing indirection of selecting from table to be
               // deleted from when joins are against PK fields of target table

               // TODO (sb): take care that the selected join joins with the 'deletion' table
               AndTerm deletePredicate = new AndTerm();
               for (int i = 0; i < pkFields.length; i++ )
               {
                  // TODO consider restricting second or later sub-SELECT by selected
                  // value of first subselect to prevent invalid combinations
                  QueryDescriptor from = QueryDescriptor.from(delete.getSchemaName(), delete.getType(), delete.getQueryExtension());
                  deletePredicate.add(Predicates.inList(
                              // Alias on attribute doesn't work with unaliased tablename
                              // (at least with Oracle) -> join.getLhsAlias()
                        delete.fieldRef(pkFields[i].getName()), from
                              .select(pkFields[i].getName())));
               }

               predicateTerm = deletePredicate;
            }

            whereBuffer.append(buildWhereClause(predicateTerm, bindValueList, delete, false,
                  false));
         }

         if (whereBuffer.length() > 0)
         {
            buffer.append(" WHERE ").append(whereBuffer);
         }
      }
      else if (DBMSKey.MYSQL.equals(dbDescriptor.getDbmsKey()) || DBMSKey.MYSQL_SEQ.equals(dbDescriptor.getDbmsKey()))
      {
         final FieldRefResolver resolver = delete;

         StringBuffer whereBuffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
         if (null != delete.getPredicateTerm())
         {
            if ( !delete.getQueryExtension().getJoins().isEmpty())
            {
               buffer.append(" USING ");
               sqlUtils.appendTableRef(buffer, delete, false);

               applyJoins(delete.getQueryExtension().getJoins(), whereBuffer, buffer,
                     bindValueList, resolver, false, false, null);
            }

            StringBuffer predicateBuffer = buildWhereClause(delete.getPredicateTerm(),
                  bindValueList, resolver, false);
            if ((null != predicateBuffer) && (0 < predicateBuffer.length()))
            {
               if (0 < whereBuffer.length())
               {
                  whereBuffer.append(" AND ");
               }
               whereBuffer.append(predicateBuffer);
            }
         }

         if (0 < whereBuffer.length())
         {
            buffer.append(" WHERE ").append(whereBuffer);
         }
      }
      else
      {
         // TODO (sb): At the moment this implements only deletion by subselect or the
         // MySQL variant
         Assert.lineNeverReached("Unsupported DELETE variant.");
      }

      if (trace.isDebugEnabled())
      {
         trace.debug("Delete statement: " + buffer.toString());
      }

      return buffer.toString();
   }

   public boolean isLockRowStatementSQLQuery()
   {
      return dbDescriptor.isLockRowStatementSQLQuery();
   }

   public String getLockRowStatementString(long oid, boolean tryToUseDistinctLockTable,
         List<Pair<Class<?>, ?>> bindValueList)
   {
      final ITableDescriptor table = tryToUseDistinctLockTable
            ? typeDescriptor.getLockTableDescriptor()
            : typeDescriptor;

      StringBuffer predicateBuffer = buildWhereClause(Predicates.isEqual(
            table.fieldRef(IdentifiablePersistentBean.FIELD__OID), oid),
            bindValueList, FieldRefResolver.NONE, false, false);
      String statement = dbDescriptor.getLockRowStatementString(sqlUtils,
            getTypeDescriptor(), tryToUseDistinctLockTable, predicateBuffer.toString());

      if (trace.isDebugEnabled())
      {
         trace.debug("Lock statement: " + statement);
      }

      return statement;
   }

   /**
    * @param queryExtension
    * @param primaryStatement
    * @param bindValues
    * @return String representation of an by queryExtension extended simple SQL-query
    */
   public String prepareSelectStatement(QueryDescriptor query, boolean primaryStatement,
         List<Pair<Class<?>, ?>> bindValues, boolean useLiteralsWhereAppropriate)
   {
      final QueryExtension queryExtension = query.getQueryExtension();

      FieldRefResolver resolver = query;

      StringBuffer selectBuffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
      StringBuffer fromBuffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
      StringBuffer whereBuffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
      StringBuffer groupBuffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);
      StringBuffer orderBuffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      if (null != queryExtension)
      {
         // build SELECT-Clause by provided selection extension
         String selectAlias = queryExtension.getSelectAlias();
         String concatToken = "";
         Column[] selectList = queryExtension.getSelection();
         for (int i = 0; i < selectList.length; ++i)
         {
            selectBuffer.append(concatToken);
            concatToken = ", ";

            if (selectList[i] instanceof FieldRef)
            {
               FieldRef field = resolver.resolveFieldRef((FieldRef) selectList[i]);
               sqlUtils.appendFieldRef(selectBuffer, field, selectAlias);
            }
            else if (selectList[i] instanceof Functions.BoundFunction)
            {
               Functions.BoundFunction func = (Functions.BoundFunction) selectList[i];
               if (func.getFunction() instanceof Function.ParamLess)
               {
                  selectBuffer.append(func.getFunction().getId());
               }
               else if (func.getFunction() instanceof Function.Unary)
               {
                  selectBuffer.append(func.getFunction().getId()).append("(");
                  FieldRef operand = resolver.resolveFieldRef(func.getOperands()[0]);
                  sqlUtils.appendFieldRef(selectBuffer, operand, selectAlias);
                  selectBuffer.append(")");
               }
               else if (func.getFunction() instanceof Function.Nary)
               {
                  Function.Nary naryFunc = (Function.Nary) func.getFunction();
                  FieldRef[] fParams = func.getOperands();

                  // TODO need protocol to let function do the SQL append
                  selectBuffer.append(naryFunc.getId()).append(" ");

                  String joinToken = "";
                  for (int paramId = 0; paramId < fParams.length; ++paramId)
                  {
                     selectBuffer.append(joinToken);
                     joinToken = ", ";

                     FieldRef operand = resolver.resolveFieldRef(fParams[paramId]);
                     sqlUtils.appendFieldRef(selectBuffer, operand, selectAlias);
                  }
                  selectBuffer.append(")");
               }
               else
               {
                  throw new InternalException(
                        "Non-unary functions are not yet implemented: "
                              + func.getFunction());
               }
            }
         }
      }

      if (primaryStatement && selectBuffer.length() == 0)
      {
         sqlUtils.appendDefaultSelectList(selectBuffer, query);
      }

      // append primary from clause
      sqlUtils.appendTableRef(fromBuffer, query);

      if (null != queryExtension)
      {
         if (!queryExtension.getJoins().isEmpty())
         {
            // add joins behind primary table
            applyJoins(queryExtension.getJoins(), whereBuffer, fromBuffer, bindValues,
                  resolver, true, useLiteralsWhereAppropriate, queryExtension.getSelectAlias());
         }

         // add predicates
         StringBuffer predicateWhere = buildWhereClause(
               queryExtension.getPredicateTerm(), bindValues, resolver, true, useLiteralsWhereAppropriate);

         if (0 < whereBuffer.length() && 0 < predicateWhere.length())
         {
            whereBuffer.append(" AND ");
         }
         whereBuffer.append(predicateWhere);

         // add group by elements
         String groupByConcatToken = "";

         for (Iterator<FieldRef> i = queryExtension.getGroupCriteria().iterator(); i.hasNext();)
         {
            groupBuffer.append(groupByConcatToken);
            groupByConcatToken = ", ";

            FieldRef field = resolver.resolveFieldRef(i.next());

            if (null != query.getQueryExtension()
                  .getHints()
                  .get(CasePolicy.class.getName()))
            {
               sqlUtils.appendFieldRef(groupBuffer, field, true, "CASE_PI");
            }
            else
            {
               sqlUtils.appendFieldRef(groupBuffer, field);
            }
         }

         // add order by elements
         String orderByConcatToken = "";
         for (Iterator<OrderCriterion> i = queryExtension.getOrderCriteria().iterator(); i.hasNext();)
         {
            orderBuffer.append(orderByConcatToken);
            orderByConcatToken = ", ";

            OrderCriterion criterion = i.next();

            FieldRef field = resolver.resolveFieldRef(criterion.getFieldRef());
            sqlUtils.appendFieldRef(orderBuffer, field);
            orderBuffer.append(criterion.isAscending() ? " ASC" : " DESC");
         }
      }

      StringBuffer statement = new StringBuffer(4 * DEFAULT_STMT_BUFFER_SIZE);

      if (0 != selectBuffer.length())
      {
         statement.append("SELECT ");
         if ((null != queryExtension) && queryExtension.isDistinct())
         {
            statement.append("DISTINCT ");
         }
         statement.append(selectBuffer);
      }

      if (0 != fromBuffer.length())
      {
         statement.append(" FROM ");
         statement.append(fromBuffer);
      }

      if (0 != whereBuffer.length())
      {
         statement.append(" WHERE ");
         statement.append(whereBuffer);
      }

      if (0 != groupBuffer.length())
      {
         statement.append(" GROUP BY ");
         statement.append(groupBuffer);
      }

      if (0 != orderBuffer.length())
      {
         statement.append(" ORDER BY ");
         statement.append(orderBuffer);
      }

            if (trace.isDebugEnabled())
      {
         trace.debug("Select statement: " + statement.toString());
      }

      return statement.toString();
   }

   /**
    * Prepares a statement to find an object with the same pk as the one of the
    * provided persistent object <tt>persistent</tt>.
    */
   public StatementWrapper prepareReloadStatement(Connection connection,
         Persistent persistent)
   {
      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      buffer.append("SELECT ");
      sqlUtils.appendDefaultSelectList(buffer, typeDescriptor);

      buffer.append(" FROM ");
      sqlUtils.appendTableRef(buffer, typeDescriptor);

      String statementString = extendPKPredicateStatement(buffer.toString(), connection, 0, false);
      try
      {
         PreparedStatement statement = connection.prepareStatement(statementString);
         setPkFields(persistent, statement, 0);
         return new StatementWrapper(statementString, statement);
      }
      catch (Exception e)
      {
         throw new InternalException("Statement: " + statementString, e);
      }

   }

   public String getReloadStatementString(Persistent persistent)
   {
      StringBuffer buffer = new StringBuffer(DEFAULT_STMT_BUFFER_SIZE);

      buffer.append("SELECT ");
      sqlUtils.appendDefaultSelectList(buffer, typeDescriptor);

      buffer.append(" FROM ");
      sqlUtils.appendTableRef(buffer, typeDescriptor);

      return getPKPredicateStatementString(buffer.toString(), persistent);
   }

   /**
    * Creates an instance of the type manager's type and populates its attributes
    * with the current row values in the result set <tt>resultSet</tt>.
    */
   public PersistenceController createObjectFromRow(Session session, ResultSet resultSet)
   {
      try
      {
         return createObjectFromRow(session, resultSet,
               (Persistent) typeDescriptor.getType().newInstance());
      }
      catch (Exception x)
      {
         throw new InternalException(x);
      }
   }

   /**
    * Creates an instance of the type manager's type and populates its
    * attributes with the current row values in the result set
    * <tt>resultSet</tt>.
    */
   public PersistenceController createObjectFromRow(Session session, ResultSet resultSet,
         Persistent persistent)
   {
      try
      {
         return new DefaultPersistenceController(session, typeDescriptor, persistent,
               updateObjectFromRow(persistent, resultSet));
      }
      catch (Throwable x)
      {
         throw new InternalException("Failed to load persistent object.", x);
      }
   }

   public void reloadObjectFromRow(Session session, Persistent persistent)
         throws PhantomException
   {
      Assert.isNotNull(persistent.getPersistenceController(),
            "Persistence controller is not null.");

      ResultSet resultSet = null;

      try
      {
         long startTime;
         long stopTime;
         String statementString;

         Connection connection = session.getConnection();
         if (session.isUsingPreparedStatements(persistent.getClass()))
         {
            StatementWrapper stmtWrapper = prepareReloadStatement(connection, persistent);
            PreparedStatement stmt = stmtWrapper.getStatement();

            statementString = stmtWrapper.getStatementString();
            startTime = System.currentTimeMillis();
            final ResultSet rs = stmt.executeQuery();
            stopTime = System.currentTimeMillis();

            resultSet = ManagedResultSet.createManager(stmt, rs);
         }
         else
         {
            Statement stmt = connection.createStatement();

            statementString = getReloadStatementString(persistent);
            startTime = System.currentTimeMillis();
            final ResultSet rs = stmt.executeQuery(statementString);
            stopTime = System.currentTimeMillis();

            resultSet = ManagedResultSet.createManager(stmt, rs);
         }
         session.monitorSqlExecution(statementString, startTime, stopTime);

         if (resultSet.next())
         {
            Object[] linkBuffer = updateObjectFromRow(persistent, resultSet);
            ((DefaultPersistenceController) persistent
                  .getPersistenceController()).setLinkBuffer(linkBuffer);
         }
         else
         {
            throw new PhantomException("Try to rebind a persistent object which does not "
                  + "exist in the database: " + persistent);
         }
      }
      catch (PhantomException e)
      {
         throw e;
      }
      catch (SQLException e)
      {
         // todo
         throw new ConcurrencyException(e);
      }
      catch (Throwable e)
      {
         throw new InternalException("Failed to reload persistent object.", e);
      }
      finally
      {
         QueryUtils.closeResultSet(resultSet);
      }
   }

   public void reloadAttributeFromRow(Session session, Persistent persistent,
         String attributeName) throws PhantomException
   {
      ResultSet resultSet = null;

      try
      {
         long startTime;
         long stopTime;
         String statementString;

         Connection connection = session.getConnection();
         if (session.isUsingPreparedStatements(persistent.getClass()))
         {
            StatementWrapper stmtWrapper = prepareReloadStatement(connection, persistent);
            PreparedStatement stmt = stmtWrapper.getStatement();

            statementString = stmtWrapper.getStatementString();
            startTime = System.currentTimeMillis();
            final ResultSet rs = stmt.executeQuery();
            stopTime = System.currentTimeMillis();

            resultSet = ManagedResultSet.createManager(stmt, rs);
         }
         else
         {
            Statement stmt = connection.createStatement();
            statementString = getReloadStatementString(persistent);

            startTime = System.currentTimeMillis();
            final ResultSet rs = stmt.executeQuery(statementString);
            stopTime = System.currentTimeMillis();

            resultSet = ManagedResultSet.createManager(stmt, rs);
         }
         session.monitorSqlExecution(statementString, startTime, stopTime);

         if (resultSet.next())
         {
            updateAttributeFromRow(persistent, attributeName, resultSet);
         }
         else
         {
            throw new PhantomException("Try to rebind a persistent object which does not "
                  + "exist in the database: " + persistent);
         }
      }
      catch (PhantomException e)
      {
         throw e;
      }
      catch (SQLException e)
      {
         // todo
         throw new ConcurrencyException(e);
      }
      catch (Throwable e)
      {
         throw new InternalException("Failed to reload persistent object.", e);
      }
      finally
      {
         QueryUtils.closeResultSet(resultSet);
      }
   }

   /**
    * Creates a control block for a newly created object.
    * Stored in a control block, the object can be treated the same way as
    * objects retrieved from the JDBC source.
    */
   public PersistenceController createPersistenceController(Session session,
         Persistent persistent)
   {
      try
      {
         final List<LinkDescriptor> links = typeDescriptor.getLinks();

         Object[] linkBuffer = links.isEmpty()
               ? DefaultPersistenceController.NO_LINK_BUFFER
               : new Object[links.size()];

         for (int i = 0; i < links.size(); ++i)
         {
            LinkDescriptor link = links.get(i);

            Object linkedObject = link.getField().get(persistent);

            if (null != linkedObject)
            {
               Field fkField = link.getFkField();

               linkBuffer[i] = fkField.get(linkedObject);
            }
         }

         DefaultPersistenceController pc = new DefaultPersistenceController(session,
               typeDescriptor, persistent, linkBuffer);

         // for just created objects, all links are fetched
         pc.markAllLinksFetched();
         pc.markAllVectorsFetched();

         return pc;
      }
      catch (Throwable x)
      {
         throw new InternalException(x);
      }
   }

   /**
    * Returns the PK of a PersistenceController in a format suitable as a map key.
    * Identities are guaranteed to be unique per given PK.
    */
   public Object getIdentityKey(ResultSet resultSet)
   {
      try
      {
         Field[] pkFields = typeDescriptor.getPkFields();
         if ((1 == pkFields.length)
               && (Long.class.isAssignableFrom(pkFields[0].getType()) || Long.TYPE.isAssignableFrom(pkFields[0].getType())))
         {
            return getJavaValue(pkFields[0].getType(), 0, resultSet,
                  1 + typeDescriptor.getFieldColumnIndex(pkFields[0]), true, false);
         }
         else
         {
            //Object[] key = new Object[pkFields.length];
            CompositeKey key = TypeDescriptor.createKey(pkFields.length);
            for (int i = 0; i < pkFields.length; i++ )
            {
               //key[i] = getJavaValue(pkFields[i].getType(), 0, resultSet,
               //      1 + typeDescriptor.getFieldColumnIndex(pkFields[i]), true, false);
               key.setKey(i, getJavaValue(pkFields[i].getType(), 0, resultSet,
                     1 + typeDescriptor.getFieldColumnIndex(pkFields[i]), true, false));
            }
            return key;
         }
      }
      catch (Exception e)
      {
         throw new InternalException(e);
      }
   }

   private Object[] updateObjectFromRow(Persistent persistent, ResultSet resultSet)
         throws IllegalAccessException
   {
      // update fields

      int elementCount = 0;
      List<FieldDescriptor> persistentFields = typeDescriptor.getPersistentFields();

      for (int n = 0; n < persistentFields.size(); ++n)
      {
         FieldDescriptor descriptor = persistentFields.get(n);

         descriptor.getField().setAccessible(true);
         descriptor.getField().set(persistent, getJavaValue(
               descriptor.getField().getType(), descriptor.getLength(), resultSet, n + 1,
               true, descriptor.isSecret()));
         elementCount++;
      }

      // update vectors
      List<PersistentVectorDescriptor> persistentVectors = typeDescriptor.getPersistentVectors();
      for (int n = 0; n < persistentVectors.size(); n++)
      {
         Field field = persistentVectors.get(n).getField();
         field.setAccessible(true);
         field.set(persistent, new DefaultPersistentVector());
      }

      // update links
      final List<LinkDescriptor> links = typeDescriptor.getLinks();
      final Object[] linkBuffer = links.isEmpty()
            ? DefaultPersistenceController.NO_LINK_BUFFER
            : new Object[links.size()];
      for (int n = 0; n < links.size(); ++n)
      {
         LinkDescriptor link = links.get(n);
         Object fkValue = getJavaValue(link.getFkField().getType(), 0, resultSet,
               elementCount + 1, false, false);
         linkBuffer[n] = fkValue;
         elementCount++;
      }
      return linkBuffer;
   }


   private void updateAttributeFromRow(Persistent persistent, String attributeName,
         ResultSet resultSet)
         throws IllegalAccessException
   {
      int index = typeDescriptor.getColumnIndex(attributeName);
      FieldDescriptor descriptor = typeDescriptor.getPersistentField(index);

      descriptor.getField().setAccessible(true);
      descriptor.getField().set(persistent,
            getJavaValue(descriptor.getField().getType(), descriptor.getLength(),
                  resultSet, index + 1, true, descriptor.isSecret()));
   }

   public void setVector(Persistent persistent, String vectorName,
         ResultIterator iterator)
   {
      PersistentVectorDescriptor descriptor = typeDescriptor.getPersistentVector(vectorName);
      if (descriptor == null)
      {
         throw new InternalException("Unknown vector name '" + vectorName + "'.");
      }
      try
      {
         Field field = descriptor.getField();
         field.setAccessible(true);

         PersistentVector vector = (PersistentVector) field.get(persistent);
         if (vector == null)
         {
            vector = new DefaultPersistentVector();
            field.set(persistent, vector);
         }

         vector.clear();
         while (iterator.hasNext())
         {
            vector.add(iterator.next());
         }
         return;
      }
      catch (Exception x)
      {
         throw new InternalException(x);
      }
   }

   /**
    * Sets the PK of a persistent object if the automatic, sequence-based
    * PK initialization.
    *
    * May only be called if only one PK field is used.
    */
   public void setPK(Persistent persistent, long pk)
   {
      if (!typeDescriptor.requiresPKCreation())
      {
         return;
      }
      try
      {
         if (trace.isDebugEnabled())
         {
            trace.debug("Setting PK of " + persistent + " to " + pk);
         }
         Field[] pkFields = typeDescriptor.getPkFields();
         if (1 != pkFields.length)
         {
            Assert.condition(1 == pkFields.length,
                  "Automatic PK values are only supported for types with a single PK field.");
         }
         pkFields[0].set(persistent, new Long(pk));
      }
      catch (Exception x)
      {
         throw new InternalException("Failed to initialize primary key from long value.",
               x);
      }
   }

   public long getIdentityOID(Session session)
   {
      if (dbDescriptor.supportsIdentityColumns())
      {
         Field[] pkFields = typeDescriptor.getPkFields();

         Assert.condition(1 == pkFields.length,
               "Automatic PK values are only supported for types with a single PK field.");

         if (session.isUsingPreparedStatements(typeDescriptor.getType()))
         {
            PreparedStatement pkStatement = null;
            try
            {
               try
               {
                  Connection connection = session.getConnection();
                  pkStatement = connection.prepareStatement(dbDescriptor
                        .getSelectIdentityStatementString(sqlUtils.getSchemaName(),
                              typeDescriptor.getTableName()));
               }
               catch (SQLException x)
               {
                  throw new InternalException(x);
               }
               ResultSet pkQuery = pkStatement.executeQuery();
               pkQuery.next();
               return ((Number) getJavaValue(pkFields[0].getType(), 0, pkQuery, 1, true,
                     false)).longValue();
            }
            catch (SQLException e)
            {
               throw new InternalException(e);
            }
            finally
            {
               QueryUtils.closeStatement(pkStatement);
            }
         }
         else
         {
            Statement pkStmt = null;
            try
            {
               String pkStmtString = dbDescriptor.getSelectIdentityStatementString(
                     sqlUtils.getSchemaName(), typeDescriptor.getTableName());
               Connection connection = session.getConnection();
               pkStmt = connection.createStatement();
               ResultSet pkQuery = pkStmt.executeQuery(pkStmtString);
               pkQuery.next();
               return ((Number) getJavaValue(pkFields[0].getType(), 0, pkQuery, 1, true,
                     false)).longValue();
            }
            catch (SQLException e)
            {
               throw new InternalException(e);
            }
            finally
            {
               QueryUtils.closeStatement(pkStmt);
            }
         }
      }
      else
      {
         throw new InternalException("Database Type does not support identity OIDs.");
      }
   }

   public TypeDescriptor getTypeDescriptor()
   {
      return typeDescriptor;
   }

   /**
    * Sybase is writing a single space for an empty string. Due to
    * 'trailing spaces trimming' an string containing only spaces would be reduced
    * to an empty string as well.
    *
    * @param string
    * @return true if the given string would be handled as empty string in Sybase.
    */
   private static boolean isSybaseEmptyString(String string)
   {
      if (string.length() == 0 || string.trim().length() == 0)
      {
         return true;
      }

      return false;
   }

}


