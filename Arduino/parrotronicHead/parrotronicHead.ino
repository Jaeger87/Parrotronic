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

ServoValues mouthServo;
const char eyesC = 'E';
const char mouthC = 'M';

const char thanos = 'T';
const char eyesOn = '1';

const int eyesPin =  13; //poi cambiamo

int aliveCounter = 0;
const byte aliveTrigger = 10;

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(20);

  pinMode(eyesPin, OUTPUT);

  mouthServo.minValue = 2400;
  mouthServo.maxValue = 7200;
  mouthServo.channel = 5;
  mouthServo.servoName = "Bocca";
  mouthServo.mirror = true;
  mouthServo.stopAndGo = true;
  mouthServo.shutDownWhen = 50;


}
//formato messaggi
//M;T;500 bocca;OkThanos;valore
//M;A;500 bocca;NoThanos;valore
//E;1 accendi occhi

void loop() {
  String message = Serial.readStringUntil('\n');
  if (message.length() > 0)
  {
    bool doIt = true;
    if (message.charAt(0) == mouthC && message.charAt(0) == thanos) //guanto dell'infinito?
    {
      long randNumber = random(100);

      if (randNumber < 20)
      {
        doIt = false;//Thanos
      }
    }

    if (doIt)
    {
      if (message.charAt(0) == mouthC)
        mouthMessage(message);
      else
        eyesMessage(message.charAt(2) == eyesOn);
    }
  }

  deadManButton();

}

void eyesMessage(bool onOff)
{
  if (onOff)
    digitalWrite(eyesPin, HIGH);
  else
    digitalWrite(eyesPin, LOW);
}

void mouthMessage(String message)
{

  String valueString = getValueStringSplitter(message, ';', 2);
  int value = valueString.toInt();

  mouthServo.lastPosition = value;
}

String getValueStringSplitter(String data, char separator, int index)
{
  int found = 0;
  int strIndex[] = {0, -1};
  int maxIndex = data.length() - 1;

  for (int i = 0; i <= maxIndex && found <= index; i++) {
    if (data.charAt(i) == separator || i == maxIndex) {
      found++;
      strIndex[0] = strIndex[1] + 1;
      strIndex[1] = (i == maxIndex) ? i + 1 : i;
    }
  }

  return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}


void deadManButton()
{
  if (aliveCounter % aliveTrigger == 0)
    Serial.println("ALIVE");
  aliveCounter++;
}

int analogServoConversion(int analogValue, ServoValues & servo)
{
  return map(analogValue, 0, 1023, servo.minValue, servo.maxValue);
}
