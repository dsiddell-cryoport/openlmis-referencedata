package org.openlmis.referencedata.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;

import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Informative category a Product is in when assigned to a {@link Program}.
 */
@Entity
@Table(name = "product_categories", schema = "referencedata")
public class ProductCategory extends BaseEntity {

  @Embedded
  @Getter
  private Code code;

  @Embedded
  @JsonUnwrapped
  private OrderedDisplayValue orderedDisplayValue;

  private ProductCategory() {}

  /**
   * Creates a new ProductCategory.
   * @param code this ProductCategory's unique implementation code.
   * @param displayValue the display values of this ProductCategory.
   * @return a new ProductCategory.
   * @throws NullPointerException if either paramater is null.
   */
  protected ProductCategory(Code code, OrderedDisplayValue displayValue) {
    Objects.requireNonNull(code);
    Objects.requireNonNull(displayValue);
    this.code = code;
    this.orderedDisplayValue = displayValue;
  }

  /**
   * Update this from another.  Copies display values from the other ProductCategory into this one.
   * @param productCategory ProductCategory to update from.
   */
  public void updateFrom(ProductCategory productCategory) {
    this.orderedDisplayValue = productCategory.orderedDisplayValue;
  }

  /**
   * Creates a new ProductCategory.
   * @param productCategoryCode this ProductCategory's unique implementation code.
   * @param displayValue the display values of this ProductCategory.
   * @return a new ProductCategory.
   * @throws NullPointerException if either paramater is null.
   */
  public static ProductCategory createNew(Code productCategoryCode, OrderedDisplayValue
      displayValue) {
    ProductCategory category = new ProductCategory(productCategoryCode, displayValue);
    return category;
  }
}