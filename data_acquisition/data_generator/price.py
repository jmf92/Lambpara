import argparse
import time
import pulsar
import random

# Function that reproduces a price generator for kilowatt/minute
def rand_price_generator(pulsarIP, pulsarTopic):
    # Pulsar producer configuration
    client = pulsar.Client('pulsar://'+pulsarIP+':6650')
    producer = client.create_producer(pulsarTopic)
    while True:
        # Get actual timestamp (Event time)
        eventTime = str(time.time()).split(".")[0]
        price = str(random.uniform(0.07, 0.2))
        # Produce messages with price to pulsar
        producer.send(price.encode('utf-8'), event_timestamp=int(eventTime))
        time.sleep(10)  # interval between events is 1 minute

    client.close()

if __name__ == '__main__':
    ################# Get arguments from command line #################################
    # Pulsar:
    # -Pulsar IP
    # -Topic
    parser = argparse.ArgumentParser(description='Random generator price for power consumption.')

    # Pulsar arguments
    parser.add_argument('--pulsarIP',
                        action="store",
                        dest="pulsarIP",
                        help="IP where pulsar is located",
                        required=True)
    parser.add_argument('--pulsarTopic',
                        action="store",
                        dest="pulsarTopic",
                        help="Pulsar Topic where produce",
                        default="price")

    arguments = parser.parse_args()
    args = vars(arguments)
    rand_price_generator(pulsarIP=args.get('pulsarIP'),
                         pulsarTopic=args.get('pulsarTopic'))