package com.onidza.hibernatecore.model.dto;

public record ProfileDTO(
        Long id,
        String address,
        String phone,
        Long clientId
) {}
