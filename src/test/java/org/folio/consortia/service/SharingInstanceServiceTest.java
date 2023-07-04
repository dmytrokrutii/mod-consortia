package org.folio.consortia.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.consortia.utils.EntityUtils.ACTION_ID;
import static org.folio.consortia.utils.EntityUtils.CONSORTIUM_ID;
import static org.folio.consortia.utils.EntityUtils.createSharingInstance;
import static org.folio.consortia.utils.EntityUtils.createSharingInstanceEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.folio.consortia.domain.dto.SharingInstance;
import org.folio.consortia.domain.entity.SharingInstanceEntity;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.SharingInstanceRepository;
import org.folio.consortia.service.impl.SharingInstanceServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
class SharingInstanceServiceTest {

  private static final UUID instanceIdentifier = UUID.fromString("5b157ec2-8134-4363-a7b1-c9531a7c6a54");
  @InjectMocks
  private SharingInstanceServiceImpl sharingInstanceService;
  @Mock
  private ConsortiumRepository consortiumRepository;
  @Mock
  private ConsortiumService consortiumService;
  @Mock
  private TenantService tenantService;
  @Mock
  private SharingInstanceRepository sharingInstanceRepository;
  @Mock
  private ConversionService conversionService;

  @Test
  void shouldGetSharingInstanceById() {
    SharingInstance expectedSharingInstance = createSharingInstance(ACTION_ID, instanceIdentifier, "college", "mobius");
    SharingInstanceEntity savedSharingInstance = createSharingInstanceEntity(ACTION_ID, instanceIdentifier, "college", "mobius");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(conversionService.convert(any(), any())).thenReturn(toDto(savedSharingInstance));
    doNothing().when(tenantService).checkTenantExistsOrThrow(anyString());
    when(sharingInstanceRepository.findById(any())).thenReturn(Optional.of(savedSharingInstance));

    var actualSharingInstance = sharingInstanceService.getById(UUID.randomUUID(), ACTION_ID);

    assertThat(actualSharingInstance.getId()).isEqualTo(expectedSharingInstance.getId());

    verify(sharingInstanceRepository, times(1)).findById(ACTION_ID);
  }

  @Test
  void shouldSaveSharingInstance() {
    SharingInstance sharingInstance = createSharingInstance(instanceIdentifier, "college", "mobius");
    SharingInstanceEntity savedSharingInstance = createSharingInstanceEntity(instanceIdentifier, "college", "mobius");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(conversionService.convert(any(), any())).thenReturn(toDto(savedSharingInstance));
    doNothing().when(tenantService).checkTenantExistsOrThrow(anyString());
    when(tenantService.getCentralTenantId()).thenReturn("college");
    when(sharingInstanceRepository.save(any())).thenReturn(savedSharingInstance);

    var expectedSharingInstance = createSharingInstance(instanceIdentifier, "college", "mobius");
    var actualSharingInstance = sharingInstanceService.start(UUID.randomUUID(), sharingInstance);

    assertThat(actualSharingInstance.getInstanceIdentifier()).isEqualTo(expectedSharingInstance.getInstanceIdentifier());
    assertThat(actualSharingInstance.getSourceTenantId()).isEqualTo(expectedSharingInstance.getSourceTenantId());
    assertThat(actualSharingInstance.getTargetTenantId()).isEqualTo(expectedSharingInstance.getTargetTenantId());

    verify(sharingInstanceRepository, times(1)).save(any());
  }

  /* Negative cases */
  @Test
  void shouldThrowResourceNotFoundExceptionWhenTryingToGetSharingInstanceById() {
    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(sharingInstanceRepository.findById(any())).thenReturn(Optional.empty());

    Assertions.assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class,
      () -> sharingInstanceService.getById(CONSORTIUM_ID, ACTION_ID));
  }

  private SharingInstance toDto(SharingInstanceEntity entity) {
    SharingInstance sharingInstance = new SharingInstance();
    sharingInstance.setId(entity.getId());
    sharingInstance.setInstanceIdentifier(entity.getInstanceId());
    sharingInstance.setSourceTenantId(entity.getSourceTenantId());
    sharingInstance.setTargetTenantId(entity.getTargetTenantId());
    sharingInstance.setStatus(entity.getStatus());
    sharingInstance.setError(entity.getError());
    return sharingInstance;
  }
}
