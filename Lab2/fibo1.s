start:  not r1 r0         ; initialize values
        add r1 r1 r1     
        not r3 r1        
        add r3 r3 r0     
        add r2 r3 r0     
        add r3 r2 r3     
        not r0 r0        
loop:   and r1 r2 r2     ; compute sequence
        and r2 r3 r3     
        add r3 r2 r1     
        bnz loop         ; repeat indefinitely