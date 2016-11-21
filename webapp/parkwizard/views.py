"""
    Application logic for parkwizard
"""
import os
import json
from elasticsearch import Elasticsearch, RequestsHttpConnection
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
    name = request.POST.get('name')
    location = request.POST.get('location')
    spots = request.POST.get('spots')
    esindex.add_parking(ES, name, location, spots)
    return HttpResponse("Hello World", content_type="application/json")


@require_GET
def getparking(request):
    """
        Get available parking locations
    """
    return HttpResponse("Hello World", content_type="application/json")
