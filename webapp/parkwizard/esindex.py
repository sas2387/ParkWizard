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

def add_parking(es, name, location, spots):
    """
        Confirm and add a parking spot
    """
    print name, location, spots


def search_parking(es, location):
    """
        get parking search in 500m radius
    """
    print location

"""
    BEWARE: This will nuke your data completely
"""
def __delete_indices(es,indices):
    """
        Helper to delete indices found in list
    """
    es.indices.delete(index=indices, ignore=[404])