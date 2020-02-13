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


    private static final int ValidProgramArea = 3499;
    private final static int OK = 0;
    private final static long ENDOFPROGRAM = -1;



    public static void main(String[] args) throws Exception{

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


    }

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
    }

} //End of main()
