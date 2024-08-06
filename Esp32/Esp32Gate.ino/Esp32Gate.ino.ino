#include <WiFi.h>
#include <PubSubClient.h>
#include <WiFiClientSecure.h>
#include <ESP32Servo.h>  // Include the ESP32Servo library for servo control

// WiFi settings
const char* ssid = "MeoMaster";
const char* password = "inesleandro6122016";

// MQTT broker settings
const char* mqtt_server = "05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud";
const char* mqtt_user = "subscribeTest";
const char* mqtt_password = "subscribeTest123";
const int mqtt_port = 8883; // SSL/TLS port

// CA certificate for secure connection
static const char* root_ca PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4
WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu
ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY
MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc
h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+
0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U
A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW
T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH
B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC
B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv
KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn
OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn
jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw
qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI
rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV
HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq
hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL
ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ
3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK
NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5
ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur
TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC
jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc
oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq
4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA
mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d
emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=
-----END CERTIFICATE-----
)EOF";

// Define the servo and button pins
const int servoPin = 12; // Pin where the servo is connected
const int buttonPin = 14; // Pin where the button is connected

int buttonState = 0;
int lastButtonState = 0;
int servoPosition = 0; // Initial servo position
unsigned long lastDebounceTime = 0;
const int debounceDelay = 50; // Debounce delay for the button

WiFiClientSecure espClient;
PubSubClient client(espClient);
Servo myServo; // Create a Servo object compatible with ESP32

void setup() {
  Serial.begin(115200); // Initialize serial communication
  myServo.attach(servoPin); // Attach the servo to the pin
  myServo.write(servoPosition); // Set initial position

  pinMode(buttonPin, INPUT);

  Serial.println("Connecting to WiFi...");
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("Connected to WiFi!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  // Set the CA certificate
  espClient.setCACert(root_ca);
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);

  reconnect(); // Attempt to connect to the MQTT broker
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  int reading = digitalRead(buttonPin);

  if (reading != lastButtonState) {
    lastDebounceTime = millis();
  }

  if ((millis() - lastDebounceTime) > debounceDelay) {
    if (reading != buttonState) {
      buttonState = reading;

      if (buttonState == HIGH) {
        // Button pressed, send MQTT command to open or closed
        String stateMessage = (servoPosition == 0) ? "OPEN" : "CLOSE";
        Serial.print("Sending MQTT message to topic 'home/gate/frontgate': ");
        Serial.println(stateMessage);
        client.publish("home/gate/frontgate", stateMessage.c_str());

        // Update servo position
        servoPosition = (servoPosition == 0) ? 180 : 0;
        Serial.print("Moving servo to position: ");
        Serial.println(servoPosition);
        myServo.write(servoPosition);
      }
    }
  }

  lastButtonState = reading;
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message received on topic: ");
  Serial.println(topic);
  String message;

  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  if (String(topic) == "home/gate" || String(topic) == "home/gate/frontgate") {
    if (message == "OPEN" && servoPosition == 0) {
      servoPosition = 180;
      Serial.print("Opening gate, moving servo to position: ");
      Serial.println(servoPosition);
      myServo.write(servoPosition);
    } else if (message == "CLOSE" && servoPosition == 180) {
      servoPosition = 0;
      Serial.print("Closing gate, moving servo to position: ");
      Serial.println(servoPosition);
      myServo.write(servoPosition);
    }else if(message == "Connected" && String(topic) == "home/gate"){
    // Send current state to "home/gate/frontgate"
    String stateMessage = (servoPosition == 0) ? "CLOSE" : "OPEN";
    Serial.print("Sending current state to topic 'home/gate/frontgate': ");
    Serial.println(stateMessage);
    client.publish("home/gate/frontgate", stateMessage.c_str());
    }
  }
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Attempting to reconnect to MQTT broker...");
    if (client.connect("ESP32Client", mqtt_user, mqtt_password)) {
      Serial.println("Connected!");
      client.subscribe("home/gate");
      client.subscribe("home/gate/frontgate"); 
    } else {
      Serial.print("Failed to connect, rc=");
      Serial.print(client.state());
      Serial.println(" Try again in 5 seconds");
      delay(5000);
    }
  }
}
