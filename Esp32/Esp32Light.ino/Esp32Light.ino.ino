#include <WiFi.h>
#include <PubSubClient.h>
#include <WiFiClientSecure.h>

// WiFi network configuration
const char* ssid = "";
const char* password = ""; 

// MQTT broker configuration
const char* mqtt_server = "";
const char* mqtt_user = "";
const char* mqtt_password = "";
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

// Defining the LED pins and button pins
const int ledPin1 = 12; // LED 1 pin
const int buttonPin1 = 14; // Button 1 pin
const int ledPin2 = 27; // LED 2 pin
const int buttonPin2 = 26; // Button 2 pin

int buttonState1 = 0;
int lastButtonState1 = 0;
int ledState1 = LOW;
unsigned long lastDebounceTime1 = 0;
const int debounceDelay1 = 50; // Delay for debouncing button 1

int buttonState2 = 0;
int lastButtonState2 = 0;
int ledState2 = LOW;
unsigned long lastDebounceTime2 = 0;
const int debounceDelay2 = 50; // Delay for debouncing button 2

WiFiClientSecure espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);

  pinMode(ledPin1, OUTPUT);
  digitalWrite(ledPin1, LOW);
  pinMode(buttonPin1, INPUT);
  
  pinMode(ledPin2, OUTPUT);
  digitalWrite(ledPin2, LOW);
  pinMode(buttonPin2, INPUT);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.println("Connecting to WiFi...");
  }

  Serial.println("WiFi connected");

  // Configure the CA certificate
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

  handleButton(buttonPin1, buttonState1, lastButtonState1, ledState1, lastDebounceTime1, debounceDelay1, ledPin1, "home/light/livingroom");
  handleButton(buttonPin2, buttonState2, lastButtonState2, ledState2, lastDebounceTime2, debounceDelay2, ledPin2, "home/light/Garage");
}

void handleButton(int buttonPin, int& buttonState, int& lastButtonState, int& ledState, unsigned long& lastDebounceTime, const int debounceDelay, int ledPin, const char* topic) {
  int reading = digitalRead(buttonPin);

  if (reading != lastButtonState) {
    lastDebounceTime = millis();
  }

  if ((millis() - lastDebounceTime) > debounceDelay) {
    if (reading != buttonState) {
      buttonState = reading;

      if (buttonState == HIGH) {
        // Button is pressed, toggle the LED state
        ledState = !ledState;
        digitalWrite(ledPin, ledState);

        // Publish the new LED state
        if (ledState == HIGH) {
          client.publish(topic, "ON");
          Serial.println(String(topic) + " ON");
        } else {
          client.publish(topic, "OFF");
          Serial.println(String(topic) + " OFF");
        }
      }
    }
  }
  lastButtonState = reading;
}

void reconnect() {
  while (!client.connected()) {
    String clientId = "ESP32Client-";
    clientId += String(random(0xffff), HEX);

    if (client.connect(clientId.c_str(), mqtt_user, mqtt_password)) {
      Serial.println("Connected to MQTT broker");
      client.subscribe("home/light"); // Subscribe to the topic to control the LED
      client.subscribe("home/light/livingroom"); // Subscribe to the topic for LED 1
      client.subscribe("home/light/Garage"); // Subscribe to the topic for LED 2
    } else {
      Serial.print("Failed to connect, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

void callback(char* topic, byte* payload, unsigned int length) {
  String incomingMessage = "";
  for (unsigned int i = 0; i < length; i++) {
    incomingMessage += (char)payload[i];
  }

  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  Serial.println(incomingMessage);

  if (String(topic) == "home/light") {
    if (incomingMessage == "ON") {
      digitalWrite(ledPin1, HIGH); // Turn on LED 1
      digitalWrite(ledPin2, HIGH); // Turn on LED 2
      Serial.println("home/light ON");
      // Publish states to respective topics
      client.publish("home/light/livingroom", "ON");
      client.publish("home/light/Garage", "ON");
    } else if (incomingMessage == "OFF") {
      digitalWrite(ledPin1, LOW); // Turn off LED 1
      digitalWrite(ledPin2, LOW); // Turn off LED 2
      Serial.println("home/light OFF");
      // Publish states to respective topics
      client.publish("home/light/livingroom", "OFF");
      client.publish("home/light/Garage", "OFF");
    }else if(incomingMessage == "Connected"){

      client.publish("home/light/livingroom", ledState1 == HIGH ? "ON" : "OFF");
      Serial.print("Sent current state of LED 1: ");
      Serial.println(ledState1 == HIGH ? "ON" : "OFF");

      client.publish("home/light/Garage", ledState2 == HIGH ? "ON" : "OFF");
      Serial.print("Sent current state of LED 2: ");
      Serial.println(ledState2 == HIGH ? "ON" : "OFF");

      Serial.println("Sent current LED states due to 'Connected' message");
    }
  } else if (String(topic) == "home/light/livingroom") {
    if (incomingMessage == "ON") {
      digitalWrite(ledPin1, HIGH); // Turn on LED 1
      Serial.println("home/light/livingroom ON");
    } else if (incomingMessage == "OFF") {
      digitalWrite(ledPin1, LOW); // Turn off LED 1
      Serial.println("home/light/livingroom OFF");
    }
  } else if (String(topic) == "home/light/Garage") {
    if (incomingMessage == "ON") {
      digitalWrite(ledPin2, HIGH); // Turn on LED 2
      Serial.println("home/light/Garage ON");
    } else if (incomingMessage == "OFF") {
      digitalWrite(ledPin2, LOW); // Turn off LED 2
      Serial.println("home/light/Garage OFF");
    }
  } 
}
