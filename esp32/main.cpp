#include <Arduino.h>
#include <HardwareSerial.h>
#include <cstring>
#include "Eigen30.h"
#include <Eigen/Core>
#include <Eigen/LU>
#include <Eigen/Dense>
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
uint8_t cmdINTTIME143[14] = {'A','T','I','N','T','T','I','M','E','=','1','4','3',0};
uint8_t cmdINTTIME[9] = {'A','T','I','N','T','T','I','M','E'};
uint8_t cmdGAIN3[9] = {'A','T','G','A','I','N','=','3',0};
uint8_t cmdGAIN[6] = {'A','T','G','A','I','N'};
uint8_t cmdMODE2[10] = {'A','T','T','C','S','M','D','=','2',0};
uint8_t cmdATINTRVL[8] = {'A','T','I','N','T','R','V','L'};
uint8_t cmdINTRVL1[10] = {'A','T','I','N','T','R','V','L','=','1'};
uint8_t cmdDUV[6] = {'A','T','D','U','V','C'};
uint8_t cmdPRIME[10] = {'A','T','U','V','P','R','I','M','E','C'};

HardwareSerial sensorSerial(1);

int sample_num=10;
float* XYZ;
float* XYZ_source;
float* xy;
Eigen::MatrixXd ccm;
bool isCcmAvailable=false;
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
  delay(100);
  transmitCommand(cmdMODE2, TERMINATION_CHAR);
  delay(100);
  transmitCommand(cmdINTTIME143,0x0A);
  delay(100);
  transmitCommand(cmdINTRVL1,0x0A);
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

  double X_avg=0;
  double Y_avg=0;
  double Z_avg=0;
  for(int i=0;i<sample_num;i++){
    transmitCommand(cmdDATA,TERMINATION_CHAR);
    delay(143);
    int X=sensorSerial.parseInt();
    int Y=sensorSerial.parseInt();
    int Z=sensorSerial.parseInt();
    int NIR=sensorSerial.parseInt();
    int D=sensorSerial.parseInt();
    int C=sensorSerial.parseInt();

    double W=143*2.8*64*0.7289;
    double Xf=250*(X)/W;
    double Yf=250*(Y)/W;
    double Zf=250*(Z)/W;

    
    //Serial.println(Xf);
    //Serial.println(Yf);
    //Serial.println(Zf);

    X_avg+=Xf;
    Y_avg+=Yf;
    Z_avg+=Zf;
    
  }
  X_avg/=sample_num;
  Y_avg/=sample_num;
  Z_avg/=sample_num;
  

  // Serial.println(X_avg);
  // Serial.println(Y_avg);
  // Serial.println(Z_avg);

  XYZ[id*3]=X_avg;
  XYZ[id*3+1]=Y_avg;
  XYZ[id*3+2]=Z_avg;

  if(isCcmAvailable){
    double X=X_avg;
    double Y=Y_avg;
    double Z=Z_avg;
    X_avg=X*ccm(0,0)+Y*ccm(1,0)+ Z*ccm(2,0)+ccm(3,0);
    Y_avg=X*ccm(0,1)+Y*ccm(1,1)+ Z*ccm(2,1)+ccm(3,1);
    Z_avg=X*ccm(0,2)+Y*ccm(1,2)+ Z*ccm(2,2)+ccm(3,2);
  }
  //  Serial.println(X_avg);
  //  Serial.println(Y_avg);
  //  Serial.println(Z_avg);
  double sum=X_avg+Y_avg+Z_avg;
  X_avg/=sum;
  Y_avg/=sum;
  Z_avg/=sum;
  //  Serial.println(X_avg);
  //  Serial.println(Y_avg);
  //  Serial.println(Z_avg);

  *x=X_avg;
  *y=Y_avg;

  //Serial.println(*x,5);
  //Serial.println(*y,5);
}
void rgb_to_xyz(uint8_t* rgb,int id){
  float R=rgb[0]/255.0;
  float G=rgb[1]/255.0;
  float B=rgb[2]/255.0;

  double X=0.4124564*R+0.3575761*G+0.1804375*B;
  double Y=0.2126729*R+0.7151522*G+0.072175*B;
  double Z=0.0193339*R+0.119192*G+0.9503041*B;

  XYZ_source[id*3]=1.0521111*X + 0.0000000*Y + 0.0000000*Z;
  XYZ_source[id*3+1]=0.0000000*X + 1.0000000*Y + 0.0000000*Z;
  XYZ_source[id*3+2]=0.0000000*X + 0.0000000*Y + 0.9184170*Z;

  XYZ_source[id*3]=X;
  XYZ_source[id*3+1]=Y;
  XYZ_source[id*3+2]=Z;


}

void calculate_calib_mat(int test_num){
  // Eigen::Matrix<double, 3, 3> Y;
  // Eigen::Matrix<double, 4, 3> X;
  Eigen::MatrixXd X(test_num,4);
  Eigen::MatrixXd Y(test_num,3);
  for(int i=0;i<test_num;i++){
    X(i,0)=XYZ[3*i];
    X(i,1)=XYZ[3*i+1];
    X(i,2)=XYZ[3*i+2];
    X(i,3)=1.;
    Y(i,0)=XYZ_source[3*i];
    Y(i,1)=XYZ_source[3*i+1];
    Y(i,2)=XYZ_source[3*i+2];
  }
    // std::stringstream ss;
    // ss<<Y;
    // Serial.println(ss.str().c_str());
  Eigen::MatrixXd XT=X.transpose();
  ccm = (XT * X).ldlt().solve(XT*Y);

  //  std::stringstream ss;
  //  ss<<ccm;
  //  Serial.println(ss.str().c_str());

  isCcmAvailable=true;

  for(int i=0;i<3;i++){
    Serial.print(ccm(0,i),6);
    Serial.print(",");
    Serial.print(ccm(1,i),6);
    Serial.print(",");
    Serial.print(ccm(2,i),6);
    Serial.print(",");
    Serial.print(ccm(3,i),6);
    Serial.print(",");
  }
  Serial.write(255);
}

void compare_xy_RGB(float x,float y,uint8_t* rgb,int id){
  //convert to XYZ
  float R=rgb[0]/255.0;
  float G=rgb[1]/255.0;
  float B=rgb[2]/255.0;
  //gamma correction
  //for(int i=0;i<3;i++){
   // if(rgb[i]<=0.04045)
    //  rgb[i]=rgb[i]/12.92;
   //else
   //   rgb[i]=pow((rgb[i]+0.055)/1.055, 2.0);
  //}

 //for(int i=0;i<3;i++){
 //   if(rgb[i]<=0.0031308)
 //     rgb[i]=12.92*rgb[i];
 //   else
 //     rgb[i]=(1.055*pow(rgb[i],1/2.2)-0.055);
 // }


  //srgb
  double X=0.4124564*R+0.3575761*G+0.1804375*B;
  double Y=0.2126729*R+0.7151522*G+0.072175*B;
  double Z=0.0193339*R+0.119192*G+0.9503041*B;
  //cie
  //double X=0.4887180*R+  0.3106803*G+  0.2006017*B;
  //double Y=0.1762044*R+  0.8129847*G+  0.0108109*B;
  //double Z=0.0000000*R+  0.0102048*G+  0.9897952*B;
  //adobe
  //double X=0.5767309*R+  0.1855540*G+  0.1881852*B;
  //double Y=0.2973769*R+  0.6273491*G+  0.0752741*B;
  //double Z=0.0270343*R+  0.0706872*G+  0.9911085*B;
  
  //chromatic adaptation D65->E
   X=1.0521111*X + 0.0000000*Y + 0.0000000*Z;
   Y=0.0000000*X + 1.0000000*Y + 0.0000000*Z;
   Z=0.0000000*X + 0.0000000*Y + 0.9184170*Z;


  //convert to xy
  double sum=X+Y+Z;
  double x_in=X/sum;
  double y_in=Y/sum;


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
  XYZ_source=(float*)malloc(test_num*3*sizeof(float));
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
    rgb_to_xyz(color,i);
    get_xy(&x,&y,i);
    compare_xy_RGB(x,y,color,i);
    Serial.write(255);
  }

  //analyze();
  sendDifferences(xy,test_num);
  free(XYZ);
  free(XYZ_source);
}
void sendArrays(int test_num){
  for(int i=0;i<test_num;i+=2){
    Serial.print(XYZ[i*3],4);
    Serial.print(",");
    Serial.print(XYZ[i*3+1],4);
    Serial.print(",");
    Serial.print(XYZ[i*3+2],4);
  }
  Serial.write(255);
  for(int i=0;i<test_num;i+=2){
    Serial.print(XYZ_source[i*3],4);
    Serial.print(",");
    Serial.print(XYZ_source[i*3+1],4);
    Serial.print(",");
    Serial.print(XYZ_source[i*3+2],4);
  }
  Serial.write(255);
}
void calibSeq(){
  int test_num;
  while(Serial.available()<1)
    continue;
  test_num=Serial.parseInt();
  XYZ=(float*)malloc(test_num*3*sizeof(float));
  XYZ_source=(float*)malloc(test_num*3*sizeof(float));
  Serial.write(255);
  for(int i=0;i<test_num;i++){
    while(Serial.available()<3)
      continue;
    uint8_t color[3];
    Serial.readBytes(color, 3);
    float x,y;
    rgb_to_xyz(color,i);
    get_xy(&x,&y,i);
    Serial.write(255);
  }
  calculate_calib_mat(test_num);
  free(XYZ);
  free(XYZ_source);
}

void save_CCT(int test_num){
  float cct_avg=0;
  for(int i=0;i<test_num*sample_num;i++){
    transmitCommand(cmdCCT,TERMINATION_CHAR);
    float cct=sensorSerial.parseInt();
    cct_avg+=cct;
  }
  cct_avg/=(test_num*sample_num);
  Serial.print(cct_avg,4);
}

void testCCT(){
  int test_num;
  while(Serial.available()<1)
    continue;
  test_num=Serial.parseInt();
  Serial.write(255);
  delay(50);
  save_CCT(test_num);

}

void save_LUX(int test_num){
  float lux_avg=0;
  for(int i=0;i<test_num*sample_num;i++){
    transmitCommand(cmdLUX,TERMINATION_CHAR);
    float lux=sensorSerial.parseInt();
    lux_avg+=lux;
  }
  lux_avg/=(test_num*sample_num);
  Serial.print(lux_avg,4);
}

void testLUX(){
  int test_num;
  while(Serial.available()<1)
    continue;
  test_num=Serial.parseInt();
  Serial.write(255);
  delay(50);
  save_LUX(test_num);
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
    case 'l':
    testLUX();
    break;
    case 'd':
      calibSeq();
    break;
  }

}

void loop() {


  //uint8_t color[3]={255,255,255};
  //float x,y;
  //get_xy(&x,&y,0);
  //compare_xy_RGB(x,y,color,0);
  //emptySensorSerial();


  delay(1000);
  transmitCommand(cmdLED0,TERMINATION_CHAR);
  //delay(1000);
  //transmitCommand(cmdLED100,TERMINATION_CHAR);
  delay(1000);
  emptySensorSerial();
  statusUpdate();


  // sensorSerial.write(cmdPRIME,10);
  // sensorSerial.write('\n');
  // delay(100);
  // while(sensorSerial.available())
  // Serial.println(sensorSerial.parseFloat());

  // while(true){
  // float x,y;
  // get_xy(&x,&y,0);
  // delay(1000);
  // }



//   XYZ=(float*)malloc(15*sizeof(float));
//   XYZ_source=(float*)malloc(15*sizeof(float));
//   XYZ[0]=45.7;
//   XYZ[1]=23.87;
//   XYZ[2]=1.16;

//   XYZ[3]=92.10;
//   XYZ[4]=117.98;
//   XYZ[5]=5.44;

//   XYZ[6]=111.49;
//   XYZ[7]=130.37;
//   XYZ[8]=57.08;

//   XYZ[9]=65.65;
//   XYZ[10]=106.90;
//   XYZ[11]=55.30;

//   XYZ[12]=19.67;
//   XYZ[13]=12.01;
//   XYZ[14]=50.91;

//   uint8_t a[]={255,0,0};
//   rgb_to_xyz(a,0);

//  uint8_t b[]={255,255,0};
//   rgb_to_xyz(b,1);

// uint8_t c[]={255,255,255};
//   rgb_to_xyz(c,2);

// uint8_t d[]={0,255,255};
//   rgb_to_xyz(d,3);

// uint8_t e[]={0,0,255};
//   rgb_to_xyz(e,4);

//   calculate_calib_mat(5);
//   while(true){}
}