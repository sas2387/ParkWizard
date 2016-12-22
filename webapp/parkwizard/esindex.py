"""
    set up elasticsearch indices
"""
from elasticsearch import TransportError

def create_parking_index(es):
    """
        create mapping of data
    """
    mappings = '''
    {
        "mappings":{
            "parking":{
                "properties": {
                    "name":{
                        "type": "string"
                    },
                    "location":{
                        "type": "geo_point"
                    },
                    "spots":{
                        "type": "integer"
                    },
                    "available":{
                        "type": "integer"
                    }
                }
            }
        }
    }
    '''
    # Ignore if index already exists
    es.indices.create(index='parkinglocations', ignore=400, body=mappings)


def create_user_index(es):
    """
        create mapping of data
    """
    mappings = '''
    {
        "mappings":{
            "user":{
                "properties": {
                    "name":{
                        "type": "string"
                    },
                    "score":{
                        "type": "integer"
                    }
                }
            }
        }
    }
    '''
    # Ignore if index already exists
    es.indices.create(index='users', ignore=400, body=mappings)


def setup(es):

    """
        Setup parking indices if does not exists already
    """
    create_parking_index(es)
    create_user_index(es)


def add_user(es, user_id, record):
    """s
        Create new user in users index
    """
    response = {"success": True}
    try:
        _es_response = es.create(index="users", doc_type="user",
                             id=user_id, body=record)
    except TransportError as error:
        if error.status_code == 409:
            """
                user already exists
            """
            score = getscore(es, user_id)
            if score['success'] is False:
                response['success'] = False
                response['message'] = "Error in confirmation"
            else:
                response['user'] = {
                    "id": user_id,
                    "name": record['name'],
                    "score": score['score']
                }
            return response
    
    """
        user created successfully
    """
    if _es_response['created'] is True:
        score = getscore(es, user_id)
        if score["success"] is False:
            response["status"] = False
            response["message"] = "Error in confirmation"
        else:
            response['user'] = {
                "id": user_id,
                "name": record['name'],
                "score": score['score']}

    return response


def getscore(es, user_id):
    """
        search user_id profile data
    """
    response = {"success": True}
    query = {
        "query": {
            "ids":{
                "values": [user_id]
            }
        }
    }
    result = es.search(index="users", size=1,
                       filter_path=['hits.hits._source.score'],
                       body={"query": query})

    if bool(result) is False or len(result) < 1:
        response["success"] = False
    else:
        result = result['hits']['hits']
        response["score"] = int(result[0]['_source']['score'])
    return response

def __search_parking(es, location, available, radius):
    """
        Get parking locations with
        atleast available number of 
        available spaces in a parking
    """
    query = {
        "filtered":{
            "query":{
                "range":{
                    "available":{
                        "gte": available
                    }
                }
            },
            "filter":{
                "geo_distance": {
                    "distance": radius,
                    "location": location
                }
            }
        }
    }

    results = es.search(index="parkinglocations", size=50,
                        filter_path=['hits.hits._id',
                                     'hits.hits._source.location',
                                     'hits.hits._source.name',
                                     'hits.hits._source.available',
                                     'hits.hits._source.spots'],
                        body={"query": query})

    if bool(results) is True:
        results = results['hits']['hits']
    else:
        results = []
    return results


def add_parking(es, user, parking):
    """
        Confirm and add a parking spot
    """
    reward = 10
    response = {"success": True}
    score = getscore(es, user)
    if score["success"] is False:
        response['success'] = False
        response['message'] = "User record not found !"
    else:
        existing = __search_parking(es, parking['location'], 0, "50m")
        # ignore if parking reported in 50m radius previously
        if len(existing) > 0:
            response["success"] = False
            response["message"] = "Duplicate parking spot !"
        else:
            es.index(index="parkinglocations", doc_type='parking',
                     body=parking)
            update = {"doc":{"score": score["score"] + reward}}
            es.update(index="users", doc_type="user", id=user, body=update)
            response['score'] = update['doc']['score']
            response['message'] = 'Score updated!'
    return response

def search_parking(es, user, cost, location, radius):
    """
        get parking search in radius
        cost of valid search
    """
    parkings = list()
    response = {
        "success": True,
        "message": None,
        "parkings": parkings}
    """
        Check valid user and number of points
    """
    score = getscore(es, user)
    if score["success"] is False:
        response['success'] = False
        response['message'] = "User record not found !"

    elif score["score"] < cost:
        response['success'] = False
        response['message'] = "Insufficient score !"

    else:
        results = __search_parking(es, location, 1, radius)
        if len(results) < 1:
            response['message'] = "No parking found !"

        else:
            update = {
                "doc":{
                    "score": score["score"] - cost
                }
            }
            es.update(index="users", doc_type="user", id=user, body=update)

            # parse results
            for result in results:
                record = dict()
                record['locid'] = result['_id']
                record['name'] = result['_source']['name']
                record['location'] = result['_source']['location']
                record['available'] = int(result['_source']['available'])
                record['spots'] = int(result['_source']['spots'])
                parkings.append(record)
            response['parkings'] = parkings
            response['message'] = str(len(parkings)) + " parking locations !"

    return response

def __getparking(es, locid):
    """
        get parking information for locid
    """
    response = {"success": True}
    query = {
        "query": {
            "ids":{
                "values": [locid]
            }
        }
    }
    result = es.search(index="parkinglocations", size=1,
                       filter_path=['hits.hits._source.spots',
                                    'hits.hits._source.available'],
                       body={"query": query})

    result = result['hits']['hits']
    if bool(result) is False or len(result) < 1:
        response["success"] = False
    else:
        response["spots"] = int(result[0]['_source']['spots'])
        response["available"] = int(result[0]['_source']['available'])
    return response


def updateparking(es, user, locid, available):
    """
        update available positions at location id
    """
    reward = 2
    response = {"success": True,
                "score": 0}
    body = {"doc":{"score": 0}}

    #validate user
    score = getscore(es, user)
    if score['success'] is False:
        response['success'] = False
        response['message'] = 'Invalid User !'
        return response

    #update parking
    parking = __getparking(es, locid)
    if available < 0 or parking['success'] is False:
        response["success"] = False
        response["message"] = "Invalid query !"
    else:
        #check available max value
        if available > parking['spots']:
            body['doc']['score'] = score['score'] - reward
            response['success'] = False
            response['message'] = "False Information"
        else:
            body['doc']['score'] = score['score'] + reward
            update = {"doc":{"available": available}}
            es.update(index='parkinglocations', doc_type='parking',
                      id=locid, body=update)
            response['message'] = "Thanks for the update!"

        #send user score update
        es.update(index='users', doc_type='user', id=user, body=body)
        response['score'] = body['doc']['score']

    return response


"""
    BEWARE: This will nuke your data completely
"""
def __delete_indices(es,indices):
    """
        Helper to delete indices found in list
    """
    es.indices.delete(index=indices, ignore=[404])
