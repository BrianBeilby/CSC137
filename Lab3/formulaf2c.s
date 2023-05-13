	 mvi r0 0
	 mvi r1 32
convert: mvi r3 127
	 mvi r2 113
	 add r3 r3 r2
	 st r0 r3
	 mvi r2 114
	 add r3 r3 r2
	 st r1 r3
	 mov r2 r0
	 mvi r3 32
	 sub r2 r2 r3
	 mvi r3 5
	 shl r2
	 add r2 r2 r3
	 asr r2
	 mov r1 r2
	 mvi r3 1
	 add r0 r0 r3
	 mvi r0 -128
	 bnz 2
	 
