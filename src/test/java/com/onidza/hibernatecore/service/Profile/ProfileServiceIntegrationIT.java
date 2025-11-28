package com.onidza.hibernatecore.service.Profile;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.service.ClientService;
import com.onidza.hibernatecore.service.ProfileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileServiceIntegrationIT {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ClientService clientService;

    @Test
    void getProfileById_returnProfileDTOWithRelations() {
        ClientDTO inputClientDTO = ProfileDataFactory.createInputClientDTO();

        ProfileDTO saved = clientService.addClient(inputClientDTO).profile();
        ProfileDTO existing = profileService.getProfileById(saved.id());

        Assertions.assertEquals(saved.id(), existing.id());
        Assertions.assertEquals(saved.address(), existing.address());
        Assertions.assertEquals(saved.phone(), existing.phone());
        Assertions.assertEquals(saved.clientId(), existing.clientId());
    }

    @Test
    void getAllProfiles_returnListOfProfilesDTOWithRelations() {
        ClientDTO firstInputClientDTO = ProfileDataFactory.createInputClientDTO();
        ClientDTO secondInputClientDTO = ProfileDataFactory.createDistinctInputClientDTO();

        clientService.addClient(firstInputClientDTO);
        clientService.addClient(secondInputClientDTO);

        List<ProfileDTO> result = profileService.getAllProfiles();

        Assertions.assertEquals(2, result.size());
        Assertions.assertNotNull(result.get(0).id());
        Assertions.assertNotNull(result.get(0).clientId());

        Assertions.assertTrue(result.stream().anyMatch(p -> p.id().equals(2L)));
        Assertions.assertTrue(result.stream().anyMatch(p -> p.phone().equals("8(904)084-47-07")));
        Assertions.assertTrue(result.stream().anyMatch(p -> p.phone().equals("8(111)111-111-11")));

        List<Long> ids = result.stream().map(ProfileDTO::id).toList();
        Assertions.assertEquals(2, ids.stream().distinct().count());
    }

    @Test
    void updateProfile_returnProfileDTOWithRelations() {
        ClientDTO firstInputClientDTO = ProfileDataFactory.createInputClientDTO();
        ClientDTO distinctInputClientDTO = ProfileDataFactory.createDistinctInputClientDTO();

        ProfileDTO saved = clientService.addClient(firstInputClientDTO).profile();
        ProfileDTO updated = profileService.updateProfile(saved.id(), distinctInputClientDTO.profile());

        Assertions.assertNotEquals(saved.address(), updated.address());
        Assertions.assertNotEquals(saved.phone(), updated.phone());
        Assertions.assertEquals(saved.id(), updated.id());
        Assertions.assertEquals(saved.clientId(), updated.clientId());
    }
}
