package com.onidza.hibernatecore.model.automapper_test;

import lombok.AllArgsConstructor;
import lombok.Generated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Generated
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class BookController {

    private final BookMapper bookMapper;
    private final List<Book> library = new ArrayList<>();

    @PostMapping("/create")
    public BookDTO createBook(@RequestBody BookDTO bookDTO) {
        Book book = bookMapper.dtoToBook(bookDTO);
        library.add(book);
        return bookMapper.bookToBookDTO(library.get(0));
    }
}
