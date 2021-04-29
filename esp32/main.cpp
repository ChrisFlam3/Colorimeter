#include <Arduino.h>
#include <HardwareSerial.h>
#include <cstring>
#define TERMINATION_CHAR 0x0D

char buffer[16];
uint8_t cmdAT[3] = {'A','T',0};
uint8_t cmdXYZ[7] = {'A','T','X','Y','Z','C',0};
uint8_t cmdXY[11] = {'A','T','S','M','A','L','L','X','Y','C',0};
uint8_t cmdDATA[7] = {'A','T','D','A','T','A',0};
uint8_t cmdLUX[7] = {'A','T','L','U','X','C',0};
uint8_t cmdCCT[7] = {'A','T','C','C','T','C',0};
uint8_t cmdLED0[9]={'A','T','L','E','D','0','=','0',0};
uint8_t cmdLED100[9]={'A','T','L','E','D','0','=','1',0};
uint8_t cmdLED[7]={'A','T','L','E','D','0',0};
uint8_t cmdINTTIME100[14] = {'A','T','I','N','T','T','I','M','E','=','1','0','0',0};
uint8_t cmdGAIN3[9] = {'A','T','G','A','I','N','=','3',0};
uint8_t cmdMODE2[10] = {'A','T','T','C','S','M','D','=','2',0};

HardwareSerial sensorSerial(1);

int sample_num=10;
float* XYZ;
float* xy;
float* CCT;
static void transmitCommand( uint8_t *pInput, uint8_t delimiter )
{
    while (0 != *pInput)
    {
        sensorSerial.write(*pInput++);
    }
    sensorSerial.write(delimiter);
    sensorSerial.write('\n');
}

static char* readAT(){
  for(int i=0;sensorSerial.available();i++)
    buffer[i]= sensorSerial.read();

  Serial.println(buffer);
  return buffer;
}

void emptySensorSerial(){
  while(sensorSerial.available())
    char t = sensorSerial.read();
}

void configure(){
  transmitCommand(cmdGAIN3, TERMINATION_CHAR);
  transmitCommand(cmdMODE2, TERMINATION_CHAR);
  while(sensorSerial.available()==0)
    continue;
  emptySensorSerial();
}

void setup() {
  Serial.begin(115200);
  sensorSerial.begin(115200, SERIAL_8N1, 26, 27);
  configure();
}

float adj(float C) {
  if (abs(C) < 0.0031308) {
    return 12.92 * C;
  }
  return 1.055 * pow(C, 0.41666) - 0.055;
}

void get_xy(float* x,float* y,int id){

  float X_avg=0;
  float Y_avg=0;
  float Z_avg=0;
  for(int i=0;i<sample_num;i++){
    transmitCommand(cmdDATA,TERMINATION_CHAR);
    float X=sensorSerial.parseInt();
    float Y=sensorSerial.parseInt();
    float Z=sensorSerial.parseInt();
    float NIR=sensorSerial.parseInt();
    float D=sensorSerial.parseInt();
    float C=sensorSerial.parseInt();
    
    //Serial.println(X);
    //Serial.println(Y);
    //Serial.println(Z);

    X_avg+=X;
    Y_avg+=Y;
    Z_avg+=Z;
    
  }
  X_avg/=sample_num;
  Y_avg/=sample_num;
  Z_avg/=sample_num;
  
  XYZ[id*3]=X_avg;
  XYZ[id*3+1]=Y_avg;
  XYZ[id*3+2]=Z_avg;

  //Serial.println(X_avg);
  //Serial.println(Y_avg);
  //Serial.println(Z_avg);
  
  int sum=X_avg+Y_avg+Z_avg;
  X_avg/=sum;
  Y_avg/=sum;
  Z_avg/=sum;

  //Serial.println(X_avg);
  //Serial.println(Y_avg);
  //Serial.println(Z_avg);

  *x=X_avg;
  *y=Y_avg;
}
void compare_xy_RGB(float x,float y,uint8_t* rgb,int id){
  //convert to XYZ
  float R=rgb[0]/255.0;
  float G=rgb[1]/255.0;
  float B=rgb[2]/255.0;

  float X=0.4124564*R+0.3575761*G+0.1804375*B;
  float Y=0.2126729*R+0.7151522*G+0.072175*B;
  float Z=0.0193339*R+0.119192*G+0.9503041*B;

  //convert to xy
  float sum=X+Y+Z;
  float x_in=X/sum;
  float y_in=Y/sum;

  //compare
  xy[id*2]=x-x_in;
  xy[id*2+1]=y-y_in;
  //Serial.println(x-x_in,4);
  //Serial.println(y-y_in,4);

}

void sendDifferences(float* xy,int test_num){
  for(int i=0;i<2*test_num;i+=2){
    Serial.print(xy[i],4);
    Serial.print(",");
    Serial.print(xy[i+1],4);
    Serial.print(",");
  }
  Serial.write(255);
}

void testSeq(){
  int test_num;
  while(Serial.available()<1)
    continue;
  test_num=Serial.parseInt();
  XYZ=(float*)malloc(test_num*3*sizeof(float));
  xy=(float*)malloc(test_num*2*sizeof(float));

  Serial.write(255);//confirmation to desktop
  for(int i=0;i<test_num;i++){

    while(Serial.available()<3)
      continue;

    uint8_t color[3];
    Serial.readBytes(color, 3);
    //Serial.println(color[0]);
    //Serial.println(color[1]);
    //Serial.println(color[2]);
    float x,y;
    get_xy(&x,&y,i);
    compare_xy_RGB(x,y,color,i);
    Serial.write(255);
  }

  //analyze();
  sendDifferences(xy,test_num);
}

void save_CCT(int id){
  float cct_avg;
  for(int i=0;i<sample_num;i++){
    transmitCommand(cmdCCT,TERMINATION_CHAR);
    float cct=sensorSerial.parseInt();
    cct_avg+=cct;
  }
  cct_avg/=sample_num;
  CCT[id]=cct_avg;
}

void testCCT(){
  int test_num;
  while(Serial.available()<4)
    continue;
  test_num=Serial.parseInt();
  CCT=(float*)malloc(test_num*sizeof(float));
  for(int i=0;i<test_num;i++){
    save_CCT(i);
    Serial.println(255);
  }
}

void statusUpdate(){
  char sig=0;
  while(true){
    if(Serial.available()){
      int length=Serial.available();
      sig=Serial.read();
      break;
    }
  }

  switch(sig){
    case 116://start testing procedure
      testSeq();
    break;
    case 'c':
      testCCT();
    break;





  }

}

void loop() {


  //uint8_t color[3]={255,255,255};
  //float x,y;
  //get_xy(&x,&y,0);
  //compare_xy_RGB(x,y,color,0);
  //emptySensorSerial();



  transmitCommand(cmdLED0,TERMINATION_CHAR);
  delay(1000);
  transmitCommand(cmdLED100,TERMINATION_CHAR);
  //delay(1000);
  //emptySensorSerial();
  statusUpdate();


}