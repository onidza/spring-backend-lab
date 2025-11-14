package com.onidza.hibernatecore.model.mapper;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;
import com.onidza.hibernatecore.model.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

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
                        .collect(Collectors.toList()),

                client.getCoupons()
                        .stream()
                        .map(couponMapper::toDTO)
                        .collect(Collectors.toList())
                );
    }

    public Client toEntity(ClientDTO clientDTO) {
        if (clientDTO == null) return null;

        Client client = new Client(
                clientDTO.name(),
                clientDTO.email(),
                profileMapper.toEntity(clientDTO.profile())
        );

        if (clientDTO.coupons() != null) {
            Set<Coupon> coupons = clientDTO.coupons()
                    .stream()
                    .map(couponMapper::toEntity)
                    .peek(coupon -> coupon.getClients().add(client))
                    .collect(Collectors.toSet());
            client.setCoupons(coupons);
        }

        if (clientDTO.orders() != null) {
            Set<Order> orders = clientDTO.orders()
                    .stream()
                    .map(orderMapper::toEntity)
                    .peek(order -> order.setClient(client))
                    .collect(Collectors.toSet());
            client.setOrders(orders);
        }

        return client;
    }
}
