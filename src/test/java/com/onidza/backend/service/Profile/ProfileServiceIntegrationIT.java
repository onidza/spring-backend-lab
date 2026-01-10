package com.onidza.backend.service.Profile;

import com.onidza.backend.model.dto.ClientDTO;
import com.onidza.backend.model.dto.ProfileDTO;
import com.onidza.backend.service.profile.ProfileServiceImpl;
import com.onidza.backend.service.client.ClientServiceImpl;
import com.onidza.backend.service.testcontainers.AbstractITConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
class ProfileServiceIntegrationIT extends AbstractITConfiguration {

    @Autowired
    private ProfileServiceImpl profileServiceImpl;

    @Autowired
    private ClientServiceImpl clientServiceImpl;

    @Test
    void getProfileById_returnProfileDTOWithRelations() {
        ClientDTO inputClientDTO = ProfileDataFactory.createInputClientDTO();

        ProfileDTO saved = clientServiceImpl.addClient(inputClientDTO).profile();
        ProfileDTO existing = profileServiceImpl.getProfileById(saved.id());

        Assertions.assertEquals(saved.id(), existing.id());
        Assertions.assertEquals(saved.address(), existing.address());
        Assertions.assertEquals(saved.phone(), existing.phone());
        Assertions.assertEquals(saved.clientId(), existing.clientId());
    }

    @Test
    void getAllProfiles_returnListOfProfilesDTOWithRelations() {
        ClientDTO firstInputClientDTO = ProfileDataFactory.createInputClientDTO();
        ClientDTO secondInputClientDTO = ProfileDataFactory.createDistinctInputClientDTO();

        clientServiceImpl.addClient(firstInputClientDTO);
        clientServiceImpl.addClient(secondInputClientDTO);

        List<ProfileDTO> result = profileServiceImpl.getAllProfiles();

        Assertions.assertEquals(2, result.size());
        Assertions.assertNotNull(result.get(0).id());
        Assertions.assertNotNull(result.get(0).clientId());

        Assertions.assertTrue(result.stream().anyMatch(p -> p.phone().equals("8(904)084-47-07")));
        Assertions.assertTrue(result.stream().anyMatch(p -> p.phone().equals("8(111)111-111-11")));

        List<Long> ids = result.stream().map(ProfileDTO::id).toList();
        Assertions.assertEquals(2, ids.stream().distinct().count());
    }

    @Test
    void updateProfile_returnProfileDTOWithRelations() {
        ClientDTO firstInputClientDTO = ProfileDataFactory.createInputClientDTO();
        ClientDTO distinctInputClientDTO = ProfileDataFactory.createDistinctInputClientDTO();

        ProfileDTO saved = clientServiceImpl.addClient(firstInputClientDTO).profile();
        ProfileDTO updated = profileServiceImpl.updateProfile(saved.id(), distinctInputClientDTO.profile());

        Assertions.assertNotEquals(saved.address(), updated.address());
        Assertions.assertNotEquals(saved.phone(), updated.phone());
        Assertions.assertEquals(saved.id(), updated.id());
        Assertions.assertEquals(saved.clientId(), updated.clientId());
    }
}
