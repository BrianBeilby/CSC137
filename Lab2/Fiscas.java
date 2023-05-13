import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fiscas {

    private static HashMap<String, Integer> symbolTable;
    private int currentAddress;

    public Fiscas() {
        symbolTable = new HashMap<>();
        currentAddress = 0;
    }

    public static void main(String[] args) throws Exception {
    	// If user doesn't specify both files,
    	// A USAGE message is displayed.
        if (args.length < 2) {
            System.out.println("USAGE:  "
            		+ "java Fiscas.java <source file> <object file> [-l]\r\n"
            		+ "		-l : print listing to standard error");
            return;
        }

        // Get input and output file name
        String inputFileName = args[0];
        String outputFileName = args[1];

        // Create an instance of this assembler
        Fiscas assembler = new Fiscas();
        
        // Check for the -l option, if it is specified
        // set the flag variable to true
        boolean printListing = false;
        if (args.length == 3 && args[2].equals("-l")) {
        	printListing = true;
        }

       // read input from the source file
        StringBuilder sourceBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
        		new FileReader(inputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sourceBuilder.append(line).append('\n');
            }
        }

        String source = sourceBuilder.toString();

        // Assemble the source code
        String output = assembler.assemble(source, printListing);

        // write output to file
        try (BufferedWriter writer = new BufferedWriter(
        		new FileWriter(outputFileName))) {
        	// Writes the version header to be compatible with Logisim
        	writer.write("v2.0 raw\n");
        	// Write the assembled output
            writer.write(output);
        }
        
        // If specified by the user, print listing to standard error
        if (printListing) {
        	System.err.println("*** LABEL LIST ***");
        	List<Map.Entry<String, Integer>> labelList = new ArrayList<>(
        			// Get list of symbol table entries
        			assembler.symbolTable.entrySet());
        	// Reverse list so it's in order of appearance
        	Collections.reverse(labelList);
        	// Iterate through the list printing the symbol name
        	// and address
        	for (Map.Entry<String, Integer> entry : labelList) {
        	    System.err.printf("%-8s %02X\n", entry.getKey(), 
        	    		entry.getValue());
        	}

        	System.err.println("*** MACHINE PROGRAM ***");
        	// Split assembled output into individual instructions
        	String[] instructions = output.split("\\r?\\n");
        	int address = 0;
        	int labelAddress = 1;
        	List<Map.Entry<String, Integer>> labelList2 = new ArrayList<>(
        			assembler.symbolTable.entrySet());
        	Collections.reverse(labelList2);

        	for (String instruction : instructions) {
        	    String machineCode = "";
        	    int i = 0;
        	    for (Map.Entry<String, Integer> entry : labelList2) {
        	        String key = entry.getKey();
        	        // Convert the instruction to machine code
        	        machineCode = Fiscas.toMachineCode(instruction);
        	        if ((machineCode.startsWith("bnz") 
        	        		&& i == labelAddress) || 
        	        		(machineCode.startsWith("bnz") 
        	        				&& labelList2.size() == 1)) {
        	        	// Print the address, instruction, and
        	        	// machine code
        	            System.err.printf("%02X:%-8s %s %s\n", 
        	            		address, instruction, machineCode, key);
        	            labelAddress++;
        	            i++;
        	        }
        	        i++;
        	    }
        	    if (!machineCode.startsWith("bnz")) {
        	        System.err.printf("%02X:%-8s %s\n", address, 
        	        		instruction, machineCode);
        	    }
        	    address += 1;
        	}

        }
    }
    
    public String assemble(String source, boolean printListing) {
        // First pass: generate symbol table
    	int lineNum = 0;
    	// Split the source into individual lines
        String[] lines = source.split("\\r?\\n");
        for (String line : lines) {
        	lineNum++;
        	// Remove any leading or trailing white space
            line = line.trim();
            if (line.isEmpty() || line.startsWith(";")) {
                continue; // ignore comments and blank lines
            }
            String[] tokens = line.split("\\s+");
            if (tokens[0].endsWith(":")) {
                // label definition
                String label = tokens[0].substring(0, tokens[0].length() - 1);
                if (symbolTable.containsKey(label)) {
                    // duplicate label definition
                    return "Label " + label + " on line " + lineNum + 
                    		" is already defined.";
                }
                symbolTable.put(label, currentAddress);
                // If there are more tokens on the same line, 
                // treat them as a regular instruction
                if (tokens.length > 1) {
                    currentAddress += 1;
                }
            } else {
                // instruction
                currentAddress += 1;
            }
        }

        // Second pass: generate machine code
        // Holds the output machine code
        StringBuilder output = new StringBuilder();
        currentAddress = 0;
        for (String line : lines) {
            line = line.trim();
            String[] tokens = line.split("\\s+");
            // Initialize opcode, registers, and flag
            String opcode = "";
            int rn = 0, rd = 0, rm = 0;
            int instruction = 0;
            boolean isBnz = false;
            if (line.isEmpty() || line.startsWith(";")) {
                continue; // ignore comments and blank lines
            }
            if (tokens[0].endsWith(":")) {
                if (tokens.length > 1) {
                	currentAddress += 1;
                	// Create a new array with all tokens except the label
                	String[] newTokens = new String[tokens.length - 1];
                    System.arraycopy(tokens, 1, newTokens, 0, 
                    		tokens.length - 1);
                    // Check the opcode and parse registers
                    // accordingly
                    switch (newTokens[0]) {
                    case "add":
                        opcode = "00";
                        rn = parseRegister(newTokens[1]);
                        rd = parseRegister(newTokens[2]);
                        rm = parseRegister(newTokens[3]);
                        break;
                    case "and":
                        opcode = "01";
                        rn = parseRegister(newTokens[2]);
                        rd = parseRegister(newTokens[1]);
                        rm = parseRegister(newTokens[3]);
                        break;
                    case "not":
                        opcode = "10";
                        rn = parseRegister(newTokens[2]);
                        rd = parseRegister(newTokens[1]);
                        rm = 0; // ignore Rm for not instruction
                        break;
                    case "bnz":
                    	opcode = "11";
                        String secondToken = newTokens[1];
                        if (symbolTable.containsKey(secondToken)) {
                            int labelAddress = symbolTable.get(secondToken);
                            rn = labelAddress;
                        } else {
                            rn = parseRegister(secondToken);
                        }
                        rd = 0; // ignore Rd for bnz instruction
                        rm = 0; // ignore Rm for bnz instruction
                        isBnz = true;
                        break;
                    default:
                    	// check if it's a label
                        if (symbolTable.containsKey(newTokens[0].substring(0, 
                        		newTokens[0].length() - 1))) {
                            // label definition (already processed)
                            continue;
                        } else {
                        	// Throw an exception if the instruction
                        	// is unknown
                            throw new RuntimeException(
                            		"Unknown instruction: " + 
                        newTokens[0]);
                        }
                    }
                    // Generate the instruction based on the opcode
                    // and operands
                    if (isBnz) instruction = Integer.parseInt(opcode + 
                    		toBinary(rn, 6), 2);
                    else instruction = Integer.parseInt(opcode + 
                    		toBinary(rn, 2) + 
                    		toBinary(rm, 2) + toBinary(rd, 2), 2);
                    // Append to the output string
                    output.append(toHex(instruction));
                    output.append("\n");
                    currentAddress += 1; 
                }
            } else if (symbolTable.containsKey(tokens[0])) {
            	// label reference
            	// Get the address of the label from the symbol table
                int labelAddress = symbolTable.get(tokens[0]);
                // Concatenate the opcode, register numbers, 
                // and label address 
                // to create the instruction and convert to an integer
                instruction = Integer.parseInt(opcode + toBinary(rn, 2) + 
                		toBinary(rd, 2) + toBinary(labelAddress, 2), 2);
                // Append to the output string
                output.append(toHex(instruction));
                output.append("\n");
                currentAddress += 1;
            } else {
                // instruction
                switch (tokens[0]) {
                    case "add":
                        opcode = "00";
                        rn = parseRegister(tokens[2]);
                        rd = parseRegister(tokens[1]);
                        rm = parseRegister(tokens[3]);
                        break;
                    case "and":
                        opcode = "01";
                        rn = parseRegister(tokens[2]);
                        rd = parseRegister(tokens[1]);
                        rm = parseRegister(tokens[3]);
                        break;
                    case "not":
                        opcode = "10";
                        rn = parseRegister(tokens[2]);
                        rd = parseRegister(tokens[1]);
                        rm = 0; // ignore Rm for not instruction
                        break;
                    case "bnz":
                    	opcode = "11";
                        String secondToken = tokens[1];
                        if (symbolTable.containsKey(secondToken)) {
                            int labelAddress = symbolTable.get(secondToken);
                            rn = labelAddress;
                        } else {
                            rn = parseRegister(secondToken);
                        }
                        rd = 0; // ignore Rd for bnz instruction
                        rm = 0; // ignore Rm for bnz instruction
                        isBnz = true;
                        break;
                    default:
                    	// check if it's a label
                        if (symbolTable.containsKey(tokens[0].substring(0, 
                        		tokens[0].length() - 1))) {
                            // label definition (already processed)
                            continue;
                        } else {
                            throw new RuntimeException(""
                            		+ "Unknown instruction: " + tokens[0]);
                        }
                }
                if (isBnz) instruction = Integer.parseInt(opcode + 
                		toBinary(rn, 6), 2);
                else instruction = Integer.parseInt(opcode + 
                		toBinary(rn, 2) + toBinary(rm, 2) + 
                		toBinary(rd, 2), 2);
                // Append to the output string
                output.append(toHex(instruction));
                output.append("\n");
                currentAddress += 1;
            }
        }

        return output.toString().trim();
    }

    // This method takes a register string in the form of "rX", 
    // where X is the register number,
    // and returns the integer value of the register number
    private int parseRegister(String register) {
        int index = register.indexOf('r');
        if (index != 0 || register.length() != 2) {
            throw new RuntimeException("Invalid register: " + register);
        }
        return Integer.parseInt(register.substring(1));
    }

    // This method takes an integer value and the desired 
    // number of digits and returns the binary string
    // representation of the integer value 
    // with the specified number of digits
    private String toBinary(int value, int digits) {
        String binaryString = Integer.toBinaryString(value);
        int numZeroes = digits - binaryString.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numZeroes; i++) {
            sb.append('0');
        }
        sb.append(binaryString);
        return sb.toString();
    }

    // This method takes an integer value and returns the 
    // hex string representation of the integer value
    private String toHex(int value) {
        return String.format("%02X", value);
    }
    
    // This method takes a hex string representation 
    // of a machine code instruction
    // and converts it to the corresponding 
    // assembly language instruction.
    public static String toMachineCode(String instruction) {
        int inst = Integer.parseInt(instruction, 16);
        String binary = String.format("%8s", 
        		Integer.toBinaryString(inst)).replace(' ', '0');
        int opcode = Integer.parseInt(binary.substring(0, 2), 2);
        int rd = Integer.parseInt(binary.substring(6, 8), 2);
        int rn = Integer.parseInt(binary.substring(2, 4), 2);
        int rm = Integer.parseInt(binary.substring(4, 6), 2);
        String[] registers = { "r0", "r1", "r2", "r3" };
        String rdReg = registers[rd];
        String rnReg = registers[rn];
        String rmReg = registers[rm];
        switch (opcode) {
            case 0:
                return String.format("add %s %s %s", rdReg, rnReg, rmReg);
            case 1:
                return String.format("and %s %s %s", rdReg, rnReg, rmReg);
            case 2:
                return String.format("not %s %s", rdReg, rnReg);
            case 3:
                return "bnz ";
            default:
                return "";
        }
    }

}