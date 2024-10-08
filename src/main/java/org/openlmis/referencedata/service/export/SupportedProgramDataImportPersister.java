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

package org.openlmis.referencedata.service.export;

import java.io.InputStream;
import java.util.List;
import org.openlmis.referencedata.domain.SupportedProgram;
import org.openlmis.referencedata.dto.SupportedProgramDto;
import org.springframework.stereotype.Service;

@Service("supportedProgram.csv")
public class SupportedProgramDataImportPersister
    implements DataImportPersister<SupportedProgram, SupportedProgramDto, SupportedProgramDto> {
  @Override
  public List<SupportedProgramDto> processAndPersist(InputStream dataStream) {
    return null;
  }

  @Override
  public List<SupportedProgram> createOrUpdate(List<SupportedProgramDto> dtoList) {
    return null;
  }
}
