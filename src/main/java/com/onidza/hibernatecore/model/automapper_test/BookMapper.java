package com.onidza.hibernatecore.model.automapper_test;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookMapper {

    Book dtoToBook(BookDTO bookDTO);

    BookDTO bookToBookDTO(Book book);
}
