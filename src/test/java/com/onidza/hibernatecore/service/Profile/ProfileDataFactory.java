package com.onidza.hibernatecore.service.Profile;

import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;

import java.time.LocalDateTime;
import java.util.HashSet;

public class ProfileDataFactory {

    static Client createPersistentClientEntityWithNullableProfile() {
        Client persistentClient = new Client(
                "Ivan",
                "ivan-st233@mail.ru",
                null
        );

        persistentClient.setId(1L);
        persistentClient.setRegistrationDate(LocalDateTime
                .of(2020, 1, 1, 12, 0));
        persistentClient.setOrders(new HashSet<>());
        persistentClient.setCoupons(new HashSet<>());

        return persistentClient;
    }

    static Client createPersistentClientEntityOneToOne() {
        Client persistentClientEntity = createPersistentClientEntityWithNullableProfile();
        Profile persistentProfileEntity = createPersistentProfileEntity();
        persistentClientEntity.setProfile(persistentProfileEntity);
        return persistentClientEntity;
    }

    static Profile createPersistentProfileEntity() {
        Profile persistentProfile = new Profile(
                "Voronezh, d.123",
                "8(904)084-47-07"
        );

        Client persistentClientEntity = createPersistentClientEntityWithNullableProfile();
        persistentClientEntity.setProfile(persistentProfile);

        persistentProfile.setId(1L);
        persistentProfile.setClient(persistentClientEntity);

        return persistentProfile;
    }

    static Profile createDistinctPersistentProfileEntity() {
        Profile persistentProfile = new Profile(
                "Moscow, d.1",
                "8(111)111-111-11"
        );

        Client persistentClientEntity = createPersistentClientEntityWithNullableProfile();
        persistentClientEntity.setProfile(persistentProfile);

        persistentProfile.setId(2L);
        persistentProfile.setClient(persistentClientEntity);

        return persistentProfile;
    }


    static ProfileDTO createPersistentProfileDTO() {
        return new ProfileDTO(
                1L,
                "Voronezh, d.123",
                "8(904)084-47-07",
                1L
        );
    }

    static ProfileDTO createPersistentProfileDTOAfterUpdate() {
        return new ProfileDTO(
                1L,
                "Moscow, d.1",
                "8(111)111-111-11",
                1L
        );
    }

    static ProfileDTO createDistinctPersistentProfileDTO() {
        return new ProfileDTO(
                2L,
                "Moscow, d.1",
                "8(111)111-111-11",
                2L
        );
    }

    static ProfileDTO createProfileDTOForUpdate() {
        return new ProfileDTO(
                null,
                "Moscow, d.1",
                "8(111)111-111-11",
                null
        );
    }
}
