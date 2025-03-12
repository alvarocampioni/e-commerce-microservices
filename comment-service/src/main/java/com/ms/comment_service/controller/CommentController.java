package com.ms.comment_service.controller;

import com.ms.comment_service.dto.CommentDTO;
import com.ms.comment_service.model.Comment;
import com.ms.comment_service.service.CommentCacheService;
import com.ms.comment_service.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;
    private final CommentCacheService commentCacheService;

    @Autowired
    public CommentController(CommentService commentService, CommentCacheService commentCacheService) {
        this.commentService = commentService;
        this.commentCacheService = commentCacheService;
    }

    @PostMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> postComment(@RequestHeader(value = "X-USER-EMAIL") String email, @PathVariable String productId, @RequestBody CommentDTO commentDTO){
        commentService.postComment(email, productId, commentDTO);
        return new ResponseEntity<>("Comment posted !", HttpStatus.CREATED);
    }

    @PutMapping("/me/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updateComment(@RequestHeader(value = "X-USER-EMAIL") String email, @PathVariable String id, @RequestBody CommentDTO commentDTO){
        commentService.updateCommentByCommentId(email, id, commentDTO);
        return new ResponseEntity<>("Comment updated !", HttpStatus.OK);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteCommentByCustomerId(@RequestHeader(value = "X-USER-EMAIL") String email){
        commentService.deleteCommentByCustomerId(email);
        return new ResponseEntity<>("Comments deleted !", HttpStatus.OK);
    }

    @DeleteMapping("/me/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteOwnCommentByCommentId(@RequestHeader(value = "X-USER-EMAIL") String email, @PathVariable String id){
        commentService.deleteOwnCommentById(email, id);
        return new ResponseEntity<>("Comment deleted !", HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteAnyCommentById(@RequestHeader(value = "X-USER-ROLE") String role, @PathVariable String id){
        commentService.deleteAnyCommentById(role, id);
        return new ResponseEntity<>("Comment deleted !", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> getCommentById(@PathVariable String id){
        return new ResponseEntity<>(commentCacheService.getCommentById(id), HttpStatus.OK);
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Comment>> getCommentsByCustomerId(@RequestHeader(value = "X-USER-EMAIL") String email){
        return new ResponseEntity<>(commentCacheService.getCommentsByCustomerId(email), HttpStatus.OK);
    }

    @GetMapping("/me/product/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Comment>> getCommentsByCustomerIdAndProductId(@RequestHeader(value = "X-USER-EMAIL") String email, @PathVariable String productId){
        return new ResponseEntity<>(commentCacheService.getCommentsByCustomerIdAndProductId(email, productId), HttpStatus.OK);
    }

    @GetMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Comment>> getCommentsByProductId(@PathVariable String productId){
        return new ResponseEntity<>(commentCacheService.getCommentsByProductId(productId), HttpStatus.OK);
    }
}
