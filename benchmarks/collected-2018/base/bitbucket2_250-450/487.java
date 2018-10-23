// https://searchcode.com/api/result/132189764/

package org.jooq.codegen.generators;

import com.google.inject.Inject;
import java.util.List;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.jooq.codegen.JavaType;
import org.jooq.codegen.NamingStrategy;
import org.jooq.codegen.NamingStrategy.Mode;
import org.jooq.codegen.generators.GeneratorBase;
import org.jooq.codegen.generators.PojoJavaCodeHelper;
import org.jooq.tools.StringUtils;
import org.jooq.util.ColumnDefinition;
import org.jooq.util.DataTypeDefinition;
import org.jooq.util.TableDefinition;

@SuppressWarnings("all")
public class PojoGenerator extends GeneratorBase {
  private TableDefinition _table;
  
  public TableDefinition getTable() {
    return this._table;
  }
  
  public void setTable(final TableDefinition table) {
    this._table = table;
  }
  
  private JavaType _type;
  
  public JavaType getType() {
    return this._type;
  }
  
  public void setType(final JavaType type) {
    this._type = type;
  }
  
  @Inject
  protected PojoJavaCodeHelper javaHelper;
  
  public void generate(final TableDefinition table) {
    this.setTable(table);
    NamingStrategy _namingStrategy = this.namingStrategy();
    JavaType _type = _namingStrategy.getType(table, Mode.POJO);
    this.setType(_type);
    JavaType _type_1 = this.getType();
    CharSequence _javaCode = this.javaCode();
    this.write(_type_1, _javaCode);
  }
  
  public CharSequence javaCode() {
    CharSequence _xblockexpression = null;
    {
      Integer _determinePadding = this.determinePadding();
      this.setPadding((_determinePadding).intValue());
      StringConcatenation _builder = new StringConcatenation();
      JavaType _type = this.getType();
      CharSequence _classHeader = this.classHeader(_type);
      _builder.append(_classHeader, "");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.newLine();
      {
        TableDefinition _table = this.getTable();
        List<ColumnDefinition> _columns = _table.getColumns();
        for(final ColumnDefinition e : _columns) {
          _builder.append("\t");
          CharSequence _generateField = this.generateField(e);
          _builder.append(_generateField, "	");
          _builder.newLineIfNotEmpty();
        }
      }
      {
        TableDefinition _table_1 = this.getTable();
        List<ColumnDefinition> _columns_1 = _table_1.getColumns();
        for(final ColumnDefinition e_1 : _columns_1) {
          _builder.append("\t");
          CharSequence _generateGetter = this.generateGetter(e_1);
          _builder.append(_generateGetter, "	");
          _builder.newLineIfNotEmpty();
          _builder.append("\t");
          CharSequence _generateSetter = this.generateSetter(e_1);
          _builder.append(_generateSetter, "	");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("\t");
      String _extraCode = this.extraCode();
      _builder.append(_extraCode, "	");
      _builder.newLineIfNotEmpty();
      _builder.append("}");
      _builder.newLine();
      _xblockexpression = (_builder);
    }
    return _xblockexpression;
  }
  
  public String extraCode() {
    return "";
  }
  
  private int _padding = 0;
  
  public int getPadding() {
    return this._padding;
  }
  
  public void setPadding(final int padding) {
    this._padding = padding;
  }
  
  public Integer determinePadding() {
    TableDefinition _table = this.getTable();
    List<ColumnDefinition> _columns = _table.getColumns();
    final Function1<ColumnDefinition,Integer> _function = new Function1<ColumnDefinition,Integer>() {
        public Integer apply(final ColumnDefinition it) {
          JavaType _javaType = PojoGenerator.this.getJavaType(it);
          String _string = _javaType.toString();
          int _length = _string.length();
          return Integer.valueOf(_length);
        }
      };
    List<Integer> _map = ListExtensions.<ColumnDefinition, Integer>map(_columns, _function);
    final Function2<Integer,Integer,Integer> _function_1 = new Function2<Integer,Integer,Integer>() {
        public Integer apply(final Integer a, final Integer b) {
          int _max = Math.max((a).intValue(), (b).intValue());
          return Integer.valueOf(_max);
        }
      };
    Integer _reduce = IterableExtensions.<Integer>reduce(_map, _function_1);
    return _reduce;
  }
  
  private static JavaType STRING_TYPE = new Function0<JavaType>() {
    public JavaType apply() {
      JavaType _javaType = new JavaType(String.class);
      return _javaType;
    }
  }.apply();
  
  public CharSequence generateField(final ColumnDefinition column) {
    CharSequence _xblockexpression = null;
    {
      final JavaType columnType = this.getJavaType(column);
      NamingStrategy _namingStrategy = this.namingStrategy();
      final String fieldName = _namingStrategy.getJavaMemberName(column, Mode.POJO);
      String _string = columnType.toString();
      int _padding = this.getPadding();
      final String paddedType = StringUtils.rightPad(_string, _padding);
      StringConcatenation _builder = new StringConcatenation();
      CharSequence _generateColumnValidationAnnotation = this.generateColumnValidationAnnotation(column, columnType);
      _builder.append(_generateColumnValidationAnnotation, "");
      _builder.newLineIfNotEmpty();
      _builder.append("private ");
      _builder.append(paddedType, "");
      _builder.append(" ");
      _builder.append(fieldName, "");
      _builder.append(";");
      _builder.newLineIfNotEmpty();
      _xblockexpression = (_builder);
    }
    return _xblockexpression;
  }
  
  public CharSequence generateColumnValidationAnnotation(final ColumnDefinition column, final JavaType columnType) {
    CharSequence _xblockexpression = null;
    {
      final boolean isStringType = PojoGenerator.STRING_TYPE.equals(columnType);
      DataTypeDefinition _type = column.getType();
      final int length = _type.getLength();
      boolean _isNullable = column.isNullable();
      final boolean notNull = (!_isNullable);
      boolean _and = false;
      if (!isStringType) {
        _and = false;
      } else {
        boolean _greaterThan = (length > 0);
        _and = (isStringType && _greaterThan);
      }
      final boolean hasMaxSize = _and;
      boolean _or = false;
      if (notNull) {
        _or = true;
      } else {
        _or = (notNull || hasMaxSize);
      }
      final boolean needsNewLine = _or;
      StringConcatenation _builder = new StringConcatenation();
      {
        if (needsNewLine) {
          _builder.newLine();
        }
      }
      {
        if (notNull) {
          _builder.append("@javax.validation.constraints.NotNull");
          _builder.newLine();
        }
      }
      {
        if (hasMaxSize) {
          _builder.append("@javax.validation.constraints.Size(max = ");
          _builder.append(length, "");
          _builder.append(")");
          _builder.newLineIfNotEmpty();
        }
      }
      _xblockexpression = (_builder);
    }
    return _xblockexpression;
  }
  
  public CharSequence generateSetter(final ColumnDefinition column) {
    CharSequence _xblockexpression = null;
    {
      NamingStrategy _namingStrategy = this.namingStrategy();
      final String name = _namingStrategy.getJavaSetterName(column);
      final JavaType columnType = this.getJavaType(column);
      NamingStrategy _namingStrategy_1 = this.namingStrategy();
      final String fieldName = _namingStrategy_1.getJavaMemberName(column, Mode.POJO);
      StringConcatenation _builder = new StringConcatenation();
      _builder.newLine();
      _builder.append("@Override");
      _builder.newLine();
      _builder.append("public void ");
      _builder.append(name, "");
      _builder.append("(");
      _builder.append(columnType, "");
      _builder.append(" ");
      _builder.append(fieldName, "");
      _builder.append(") {");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("this.");
      _builder.append(fieldName, "	");
      _builder.append(" = ");
      _builder.append(fieldName, "	");
      _builder.append(";");
      _builder.newLineIfNotEmpty();
      _builder.append("}");
      _builder.newLine();
      _xblockexpression = (_builder);
    }
    return _xblockexpression;
  }
  
  public CharSequence generateGetter(final ColumnDefinition column) {
    CharSequence _xblockexpression = null;
    {
      final JavaType columnType = this.getJavaType(column);
      NamingStrategy _namingStrategy = this.namingStrategy();
      final String name = _namingStrategy.getJavaGetterName(column);
      NamingStrategy _namingStrategy_1 = this.namingStrategy();
      final String fieldName = _namingStrategy_1.getJavaMemberName(column, Mode.POJO);
      StringConcatenation _builder = new StringConcatenation();
      _builder.newLine();
      _builder.append("@Override");
      _builder.newLine();
      _builder.append("public ");
      _builder.append(columnType, "");
      _builder.append(" ");
      _builder.append(name, "");
      _builder.append("() {");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("return this.");
      _builder.append(fieldName, "	");
      _builder.append(";");
      _builder.newLineIfNotEmpty();
      _builder.append("}");
      _builder.newLine();
      _xblockexpression = (_builder);
    }
    return _xblockexpression;
  }
  
  public JavaType getJavaType(final ColumnDefinition column) {
    DataTypeDefinition _type = column.getType();
    return this.getJavaType(_type);
  }
  
  public List<JavaType> getImplements() {
    NamingStrategy _namingStrategy = this.namingStrategy();
    TableDefinition _table = this.getTable();
    List<JavaType> _javaClassImplements = _namingStrategy.getJavaClassImplements(_table, Mode.POJO);
    return _javaClassImplements;
  }
}

