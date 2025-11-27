package com.onidza.hibernatecore.service.Profile;

import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import com.onidza.hibernatecore.repository.ProfileRepository;
import com.onidza.hibernatecore.service.ProfileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MapperService mapperService;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfileById_returnProfileDTOWithRelations() {
        Profile persistentProfileEntity = ProfileDataFactory.createPersistentProfileEntity();
        ProfileDTO persistentProfileDTO = ProfileDataFactory.createPersistentProfileDTO();

        Mockito.when(profileRepository.findById(1L)).thenReturn(Optional.of(persistentProfileEntity));
        Mockito.when(mapperService.profileToDTO(persistentProfileEntity)).thenReturn(persistentProfileDTO);

        ProfileDTO result = profileService.getProfileById(1L);

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
                () -> profileService.getProfileById(1L));

        Mockito.verify(profileRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }


    @Test
    void getAllProfiles_returnListProfilesDTOWithRelations() {
        Profile persistentProfileEntity = ProfileDataFactory.createPersistentProfileEntity();
        Profile persistentDistinctProfileEntity = ProfileDataFactory.createDistinctPersistentProfileEntity();

        ProfileDTO persistentProfileDTO = ProfileDataFactory.createPersistentProfileDTO();
        ProfileDTO persistentDistinctProfileDTO = ProfileDataFactory.createDistinctPersistentProfileDTO();

        Mockito.when(profileRepository.findAll()).thenReturn
                (List.of(persistentProfileEntity, persistentDistinctProfileEntity));
        Mockito.when(mapperService.profileToDTO(persistentProfileEntity))
                .thenReturn(persistentProfileDTO);
        Mockito.when(mapperService.profileToDTO(persistentDistinctProfileEntity))
                .thenReturn(persistentDistinctProfileDTO);

        List<ProfileDTO> result = profileService.getAllProfiles();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(profileDTO
                -> profileDTO.clientId().equals(1L)));

        Assertions.assertTrue(result.stream().anyMatch(profileDTO
                -> profileDTO.id().equals(2L)));

        Assertions.assertTrue(result.stream().anyMatch(profileDTO
                -> profileDTO.address().equals("Moscow, d.1")));

        Mockito.verify(profileRepository).findAll();
        Mockito.verify(mapperService, Mockito.times(2))
                .profileToDTO(Mockito.any(Profile.class));
    }

    @Test
    void getAllProfiles_returnEmptyList() {
        Mockito.when(profileRepository.findAll()).thenReturn(Collections.emptyList());

        List<ProfileDTO> result = profileService.getAllProfiles();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        Mockito.verify(profileRepository).findAll();
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

        ProfileDTO result = profileService.updateProfile(1L, profileDTOForUpdate);

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
                () -> profileService.updateProfile(1L, profileDTOForUpdate));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void updateProfile_profileIsNull() {
        Client persistentClientEntity = ProfileDataFactory.createPersistentClientEntityWithNullableProfile();
        ProfileDTO profileDTOForUpdate = ProfileDataFactory.createProfileDTOForUpdate();

        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.of(persistentClientEntity));

        Assertions.assertThrows(ResponseStatusException.class,
                () -> profileService.updateProfile(1L, profileDTOForUpdate));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }
}