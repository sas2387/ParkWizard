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


def setup(es):
    """
        Setup parking indices if does not exists already
    """
    create_parking_index(es)


def add_parking(es, name, location, spots):
    """
        Confirm and add a parking spot
    """
    print name, location, spots
    pass


"""
    BEWARE: This will nuke your data completely
"""
def __delete_indices(es,indices):
    """
        Helper to delete indices found in list
    """
    es.indices.delete(index=indices, ignore=[404])