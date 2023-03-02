# Contents
* [Credits](#credits)
* [Disclaimer](#disclaimers)
* [Overview](#overview)
    * [Setup](#setup)
    * [Single-Stage Aggregation](#single-stage-aggregation)
    * [Multi-Stage Aggregation](#multi-stage-aggregation)
* [Working Examples](#working-examples)



## Credits
This Java Class is adapted ~~(read: stolen)~~ from [this](https://stackoverflow.com/questions/59697496/how-to-do-a-mongo-aggregation-query-in-spring-data) StackOverflow thread. 

I'm not that good at programming. I'm just good at searching Google.
But sometimes they are the same thing. 


## Disclaimers
1. This class **DOES NOT** replace the need to know Mongo Aggregation Queries.
2. This class **DOES NOT** replace the functionalities provided by `org.springframework.data.mongodb.core.aggregation.*`. There may be some functionalities that Spring provides that this class cannot do. **USE AT YOUR OWN RISK**.
3. This class is __possbly__ **unsafe for production**. 

See [Limitations](#limitations) for details.


## Overview
Spring's `MongoTemplate` provides some methods to form and run Mongo Aggregation Queries. However, these methods are (1) very-far-removed from the native mongo query, (2) inconsistent amongst themselves, and (3) not a one-to-one map to native mongo query.

This repo showcases the `GenericAggregationOperation` class that implements `AggregationOperation` interface. It creates an `AggregationOperation` directly from native mongo query. The created instance can be used directly in the aggregation pipeline. Of course, this class bypasses all the checks that other methods may have. 

**Use at your own risk**. 

See [Limitations](#limitations).

### Setup
To use this method, copy the `GenericAggregationOperation.java` class to your project folder, ideally under the `utils` folder. Below are examples of how to use this class:

### Single-Stage Aggregation

Assume you want to do the following Mongo aggregation:
```
db.games.aggregate([
    {
        $match: {'gid': 5}
    }
])
```

First, you define the native Mongo Query as a String:
```
final String MONGO_MATCH_GID = 
"""
    {
        $match: {'gid': %s}
    }
""".formatted(id);
```

Then pass the String to the `GenericAggregationOperation` constructor to create an `AggregationOperation` instance:
```
AggregationOperation matchGid = new GenericAggregationOperation(MONGO_MATCH_GID);
```

And use the created `AggregationOperation` as you normally would:
```
Aggregation pipeline = Aggregation.newAggregation(matchGid);
AggregationResults<Document> res = mongoTemplate.aggregate(pipeline, COLLECTION_GAMES, Document.class);
```


### Multi-Stage Aggregation

For a more realistic example, assume you want to perform this multi-stage aggregation:
```
    db.games.aggregate([
        {
            $match: {'name': {$regex: 'die'}}
        }
        ,
        {
            $project: {
                _id: 0,
                'timestamp': {$toString:'$$NOW'},
                'name': {$concat: ['$name', ' (', {$toString: '$year'}, ')']},
                'rank': {$concat: [{$toString: '$ranking'} , ' (', {$toString: '$users_rated'}, ' users rated)']},
            }
        }
        ,
        {
            $group: {
                _id: null,
                games: {$push: '$$ROOT'}
            }
        }
        ,
        {
            $project: {
                _id: 0,
                status: '200',
                timestamp: {$toString: '$$NOW'},
                games: '$games'
            }
        }
    ]);
```

Similar to the previous section, you first define the each stage as a separate String:

```
final String MONGO_MATCH_GID = 
"""
    {
        $match: {'name': {$regex: %s}}
    }
""".formatted("'" + word + "'"); // use single quotes for String

final String MONGO_PROJECT_GAMES = 
"""
    {
        $project: {
            _id: 0,
            'timestamp': {$toString:'$$NOW'},
            'name': {$concat: ['$name', ' (', {$toString: '$year'}, ')']},
            'rank': {$concat: [{$toString: '$ranking'} , ' (', {$toString: '$users_rated'}, ' users rated)']}
        }
    }
""";

final String MONGO_GROUP_NULL = 
"""
    {
        $group: {
            _id: null,
            games: {$push: '$$ROOT'}
        }
    }
""";

final String MONGO_PROJECT_OUTPUT = 
"""
    {
        $project: {
            _id: 0,
            status: '200',
            timestamp: {$toString: '$$NOW'},
            games: '$games'
        }
    }
""";
```

Then pass each String to the `GenericAggregationOperation` constructor to create instances of `AggregationOperation`:

```
AggregationOperation matchGid = new GenericAggregationOperation(MONGO_MATCH_GID);
AggregationOperation projectGames = new GenericAggregationOperation(MONGO_PROJECT_GAMES);
AggregationOperation groupNull = new GenericAggregationOperation(MONGO_GROUP_NULL);        
AggregationOperation projectOutput = new GenericAggregationOperation(MONGO_PROJECT_OUTPUT);
```

And finally use the `AggregationOperation`s as you normally would:
```
Aggregation pipeline = Aggregation.newAggregation(matchGid, projectGames, groupNull, projectOutput);
AggregationResults<Document> res = mongoTemplate.aggregate(pipeline, COLLECTION_GAMES, Document.class);
```
## Working Examples

If you want to see the above code in action, you can clone this repo:
```
git clone https://github.com/GuyAtTheFront/Java-Mongo-GenericAggregationOperation.git
```

Import game.json into boardgames database as games collection:
```
mongoimport "mongodb://localhost:27017" -d boardgames -c games --jsonArray --file json/game.json --drop
```

Then start the spring-boot application:
```
mvn spring-boot:run
```

The REST endpoints are listed on `localhost:8080/` for you to test.

------------------------------------------------------------------------------------------------------------
# Contents

* [Mongo Queries](#mongo-queries)
    * `insertOne()`
    * `updateOne()`

* [Mongo Aggregation](#mongo-aggregations)
    * `$project`
        * `$ifNull` / `$last` / `$$NOW` / `$concat`
        * `$cond` / `$isNumber` / `$not`
    * `$group`
        * `$push`
        * `{$push: $$ROOT}`
    * `$lookup`
    * `$unwind`
        * `{$toString: '$$NOW'}`
    * `$sort`
    * `$group`


* [Mongo Aggregation to Java](#mongo-aggregation-to-java)
    * [`$match`](#match)
    * [`$addfields`](#addfields--set)
    * [`$project`](#project)
    * [`$group`](#group)
        * [Value](##values--objects)
        * [Object](##values--objects)
        * [Literal](#literals)
    * [`$lookup`](#lookup)
    * [`$unwind`](#unwind)
    * [`$sort`](#sort)

## Mongo Queries

* `insertOne()`

```
db.reviews.insertOne(
    {
        "user" : "test",
        "rating" : NumberInt(5),
        "comment" : "hahaha",
        "ID" : NumberInt(1),
        "posted" : ISODate("2023-02-16T16:00:00.000+0000"),
        "name" : "Die Macher"
    }
)
```


* `updateOne()` with `$push`

```
db.reviews.updateOne(
    {"_id" : ObjectId("63ee6574c60f97330fc81513")},
    {$push: {"edited": {comment: "new comment", rating: "10", posted: ISODate("2023-02-18T17:19:23.067+0000")}}}
);
```

Back to [Contents](#contents)

## Mongo Aggregations


* `$project` with `$ifNull` / `$last` / `$$NOW`

```
db.reviews.aggregate([
{
    $match: {_id: ObjectId("63ee6574c60f97330fc81513")}
}
,
{
    $project: {
        user: 1,
        rating: {$ifNull: [{$last: "$edited.rating"}, "$rating"]},
        comment: {$ifNull: [{$last: "$edited.comment"}, "$comment"]},
        ID: 1,
        posted: 1,
        name: 1,
        edited: {$ifNull: [true, false]},
        timestamp: "$$NOW"
    }
])
```

* `$project` with `$concat`
* `$group` with `$push`
* `$lookup`
* `$unwind`
* `$project` with `{$toString: '$$NOW'}`

```
db.reviews.aggregate([
    {
        $match: { ID: 1 }
    }
    ,
    {
        $project: {
            ID: 1,
            _id: 1,
            path: { $concat: ["/review/", { $toString: "$_id" }] }
        }
    }
    ,
    {
        $group: {
            _id: "$ID",
            reviews: {$push: "$path"}
        }
    }
    ,
    {
        $lookup: {
            from: "games",
            localField: "_id",
            foreignField: "gid",
            as: "game"
        }
    }
    ,
    {
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
]);
```


* `$addFields` (aka `$set`)
* `$project` with `$cond` / `$isNumber` / `$not`
* `$sort`
* `$group` with `{$push: $$ROOT}`
* `$addFields` (aka `$set`)
* `$project`
* `$group`
* `$project`

```
db.reviews.aggregate([
    {
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
    {
        $group: {
            _id: "$gid",
            others: {$push: "$$ROOT"}
        }
    }
    ,
    {
        $set: {
            "others": {$first: "$others"}
        }
    }
    ,
    {
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
    {
        $group: {
            _id: null,
            games: {$push: '$$ROOT'}
        }
    }
    ,
    {
        $project: {
            _id: 0,
            rating: "highest",
            timestamp: {$toString: "$$NOW"},
            games: '$games'
        }
    }
]);
```

Back to [Contents](#contents)

## Mongo Aggregation To Java

### `$match`

> Returns all matching documents

**Mongo:** 
```
{
    $match: { ID: <Variable> }
}
```

**Java:**
```
MongoExpression matchIdExp = MongoExpression.create("""
        { ID: ?0 }""", <Variable>);

MatchOperation matchId = Aggregation.match(AggregationExpression.from(matchIdExp));

```

Back to [Contents](#contents)

### `$addfields` / `$set`

> (a) Insert a new field to all documents, AND/OR (b) Update an existing field in all documents. 
> Will returns all fields in all documents, including the insertion/update. 
> `$project` stage can acheive the same effect, but `$project` only returns projected fields  
> i.e. `$addfields` needs fewer lines-of-code than `$project`

**Mongo:**
```
{
    $addFields: {
        edited: {$last: "$edited"}
    }
}
```

**Java:**
```
AddFieldsOperation addEdited = Aggregation.addFields()
                                .addField("edited")
                                .withValueOfExpression("{$last: '$edited'}")
                                .build();

```

Back to [Contents](#contents)

### `$project`

#### Values / Objects

**Mongo:**
```
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
```

**Java:**
```
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

ProjectionOperation projectData = Aggregation.project("gid")
                                    .andExpression("$ID").as("gid")
                                    .andExpression("$name").as("name")
                                    .and(AggregationExpression.from(addCommentExp)).as("comment")
                                    .and(AggregationExpression.from(addRatingExp)).as("rating");
```

Back to [Contents](#contents)

#### Literals

**Mongo:**
```
{
    $project: {
        _id: 0,
        rating: "highest",
        timestamp: {$toString: "$$NOW"},
        games: '$games'
    }
}
```

**Java:**
```
ProjectionOperation projectOutput = Aggregation.project().andExclude("_id")
                .and(direction).asLiteral().as("rating")
                .andExpression("{$toString: '$$NOW'}").as("timestamp")
                .andExpression("'$games'").as("games");
```

Back to [Contents](#contents)

### `$group`

#### Group by _id and push original document into output

**Mongo:**
```
{
    $group: {
        _id: "$gid",
        others: {$push: "$$ROOT"}
    }
}
```

**Java:**
```
final String pushRoot = 
"""
$push: '$$ROOT'
""";

MongoExpression pushRootExp = MongoExpression.create(pushRoot);


GroupOperation groupByGid = Aggregation.group("$gid")
                            .and("others", AggregationExpression.from(pushRootExp));
```

---------------------

#### Group Null - Group all documents into a single Array

**Mongo:**
```
{
    $group: {
        _id: null,
        games: {$push: '$$ROOT'}
    }
}
```

**Java:**
```
final String pushRoot = 
"""
$push: '$$ROOT'
""";

MongoExpression pushRootExp = MongoExpression.create(pushRoot);


GroupOperation groupByGid = Aggregation.group()
                            .and("others", AggregationExpression.from(pushRootExp));
```

Back to [Contents](#contents)

### `$lookup`
**Mongo:**
```
{
    $lookup: {
        from: "games",
        localField: "_id",
        foreignField: "gid",
        as: "game"
    }
}
```

**Java:**
```
LookupOperation lookupGames = Aggregation.lookup(
                                COLLECTION_GAMES, 
                                FIELD_OBJECT_ID, 
                                FIELD_GID, 
                                "game");
```

Back to [Contents](#contents)

### `$unwind`
**Mongo:**
```
{
    $unwind: "$game"
}
```

**Java:**
```
UnwindOperation unwindGame = Aggregation.unwind("game");
```

Back to [Contents](#contents)

### `$sort`
**Mongo:**
```
{
    $sort: {"rating": -1}
}
```

**Java:**
```
SortOperation sortRating = Aggregation.sort(Sort.Direction.DESC, "rating");
```

Back to [Contents](#contents)