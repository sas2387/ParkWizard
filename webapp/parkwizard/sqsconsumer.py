"""
    SQS message consumer
"""
import json
import esindex
from time import sleep
import boto3

SQS = boto3.resource('sqs')
QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/908762746590/parkinglocations"

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
                record = json.loads(message.body)
                print record

def main():
    """
        consume queue messages
    """
    process_sqs(QUEUE_URL)

if __name__ == '__main__':
    main()
