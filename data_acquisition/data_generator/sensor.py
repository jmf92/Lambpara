import argparse
import csv
import time
import pulsar

# Function that emulates a sensor:
# reading a CSV and
# then send it line-by-line to Apache Pulsar
def send_data(sensorPath, sensorName, pulsarIP, pulsarTopic):
    # Pulsar producer configuration
    client = pulsar.Client('pulsar://'+pulsarIP+':6650')
    producer = client.create_producer(pulsarTopic)

    # Read sensor data from CSV file
    with open(sensorPath+sensorName+'.csv') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            # Get actual timestamp (Event time)
            eventTime = str(time.time()).split(".")[0]

            # Create sensor data as CSV
            csvToSend = eventTime + ','\
                        + sensorName + ','\
                        + row['V'] + ','\
                        + row['I'] + ','\
                        + row['f'] + ','\
                        + row['DPF'] + ','\
                        + row['APF'] + ','\
                        + row['P'] + ','\
                        + row['Pt'] + ','\
                        + row['Q'] + ','\
                        + row['Qt'] + ',' \
                        + row['S'] + ','\
                        + row['St']
            print(csvToSend)

            # Produce messages to pulsar
            producer.send(csvToSend.encode('utf-8'), event_timestamp=int(eventTime))
            time.sleep(1)  # interval between events is 1 second
    client.close()


if __name__ == '__main__':

    ################# Get arguments from command line #################################
    # Sensor:
    # -Sensor path
    # -Sensor name
    # Pulsar:
    # -Pulsar IP
    # -Topic
    help_message ="Usage:\n python -sensor_path <sensor_path> "

    parser = argparse.ArgumentParser(description='Sensor emulator from CSV file.')

    # Sensor arguments
    parser.add_argument('--sensorPath',
                        action="store",
                        dest="sensorPath",
                        help="path where is sensor data",
                        required=True)
    parser.add_argument('--sensorName',
                        action="store",
                        dest="sensorName",
                        help="sensor name, help to read data file",
                        required=True)

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
                        default="raw")

    arguments = parser.parse_args()
    args = vars(arguments)

    ################# Read sensor data from file and send it to Pulsar #################################
    send_data(sensorPath=args.get('sensorPath'),
              sensorName=args.get('sensorName'),
              pulsarIP=args.get('pulsarIP'),
              pulsarTopic=args.get('pulsarTopic'))

