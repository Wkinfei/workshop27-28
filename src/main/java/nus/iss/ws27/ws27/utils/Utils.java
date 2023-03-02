package nus.iss.ws27.ws27.utils;

import org.bson.Document;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import nus.iss.ws27.ws27.models.Review;
import static nus.iss.ws27.ws27.utils.Constant.*;

import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Utils {
    public static Document toDocument(Review review) {
        Document doc = new Document();
        doc.append("user", review.getUser())
        .append("rating", review.getRating())
        .append("comment", review.getComment())
        .append("ID", review.getGid())
        .append("posted", review.getPosted()) 
        .append("name", review.getGameName());
        // .append("edited",review.getEdited())
        // .append("timestamp", review.getTimestamp());
        return doc;
    }

    public static Review fromMongoDocument(Document doc) {
        Review review = new Review();
        review.setUser(doc.getString(FIELD_USER));
        review.setRating(doc.getInteger(FIELD_RATING));
        review.setComment(doc.getString(FIELD_COMMENT));
        review.setId(doc.getInteger(FIELD_ID));
        review.setPosted(doc.getDate(FIELD_POSTED).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        review.setGameName(doc.getString(FIELD_NAME));
        review.setEdited(doc.getBoolean(FIELD_EDITED));
        review.setTimestamp(LocalDateTime.now());
        return review;
    }

    public static JsonObject toJson(String str) {
		Reader reader = new StringReader(str);
		JsonReader jsonReader = Json.createReader(reader);
		return jsonReader.readObject();
	}

    public static Review toReview(JsonObject json) {
		Review r = new Review();
		r.setComment(json.getString("comment"));
        r.setRating(json.getInt("rating"));
		r.setPosted(LocalDate.now());
		return r;
	}

    public static JsonObject toJsonObject(Review review){
        return Json.createObjectBuilder()
                    .add("user", review.getUser())
                    .add("rating", review.getRating())
                    .add("comment", review.getComment())
                    .add("ID", review.getId())
                    .add("posted", review.getPosted().toString())
                    .add("name", review.getGameName())
                    .add("edited", review.getEdited())
                    .add("timestamp", review.getTimestamp().toString())
                    .build();
    } 
}
