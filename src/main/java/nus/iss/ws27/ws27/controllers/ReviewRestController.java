package nus.iss.ws27.ws27.controllers;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.validation.Valid;
import nus.iss.ws27.ws27.models.Review;
import nus.iss.ws27.ws27.models.ReviewValidationResult;
import nus.iss.ws27.ws27.services.ReviewService;
import nus.iss.ws27.ws27.utils.Utils;

@RestController
@RequestMapping("/review")
public class ReviewRestController {
    @Autowired
    private ReviewService reviewService;


    @PostMapping(consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
    produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addReview(@Valid Review review, BindingResult bindingResult) {
        
        Optional<String> game = reviewService.findGameByGid(review.getGid());

        // custom validation
        if(game.isEmpty()) {
            FieldError err = new FieldError("review", "gid", "Invalid ID");
            bindingResult.addError(err);
        }

        // if validation fails
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors()
                                                .stream()
                                                .map(x -> x.getDefaultMessage())
                                                .toList();

            ReviewValidationResult validation = 
                new ReviewValidationResult(HttpStatus.BAD_REQUEST.value(), errors);

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(validation.toJson().toString());
        }
         // Insert document to Mongo db
        String id = reviewService.insertReview(review);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("status", HttpStatus.CREATED.value())
                        .add("timestamp", LocalDateTime.now().toString())
                        .add("message", "Review created with id %s".formatted(id))
                        .build().toString());
    }

    @PutMapping(path="/{review_id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateReview(@PathVariable(name="review_id") String id, @RequestBody String json){

        // System.out.println("\n\n\n\n\n Json"+json);
        JsonObject j = Utils.toJson(json);
        Review review = Utils.toReview(j);

        // System.out.println("\n\n\n\n\n\n Review"+review);
     
        Optional<Boolean> updated = reviewService.updateReview(id, review);

        if(updated.isEmpty()) {
            // not updated --> document not found
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                    .add("status", HttpStatus.BAD_REQUEST.value())
                    .add("timestamp", LocalDateTime.now().toString())
                    .add("error", "No record of review with id=%s".formatted(id))
                    .build().toString());
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Json.createObjectBuilder()
                .add("status", HttpStatus.CREATED.value())
                .add("timestamp", LocalDateTime.now().toString())
                .add("message", "Review with id=%s updated".formatted(id))
                .build().toString());
    }

    @GetMapping(path="/{review_id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateReview(@PathVariable(name="review_id") String id){

        Optional<String> review = reviewService.getLatestReviewById(id);
        // JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

        // for(Review r : reviews){
        //     arrBuilder.add(Utils.toJsonObject(r));
        // }

        if(review.isEmpty()){
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                    .add("status", HttpStatus.BAD_REQUEST.value())
                    .add("timestamp", LocalDateTime.now().toString())
                    .add("error", "No record of review with id=%s".formatted(id))
                    .build().toString());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(review.get());

       
    }

    @GetMapping(path="/{review_id}/history", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCommentHistory(@PathVariable(name="review_id") String id) {
        //CommentHistoryById(id) -> Using Generic Aggregation Operation
        //getCommentHistoryById(id) -> Using MongoTemplate
        Optional<String> payload = reviewService.CommentHistoryById(id);
        
        if(payload.isEmpty()){
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                    .add("status", HttpStatus.BAD_REQUEST.value())
                    .add("timestamp", LocalDateTime.now().toString())
                    .add("error", "No record of review with id=%s".formatted(id))
                    .build().toString());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload.get());
    }
}
