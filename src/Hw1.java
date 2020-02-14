/*
Calvin Cuff
Operating System Internals
Prof Krishnamoorthy
HW1
2/11/20
 */

// TODO: ints vs long ? Return type as int, variables listed as long
import java.io.File;
import java.util.Scanner;

public class Hw1 {

    private static long[] HypoMem = new long[10000];
    private static long MAR, MBR;
    private static long clock;
    private static long[] GPR = new long[8];
    private static long IR, PSR, PC, SP;

    public static long  Op1Address, Op1Value, Op2Address, Op2Value;

    private static final int ValidProgramArea = 3499;
    private final static int OK = 0;
    private final static long ENDOFPROGRAM = -1;
    private final static int HaltStatus = -5;
    private final static int InvalidPCValue = -6;
    private final static int InvalidOpMode = -7;
    private final static int InvalidGPR = -8;
    private final static int InvalidImmediateMode = -9;
    private final static int DivideBy0Error = -10;
    private final static int InvalidOpCode = -11;
    private final static int InvalidGPRAddr = -12;
    private final static int InvalidMode = -13;



    public static void main(String[] args) throws Exception{

        Hw1 Mem = new Hw1();
        InitializeSystem();

        Scanner in = new Scanner(System.in);

        System.out.println("Enter the filename of machine language executable");

        String machineFile = in.nextLine();

        long returnValue = AbsoluteLoader("C:\\Users\\calcu\\OSI\\src\\" + machineFile);
        //TODO: Check for error and return code

        PC = returnValue;
        System.out.println(PC);

        //Dump memory after loading user program: Range 0 to 99 locations

        //ExecuteProgram()
        long ExecutionCompletionStatus = CPU();

        //Dump memory after executing user program: Range 0 to 99 locations


    } // End of main

    public static void InitializeSystem(){
        //(1)
        for(int i =0 ; i < HypoMem.length; i++){
            HypoMem[i] = 0;
        }

        //(2)
        MAR = 0;
        MBR = 0;
        //(3)
        clock = 0;

        //(4)
        for ( int j = 0; j < GPR.length; j++){
            GPR[j] = 0;
        }

        //(5)
        IR = 0;
        PSR = 0;
        PC = 0;
        SP = 0;

    } // End of InitializeSystem()

    public static int AbsoluteLoader(String filename) throws Exception {

        int Address;
        int Content;
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("File does not exist");
            return -99; //TODO: Fix error check
        }
        Scanner in = new Scanner(file);

        while (in.hasNextLine()) {
            Address = in.nextInt();
            Content = in.nextInt();
            if (Address == ENDOFPROGRAM) {
                System.out.println("Reached end of program");
                in.close();
                return Content;  // Value to be stored in PC main
            } else if (Address >= 0 && Address < 10000) { //TODO: Is this range accurate, off by 1
                HypoMem[Address] = Content;
            } else {
                System.out.println("There was an unexpected error that should not have happened"); //Get approp error
                in.close();
                return -77; //TODO Get right error code
            }
        } //end of while loop
        System.out.println("End of file encountered without End of Program line");
        in.close();
        return -96; //TODO: Get right error code
    } //End of Absolute Loader

    public static long CPU(){
        long Opcode, Remainder, Op1Mode, Op1Gpr, Op2Mode, Op2Gpr;
        long Returned[] = new long[3];
        long Result;
        boolean halt = false;
        boolean error = false;

        int err = 0;
        int OpAddr = 1;
        int OpVal = 2;


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
                    return HaltStatus;


                case 1: //ADD
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
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

                    // If OpMode is Register mode
                    if ( Op1Mode == 1 ){
                        GPR[(int)Op1Gpr] = Result;
                    }

                    else if ( Op1Mode == 6 ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 2: //Subtract
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
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

                    // If OpMode is Register mode
                    if ( Op1Mode == 1 ){
                        GPR[(int)Op1Gpr] = Result;
                    }

                    else if ( Op1Mode == 6 ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 3: // MULTIPLY
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
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

                    // If OpMode is Register mode
                    if ( Op1Mode == 1 ){
                        GPR[(int)Op1Gpr] = Result;
                    }

                    else if ( Op1Mode == 6 ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 4: // DIVIDE
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
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
                        System.out.println("ERRPR: Division by 0 is a fatal run-time error.");
                        return DivideBy0Error;
                    }
                    // Divide the operand values
                    Result = Op1Value / Op2Value;

                    // If OpMode is Register mode
                    if ( Op1Mode == 1 ){
                        GPR[(int)Op1Gpr] = Result;
                    }

                    else if ( Op1Mode == 6 ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 5: // MOVE
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    Returned = FetchOperand(Op2Mode, Op2Gpr);
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op2Address = Returned[OpAddr];
                    Op2Value = Returned[OpVal];

                    // Add the operand values
                    Result = Op2Value;

                    // If OpMode is Register mode
                    if ( Op1Mode == 1 ){
                        GPR[(int)Op1Gpr] = Result;
                    }

                    else if ( Op1Mode == 6 ){
                        System.out.println("ERROR: Destination operand cannot be immediate value");
                        return InvalidImmediateMode;
                    }
                    else{
                        HypoMem[(int)Op1Address] = Result;
                    }

                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 6: //BRANCH
                    if ( PC >= 0 && PC <= ValidProgramArea)
                        PC = HypoMem[(int)PC];
                    else {
                        System.out.println("Error: Invalid PC range found");
                        return InvalidPCValue;
                    }
                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 7: //BrOnMinus
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];


                    if (Op1Value < 0){
                        if ( PC >= 0 && PC <= ValidProgramArea)
                            PC = HypoMem[(int)PC];
                        else {
                            System.out.println("Error: Invalid PC range found");
                            return InvalidPCValue;
                        }
                    }
                    else{
                        PC++; // Skip branch address to go to next instruction
                    }
                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 8: // BrOnPlus
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    if (Op1Value > 0){
                        if ( PC >= 0 && PC <= ValidProgramArea)
                            PC = HypoMem[(int)PC];
                        else {
                            System.out.println("Error: Invalid PC range found");
                            return InvalidPCValue;
                        }
                    }
                    else{
                        PC++; // Skip branch address to go to next instruction
                    }
                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 9: //BrOnZero
                    Returned = FetchOperand(Op1Mode, Op1Gpr);
                    // check for error and return error code
                    if ( Returned[err] < 0){
                        return Returned[err];
                    }
                    Op1Address = Returned[OpAddr];
                    Op1Value = Returned[OpVal];

                    if (Op1Value == 0){
                        if ( PC >= 0 && PC <= ValidProgramArea)
                            PC = HypoMem[(int)PC];
                        else {
                            System.out.println("Error: Invalid PC range found");
                            return InvalidPCValue;
                        }
                    }
                    else{
                        PC++; // Skip branch address to go to next instruction
                    }
                    //TODO: Increment clock by Add instruction execution time
                    break;

                case 10: // PUSH
                    break;

                case 11: // POP
                    break;

                case 12: // SYSTEM CALL
                    break;

                default: //INVALID OPCODE
                    System.out.println("ERROR: Invalid opcode entered");
                    return InvalidOpCode;

            } // End of switch statement
        } // End of while loop
        return OK;
    } // End of CPU()

    private static long[] FetchOperand(long OpMode, long OpReg){

        long returnValue[] = new long[3];
        int Error = 0;
        int OpAddress = 1;
        int OpValue  =2;

        switch ((int)OpMode){
            case 1: //REGISTER MODE
                returnValue[OpAddress] = -1;
                returnValue[OpValue] = GPR[(int)OpReg];
                break;

            case 2: //REGISTER DEFERRED MODE
                returnValue[OpAddress] = GPR[(int)OpReg];
                // TODO: Push these into static vars at top for user free space
                if ( OpAddress >= 0 && OpAddress <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)OpAddress];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] =  InvalidGPRAddr;
                }
                break;

            case 3: //AUTOINCREMENT MODE
                returnValue[OpAddress] = GPR[(int)OpReg];
                if ( OpAddress >= 0 && OpAddress <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)OpAddress];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] = InvalidGPRAddr;
                }
                GPR[(int)OpReg]++;
                break;

            case 4: //AUTO-DECREMENT MODE
                --GPR[(int)OpReg];
                returnValue[OpAddress] = GPR[(int)OpReg];

                if ( OpAddress >= 0 && OpAddress <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)OpAddress];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] =  InvalidGPRAddr;
                }
                break;

            case 5: //DIRECT MODE
                returnValue[OpAddress] = HypoMem[(int)PC++];

                if ( OpAddress >= 0 && OpAddress <= ValidProgramArea){
                    returnValue[OpValue] = HypoMem[(int)OpAddress];
                }
                else{
                    System.out.println("ERROR: Invalid address range. ");
                    returnValue[Error] = InvalidGPRAddr;
                }
                break;

            case 6: //IMMEDIATE MODE
                if (PC >= 0 && PC <= ValidProgramArea){
                    returnValue[OpAddress] = -1;
                    returnValue[OpValue] = HypoMem[(int)PC++];
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

}
