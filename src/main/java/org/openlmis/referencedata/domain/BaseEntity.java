package org.openlmis.referencedata.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.util.UUID;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(generator = "uuid-gen")
  @GenericGenerator(name = "uuid-gen",
      strategy = "org.openlmis.referencedata.util.ConditionalUuidGenerator")
  @Type(type = "pg-uuid")
  private UUID id;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public UUID getId() {
    return id;
  }

  @JsonIgnore
  public void setId(UUID id) {
    this.id = id;
  }
}
