package com.solarvision.backend.equipment;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.equipment.dto.EquipmentRequest;
import com.solarvision.backend.project.Project;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.project.SiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private AccessService accessService;

    @InjectMocks
    private EquipmentService service;

    private Site siteOfProject(Long siteId, Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        Site site = new Site();
        site.setId(siteId);
        site.setProject(project);
        return site;
    }

    private Equipment equipmentAt(Long id, Site site) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setSite(site);
        return equipment;
    }

    private EquipmentRequest validRequest(Long siteId) {
        EquipmentRequest request = new EquipmentRequest();
        request.setSiteId(siteId);
        request.setEquipmentType("PANEL");
        request.setQrCode("QR-TEST-1");
        return request;
    }

    // ---------- getEquipmentById ----------

    @Test
    void getEquipmentById_returnsEquipment_whenProjectIsVisible() {
        Equipment equipment = equipmentAt(10L, siteOfProject(1L, 1L));
        when(equipmentRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(equipment));
        when(accessService.canAccessProject(1L)).thenReturn(true);

        assertThat(service.getEquipmentById(10L)).isSameAs(equipment);
    }

    @Test
    void getEquipmentById_throws404_whenProjectIsNotVisible() {
        Equipment equipment = equipmentAt(10L, siteOfProject(1L, 1L));
        when(equipmentRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(equipment));
        when(accessService.canAccessProject(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.getEquipmentById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Equipment not found");
    }

    // ---------- getEquipmentBySite ----------

    @Test
    void getEquipmentBySite_returnsList_whenSiteIsVisible() {
        Site site = siteOfProject(1L, 1L);
        Equipment equipment = equipmentAt(10L, site);
        when(siteRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(site));
        when(accessService.canAccessProject(1L)).thenReturn(true);
        when(equipmentRepository.findBySiteIdWithDetails(1L)).thenReturn(List.of(equipment));

        assertThat(service.getEquipmentBySite(1L)).containsExactly(equipment);
    }

    @Test
    void getEquipmentBySite_throws404_andListsNothing_whenSiteIsNotVisible() {
        when(siteRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(siteOfProject(1L, 1L)));
        when(accessService.canAccessProject(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.getEquipmentBySite(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Site not found");
        verify(equipmentRepository, never()).findBySiteIdWithDetails(anyLong());
    }

    // ---------- getEquipmentByQrCode ----------

    @Test
    void getEquipmentByQrCode_throws404_whenProjectIsNotVisible() {
        Equipment equipment = equipmentAt(10L, siteOfProject(1L, 1L));
        when(equipmentRepository.findByQrCode("QR-1")).thenReturn(Optional.of(equipment));
        when(accessService.canAccessProject(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.getEquipmentByQrCode("QR-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Equipment not found for this QR code");
    }

    @Test
    void getEquipmentByQrCode_returnsEquipment_withoutReloading_whenVisible() {
        Equipment equipment = equipmentAt(10L, siteOfProject(1L, 1L));
        when(equipmentRepository.findByQrCode("QR-1")).thenReturn(Optional.of(equipment));
        when(accessService.canAccessProject(1L)).thenReturn(true);

        assertThat(service.getEquipmentByQrCode("QR-1")).isSameAs(equipment);
        // only ONE query: the old second lookup by id must be gone
        verify(equipmentRepository, never()).findByIdWithDetails(anyLong());
    }

    // ---------- createEquipment ----------

    @Test
    void createEquipment_throws404_andSavesNothing_whenSiteIsNotVisible() {
        when(siteRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(siteOfProject(1L, 1L)));
        when(accessService.canAccessProject(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.createEquipment(validRequest(1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Site not found");

        // the QR code was never checked, so nothing leaks to a stranger
        verify(equipmentRepository, never()).existsByQrCode(anyString());
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void createEquipment_saves_whenSiteIsVisible() {
        Site site = siteOfProject(1L, 1L);
        Equipment saved = equipmentAt(99L, site);
        when(siteRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(site));
        when(accessService.canAccessProject(1L)).thenReturn(true);
        when(equipmentRepository.existsByQrCode("QR-TEST-1")).thenReturn(false);
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(saved);
        when(equipmentRepository.findByIdWithDetails(99L)).thenReturn(Optional.of(saved));

        assertThat(service.createEquipment(validRequest(1L))).isSameAs(saved);
        verify(equipmentRepository).save(any(Equipment.class));
    }
}