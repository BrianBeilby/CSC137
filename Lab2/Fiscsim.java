import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Fiscsim {

    // Instance variables for CPU
    private int[] registers;
    private byte[] memory;
    private boolean zeroFlag;
    private int pc;		// Program Counter

    // Constructor to initialize CPU state
    public Fiscsim() {
        // Initialize registers
    	// creates an array of 4 integers to hold register values
        this.registers = new int[4];
        for (int i = 0; i < this.registers.length; i++) {
            this.registers[i] = 0;		// initializes each register to 0
        }

        // Initialize memory
        // creates a byte array to hold memory values
        this.memory = new byte[256];	
        
        this.zeroFlag = false;		// Initialize flags
        this.pc = 0;				// Initialize program counter
    }

    // Main method to run simulator
    public static void main(String[] args) {
    	String fileName = null;
    	// boolean variable to indicate if disassembly should be printed
    	boolean disassemble = false;
    	// number of cycles to run the simulator for. Default is 20
    	int cycles = 20;
    	
    	// Prints a message on how to use this 
    	// program and exits if used incorrectly
    	if (args.length < 1 || args.length > 3) {
    		System.out.println("USAGE: "
    				+ "java Fiscsim <object file> [<cycles>] [-d]\n"
    				+ "-d : print disassembly listing with each cycle\n"
    				+ "if cycles are unspecified the "
    				+ "CPU will run for 20 cycles");
            System.exit(1);
        }
    	
    	fileName = args[0];		// sets filename to the first argument
    	
    	// If the number of cycles is specified, try to 
    	// parse it as an integer.
    	// If it cannot be parsed as an integer, an error 
    	// message is printed and program terminates.
    	if (args.length >= 2) {
            try {
                cycles = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number of cycles specified.");
                System.exit(1);
            }
        }

    	// If the disassembly option is specified, set the flag.
    	// If it's an invalid option, print error message and exit
        if (args.length >= 3) {
            if (args[2].equals("-d")) {
                disassemble = true;
            } else {
                System.out.println("Invalid option specified.");
                System.exit(1);
            }
        }

        // create an instance of the simulator
        Fiscsim simulator = new Fiscsim();

        try {
        	// load the instructions from the file
            simulator.loadInstructions(fileName);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        if (disassemble) {
        	// run the disassembler if the flag is true
            simulator.runDisassembler(cycles);
        } else {
        	// run the standard simulator if the flag is false
            simulator.run(cycles);
        }
    }

    // Method to load instructions from file into memory
    private void loadInstructions(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line = reader.readLine();

        // Verify that the first line matches the expected format
        if (!line.equals("v2.0 raw")) {
            throw new IOException("Invalid file format");
        }

        // Loop through each line of the file and store them in memory.
        int address = 0;
        while ((line = reader.readLine()) != null) {
            String[] hexValues = line.split(" ");
            for (String hex : hexValues) {
                memory[address++] = (byte) Integer.parseInt(hex, 16);
            }
        }

        reader.close();
    }
    
    // Method to run the simulator
    private void run(int cycles) {
    	for (int i = 1; i <= cycles; i++) {
    		// fetch the instruction from memory
            byte instruction = memory[pc++];
            // call the executeInstruction() method on the instruction
            executeInstruction(instruction);
            
            // Print out the current state of the simulator
            System.out.printf("Cycle:%d State:PC:%02X Z:%d "
            		+ "R0: %02X R1: %02X R2: %02X R3: %02X\n",
                    i, pc, (zeroFlag ? 1 : 0), registers[0], 
                    registers[1], registers[2], registers[3]);
        }
    }
    
    // Method to run the disassembler
    private void runDisassembler(int cycles) {
        for (int i = 1; i <= cycles; i++) {
        	// fetch the instruction from memory
            byte instruction = memory[pc++];
            // call the executeInstruction() method on the instruction
            executeInstruction(instruction);
            
            // Disassemble the instruction and print it out along with 
            // the current state of the simulator
            System.out.printf("Cycle:%d State:PC:%02X Z:%d R0: "
            		+ "%02X R1: %02X R2: %02X R3: %02X\nDisassembly: "
            		+ "%s\n\n", i, 
            		pc, zeroFlag ? 1 : 0, registers[0], registers[1], 
            				registers[2],
            				registers[3], 
            				disassembleInstruction(instruction));

        }
    }

    // Method to execute a single instruction
    private void executeInstruction(byte instruction) {
    	// Extract the fields from the instruction using bit manipulation
    	int opcode = (instruction >> 6) & 0x3;
    	int rn = (instruction >> 4) & 0x3;
    	int rm = (instruction >> 2) & 0x3;
    	int rd = instruction & 0x3;
        int targetAddress = instruction & 0b00111111;

        // Decode and execute the instruction based on the opcode
        switch (opcode) {
            case 0: // ADD
                add(rd, rn, rm);
                break;
            case 1: // AND
                and(rd, rn, rm);
                break;
            case 2: // NOT
                not(rd, rn);
                break;
            case 3: // BNZ
                bnz(targetAddress);
                break;
        }
    }
    
    // Method to disassemble an instruction into its original assembler code
    private String disassembleInstruction(byte instruction) {
    	// Extract the fields from the instruction using bit manipulation
        int opcode = (instruction >> 6) & 0x3;
        int rn = (instruction >> 4) & 0x3;
        int rm = (instruction >> 2) & 0x3;
        int rd = instruction & 0x3;
        int targetAddress = instruction & 0b00111111;

        // Determine the mnemonic for the instruction
        String mnemonic = "";
        switch (opcode) {
            case 0: // ADD
                mnemonic = "add";
                break;
            case 1: // AND
                mnemonic = "and";
                break;
            case 2: // NOT
                mnemonic = "not";
                break;
            case 3: // BNZ
                mnemonic = "bnz";
                break;
        }

        // Determine the operands for the instruction
        String operands = "";
        switch (opcode) {
            case 0: // ADD
            case 1: // AND
                operands = String.format("r%d r%d r%d", rd, rn, rm);
                break;
            case 2: // NOT
                operands = String.format("r%d r%d", rd, rn);
                break;
            case 3: // BNZ
                operands = String.format("%x", targetAddress);
                break;
        }

        // return the original assembler code as a string
        return String.format("%s %s", mnemonic, operands);
    }

    // Helper methods for specific instructions
    private void add(int destReg, int srcReg1, int srcReg2) {
    	// add the values of the two source registers, 
    	// masking the result to 8 bits
    	int result = registers[srcReg1] + registers[srcReg2] & 0xFF;
        setZeroFlag(result);	// update the zero flag based on the result
        // store the result in the destination register
        registers[destReg] = result;
    }

    private void and(int destReg, int srcReg1, int srcReg2) {
    	// compute the bitwise AND of the values of the two source registers,
    	// masking the result to 8 bits
    	int result = registers[srcReg1] & registers[srcReg2] & 0xFF;
    	// update the zero flag based on the result
        setZeroFlag(result);
        // store the result in the destination register
        registers[destReg] = result;
    }

    private void not(int destReg, int srcReg) {
    	// compute the bitwise NOT of the value in the source register,
    	// masking the result to 8 bits
    	int result = ~registers[srcReg] & 0xFF;
    	// update the zero flag based on the result
        setZeroFlag(result);
        // store the result in the destination register
        registers[destReg] = result;
    }

    private void bnz(int targetAddress) {
    	// Check if the zeroFlag is false
    	if (!zeroFlag) {
    		// set the program counter to the target address
            pc = targetAddress;
         // check to prevent going out of bounds
            if (pc >= memory.length) {
                pc = memory.length - 1;
            }
        }
    }

    // Helper methods for updating flags
    private void setZeroFlag(int value) {
    	zeroFlag = (value == 0);
    }
}
