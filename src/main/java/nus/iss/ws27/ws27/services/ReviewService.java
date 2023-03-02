package nus.iss.ws27.ws27.services;


import java.time.Instant;
import java.util.Optional;

import org.bson.BsonDateTime;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nus.iss.ws27.ws27.models.Review;
import nus.iss.ws27.ws27.repositories.ReviewRepo;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepo reviewRepo;

    public Optional<String> findGameByGid(Integer gid) {
        return reviewRepo.findGameById(gid);
    }

    public String insertReview(Review review) {
        return reviewRepo.insertReview(review);
    }

    public Optional<Boolean> updateReview(String id, Review review) {
        return reviewRepo.updateReview(id, review);
    }

    public Optional<String> getLatestReviewById(String id) {
        return reviewRepo.getLatestReviewById(id);
    }

    public Optional<String> getCommentHistory(String id) {
        Optional<String> result = reviewRepo.getCommentHistoryById(id);
        // doc.put("timestamp", new BsonDateTime(Instant.now().toEpochMilli()));
        
        return result;
    }

    public Optional<String> CommentHistoryById(String id) {
        Optional<String> result = reviewRepo.CommentHistoryById(id);
        
        return result;
    }
}
