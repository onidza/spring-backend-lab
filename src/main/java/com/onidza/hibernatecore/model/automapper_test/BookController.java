package com.onidza.hibernatecore.model.automapper_test;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class BookController {

    private final BookMapper bookMapper;
    private final List<Book> library = new ArrayList<>();

    @PostMapping("/create")
    public BookDTO createBook(BookDTO bookDTO) {
        Book book = bookMapper.dtoToBook(bookDTO);
        book.setAuthor("Updated");
        book.setName("Updated");
        library.add(book);
        return bookMapper.bookToBookDTO(library.get(0));
    }
}
