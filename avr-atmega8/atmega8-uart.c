
#include <avr/io.h>
#include <avr/interrupt.h>
#include <stdlib.h>
#include <stdio.h>
#include <avr/delay.h>

#define F_CPU  8000000


unsigned int data;
volatile char buf[4]; // Буфер
volatile int bufsize = 0; // Длина строки
volatile char ready = 0; // Получена ли строка? 
volatile unsigned int microSteps = 3;
int stepsInProgress = 0;


void USART_Transmit( unsigned char data )
{
	while ( !(UCSRA & (1<<UDRE)) ); //Ожидание опустошения буфера приема
	UDR = data; //Начало передачи данных
}


int initUart(void){
   UCSRA = 0x00;
   UCSRB = 0x98;
   // Turn on the transmission and reception circuitry
   UCSRC = 0x86;
   // Use 8-bit character sizes
   UBRRH = 0x00;
   // Load upper 8-bits of the baud rate value into the high byte of the UBRR register
   UBRRL = 51;
//   UBRRL = 25;
   // Load lower 8-bits of the baud rate value into the low byte of the UBRR register   
  // UCSRB |= (1 << RCXIE1);
   // Enable the USART Recieve Complete interrupt (USART_RXC)


}


int flash(void){
	_delay_ms(1000);
	PORTC&=~(1<<PORTC1);
	_delay_ms(500);
	PORTC|=(1<<PORTC1);
}

int step(){

	_delay_ms(2);
	PORTC|=(1<<PORTC0);
	_delay_ms(2);
	PORTC&=~(1<<PORTC0);
}


int oneMmStep(){
	stepsInProgress = 1;
	int onemm = microSteps;

	while(onemm){
		step();
		onemm--;
	}
	stepsInProgress = 0;

}

int stop(void){
	USART_Transmit('s');
	USART_Transmit('t');
	USART_Transmit('o');
	USART_Transmit('p');
	USART_Transmit('\r');
	USART_Transmit('\n');
	PORTC=0;
}




int initTimer(void){
	TIMSK |= (1 << TOIE0);
//    TCCR0 |= (1 << CS02);
    TCCR0 = 0x7D;
}

int main (void){

   DDRC = 0xFF;

   PORTB = 0x00;
   DDRB = 0xFF;

   PORTD = 0x00;
   DDRD = 0x00;

   initUart();
   initTimer();
			USART_Transmit("s");
			USART_Transmit("t");
			USART_Transmit("a");
			USART_Transmit("r");
			USART_Transmit("t");


   sei(); // Enable the Global Interrupt Enable flag so that interrupts can be processed
   
	PORTC&=~(1<<PORTC1);
   while (1){

   }
}



volatile unsigned int steps = 1;

volatile unsigned int stepDelay = 3;
volatile unsigned int delay = 2;


volatile unsigned int waitingSteps = 0;




unsigned int flushCounter = 0;
unsigned int flushCounterLimit = 0;


unsigned int stepCounter = 0;
unsigned int timerCounter = 0;
unsigned int stepDelayCounter = 0;


unsigned int timerSteps = 0;
unsigned int timerDelay = 0;
unsigned int timer = 0;

int move = 0;


ISR (TIMER0_OVF_vect)  // timer0 overflow interrupt
{
	timerCounter++;

	if(timerCounter>30){ //evryy seconds
		if(delay>0 && !move){
			timerDelay++;
			if(timerDelay > delay ){
				timerDelay=0;
				waitingSteps = steps;
				move = 1;
			}
		}

		timerCounter = 0;
	}



	if(move && !stepsInProgress){
		stepDelayCounter++;
		if(stepDelayCounter > stepDelay){
			stepDelayCounter=0;
			if(waitingSteps){
				waitingSteps--;
				oneMmStep();	
				if(waitingSteps==0){
					flash();
					move = 0;
				}
			}
		}
	}



stepCounter++;
}




ISR(USART_RXC_vect)
{

  buf[bufsize++] = UDR;
  if (bufsize >1 )
  {
  	if(buf[0]==1){
		steps = buf[1];
		waitingSteps = steps;
	}
  	if(buf[0]==2){
		stepDelay = buf[1];
		stepDelayCounter = stepDelay;
	}
  	if(buf[0]==3){
		delay = buf[1];
		timerDelay = delay;
	}
  	if(buf[0]==4){
		 flushCounter = 0;
		 flushCounterLimit = 0;
		 stepCounter = 0;
		 timerCounter = 0;
		 stepDelayCounter = 0;
		 timerSteps = 0;
		 timerDelay = 0;
		 timer = 0;
	}
  	if(buf[0]==5){
		if(buf[1]){
			PORTC|=(1<<PORTC2);
		}else{
			PORTC&=~(1<<PORTC2);
		}
	}
  	if(buf[0]==6){
		microSteps = buf[1];
	}
	bufsize = 0;
  	ready = 0;
  } 
  
}




