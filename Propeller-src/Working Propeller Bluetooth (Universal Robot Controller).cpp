#include "simpletools.h"
#include "fdserial.h"
#include "servo.h"
#include "servodiffdrive.h"

// declare sounds
#define C6 1047
#define C7 2093
#define A6 1760
#define B6 1976
#define D6 1175
#define E6 1319
#define F6 1397
#define G6 1568

#define SOP '<'
#define EOP '>'

// Declaration // this could go into a header file
void startBeep(void *par);
void startBlueTooth(void *par);

// volatile I believe is best used when 2 cogs wants access to the var
volatile int note[] = {C6, F6, A6, B6, G6, D6, E6, C7};

// init vars
fdserial *bt;
int *cogBeep;
int *cogBlueTooth;


// A total of 4 cogs
int main()
{
  // free terminal to be used by another cog
  simpleterm_close();
  // start 2 cogs for servos
  //servo_start();
  // set servo pins
  drive_pins(18, 19);  
  
  // start cogs
  cogBeep = cog_run(&startBeep, 10);
  cogBlueTooth = cog_run(&startBlueTooth, 250);
  
  return 0;
}


// beep cog 
void startBeep(void *par){
  for(int i = 0; i < 8; i++){
    freqout(4, 100, note[i]);
    freqout(26, 100, note[i]);
    pause(1);
  }
  // free up the cog
  cog_end(cogBeep);
}

// servo controls
void _right(){
  drive_speeds(100, -100);
}  
void _left(){
  drive_speeds(-50, 50);
}  
void _forward(){
  drive_speeds(-200, -200);
}  
void _backward(){
  drive_speeds(200, 200);
}  
void _stop(){
  drive_sleep();
}

void drive(int x, int y){
  drive_speeds(x, y);
}  

// Bluetooth cog
void startBlueTooth(void *par){
  // use terminal
  simpleterm_open();
  print("Bluetooth Started\n");
  // start 2 cogs for servos
  //servo_start();
  print("Servos Started\n");
  
  //open serial communication for bluetooth
  bt = fdserial_open(7, 6, 0, 9600);
  print("Connected to bluetooth device %d \n", bt);
  // init bluetooth
  writeChar(bt, CLS);
  
  char chr;
  char chrs[10];
  int index = 0;
  int start = 0;
  int stop = 0;
  while(bt > 0)
  { 
      // this was weird, I thought I needed to use Tx for the input but I receive input using Rx
      chr = fdserial_rxChar(bt);
      if(chr > 32 && chr < 126)
      {
        if(chr == SOP)
        {
          //print("start\n");
          start = 1;
        }
        if(chr == EOP)
        {
          //print("stop\n");
          stop = 1;
        }
        
        if(start == 1 && 
           chr != SOP && 
           chr != EOP)
        {
          chrs[index] = chr;
          index++;
          chrs[index] = '\0';
        }        
  
        // collect information
        if(start == 1 && stop == 1)
        {
          //print("(%s)\n", chrs);
          int coords[2];
          char * pch;
          pch = strtok (chrs,":");
          int c = 0;
          while (pch != NULL)
          {
              coords[c] = atoi(pch);
              pch = strtok (NULL, ":");
              c++;
          }
          print("%d %d\n", coords[0], coords[1]);
          drive(coords[0], coords[1]);
          memset(&chrs[0], 0, sizeof(chrs));
          index = 0;
          start = 0;
          stop = 0;
        }
      }
  }
}