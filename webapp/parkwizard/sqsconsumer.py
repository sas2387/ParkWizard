#!/usr/bin/env python
"""
    SQS message consumer
"""
import os
import json
import esindex
from time import sleep
import boto3
import botocore
from elasticsearch import Elasticsearch, RequestsHttpConnection, TransportError
from requests_aws4auth import AWS4Auth
from gcm import *

SQS = boto3.resource('sqs')
QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/908762746590/parkinglocations"

#everything aws about this project
CONFIG_FILE = os.path.dirname(os.path.realpath(__file__))
CONFIG_FILE = os.path.join(CONFIG_FILE, "config.json")

def load_config(filename, service):
    """
        load aws configuration
    """
    config = None
    with open(filename) as handle:
        config = json.load(handle)
    return config[service]

AWS_CONFIG = load_config(CONFIG_FILE, "aws")
AWS_AUTH = AWS4Auth(AWS_CONFIG['access_key'], AWS_CONFIG['secret_key'],
                    AWS_CONFIG["region"], AWS_CONFIG["service"])

GCM_CONFIG = load_config(CONFIG_FILE, "gcm")
GCM_TOKEN = GCM_CONFIG['key']
GCM_SENDER = GCM(GCM_TOKEN)

# Global Elasticsearch object
ES = Elasticsearch(hosts=[{'host': AWS_CONFIG["es_node"], 'port': 443}],
                   http_auth=AWS_AUTH,
                   use_ssl=True,
                   verify_certs=True,
                   connection_class=RequestsHttpConnection)


def get_queue(url):
    """
        returns the sqs queue at this object
    """
    try:
        queue = SQS.Queue(url)
        return queue
    except botocore.exceptions.ClientError as error:
        print error
        raise Exception('No Queue at given url')

def addparking(request):
    """
        Add new parking
    """
    # create a parking index if does not exists already
    response = {"success": True}
    try:
        user = request['id']
        location = {
            "lat": request["lat"],
            "lon": request["lon"]
        }
        parking = {
            'name': request['name'],
            'location': location,
            'spots': int(request['spots']),
            'available': int(request['spots'])
        }

        response = esindex.add_parking(ES, user, parking)

    except KeyError as error:
        response['success'] = False
        response['message'] = error

    except TransportError as error:
        response['success'] = False
        response['message'] = error.error

    return response


def updateparking(request):
    """
        Update parking
    """
    response = {"success": True}
    try:
        user = request['id']
        available = int(request["available"])
        locid = request['locid']
        response = esindex.updateparking(ES, user, locid, available)

    except KeyError as error:
        response['success'] = False
        response['message'] = error

    except TransportError as error:
        response['success'] = False
        response['message'] = error.error

    return response

def sendnotification(device, message):
    """
        send message notification to device
    """
    GCM_SENDER.plaintext_request(registration_id=device, data=message)

def process_sqs(url):
    """
        process sqs at url
    """
    receiver = get_queue(url)
    while True:
        sleep(1)
        for message in receiver.receive_messages(MaxNumberOfMessages=10):
            if message is not None:
                message.delete()
                try:
                    request = json.loads(message.body)
                    print request
                except ValueError:
                    continue

                if request['type'] == 'report':
                    response = addparking(request)
                else:
                    response = updateparking(request)

                if request['type'] == 'report' or request['type'] == 'update':
                    try:
                        print response
                        sendnotification(request['regid'], response)
                    except Exception as error:
                        print error

def main():
    """
        consume queue messages
    """
    process_sqs(QUEUE_URL)

if __name__ == '__main__':
    main()
