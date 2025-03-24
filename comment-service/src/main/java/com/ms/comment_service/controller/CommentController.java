package com.ms.comment_service.controller;

import com.ms.comment_service.dto.CommentDTO;
import com.ms.comment_service.model.Comment;
import com.ms.comment_service.service.CommentCacheService;
import com.ms.comment_service.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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
    @Operation(summary = "Post Comment", description = "Post a comment to a product specified by its ID.")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> postComment(@PathVariable String productId, @RequestBody CommentDTO commentDTO, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        commentService.postComment(email, productId, commentDTO);
        return new ResponseEntity<>("Comment posted !", HttpStatus.CREATED);
    }

    @PutMapping("/me/{id}")
    @Operation(summary = "Update Comment", description = "Updates user comment specified by its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updateComment(@PathVariable String id, @RequestBody CommentDTO commentDTO, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        commentService.updateCommentByCommentId(email, id, commentDTO);
        return new ResponseEntity<>("Comment updated !", HttpStatus.OK);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete Comments", description = "Deletes all user comments.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteOwnCommentsByEmail(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        commentService.deleteCommentByEmail(email);
        return new ResponseEntity<>("Comments deleted !", HttpStatus.OK);
    }

    @DeleteMapping("/me/{id}")
    @Operation(summary = "Delete Comment", description = "Deletes user comment specified by its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteOwnCommentByCommentId(@PathVariable String id, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        commentService.deleteOwnCommentById(email, id);
        return new ResponseEntity<>("Comment deleted !", HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Comment", description = "Deletes any comment specified by its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteAnyCommentById(@PathVariable String id, HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        commentService.deleteAnyCommentById(role, id);
        return new ResponseEntity<>("Comment deleted !", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Comment By ID", description = "Returns a specified comment by its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Comment> getCommentById(@PathVariable String id){
        return new ResponseEntity<>(commentCacheService.getCommentById(id), HttpStatus.OK);
    }

    @GetMapping("/me")
    @Operation(summary = "User Comments", description = "Returns all comments from a user.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Comment>> getCommentsByEmail(HttpServletRequest request){
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(commentCacheService.getCommentsByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/me/product/{productId}")
    @Operation(summary = "User Comments By Product", description = "Returns all comments from a user in a specified product by its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Comment>> getCommentsByCustomerIdAndProductId(@PathVariable String productId, HttpServletRequest request){
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(commentCacheService.getCommentsByEmailAndProductId(email, productId), HttpStatus.OK);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Product Comments", description = "Returns all comments from a product specified by its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Comment>> getCommentsByProductId(@PathVariable String productId){
        return new ResponseEntity<>(commentCacheService.getCommentsByProductId(productId), HttpStatus.OK);
    }
}
