"""
    set up elasticsearch indices
"""
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
    """
        Create new user in users index
    """
    es.create(index="users", doc_type="user", id=user_id, body=record)


def add_parking(es, user_id, parking):
    """
        Confirm and add a parking spot
    """
    es.create(index="parkinglocations", doc_type='parking', body=parking)


def search_parking(es, user, location, radius):
    """
        get parking search in 500m radius
    """
    query = {
        "filtered":{
            "query":{
                "match_all": {}
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
                                    'hits.hits._source.available'],
                       body={"query": query})
    parkings = list()
    if len(results['hits']['hits']) == 0:
        return parkings

    for result in results['hits']['hits']:
        record = dict()
        record['name'] = result['_source']['name']
        record['location'] = result['_source']['location']
        record['available'] = result['_source']['available']
        parkings.append(record)
    return parkings

"""
    BEWARE: This will nuke your data completely
"""
def __delete_indices(es,indices):
    """
        Helper to delete indices found in list
    """
    es.indices.delete(index=indices, ignore=[404])