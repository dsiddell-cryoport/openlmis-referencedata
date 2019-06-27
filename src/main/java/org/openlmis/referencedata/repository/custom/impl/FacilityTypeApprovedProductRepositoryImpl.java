/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.referencedata.repository.custom.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.hibernate.type.PostgresUUIDType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.custom.FacilityTypeApprovedProductRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class FacilityTypeApprovedProductRepositoryImpl
    implements FacilityTypeApprovedProductRepositoryCustom {

  private static final XLogger XLOGGER =
      XLoggerFactory.getXLogger(FacilityTypeApprovedProductRepositoryImpl.class);

  private static final String NATIVE_SELECT_FACILITY_TYPE_ID = "SELECT ft.id AS type_id"
      + " FROM referencedata.facility_types AS ft"
      + " INNER JOIN referencedata.facilities f ON f.typeId = ft.id AND f.id = '%s'";

  private static final String NATIVE_SELECT_FTAP_IDENTITIES = "SELECT DISTINCT"
      + "   ftap.id AS id,"
      + "   ftap.versionId AS versionId"
      + " FROM referencedata.facility_type_approved_products AS ftap"
      + " INNER JOIN (SELECT id, MAX(versionId) AS versionId"
      + "   FROM referencedata.facility_type_approved_products GROUP BY id) AS latest"
      + "   ON ftap.id = latest.id AND ftap.versionId = latest.versionId";
  private static final String NATIVE_PROGRAM_INNER_JOIN =
      " INNER JOIN referencedata.programs AS p ON p.id = ftap.programId";
  private static final String NATIVE_ORDERABLE_INNER_JOIN =
      " INNER JOIN referencedata.orderables AS o"
          + " ON o.id = ftap.orderableId"
          + " AND o.versionId IN ("
          + "   SELECT MAX(versionId)"
          + "   FROM referencedata.orderables"
          + "   WHERE id = o.id)";
  private static final String NATIVE_PROGRAM_ORDERABLE_INNER_JOIN =
      " INNER JOIN referencedata.program_orderables AS po"
          + " ON o.id = po.orderableId"
          + " AND o.versionId = po.orderableVersionId"
          + " AND p.id = po.programId"
          + " AND po.active IS TRUE";
  private static final String NATIVE_FACILITY_TYPE_INNER_JOIN =
      " INNER JOIN referencedata.facility_types AS ft ON ft.id = ftap.facilityTypeId";
  private static final String NATIVE_WHERE_FTAP_ACTIVE_FLAG = " WHERE ftap.active = :active";

  private static final String HQL_SELECT_FTAPS_BY_IDENTITES = "SELECT ftap"
      + " FROM FacilityTypeApprovedProduct AS ftap"
      + " WHERE ";

  private static final String HQL_IDENTITY = "(ftap.identity.id = '%s'"
      + " AND ftap.identity.versionId = %d)";
  private static final String OR = " OR ";

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Page<FacilityTypeApprovedProduct> searchProducts(UUID facilityId, UUID programId,
      Boolean fullSupply, List<UUID> orderableIds, Boolean active, Pageable pageable) {

    Profiler profiler = new Profiler("FTAP_REPOSITORY_SEARCH");
    profiler.setLogger(XLOGGER);

    profiler.start("SEARCH_FACILITY_TYPE_ID");
    UUID facilityTypeId = getFacilityTypeId(facilityId, profiler);

    Query nativeQuery = prepareNativeQuery(facilityTypeId, programId, fullSupply, orderableIds,
        active);
    List<Pair<UUID, Long>> identities = executeNativeQuery(nativeQuery);
    return retrieveFtaps(identities, pageable);
  }

  @Override
  public Page<FacilityTypeApprovedProduct> searchProducts(List<String> facilityTypeCodes,
      String programCode, Boolean active, Pageable pageable) {
    Query nativeQuery = prepareNativeQuery(facilityTypeCodes, programCode, active);
    List<Pair<UUID, Long>> identities = executeNativeQuery(nativeQuery);
    return retrieveFtaps(identities, pageable);
  }

  private UUID getFacilityTypeId(UUID facilityId, Profiler profiler) {
    String queryString = String.format(NATIVE_SELECT_FACILITY_TYPE_ID, facilityId);
    Query query = entityManager.createNativeQuery(queryString);

    SQLQuery sql = query.unwrap(SQLQuery.class);
    sql.addScalar("type_id", PostgresUUIDType.INSTANCE);

    try {
      return (UUID) query.getSingleResult();
    } catch (Exception ex) {
      profiler.stop().log();
      throw new ValidationMessageException(ex, FacilityMessageKeys.ERROR_NOT_FOUND);
    }
  }

  private Query prepareNativeQuery(UUID facilityTypeId, UUID programId, Boolean fullSupply,
      List<UUID> orderableIds, Boolean active) {
    StringBuilder builder = new StringBuilder(NATIVE_SELECT_FTAP_IDENTITIES);
    Map<String, Object> params = Maps.newHashMap();

    builder.append(NATIVE_PROGRAM_INNER_JOIN);
    if (null != programId) {
      builder.append(" AND p.id = :programId");
      params.put("programId", programId);
    }

    builder.append(NATIVE_ORDERABLE_INNER_JOIN);
    if (!isEmpty(orderableIds)) {
      builder.append(" AND o.id in (:orderableIds)");
      params.put("orderableIds", orderableIds);
    }

    builder.append(NATIVE_PROGRAM_ORDERABLE_INNER_JOIN);
    if (null != fullSupply) {
      builder.append(" AND po.fullSupply = :fullSupply");
      params.put("fullSupply", fullSupply);
    }

    builder.append(NATIVE_FACILITY_TYPE_INNER_JOIN);
    if (null != facilityTypeId) {
      builder.append(" AND ft.id = :facilityTypeId");
      params.put("facilityTypeId", facilityTypeId);
    }

    builder.append(NATIVE_WHERE_FTAP_ACTIVE_FLAG);
    params.put("active", null == active || active);

    return createNativeQuery(builder, params);
  }

  private Query prepareNativeQuery(List<String> facilityTypeCodes, String programCode,
      Boolean active) {
    StringBuilder builder = new StringBuilder(NATIVE_SELECT_FTAP_IDENTITIES);
    Map<String, Object> params = Maps.newHashMap();

    builder.append(NATIVE_PROGRAM_INNER_JOIN);
    if (isNotBlank(programCode)) {
      builder.append(" AND p.code = :programCode");
      params.put("programCode", programCode);
    }

    builder
        .append(NATIVE_ORDERABLE_INNER_JOIN)
        .append(NATIVE_PROGRAM_ORDERABLE_INNER_JOIN)
        .append(NATIVE_FACILITY_TYPE_INNER_JOIN);

    if (!isEmpty(facilityTypeCodes)) {
      builder.append(" AND ft.code in (:facilityTypeCodes)");
      params.put("facilityTypeCodes", facilityTypeCodes);
    }

    builder.append(NATIVE_WHERE_FTAP_ACTIVE_FLAG);
    params.put("active", null == active || active);

    return createNativeQuery(builder, params);
  }

  private Query createNativeQuery(StringBuilder builder, Map<String, Object> params) {
    Query nativeQuery = entityManager.createNativeQuery(builder.toString());
    params.forEach(nativeQuery::setParameter);

    SQLQuery sqlQuery = nativeQuery.unwrap(SQLQuery.class);
    sqlQuery.addScalar("id", PostgresUUIDType.INSTANCE);
    sqlQuery.addScalar("versionId", LongType.INSTANCE);

    return nativeQuery;
  }

  private List<Pair<UUID, Long>> executeNativeQuery(Query nativeQuery) {
    // appropriate configuration has been set in the native query
    @SuppressWarnings("unchecked")
    List<Object[]> identities = nativeQuery.getResultList();

    return identities
        .stream()
        .map(identity -> ImmutablePair.of((UUID) identity[0], (Long) identity[1]))
        .collect(Collectors.toList());
  }

  private Page<FacilityTypeApprovedProduct> retrieveFtaps(List<Pair<UUID, Long>> identities,
      Pageable pageable) {
    if (CollectionUtils.isEmpty(identities)) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);
    Integer limit = maxAndFirst.getLeft();
    Integer offset = maxAndFirst.getRight();

    String hql = HQL_SELECT_FTAPS_BY_IDENTITES + identities
        .stream()
        .map(pair -> String.format(HQL_IDENTITY, pair.getLeft(), pair.getRight()))
        .collect(Collectors.joining(OR));

    List<FacilityTypeApprovedProduct> resultList = entityManager
        .createQuery(hql, FacilityTypeApprovedProduct.class)
        .setFirstResult(offset)
        .setMaxResults(limit)
        .getResultList();

    return Pagination.getPage(resultList, pageable, identities.size());
  }

}
