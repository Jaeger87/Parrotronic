struct ServoValues {
  int minValue;
  int maxValue;
  int channel;
  String servoName;
  bool mirror;
  int lastPosition = 512;
  int counterShutDown;
  bool stopAndGo;
  int shutDownWhen;
};


int aliveCounter = 0;
const byte aliveTrigger = 10;

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(20);

}

void loop() {
  // put your main code here, to run repeatedly:

}






void deadManButton()
{
  if (aliveCounter % aliveTrigger == 0)
    Serial.println("ALIVE");

  aliveCounter++;
}
