package com.onidza.backend.service.Profile;

import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.profile.ProfilesPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.ProfileRepository;
import com.onidza.backend.service.profile.ProfileServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProfileServiceUnitTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MapperService mapperService;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileServiceImpl profileServiceImpl;

    @Test
    void getProfileById_returnProfileDTOWithRelations() {
        Profile persistentProfileEntity = ProfileDataFactory.createPersistentProfileEntity();
        ProfileDTO persistentProfileDTO = ProfileDataFactory.createPersistentProfileDTO();

        Mockito.when(profileRepository.findById(1L)).thenReturn(Optional.of(persistentProfileEntity));
        Mockito.when(mapperService.profileToDTO(persistentProfileEntity)).thenReturn(persistentProfileDTO);

        ProfileDTO result = profileServiceImpl.getProfileById(1L);

        Assertions.assertNotNull(result.id());
        Assertions.assertNotNull(result.clientId());

        Assertions.assertEquals("8(904)084-47-07", result.phone());
        Assertions.assertEquals("Voronezh, d.123", result.address());


        Mockito.verify(profileRepository).findById(1L);
        Mockito.verify(mapperService).profileToDTO(persistentProfileEntity);
    }

    @Test
    void getProfileById_notFound_ThrowsException() {
        Mockito.when(profileRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> profileServiceImpl.getProfileById(1L));

        Mockito.verify(profileRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void getProfilesPage_returnPageDTOWithRelations() {
        Profile persistentProfileEntity = ProfileDataFactory.createPersistentProfileEntity();
        Profile persistentDistinctProfileEntity = ProfileDataFactory.createDistinctPersistentProfileEntity();

        ProfileDTO persistentProfileDTO = ProfileDataFactory.createPersistentProfileDTO();
        ProfileDTO persistentDistinctProfileDTO = ProfileDataFactory.createDistinctPersistentProfileDTO();

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Slice<Profile> sliceFromRepo = new SliceImpl<>(
                List.of(persistentProfileEntity, persistentDistinctProfileEntity),
                pageable,
                false
        );

        Mockito.when(profileRepository.findBy(pageable))
                .thenReturn(sliceFromRepo);

        Mockito.when(mapperService.profileToDTO(persistentProfileEntity))
                .thenReturn(persistentProfileDTO);
        Mockito.when(mapperService.profileToDTO(persistentDistinctProfileEntity))
                .thenReturn(persistentDistinctProfileDTO);

        ProfilesPageDTO result = profileServiceImpl.getProfilesPage(0, 20);

        // then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.items().size());
        Assertions.assertEquals(0, result.page());
        Assertions.assertEquals(20, result.size());
        Assertions.assertFalse(result.hasNext());

        Assertions.assertTrue(result.items().stream().anyMatch(p -> p.clientId().equals(1L)));
        Assertions.assertTrue(result.items().stream().anyMatch(p -> p.id().equals(2L)));
        Assertions.assertTrue(result.items().stream().anyMatch(p -> "Moscow, d.1".equals(p.address())));

        Mockito.verify(profileRepository).findBy(pageable);
        Mockito.verify(mapperService).profileToDTO(persistentProfileEntity);
        Mockito.verify(mapperService).profileToDTO(persistentDistinctProfileEntity);
        Mockito.verifyNoMoreInteractions(profileRepository, mapperService);
    }

    @Test
    void getProfiles_Page_returnEmptyPage() {
        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Slice<Profile> emptySlice = new SliceImpl<>(
                Collections.emptyList(),
                pageable,
                false
        );

        Mockito.when(profileRepository.findBy(pageable))
                .thenReturn(emptySlice);

        ProfilesPageDTO result = profileServiceImpl.getProfilesPage(0, 20);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.items().isEmpty());
        Assertions.assertEquals(0, result.page());
        Assertions.assertEquals(20, result.size());
        Assertions.assertFalse(result.hasNext());

        Mockito.verify(profileRepository).findBy(pageable);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void updateProfile_returnProfileDTOWithRelations() {
        Client persistentClientEntity = ProfileDataFactory.createPersistentClientEntityOneToOne();
        ProfileDTO profileDTOForUpdate = ProfileDataFactory.createProfileDTOForUpdate();
        ProfileDTO profileDTOAfterUpdate = ProfileDataFactory.createPersistentProfileDTOAfterUpdate();


        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.of(persistentClientEntity));
        Mockito.when(mapperService.profileToDTO(persistentClientEntity.getProfile()))
                .thenReturn(profileDTOAfterUpdate);

        ProfileDTO result = profileServiceImpl.updateProfile(1L, profileDTOForUpdate);

        Assertions.assertEquals(persistentClientEntity.getId(), result.id());
        Assertions.assertEquals(persistentClientEntity.getProfile().getClient().getId(), result.clientId());
        Assertions.assertEquals("Moscow, d.1", persistentClientEntity.getProfile().getAddress());
        Assertions.assertEquals("8(111)111-111-11", persistentClientEntity.getProfile().getPhone());

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verify(mapperService).profileToDTO(persistentClientEntity.getProfile());
    }

    @Test
    void updateProfile_clientNotFound() {
        ProfileDTO profileDTOForUpdate = ProfileDataFactory.createProfileDTOForUpdate();

        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> profileServiceImpl.updateProfile(1L, profileDTOForUpdate));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void updateProfile_profileIsNull() {
        Client persistentClientEntity = ProfileDataFactory.createPersistentClientEntityWithNullableProfile();
        ProfileDTO profileDTOForUpdate = ProfileDataFactory.createProfileDTOForUpdate();

        Mockito.when(clientRepository.findById(1L))
                .thenReturn(Optional.of(persistentClientEntity));

        Assertions.assertThrows(ResponseStatusException.class,
                () -> profileServiceImpl.updateProfile(1L, profileDTOForUpdate));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }
}