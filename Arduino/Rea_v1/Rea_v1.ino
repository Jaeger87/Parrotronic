
#include <Servo.h>

Servo myservo;

int potpin = 0;
int val;
int posMax = 90;
int posMin = 30;
int ledRight = 13;
int ledLeft = 12;

void setup() {
  pinMode(ledRight, OUTPUT);
  pinMode(ledLeft, OUTPUT);
  myservo.attach(11);
}

void loop() {
  digitalWrite(ledRight, HIGH);
  digitalWrite(ledLeft, HIGH);
  val = analogRead(potpin);
  val = map(val, 0, 1023, 0, 180);
  if (val < posMax && val > posMin) {
  myservo.write(val);                  
  }
}
