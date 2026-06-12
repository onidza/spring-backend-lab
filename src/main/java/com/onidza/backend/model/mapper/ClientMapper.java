package com.onidza.backend.model.mapper;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.entity.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class ClientMapper {

    private final CouponMapper couponMapper;
    private final OrderMapper orderMapper;
    private final ProfileMapper profileMapper;

    public ClientDTO toDTO(Client client) {
        if (client == null) return null;

        return new ClientDTO(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getRegistrationDate(),

                profileMapper.toDTO(client.getProfile()),

                client.getOrders()
                        .stream()
                        .map(orderMapper::toDTO)
                        .toList(),

                client.getCoupons()
                        .stream()
                        .map(couponMapper::toDTO)
                        .toList()
        );
    }

    public Client toEntity(ClientDTO clientDTO) {
        if (clientDTO == null) return null;

        Client client = new Client(
                clientDTO.name(),
                clientDTO.email(),
                profileMapper.toEntity(clientDTO.profile())
        );

        if (client.getProfile() != null)
            client.getProfile().setClient(client);

        if (!CollectionUtils.isEmpty(clientDTO.coupons())) {
            clientDTO.coupons()
                    .stream()
                    .map(couponMapper::toEntity)
                    .forEach(client::setBiCouponClient);
        }

        if (!CollectionUtils.isEmpty(clientDTO.orders())) {
            clientDTO.orders()
                    .stream()
                    .map(orderMapper::toEntity)
                    .forEach(client::setBiOrderClient);
        }

        return client;
    }
}
