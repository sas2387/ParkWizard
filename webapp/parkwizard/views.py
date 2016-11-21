"""
    Application logic for parkwizard
"""
import os
import json
from elasticsearch import Elasticsearch, RequestsHttpConnection, TransportError
from requests_aws4auth import AWS4Auth
from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.http import require_GET, require_POST
from django.views.decorators.csrf import csrf_exempt
from django.conf import settings
from . import esindex

#everything aws about this project
CONFIG_FILE = os.path.join(settings.BASE_DIR, "parkwizard", "config.json")

def load_config(filename):
    """
        load aws configuration
    """
    config = None
    with open(filename) as handle:
        config = json.load(handle)
    return config["aws"]

AWS_CONFIG = load_config(CONFIG_FILE)
AWS_AUTH = AWS4Auth(AWS_CONFIG['access_key'], AWS_CONFIG['secret_key'],
                    AWS_CONFIG["region"], AWS_CONFIG["service"])


# Global Elasticsearch object
ES = Elasticsearch(hosts=[{'host': AWS_CONFIG["es_node"], 'port': 443}],
                   http_auth=AWS_AUTH,
                   use_ssl=True,
                   verify_certs=True,
                   connection_class=RequestsHttpConnection)

#Setup elasticsearch indices on loading this module
esindex.setup(ES)

@require_POST
@csrf_exempt
def addparking(request):
    """
        Allow users to report parking location
    """
    # create a parking index if does not exists already
    try:
        user = request.POST['id']
        location = {
            "lat": request.POST["lat"],
            "lon": request.POST["lon"]
        }
        parking = {
            'name': request.POST['name'],
            'location': location,
            'spots': request.POST['spots'],
            'available': request.POST['spots']
        }
        esindex.add_parking(ES, user, parking)

    except KeyError:
        return HttpResponse(json.dumps("Invalid values for parking location"),
                            status=500)

    except TransportError as error:
        return HttpResponse(json.dumps(error.error), status=500, content_type="application/json")

    return HttpResponse(json.dumps("Parking added sucessfully"), content_type="application/json")


@require_GET
def getparking(request):
    """
        Get available parking locations
    """
    try:
        user = request.GET["id"]
        location = {
            "lat": request.GET["lat"],
            "lon": request.GET["lon"]
        }
        result = esindex.search_parking(ES, user, location, "500m")
        return HttpResponse(json.dumps(result),
                            content_type="application/json")

    except KeyError:
        return HttpResponse(json.dumps("Please provide a valid location"),
                            content_type="application/json", status=500)

    except TransportError:
        return HttpResponse(json.dumps("Error in searching parking locations"),
                            content_type="application/json", status=500)


@require_POST
@csrf_exempt
def adduser(request):
    """
        New User registers to system
    """
    try:
        user_id = request.POST["id"]
        name = request.POST["name"]
        record = {
            "name": name,
            "score": 100
        }
        esindex.add_user(ES, user_id, record)
        return HttpResponse(json.dumps("User added successfully"), content_type="application/json")
    except KeyError:
        return HttpResponse(json.dumps("Please provide valid user details"),
                            status=500, content_type="application/json")

    except (TransportError, Exception) as error:
        message = "Unkown error in adding user"
        if error.status_code == 409:
            message = "User already exists"
        return HttpResponse(json.dumps(message), status=500,
                            content_type="application/json")
