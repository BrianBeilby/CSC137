loop:	 ld r1 r0	;Load the Celsius Value into r1
	 mvi r3 127	;add up to the value of F0
	 mvi r2 113
	 add r3 r3 r2
	 st r0 r3	;Display the Fahrenheit value
	 mvi r3 127	;add up to the value of F1
	 mvi r2 114
	 add r3 r3 r2
	 st r1 r3	;Display the Celsius value
	 mvi r3 1	;Increment the Fahrenheit value
	 add r0 r0 r3
	 bnz 0