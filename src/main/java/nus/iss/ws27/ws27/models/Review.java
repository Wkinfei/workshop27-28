package nus.iss.ws27.ws27.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.bson.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class Review {
    @NotBlank(message="user cannot be blank or null")
    private String user;

    @Min(value=0, message="rating cannot be less-than 0")
    @Max(value=10, message="rating cannot be greater-than 10")
    @NotNull(message="rating cannot be null")
    private Integer rating;
    
    private String comment;
    
    private Integer gid;
    private LocalDate posted; 
    private String gameName;

    private Integer id;

    private Boolean edited;
    private LocalDateTime timestamp;
 

    
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getGid() {
        return gid;
    }

    public void setGid(Integer gid) {
        this.gid = gid;
    }

    public LocalDate getPosted() {
        return posted;
    }

    public void setPosted(LocalDate posted) {
        this.posted = posted;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getEdited() {
        return edited;
    }

    public void setEdited(Boolean edited) {
        this.edited = edited;
    }

    @Override
    public String toString() {
        return "Review [user=" + user + ", rating=" + rating + ", comment=" + comment + ", gid=" + gid + ", posted="
                + posted + ", gameName=" + gameName + ", id=" + id + ", edited=" + edited + "]";
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

   

    
}
