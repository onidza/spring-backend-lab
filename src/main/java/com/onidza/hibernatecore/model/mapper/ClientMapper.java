package com.onidza.hibernatecore.model.mapper;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.entity.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientMapper {

//    private final CouponMapper couponMapper;
//    private final OrderMapper orderMapper;
    private final ProfileMapper profileMapper;

    public ClientDTO toDTO(Client client) {
        if (client == null) return null;
        return new ClientDTO(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getRegistrationDate(),
                profileMapper.toDTO(client.getProfile()),
                null,
                null
        );

//                client.getOrders()
//                        .stream()
//                        .map(orderMapper::toDTO)
//                        .collect(Collectors.toList()),
//
//                client.getCoupons()
//                        .stream()
//                        .map(couponMapper::toDTO)
//                        .collect(Collectors.toList())
//                );
    }

    public Client toEntity(ClientDTO clientDTO) {
        if (clientDTO == null) return null;
        return new Client(
                clientDTO.name(),
                clientDTO.email(),
                clientDTO.registrationDate(),
                profileMapper.toEntity(clientDTO.profile())
        );
    }
}
