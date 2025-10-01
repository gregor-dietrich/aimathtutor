package de.vptr.aimathtutor.rest.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.rest.dto.PostDto;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("Should throw ValidationException when creating post with null title")
    void shouldThrowValidationExceptionWhenCreatingPostWithNullTitle() {
        PostDto postDto = new PostDto();
        postDto.title = null;
        postDto.content = "Content";
        postDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            postService.createPost(postDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating post with empty title")
    void shouldThrowValidationExceptionWhenCreatingPostWithEmptyTitle() {
        PostDto postDto = new PostDto();
        postDto.title = "";
        postDto.content = "Content";
        postDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            postService.createPost(postDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating post with null content")
    void shouldThrowValidationExceptionWhenCreatingPostWithNullContent() {
        PostDto postDto = new PostDto();
        postDto.title = "Title";
        postDto.content = null;
        postDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            postService.createPost(postDto);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating post with empty content")
    void shouldThrowValidationExceptionWhenCreatingPostWithEmptyContent() {
        PostDto postDto = new PostDto();
        postDto.title = "Title";
        postDto.content = "";
        postDto.userId = 1L;

        assertThrows(ValidationException.class, () -> {
            postService.createPost(postDto);
        });
    }
}
