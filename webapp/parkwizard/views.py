import os
from django.shortcuts import render
from elasticsearch import Elasticsearch, RequestsHttpConnection
from requests_aws4auth import AWS4Auth
from django.views.decorators.http import require_GET, require_POST
import json
from django.conf import settings

#everything aws about this project
AWS_CONFIG = None
CONFIG_FILE = os.path.join(settings.BASE_DIR, "parkwizard","config.json")

def load_config(filename):
    """
        load aws configuration
    """
    config = None
    with open(filename) as handle:
        config = json.load(handle)
    return config["aws"]

AWS_CONFIG = load_config(CONFIG_FILE)

# Create your views here.
def addparking(request):
    """
        Gain Geolocation data of Parking Spot and add to elasticsearch
    """
    return HttpResponse("Hello World", "text/plain")
