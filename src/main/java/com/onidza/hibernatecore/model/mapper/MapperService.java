package com.onidza.hibernatecore.model.mapper;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.model.dto.OrderDTO;
import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;
import com.onidza.hibernatecore.model.entity.Order;
import com.onidza.hibernatecore.model.entity.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MapperService {

    private final ClientMapper clientMapper;
    private final ProfileMapper profileMapper;
    private final CouponMapper couponMapper;
    private final OrderMapper orderMapper;

    public ClientDTO clientToDTO(Client client) {
        return clientMapper.toDTO(client);
    }

    public Client clientDTOToEntity(ClientDTO clientDTO) {
        return clientMapper.toEntity(clientDTO);
    }

    public CouponDTO couponToDTO(Coupon coupon) {
        return couponMapper.toDTO(coupon);
    }

    public Coupon couponDTOToEntity(CouponDTO couponDTO) {
        return couponMapper.toEntity(couponDTO);
    }

    public OrderDTO orderToDTO(Order order) {
        return orderMapper.toDTO(order);
    }

    public Order orderDTOToEntity(OrderDTO orderDTO) {
        return orderMapper.toEntity(orderDTO);
    }

    public ProfileDTO profileToDTO(Profile profile) {
        return profileMapper.toDTO(profile);
    }

    public Profile profileDTOToEntity(ProfileDTO profileDTO) {
        return profileMapper.toEntity(profileDTO);
    }
}
