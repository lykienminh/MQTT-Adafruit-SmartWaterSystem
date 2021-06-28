# Import standard python modules
import time, datetime 

import random

# Import Adafruit IO REST client.
from Adafruit_IO import Client, Feed

import json

# Set to your Adafruit IO key.
# Remember, your key is a secret,
# so make sure not to publish it when you publish this code!
ADAFRUIT_IO_KEY = 'aio_lpQL17WnRunnalwLSVG7WGiMWUUP'

# Set to your Adafruit IO username.
# (go to https://accounts.adafruit.com to find your username)
ADAFRUIT_IO_USERNAME = 'lykienminh'

# Create an instance of the REST client.
aio = Client(ADAFRUIT_IO_USERNAME, ADAFRUIT_IO_KEY)

pump_key = aio.feeds('pump').key
moisture_key = aio.feeds('soilmoisture').key
led_key = aio.feeds('led').key
autoSystem_key = aio.feeds('auto-system').key

moisture = 100

def setValueLed(data):
    # 0: OFF, 1: RED, 2: GREEN
    return json.dumps({"id":"1", "name":"LED", "data":data, "unit":""})

def setValueRelay(data):
    return json.dumps({"id":"11", "name":"RELAY", "data":data, "unit":"%"})

def setValueMoisture(data):
    return json.dumps({"id":"9", "name":"SOIL", "data":data, "unit":""})

def getValueJson(value):
    return int(json.loads(value)["data"])

def getValueMoisture(value, pump):
    global inc

    if value <= 0:
        return 0

    if value <= 65 and not inc and pump == 1:
        inc = True
    
    elif value > 65 and inc and pump == 0:
        inc = False

    if value > 100:
        return 100

    if inc:
        return value + random.randint(0, 10)

    return value - random.randint(0, 10)

inc = False

# aio.send_data(pump_key, setValueRelay(0)) 

while True:
    
    pump = getValueJson(aio.receive(pump_key).value)
    moisture = getValueMoisture(moisture, pump)

    aio.send_data(moisture_key, setValueMoisture(moisture)) 

    time.sleep(5)

