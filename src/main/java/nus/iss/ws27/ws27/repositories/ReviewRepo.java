package nus.iss.ws27.ws27.repositories;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.MongoExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.mongodb.client.result.UpdateResult;

import nus.iss.ws27.ws27.models.Review;
import nus.iss.ws27.ws27.utils.GenericAggregationOperation;
import nus.iss.ws27.ws27.utils.Utils;

import static nus.iss.ws27.ws27.utils.Constant.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ReviewRepo {
    @Autowired
	private MongoTemplate mongoTemplate;

    public String insertReview(Review review) {
            /*
        db.review.insertOne(
            {
            "user":"Peter",
            "rating":2,
            "comment": "Fun",
            "ID":2,
            "posted" : ISODate("2023-02-16T16:00:00.000+0000"),
            "name":"Pokemon"
            })
        */
        Document document = mongoTemplate.insert(Utils.toDocument(review), COLLECTION_REVIEWS);
        return document.getObjectId(FIELD_OBJECT_ID).toString();
    }

    public Optional<String> findGameById(Integer gid){
        /*
         db.getCollection("games").find({gid: 5},{name:1,_id:0});
         */
        Query query = Query.query(Criteria.where(FIELD_GID).is(gid));
        query.fields().include(FIELD_NAME).exclude(FIELD_OBJECT_ID);

        Document document = mongoTemplate.findOne(query, Document.class, COLLECTION_GAMES);

        // System.out.println("\n\n\n\n\n\n"+document);

        String name = (null == document) ? null : document.getString(FIELD_NAME);

        return Optional.ofNullable(name);
    }

    public Optional<Boolean> updateReview(String id, Review review){
        /* Using _id as it is unqiue and gid might have duplication and $push will only act on the oldest data
         db.getCollection("reviews").updateOne(
            {"_id" : ObjectId("63f9a5245801c6769abcf8aa")}
            ,
            {
                $push:{edited:{
                    comment:"hihihihi",
                        rating:8,
                        posted:ISODate("2023-02-24T18:00:00.000+0000")
                    }
                }
            }
        )
         */
        try{
         Query query = Query.query(Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id)));

         Document doc = new Document()
            .append(FIELD_COMMENT, review.getComment())
            .append(FIELD_RATING, review.getRating())
            .append(FIELD_POSTED, LocalDateTime.now());

         Update updateOps = new Update()
                            .push(FIELD_EDITED,doc);
        
        UpdateResult updateResult = mongoTemplate.updateMulti(
            query, updateOps, Document.class,COLLECTION_REVIEWS);
        
        Optional<Boolean> r = (null == updateResult) ? Optional.empty() : Optional.of(true);
        return r;
        }catch(Exception e){
            return Optional.empty();
        } 
    }

    public Optional<String> getLatestReviewById(String id){
        /*
        db.getCollection("reviews").aggregate([
    {
        $match:{"_id" : ObjectId("63f994f35801c6769abcf885")}   
    }
    ,
    {
        $project:{
            user:1,
            rating: {$ifNull: [{$last: "$edited.rating"}, "$rating"]},
            comment: {$ifNull: [{$last: "$edited.comment"}, "$comment"]},
            ID:1,
            posted:1,
            name:1,
            edited: {$cond:[{$eq:['$edited',undefined]},'false','true']
                     }
                     ,
            timestamp: "$$NOW"
            }
    }
    ]);
         */
      try{
        MatchOperation matchObjectId = Aggregation.match(
            Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id))
        );
        MongoExpression projectRating = MongoExpression.create("""
            $ifNull: [{$last: "$edited.rating"}, "$rating"]
                """);
        MongoExpression projectComment = MongoExpression.create("""
            $ifNull: [{$last: "$edited.comment"}, "$comment"]
                """);
        MongoExpression projectEdited = MongoExpression.create("""
            $cond:[{$eq:['$edited',undefined]},false,true]
            """);

        ProjectionOperation project = Aggregation.project(FIELD_USER,FIELD_ID,FIELD_POSTED,FIELD_NAME)
                                                    .andExclude(FIELD_OBJECT_ID)
                                                    .and(AggregationExpression.from(projectRating)).as(FIELD_RATING)
                                                    .and(AggregationExpression.from(projectComment)).as(FIELD_COMMENT)
                                                    .and(AggregationExpression.from(projectEdited)).as(FIELD_EDITED)
                                                    .andExpression("$$NOW").as("timestamp")
                                                    ;
        
        Aggregation pipeline = Aggregation.newAggregation(matchObjectId, project);     
        System.out.println("\n\n\n\nPipeLine"+pipeline);  
        
        AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);

        // Optional<Document> review = (null == results) ? Optional.empty() : Optional.of(results.getMappedResults().get(0));
        // List<Document> r = results.getMappedResults();
        // List<Review> reviews = r.stream() 
        //                             .map(x -> Utils.fromMongoDocument(x))
        //                             .toList();

        // List<Document> r = results.getMappedResults();
        // System.out.println("\n\n\n\n SIZE>>>>>>"+r.size());

        // return (results.getMappedResults() == null) ? 
        // Optional.empty() : 
        // Optional.of(results.getUniqueMappedResult().toJson());
        Document doc = results.getUniqueMappedResult();
        Review review = Utils.fromMongoDocument(doc);

        System.out.println("\n\n\n\n review>>>>>"+review);
        
        return (results.getMappedResults() == null) ? 
        Optional.empty() : 
        Optional.of(Utils.toJsonObject(review).toString());
      }catch(IllegalArgumentException e){
        return Optional.empty();
      }
    }

    public Optional<String> getCommentHistoryById(String id) {

        /*
                    db.getCollection("reviews").aggregate([
                {
                    $match:{"_id" : ObjectId("63f994f35801c6769abcf885")}   
                }
                ,
                {
                    $project:{
                        user:"$user",
                        rating: {$ifNull: [{$last: "$edited.rating"}, "$rating"]},
                        comment: {$ifNull: [{$last: "$edited.comment"}, "$comment"]},
                        ID:"$ID",
                        posted:{$toString: '$posted'},
                        name:"$name",
                        edited:"$edited",
                        timestamp: "{$toString: '$$NOW'}"
                        }
                }
                ]);
         */

       
         try{
            MatchOperation matchObjectId = Aggregation.match(
                Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id))
            );
         
            MongoExpression projectRating = MongoExpression.create("""
                $ifNull: [{$last: "$edited.rating"}, "$rating"]
                    """);
            MongoExpression projectComment = MongoExpression.create("""
                $ifNull: [{$last: "$edited.comment"}, "$comment"]
                    """);
    
            ProjectionOperation project = Aggregation.project()
                                                        .andExclude(FIELD_OBJECT_ID)
                                                        .andExpression("$user").as(FIELD_USER)
                                                        .and(AggregationExpression.from(projectRating)).as(FIELD_RATING)
                                                        .and(AggregationExpression.from(projectComment)).as(FIELD_COMMENT)
                                                        .andExpression("$ID").as(FIELD_ID)
                                                        .and(AggregationExpression
                                                            .from(MongoExpression.create("{$toString: '$posted'}")))
                                                            .as(FIELD_POSTED)
                                                        .andExpression("$name").as(FIELD_NAME)
                                                        .andExpression("$edited").as(FIELD_EDITED)
                                                        .and(AggregationExpression
                                                            .from(MongoExpression.create("{$toString: '$$NOW'}")))
                                                            .as("timestamp");
                                                        ;
            
            Aggregation pipeline = Aggregation.newAggregation(matchObjectId, project);     
            System.out.println("\n\n\n\nPipeLine"+pipeline);  
            
            AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);
            Document doc = results.getUniqueMappedResult();
            // Review review = Utils.fromMongoDocumentLong(doc);
    
            // System.out.println("\n\n\n\n review>>>>>"+review);
            
            return (results.getMappedResults() == null) ? 
            Optional.empty() : 
            Optional.of(doc.toJson());
        }catch(IllegalArgumentException e){
            return Optional.empty();
          }
    }

    //Using Generic Aggregation Operation
    public Optional<String> CommentHistoryById(String id) {

        /*
                    db.getCollection("reviews").aggregate([
                {
                    $match:{"_id" : ObjectId("63f994f35801c6769abcf885")}   
                }
                ,
                {
                    $project:{
                        _id:0,
                        user:"$user",
                        rating: {$ifNull: [{$last: "$edited.rating"}, "$rating"]},
                        comment: {$ifNull: [{$last: "$edited.comment"}, "$comment"]},
                        ID:"$ID",
                        posted:"{$toString: '$posted'}",
                        name:"$name",
                        edited:"$edited",
                        timestamp: {$toString: "$$NOW"}
                        }
                }
                ]);
         */
    try{
        MatchOperation matchObjectId = Aggregation.match(
            Criteria.where(FIELD_OBJECT_ID).is(new ObjectId(id))
        );
         
        final String MONGO_PROJECT_REVIEW = """
                {
                    $project:{
                        _id:0,
                        user:"$user",
                        rating: {$ifNull: [{$last: "$edited.rating"}, "$rating"]},
                        comment: {$ifNull: [{$last: "$edited.comment"}, "$comment"]},
                        ID:"$ID",
                        posted:"{$toString: '$posted'}",
                        name:"$name",
                        edited:"$edited",
                        timestamp: {$toString: '$$NOW'}
                        }
                }
                """;
        
        // AggregationOperation matchGid = new GenericAggregationOperation(MONGO_MATCH_ID);
        AggregationOperation projectReview = new GenericAggregationOperation(MONGO_PROJECT_REVIEW);
        Aggregation pipeline = Aggregation.newAggregation(matchObjectId, projectReview);

        System.out.println("\n\n\n\nPipeLine>>>>>>"+pipeline);
        AggregationResults<Document> res = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);

        Document doc = res.getUniqueMappedResult();
      
        return (res.getMappedResults() == null) ? 
        Optional.empty() : 
        Optional.of(doc.toJson());
    }catch(IllegalArgumentException e){
        return Optional.empty();
      }
    }

    //WORKSHOP 28 part a

    public Optional<String> getAllReviewsById(Integer id) {
        /*
                db.getCollection("reviews").aggregate([
            {
                $match:{ID:1}
            }
            ,
            {   //need ID for $lookup, 
                $project: { 
                            ID: 1,
                            _id: 1,
                            //"/review/<review_id>""
                            path: { $concat: ["/review/", { $toString: "$_id" }] }
                        }
            }
            ,
            {
                $group:{
                    //game_id: <ID field>
                    _id: "$ID",
                    reviews: {$push: "$path"}
                }
            }
            ,
            {
                $lookup:{
                    from:'games',
                    foreignField:'gid',
                    localField:'_id',
                    as:'game'
                }
            }
            ,
            { // to remove the []
                $unwind: "$game"
            }
            ,
            {
                $project: {
                    game_id: "$game.gid",
                    name: "$game.name",
                    year: "$game.year",
                    rank: "$game.rank",
                    users_rated: "$game.users_rated",
                    url: "$game.url",
                    thumbnail: "$game.thumbnail",
                    reviews: "$reviews",
                    timestamp: {$toString: "$$NOW"}
                }
            }  
            ])
         */
        // MongoExpression matchIdExp = MongoExpression.create("""
        //     { ID: ?0 }""", id);

        // MatchOperation matchId = Aggregation.match(AggregationExpression.from(matchIdExp));

        MatchOperation matchId = Aggregation.match(
            Criteria.where(FIELD_ID).is(id));

        MongoExpression projectPathExp = MongoExpression.create("""
                $concat: ["/review/", { $toString: "$_id" }]
                    """);
        ProjectionOperation projectPath = Aggregation.project()
                                            .andInclude(FIELD_ID, FIELD_OBJECT_ID)
                                            .and(AggregationExpression.from(projectPathExp)).as("path");
        MongoExpression groupIdExp = MongoExpression.create("""
                                                $push: "$path"
                                                    """);
        GroupOperation groupId = Aggregation.group(FIELD_ID).and("reviews", AggregationExpression.from(groupIdExp));
    
        LookupOperation lookupGames = Aggregation.lookup(COLLECTION_GAMES, FIELD_OBJECT_ID, FIELD_GID, "game");

        UnwindOperation unwindGame = Aggregation.unwind("game");
                                            
        ProjectionOperation projectOutput = Aggregation.project()
            .andExpression("'$game.gid'").as("game_id")                                                                   
            .andExpression("'$game.name'").as("name")                                                                   
            .andExpression("'$game.year'").as("year")                                                                   
            .andExpression("'$game.rank'").as("rank")                                                                    
            .andExpression("'$game.users_rated'").as("users_rated")                                                                   
            .andExpression("'$game.url'").as("url")
            .andExpression("'$game.thumbnail'").as("thumbnail")
            .andExpression("'$reviews'").as("reviews")
            .and(AggregationExpression
                .from(MongoExpression.create("{$toString: '$$NOW'}")))
                .as("timestamp");
                                            
                                            
        Aggregation pipeline = Aggregation.newAggregation(matchId, projectPath, groupId, 
                                                        lookupGames, unwindGame, projectOutput);
            
        System.out.println(pipeline.toString());

        AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);
        
        return (results.getMappedResults().size() == 0) ? 
                    Optional.empty() : 
                    Optional.of(results.getUniqueMappedResult().toJson());
    }
        
//28b
    public Optional<String> getAllGamesOrderedByRating(String direction) {
        /*
                db.getCollection("reviews").aggregate([
            {
                //need edited to get the latest rating & comment
                $addFields: {
                            edited: {$last: "$edited"}
                        }
            }
            ,
            {
                        $project: {
                            rating: {
                                $cond: [
                                    {$not: {$isNumber: "$edited.rating"}}, 
                                    "$rating",
                                    "$edited.rating"
                                ]
                            },
                            user: 1,
                            comment: {
                                $cond: [
                                    {$not: {$isNumber: "$edited.rating"}}, 
                                    "$comment",
                                    "$edited.comment"
                                ]
                            },
                            gid: "$ID",
                            name: "$name"
                        }
                    }
                    ,
                    {
                        $sort: {"rating": -1}
                    }
                    ,
                    {   //group by gid ->>question:_id: <game id>
                        $group: {
                            _id: "$gid",
                            others: {$push: "$$ROOT"}
                        }
                    }
                    ,
                    {   //$set to the take the 1st record in the array
                        $set: {
                            "others": {$first: "$others"}
                        }
                    }
                    ,
                    {   //project to change the key name
                        $project: {
                            _id: 1,
                            name: "$others.name",
                            rating: "$others.rating",
                            user: "$others.user",
                            comment: "$others.user",
                            review_id: {$toString: "$others._id"}
                        }
                    }
                    ,
                    {   //group to form array of doc -> games:[{}]
                        $group: {
                            _id: null,
                            games: {$push: '$$ROOT'}
                        }
                    }
                    ,
                    {   //result of what the question ask for
                        $project: {
                            _id: 0,
                            rating: "highest",
                            games: '$games',
                            timestamp: {$toString: "$$NOW"}   
                        }
                    }
            ])
         */

        Direction dir = null;

        switch(direction.toUpperCase()) {
            case "HIGHEST", "HIGH", "BEST", "TOP", "DESC":
                dir = Direction.DESC;
                break;
            
            case "LOWEST", "LOW", "WORST", "BOTTOM", "ASC":
                dir = Direction.ASC;
                break;

            default:
                return Optional.empty();
        }

        AddFieldsOperation addEdited = Aggregation.addFields()
        .addField("edited")
        .withValueOfExpression("{$last: '$edited'}")
        .build();
        
        final String addRatingFormatted = 
        """
            $cond: [
                {$not: {$isNumber: "$edited.rating"}}, 
                "$rating",
                "$edited.rating"
                ]
        """;

        final String addCommentFormatted = 
        """
            $cond: [
                {$not: {$isNumber: "$edited.rating"}}, 
                "$comment",
                "$edited.comment"
                ]
        """;

        MongoExpression addCommentExp = MongoExpression.create(addCommentFormatted);
        MongoExpression addRatingExp = MongoExpression.create(addRatingFormatted);

        ProjectionOperation projectData = Aggregation.project("user")
                                            .andExpression("$ID").as("gid")
                                            .andExpression("$name").as("name")
                                            .and(AggregationExpression.from(addCommentExp)).as("comment")
                                            .and(AggregationExpression.from(addRatingExp)).as("rating");

        SortOperation sortRating = Aggregation.sort(dir, "rating");

        GroupOperation groupByGid = Aggregation.group("$gid")
                                    .and("others", 
                                    AggregationExpression
                                    .from(MongoExpression
                                            .create("{$push: '$$ROOT'}")
                                            ));
        
        AddFieldsOperation addFirstReview = Aggregation.addFields()
                                            .addField("others")
                                            .withValueOfExpression("{$first: '$others'}")
                                            .build();
                                
        ProjectionOperation projectGamesList = Aggregation.project()
                    .andExpression("'$_id'").as("_id")
                    .andExpression("'$others.name'").as("name")
                    .andExpression("'$others.rating'").as("rating")
                    .andExpression("'$others.user'").as("user")
                    .andExpression("'$others.comment'").as("comment")
                    .andExpression("'$others.review_id'").as("review_id");

        GroupOperation groupNull = Aggregation.group()
                                    .and("games", 
                                    AggregationExpression.from(MongoExpression
                                                                .create("{$push: '$$ROOT'}")
                                                                ));

        ProjectionOperation projectOutput = Aggregation.project().andExclude("_id")
                        .and(direction).asLiteral().as("rating")
                        .andExpression("{$toString: '$$NOW'}").as("timestamp")
                        .andExpression("'$games'").as("games");

        Aggregation pipeline = Aggregation.newAggregation(addEdited, projectData, sortRating,
                                                            groupByGid, addFirstReview, projectGamesList,
                                                            groupNull, projectOutput);

        System.out.println(pipeline.toString());
        
        AggregationResults<Document> results = mongoTemplate.aggregate(pipeline, COLLECTION_REVIEWS, Document.class);

        // System.out.println(">>> \n\n\n\n\n" + results.getRawResults().toJson());
        
        return (null == results) ? Optional.empty() : Optional.of(results.getUniqueMappedResult().toJson());
    }
}
