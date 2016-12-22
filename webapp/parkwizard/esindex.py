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

    result = result['hits']['hits']
    if len(result) < 1:
        response["success"] = False
    else:
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
                        filter_path=['hits.hits._source.location',
                                     'hits.hits._source.name',
                                     'hits.hits._source.available',
                                     'hits.hits._source.spots'],
                        body={"query": query})

    try:
        results = results['hits']['hits']
    except KeyError:
        pass
    return results


def add_parking(es, user_id, parking):
    """
        Confirm and add a parking spot
    """
    response = {"status": True}
    existing = __search_parking(es, parking['location'], 0, "50m")

    # ignore if parking reported in 50m radius previously
    if len(existing) > 0:
        response["status"] = False
        response["message"] = "Untrusted parking. Already in 50m radius"
    else:
        es.index(index="parkinglocations", doc_type='parking', body=parking)
    return response


def search_parking(es, user, location, radius):
    """
        get parking search in radius
    """
    cost = 5 #cost of valid search
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
                record['name'] = result['_source']['name']
                record['location'] = result['_source']['location']
                record['available'] = int(result['_source']['available'])
                record['spots'] = int(result['_source']['spots'])
                parkings.append(record)
            response['parkings'] = parkings
            response['message'] = str(len(parkings)) + " parking locations !"

    return response

"""
    BEWARE: This will nuke your data completely
"""
def __delete_indices(es,indices):
    """
        Helper to delete indices found in list
    """
    es.indices.delete(index=indices, ignore=[404])
