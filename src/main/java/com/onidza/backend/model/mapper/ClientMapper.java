package com.onidza.backend.model.mapper;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

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
                    .forEach(client::setBidirectionalCouponClient);
        }

        if (!CollectionUtils.isEmpty(clientDTO.orders())) {
            clientDTO.orders()
                    .stream()
                    .map(orderMapper::toEntity)
                    .forEach(client::setBidirectionalOrderClient);
        }

        return client;
    }
}
