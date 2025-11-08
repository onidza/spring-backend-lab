package com.onidza.hibernatecore.model.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MapperService {

    private final ClientMapper clientMapper;
    private final ProfileMapper profileMapper;
    private final CouponMapper couponMapper;
    private final OrderMapper orderMapper;

}
