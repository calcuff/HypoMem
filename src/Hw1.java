/*
Calvin Cuff
Operating System Internals
Prof Krishnamoorthy
HW1
2/11/20
 */


import java.io.File;
import java.util.Scanner;

public class Hw1 {

    // List of all global variables for hardware components
    private static long[] HypoMem = new long[10000];
    private static long MAR, MBR;
    private static long clock;
    private static long[] GPR = new long[8];
    private static long IR, PSR, PC, SP;
    private static long  Op1Address, Op1Value, Op2Address, Op2Value;

    // Variables for valid memory area addressing
    private final static int StackStartAddr = 1000;
    private final static int StackEndAddr = 1199;
    private final static int StackSize = 200;
    private final static int ValidProgramArea = 3499;
    private final static int MaxMemory = 9999;

    // List of return status codes
    private final static int OK = 0;
    private final static long ENDOFPROGRAM = -1;
    private final static int FileNotFoundError = -2;
    private final static int NoEndProgError = -3;
    private final static int InvalidAddrRange = -4;
    private final static int HaltStatus = -5;
    private final static int InvalidPCValue = -6;
    private final static int InvalidOpMode = -7;
    private final static int InvalidGPR = -8;
    private final static int InvalidImmediateMode = -9;
    private final static int DivideBy0Error = -10;
    private final static int InvalidOpCode = -11;
    private final static int InvalidGPRAddr = -12;
    private final static int InvalidMode = -13;
    private final static int StackOverflow = -14;
    private final static int StackUnderflow = -15;


    public static void main(String[] args) throws Exception{

        // Initialize all components to 0
        InitializeSystem();

        // Prompt user for filename of executable
        Scanner in = new Scanner(System.in);
        System.out.println("Enter the filename of machine language executable");
        String machineFile = in.nextLine();

        // Load the specified file
        long returnValue = AbsoluteLoader("C:\\Users\\calcu\\OSI\\src\\" + machineFile);
        if (returnValue < 0){
            System.out.println("There was an error loading file, returning error");
        }
        // Set program counter to return value
        PC = returnValue;

        // Dump memory after loading the program
        DumpMemory("Memory after loading the program", 0, 99);

        // Execute the program and check for error
        long ExecutionCompletionStatus = CPU();
        if (ExecutionCompletionStatus < 0){
            System.out.println("There was an error executing the CPU()");
        }

        //Dump memory after executing user program: Range 0 to 99 locations
        DumpMemory("Memory after Executing program", 0, 99);

    } // End of main()

    // Function that sets all global system hardware components to 0
    private static void InitializeSystem(){

        // Set all elements of HypoMem array to 0 (10000)
        for(int i =0 ; i < HypoMem.length; i++){
            HypoMem[i] = 0;
        }

        // Hypo Memory registers
        MAR = 0;
        MBR = 0;

        clock = 0;

        // CPU registers: an array of 8 General Purpose Registers
        for ( int j = 0; j < GPR.length; j++){
            GPR[j] = 0;
        }

        // CPU registers
        IR = 0;
        PSR = 0;
        PC = 0;
        SP = 0;

    } // End of InitializeSystem()

    // Function that opens the file containing the HYPO machine user program and
    // loads the content into HYPO memory. On successful load, return the PC value
    // in the End of Program line. On failure, display appropriate error message
    // and return appropriate error code.
    private static int AbsoluteLoader(String filename) throws Exception {

        int Address;
        int Content;

        // Open the given file and check to make sure it exists
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("File does not exist");
            return FileNotFoundError;
        }

        Scanner in = new Scanner(file);

        // While there is a next line, read first int to the Address
        // and the next int to the content
        while (in.hasNextLine()) {
            Address = in.nextInt();
            Content = in.nextInt();
            // If (-1) for the address, display End, close file, and return the content to main
            if (Address == ENDOFPROGRAM) {
                System.out.println("Reached end of program");
                in.close();
                // Check Content for valid memory range
                if ( Content > 0 && Content < ValidProgramArea)
                    return Content;  // Value to be stored in PC main
            }
            // If the address is in the valid range, store the content to the location in main memory
            else if (Address >= 0 && Address < ValidProgramArea) {
                HypoMem[Address] = Content;
            }
            else {
                System.out.println("There was an unexpected error, address out of range");
                in.close();
                return InvalidAddrRange;
            }
        } //end of while loop

        // Return an error to show that end of file was reached without End of Program notifier (-1)
        System.out.println("End of file encountered without End of Program line");
        in.close();
        return NoEndProgError;
    } //End of Absolute Loader

    // CPU function to fetch the first word of the instruction pointed by PC into MBR.
    // Instruction needing more words (2/3) are fetched based on instruction code.
    // The decode cycle is used to retrieve the five fields in any instruction depending on
    // opcode, operand mode, and operand GPR. After decoding, the execute cycle fetches operand
    // values based on the opcode. For each opcode the appropriate operand addresses and values
    // are returned and corresponding operations are performed for add, subtract, move, etc.
    // The result of the operation is stored in main memory and the clock is increased by the
    // instruction execution time.
    private static long CPU(){
        long Opcode, Remainder, Op1Mode, Op1Gpr, Op2Mode, Op2Gpr;
        long Returned[] = new long[3];
        long Result;
        boolean halt = false;
        boolean error = false;

        int err = 0;
        int OpAddr = 1;
        int OpVal = 2;

        int ImmediateMode = 6;
        int RegisterMode = 1;
        while ( !halt && !error){
            if (PC >= 0 && PC <= ValidProgramArea){
                MAR = PC++;
                MBR = HypoMem[(int)MAR];
            }
            else{
                System.out.println("Error: Invalid PC range found");
                return InvalidPCValue;
            }

            IR = MBR;

            // Decode cycle, get first digit
            Opcode = IR / 10000;
            Remainder = IR % 10000;

            // Get 2nd digit
            Op1Mode = Remainder / 1000;
            Remainder = Remainder % 1000;

            // Get 3rd digit
            Op1Gpr = Remainder / 100;
            Remainder = Remainder % 100;

            // Get 4th digit
            Op2Mode = Remainder / 10;
            Remainder = Remainder % 10;

            // Get 5th digit
            Op2Gpr = Remainder;

            // Check for valid OpModes
            if (Op1Mode < 0 || Op1Mode > 6 || Op2Mode < 0 || Op2Mode > 6){
                System.out.println("ERROR: Invalid op mode found. ");
                return InvalidOpMode;
            }

            // Check for valid GPRs
            if (Op1Gpr < 0 || Op1Gpr > 7 || Op2Gpr < 0 || Op2Gpr > 7){
                System.out.println("ERROR: Invalid GPR found. ");
                return InvalidGPR;
            }

            // Execute cycle, fetch operand
            switch ((int)Opcode){

                case 0: //HALT
                    halt = true;
                    System.out.println("Halt instruction was encountered.");

                    // Increase clock by halt execution time
                    clock = clock + 12;
                    break;

                case 1: //ADD
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }

                    // Set OpAddrs and OpValues from fetched operands
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    Returned = FetchOperand(Op2Mode, Op2Gpr);
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op2Address = Returned[OpAddr];
                    Op2Value = Returned[OpVal];

                    // Add the operand values
                    Result = Op1Value + Op2Value;

                    // If OpMode is Register mode, store result in Op1 location
                    if ( Op1Mode == RegisterMode ){
                        GPR[(int)Op1Gpr] = Result;
                    }
                    // If OpMode is Immediate mode, return error
                    else if ( Op1Mode == ImmediateMode ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    // Store the result in main memory
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    // Increase clock by instruction execution time
                    clock = clock + 4;
                    break;

                case 2: //Subtract
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    // Set OpAddrs and OpValues from fetched operands
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    Returned = FetchOperand(Op2Mode, Op2Gpr);
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op2Address = Returned[OpAddr];
                    Op2Value = Returned[OpVal];

                    // Subtract the operand values
                    Result = Op1Value - Op2Value;

                    // If OpMode is Register mode, store result in Op1 location
                    if ( Op1Mode == RegisterMode ){
                        GPR[(int)Op1Gpr] = Result;
                    }
                    // If OpMode is Immediate mode, return error
                    else if ( Op1Mode == ImmediateMode ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    // Store the result in main memory
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    // Increase clock by instruction execution time
                    clock = clock + 4;
                    break;

                case 3: // MULTIPLY
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }

                    // Set OpAddrs and OpValues from fetched operands
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    Returned = FetchOperand(Op2Mode, Op2Gpr);
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op2Address = Returned[OpAddr];
                    Op2Value = Returned[OpVal];

                    // Multiply the operand values
                    Result = Op1Value * Op2Value;

                    // If OpMode is Register mode, store result in Op1 location
                    if ( Op1Mode == RegisterMode ){
                        GPR[(int)Op1Gpr] = Result;
                    }
                    // If OpMode is Immediate mode, return error
                    else if ( Op1Mode == ImmediateMode ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    // Store the result in main memory
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    // Increase the clock by instruction execution time
                    clock = clock + 6;
                    break;

                case 4: // DIVIDE
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }

                    // Set OpAddrs and OpValues from fetched operands
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    Returned = FetchOperand(Op2Mode, Op2Gpr);
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op2Address = Returned[OpAddr];
                    Op2Value = Returned[OpVal];

                    //Check for division by 0 before
                    if ( Op2Value == 0){
                        System.out.println("ERROR: Division by 0 is a fatal run-time error.");
                        return DivideBy0Error;
                    }
                    // Divide the operand values
                    Result = Op1Value / Op2Value;

                    // If OpMode is Register mode, store result in Op1 location
                    if ( Op1Mode == RegisterMode ){
                        GPR[(int)Op1Gpr] = Result;
                    }
                    // If OpMode is Immediate mode, return error
                    else if ( Op1Mode == ImmediateMode ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    // Store the result in main memory
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    // Increase clock by instruction execution time
                    clock = clock + 6;
                    break;

                case 5: // MOVE
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    // Set OpAddrs and OpValues from fetched operands
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    Returned = FetchOperand(Op2Mode, Op2Gpr);
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op2Address = Returned[OpAddr];
                    Op2Value = Returned[OpVal];

                    // Set the result to the operand value
                    Result = Op2Value;

                    // If OpMode is Register mode, store result in Op1 location
                    if ( Op1Mode == RegisterMode ){
                        GPR[(int)Op1Gpr] = Result;
                    }
                    // If OpMode is Immediate mode, return error
                    else if ( Op1Mode == ImmediateMode ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    // Store the result in main memory
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    // Increase the clock by instruction execution time
                    clock = clock + 2;
                    break;

                case 6: //BRANCH
                    // Check if PC is in valid range and set PC
                    if ( PC >= 0 && PC <= ValidProgramArea)
                        PC = HypoMem[(int)PC];
                    else {
                        System.out.println("Error: Invalid PC range found");
                        return InvalidPCValue;
                    }

                    // Increase clock by instruction execution time
                    clock = clock + 2;
                    break;

                case 7: //BrOnMinus
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    // Set OpAddr and OpValue from fetched operand
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    // If < 0, BrOnMinus is executed
                    if (Op1Value < 0){
                        // Check valid PC area and set PC
                        if ( PC >= 0 && PC <= ValidProgramArea)
                            PC = HypoMem[(int)PC];
                        else {
                            System.out.println("Error: Invalid PC range found");
                            return InvalidPCValue;
                        }
                    }
                    // Skip branch address to go to next instruction
                    else{
                        PC++;
                    }

                    // Increase clock by instruction execution time
                    clock = clock + 4;
                    break;

                case 8: // BrOnPlus
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }

                    // Set OpAddr and OpValue from fetched operand
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    // If > 0, BrOnPlus is executed
                    if (Op1Value > 0){
                        // Check for valid PC area and set PC
                        if ( PC >= 0 && PC <= ValidProgramArea)
                            PC = HypoMem[(int)PC];
                        else {
                            System.out.println("Error: Invalid PC range found");
                            return InvalidPCValue;
                        }
                    }
                    // Skip branch address to go to next instruction
                    else{
                        PC++;
                    }

                    // Increase clock by instruction execution time
                    clock = clock + 4;
                    break;

                case 9: //BrOnZero
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    // Set OpAddr and OpValue from fetched operand
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    // If == 0, BrOnZero is executed
                    if (Op1Value == 0){
                        // Check for valid PC range and set PC
                        if ( PC >= 0 && PC <= ValidProgramArea)
                            PC = HypoMem[(int)PC];
                        else {
                            System.out.println("Error: Invalid PC range found");
                            return InvalidPCValue;
                        }
                    }
                    // Skip branch address to go to next instruction
                    else{
                        PC++;
                    }

                    // Increase clock by instruction execution time
                    clock = clock + 4;
                    break;

                case 10: // PUSH
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    // Set OpAddr and OpValue from fetched operand
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    // Check if stack is full, return overflow
                    if (SP == StackEndAddr){
                        System.out.println("ERROR: Stack is full, stack overflow");
                        return StackOverflow;
                    }

                    // Else push Op1Value on stack pointed by SP
                    else{
                        SP++;
                        HypoMem[(int)SP] = Op1Value;
                    }

                    // Increment clock by execution time
                    clock = clock + 2;
                    break;

                case 11: // POP
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    // Set OpAddr and OpValue from fetched operand
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    //Check if SP is outside stack limit and return stack underflow error code
                    if ( SP < StackStartAddr){
                        System.out.println("ERROR: Stack is outside stack limit, stack underflow");
                        return StackUnderflow;
                    }
                    // Pop value off stack and store at Op1Address and decrement SP by 1
                    else{
                        Op1Address = HypoMem[(int)SP--];
                    }

                    // Increment clock by execution time
                    clock = clock + 2;
                    break;

                case 12: // SYSTEM CALL
                    // Check if PC is outside the valid range
                    if ( PC < 0 || PC > ValidProgramArea){
                        System.out.println("ERROR: Invalid PC range");
                        return InvalidPCValue;
                    }

                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }

                    // Set OpAddr and OpValue from fetched operand
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    // Next word has system call ID, Implement in HW2
                    // systemCallID = HypoMem[(int)PC++];
                    // Call SystemCall to process it
                    // status = SystemCall(Op1Value);

                    // Increment clock by execution time
                    clock = clock + 12;
                    break;

                default: //INVALID OPCODE
                    System.out.println("ERROR: Invalid opcode entered");
                    return InvalidOpCode;

            } // End of switch statement
        } // End of while loop
        return OK;
    } // End of CPU()


    // Fetch function that gets the operand value based on the operand mode. It will take in the
    // mode and register and depending on what mode it is the corresponding case will be executed.
    // The operand address and operand value will be set and returned to CPU() along with the status code
    // to set the appropriate global hardware components.
    private static long[] FetchOperand(long OpMode, long OpReg){

        long returnValue[] = new long[3];
        int Error = 0;
        int OpAddress = 1;
        int OpValue  =2;

        // Fetch operand value based on the operand mode
        switch ((int)OpMode){
            case 1: //REGISTER MODE
                returnValue[OpAddress] = -1; // set to any negative value
                returnValue[OpValue] = GPR[(int)OpReg]; // operand value is in the register
                break;

            case 2: //REGISTER DEFERRED MODE
                returnValue[OpAddress] = GPR[(int)OpReg]; // Op address is in the register

                // Check that Op address is in the valid range and set operand value from memory
                if ( returnValue[OpAddress] >= 0 && returnValue[OpAddress] <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)returnValue[OpAddress]];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] =  InvalidGPRAddr;
                }
                break;

            case 3: //AUTOINCREMENT MODE
                returnValue[OpAddress] = GPR[(int)OpReg]; // Op addr in GPR

                // check for valid op address range and get op value from memory
                if ( returnValue[OpAddress] >= 0 && returnValue[OpAddress] <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)returnValue[OpAddress]];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] = InvalidGPRAddr;
                }

                // Increment register value by 1
                GPR[(int)OpReg]++;
                break;

            case 4: //AUTO-DECREMENT MODE
                // Decrement register value by 1
                --GPR[(int)OpReg];
                // Op address is in the register
                returnValue[OpAddress] = GPR[(int)OpReg];

                // Check op address for valid range and get op value from memory
                if ( returnValue[OpAddress] >= 0 && returnValue[OpAddress] <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)returnValue[OpAddress]];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] =  InvalidGPRAddr;
                }
                break;

            case 5: //DIRECT MODE
                // Op address is in the instruction pointed by PC
                returnValue[OpAddress] = HypoMem[(int)PC++];

                // Checck op address for valid range and get op value from memory
                if ( returnValue[OpAddress] >= 0 && returnValue[OpAddress] <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)returnValue[OpAddress]];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] = InvalidGPRAddr;
                }
                break;

            case 6: //IMMEDIATE MODE
                // Check for valid PC range, Op value is in the instruction
                if (PC >= 0 && PC <= ValidProgramArea){
                    returnValue[OpAddress] = -1; // Any negative number
                    returnValue[OpValue] = HypoMem[(int)PC++]; // Increment PC after fetching value
                }
                else{
                    System.out.println("ERROR: Invalid PC value. ");
                    returnValue[Error] =  InvalidPCValue;
                }
                break;

            default: // INVALID MODE
                System.out.println("ERROR: Invalid mode, returning error");
                returnValue[Error] = InvalidMode;
        } // End of switch OpMode
        return returnValue;
    } // End of FetchOperand()


    // DumpMemory function displays a string passed in as a parameter. Displays content of
    // GPRs, SP, PC, PSR, system Clock and content of the specified memory locations in
    // a given format.
    private static void DumpMemory(String word, long startAddress, long size){
        // Display given string
        System.out.println(word);

        // On invalid memory address show error
        if ( startAddress < 0 || startAddress > MaxMemory || startAddress+size > MaxMemory)
            System.out.println("ERROR: There was an invalid start address, size, or end address.");

        else{
            // Display header for GPRs
            System.out.print("GPRS:");
            for(int i = 0; i < 10; i++){
                print("G"+i);
            }

            // Print content of the GPRs and other HW components
            System.out.print("\n\t");
            for(int i = 0; i < 8; i++){
                print(Long.toString(GPR[i]));
            }
            print(Long.toString(SP));
            print(Long.toString(PC));
            System.out.println();

            // Print header for addressing row
            System.out.print("Address: +0\t\t+1");
            for(int i = 2; i < 10; i++){
                print("+"+i);
            }
            System.out.println();

            long addr = startAddress;
            long endAddr = startAddress+size;

            // Display content of Hypo machine memory locations specified in the parameters
            // 11 items per line: Address of first value followed by content of 10 locations
            while(addr < endAddr){
                System.out.print(addr+"\t");
                for ( int i = 0; i < 10; i++){
                    if ( addr < endAddr) {
                        String formatted = String.format("%7s", HypoMem[(int) addr]);
                        System.out.print(formatted);
                        addr++;
                    }
                    else
                        break;
                }
                System.out.println();
            }
            System.out.println("Clock: " + clock + "\nPSR: " + PSR);
        }
    } //End of DumpMemory()

    // Helper function for formatting output grid
    // Pads a given string to 7 spaces and prints
    private static void print(String out){
        String formatted = String.format("%7s", out);
        System.out.print(formatted);
    }

}
