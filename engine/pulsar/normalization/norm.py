from pulsar import Function


class Normalization(Function):
    def __init__(self):
        self.sensor_enrich = {0:{"name": "plugs", "room":"basement", "lat":180, "lon":200},
                              1:{"name": "hotwater", "room":"bathroom", "lat":180, "lon":200},
                              2:{"name": "furnacefan", "room":"dining_room", "lat":180, "lon":200},
                              3:{"name": "plugs", "room":"dining_room", "lat":180, "lon":200},
                              4:{"name": "tv", "room":"dining_room", "lat":180, "lon":200},
                              5:{"name": "dryer", "room":"kitchen", "lat":182, "lon":200},
                              6:{"name": "washer", "room":"kitchen", "lat":182, "lon":200},
                              7:{"name": "dishwasher", "room":"kitchen", "lat":182, "lon":200},
                              8:{"name": "fridge", "room":"kitchen", "lat":182, "lon":200},
                              9:{"name": "oven", "room":"kitchen", "lat":182, "lon":200}
                              }
    def process(self, item, context):

        context.get_logger().info("Starting norm function...")
        context.get_logger().info("Enrichenment data: {}".format(self.sensor_enrich))

        data = item.split(",")
        context.get_logger().info("Data read: {}".format(data))
        ts = data[0]
        s_id = int(data[1])
        s_v = data[2]
        s_i = data[3]

        if ts and s_id and s_v and s_i:
            data_norm = {"timestamp": ts,
                         "sensor_id": s_id,
                         "V": s_v,
                         "I": s_i,
                         "sensor_name": self.sensor_enrich.get(s_id).get("name"),
                         "room": self.sensor_enrich.get(s_id).get("room"),
                         "lat": self.sensor_enrich.get(s_id).get("lat"),
                         "lon": self.sensor_enrich.get(s_id).get("lon")
                         }

            # context.get_logger().info("Topic: {}".format(context.get_sink()))
            # context.publish(str(data_norm).encode("utf-8"))
            return str(data_norm).encode("utf-8")
        else:
            warning = "Error parsing input element: {0}".format(item)
            context.get_logger().warn(warning)

