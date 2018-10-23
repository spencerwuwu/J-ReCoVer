// https://searchcode.com/api/result/101761074/

/*
 * #%L
 * FlatPack Client
 * %%
 * Copyright (C) 2012 Perka Inc.
 * %%
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
 * #L%
 */
package com.getperka.flatpack.ext;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.UUID;

import com.getperka.flatpack.BaseHasUuid;
import com.getperka.flatpack.HasUuid;
import com.getperka.flatpack.security.GroupPermissions;
import com.getperka.flatpack.util.UuidDigest;

/**
 * A description of an entity type.
 */
public class EntityDescription extends BaseHasUuid {
  private List<Annotation> docAnnotations;
  private String docString;
  private Class<? extends HasUuid> entityType;
  private GroupPermissions groupPermissions;
  private boolean persistent;
  private List<Property> properties;
  private EntityDescription supertype;
  private String typeName;

  EntityDescription() {}

  /**
   * Annotations that provide additional information about the entity. This could include
   * deprecation or JSR-303 validation constraints.
   */
  public List<Annotation> getDocAnnotations() {
    return docAnnotations;
  }

  public String getDocString() {
    return docString;
  }

  @NoPack
  public Class<? extends HasUuid> getEntityType() {
    return entityType;
  }

  public GroupPermissions getGroupPermissions() {
    return groupPermissions;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public EntityDescription getSupertype() {
    return supertype;
  }

  public String getTypeName() {
    return typeName;
  }

  /**
   * Indicates that instance of the the type may be persisted by the server. This hint can be used
   * to reduce payload sizes by transmitting only mutated properties.
   */
  public boolean isPersistent() {
    return persistent;
  }

  public void setDocAnnotations(List<Annotation> annotations) {
    this.docAnnotations = annotations;
  }

  public void setDocString(String docString) {
    this.docString = docString;
  }

  public void setGroupPermissions(GroupPermissions groupPermissions) {
    this.groupPermissions = groupPermissions;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return typeName;
  }

  @Override
  protected UUID defaultUuid() {
    if (typeName == null) {
      throw new IllegalStateException();
    }
    return new UuidDigest(getClass()).add(typeName).digest();
  }

  @NoPack
  void setEntityType(Class<? extends HasUuid> entityType) {
    this.entityType = entityType;
  }

  void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  void setSupertype(EntityDescription supertype) {
    this.supertype = supertype;
  }

  void setTypeName(String typeName) {
    this.typeName = typeName;
  }
}
