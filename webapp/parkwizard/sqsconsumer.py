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

SQS = boto3.resource('sqs')
QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/908762746590/parkinglocations"

#everything aws about this project
CONFIG_FILE = os.path.dirname(os.path.realpath(__file__))
CONFIG_FILE = os.path.join(CONFIG_FILE, "config.json")

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


def process_sqs(url):
    """
        process sqs at url
    """
    receiver = get_queue(url)
    while True:
        sleep(5)
        for message in receiver.receive_messages(MaxNumberOfMessages=10):
            if message is not None:
                message.delete()
                try:
                    print message.body
                    request = json.loads(message.body)
                except ValueError:
                    continue
                if request['type'] == 'report':
                    response = addparking(request)
                    print response
                elif request['type'] == 'update':
                    response = updateparking(request)
                    print response
                else:
                    print 'Invalid request'

def main():
    """
        consume queue messages
    """
    process_sqs(QUEUE_URL)

if __name__ == '__main__':
    main()
