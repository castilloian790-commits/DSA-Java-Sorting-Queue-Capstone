import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Scanner;

/*
 * Clinic Queue and Patient Management System
 * ------------------------------------------
 * A console program for a small clinic's front desk. The receptionist registers
 * patients, places them in the consultation queue, and calls them in the order
 * they arrived (First-In, First-Out). Patient records can also be sorted by
 * name, age, or appointment time using two hand-written sorting algorithms.
 *
 * Procedural style only: no custom classes or objects. Every method is static,
 * and each patient record is spread across several parallel ArrayLists that
 * share the same index.
 */
public class Main {

    // ---------------------------------------------------------------
    // Patient records: parallel ArrayLists.
    // The values at index i of every list belong to the same patient.
    // ---------------------------------------------------------------
    static ArrayList<Integer> patientIds = new ArrayList<Integer>();
    static ArrayList<String> patientNames = new ArrayList<String>();
    static ArrayList<Integer> patientAges = new ArrayList<Integer>();
    static ArrayList<String> contactNumbers = new ArrayList<String>();
    static ArrayList<String> complaints = new ArrayList<String>();
    static ArrayList<String> appointmentTimes = new ArrayList<String>(); // "HH:MM", 24-hour
    static ArrayList<String> statuses = new ArrayList<String>();         // Registered / Waiting / Consulted

    // ---------------------------------------------------------------
    // Consultation queue. It stores patient IDs, not list positions,
    // so sorting the records never changes who is called next.
    // ---------------------------------------------------------------
    static Queue<Integer> consultationQueue = new LinkedList<Integer>();

    // IDs of patients already consulted, in the order they were called.
    static ArrayList<Integer> consultationLog = new ArrayList<Integer>();

    static final String STATUS_REGISTERED = "Registered";
    static final String STATUS_WAITING = "Waiting";
    static final String STATUS_CONSULTED = "Consulted";

    static int nextPatientId = 1001;
    static boolean sampleDataLoaded = false;

    static Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        printBanner();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readIntInRange("Enter your choice (0-11): ", 0, 11);
            System.out.println();

            switch (choice) {
                case 1:
                    registerPatient();
                    break;
                case 2:
                    displayAllPatients();
                    break;
                case 3:
                    searchPatient();
                    break;
                case 4:
                    updatePatient();
                    break;
                case 5:
                    removePatient();
                    break;
                case 6:
                    enqueuePatient();
                    break;
                case 7:
                    callNextPatient();
                    break;
                case 8:
                    peekNextPatient();
                    break;
                case 9:
                    displayQueue();
                    break;
                case 10:
                    sortMenu();
                    break;
                case 11:
                    loadSamplePatients();
                    break;
                case 0:
                    running = false;
                    break;
            }

            if (running) {
                pause();
            }
        }

        System.out.println("Thank you for using the Clinic Queue System. Goodbye!");
    }

    // =================================================================
    // MENUS AND HEADINGS
    // =================================================================

    static void printBanner() {
        System.out.println("==========================================================");
        System.out.println("       CLINIC QUEUE AND PATIENT MANAGEMENT SYSTEM");
        System.out.println("==========================================================");
    }

    static void printMainMenu() {
        System.out.println();
        System.out.println("======================= MAIN MENU ========================");
        System.out.println(" Patient Records");
        System.out.println("   [1] Register a new patient");
        System.out.println("   [2] Display all patient records");
        System.out.println("   [3] Search for a patient");
        System.out.println("   [4] Update a patient record");
        System.out.println("   [5] Remove a patient record");
        System.out.println(" Consultation Queue");
        System.out.println("   [6] Add patient to the consultation queue  (enqueue)");
        System.out.println("   [7] Call the next patient                  (dequeue)");
        System.out.println("   [8] View the next patient                  (peek)");
        System.out.println("   [9] Display the waiting queue");
        System.out.println(" Sorting");
        System.out.println("  [10] Sort patient records");
        System.out.println(" Other");
        System.out.println("  [11] Load sample patients (for demonstration)");
        System.out.println("   [0] Exit");
        System.out.println("==========================================================");
        System.out.println(" Patients on record: " + patientIds.size()
                + "   |   Waiting in queue: " + consultationQueue.size());
    }

    static void printHeading(String title) {
        System.out.println("---------- " + title + " ----------");
    }

    // =================================================================
    // PATIENT RECORDS: ADD, DISPLAY, SEARCH, UPDATE, REMOVE
    // =================================================================

    static void registerPatient() {
        printHeading("REGISTER A NEW PATIENT");

        String name = readName("Full name: ");
        int age = readIntInRange("Age (0-120): ", 0, 120);
        String contact = readContactNumber("Contact number (11 digits, starts with 09): ");
        String complaint = readNonEmpty("Reason for visit: ");
        String time = readTime("Appointment time (HH:MM, 24-hour, e.g. 09:30 or 14:15): ");

        int id = addPatientRecord(name, age, contact, complaint, time);

        System.out.println();
        System.out.println("Patient registered successfully. Patient ID: " + id);

        // Most walk-in patients join the queue right after registering.
        if (readYesNo("Add " + name + " to the consultation queue now? (Y/N): ")) {
            addToQueue(findIndexById(id));
        } else {
            System.out.println("You can add this patient to the queue later using option [6].");
        }
    }

    // Adds one record to every parallel list at the same time so the
    // indexes stay aligned. Returns the new patient's ID.
    static int addPatientRecord(String name, int age, String contact, String complaint, String time) {
        int id = nextPatientId;
        nextPatientId++;

        patientIds.add(id);
        patientNames.add(name);
        patientAges.add(age);
        contactNumbers.add(contact);
        complaints.add(complaint);
        appointmentTimes.add(time);
        statuses.add(STATUS_REGISTERED);

        return id;
    }

    static void displayAllPatients() {
        printHeading("ALL PATIENT RECORDS");
        if (patientIds.isEmpty()) {
            System.out.println("No patient records yet. Register a patient using option [1].");
            return;
        }
        printPatientTable();
    }

    // Prints every record in the current order of the lists.
    static void printPatientTable() {
        String line = "+------+------------------------+-----+-------------+-------+------------+---------------------------+";
        System.out.println(line);
        System.out.println(String.format("| %-4s | %-22s | %-3s | %-11s | %-5s | %-10s | %-25s |",
                "ID", "Name", "Age", "Contact", "Time", "Status", "Reason for Visit"));
        System.out.println(line);
        for (int i = 0; i < patientIds.size(); i++) {
            System.out.println(String.format("| %-4d | %-22s | %3d | %-11s | %-5s | %-10s | %-25s |",
                    patientIds.get(i),
                    shorten(patientNames.get(i), 22),
                    patientAges.get(i),
                    contactNumbers.get(i),
                    appointmentTimes.get(i),
                    statuses.get(i),
                    shorten(complaints.get(i), 25)));
        }
        System.out.println(line);
        System.out.println("Total records: " + patientIds.size());
    }

    static void printPatientDetails(int index) {
        System.out.println("  Patient ID       : " + patientIds.get(index));
        System.out.println("  Name             : " + patientNames.get(index));
        System.out.println("  Age              : " + patientAges.get(index));
        System.out.println("  Contact number   : " + contactNumbers.get(index));
        System.out.println("  Reason for visit : " + complaints.get(index));
        System.out.println("  Appointment time : " + appointmentTimes.get(index));
        System.out.println("  Status           : " + statuses.get(index));
        if (statuses.get(index).equals(STATUS_WAITING)) {
            System.out.println("  Queue position   : " + getQueuePosition(patientIds.get(index))
                    + " of " + consultationQueue.size());
        }
    }

    static void searchPatient() {
        printHeading("SEARCH FOR A PATIENT");
        if (patientIds.isEmpty()) {
            System.out.println("No patient records to search.");
            return;
        }

        System.out.println("  [1] Search by patient ID");
        System.out.println("  [2] Search by name (whole or part of the name)");
        int choice = readIntInRange("Search by (1-2): ", 1, 2);

        if (choice == 1) {
            int id = readInt("Enter patient ID: ");
            int index = findIndexById(id);
            if (index == -1) {
                System.out.println("No patient found with ID " + id + ".");
            } else {
                System.out.println();
                System.out.println("Patient found:");
                printPatientDetails(index);
            }
        } else {
            String keyword = readNonEmpty("Enter name or part of the name: ").toLowerCase();
            int matches = 0;
            // Linear search: check every record and show all names that contain the keyword.
            for (int i = 0; i < patientNames.size(); i++) {
                if (patientNames.get(i).toLowerCase().contains(keyword)) {
                    matches++;
                    System.out.println();
                    System.out.println("Match #" + matches + ":");
                    printPatientDetails(i);
                }
            }
            if (matches == 0) {
                System.out.println("No patient name contains \"" + keyword + "\".");
            } else {
                System.out.println();
                System.out.println(matches + " matching record(s) found.");
            }
        }
    }

    static void updatePatient() {
        printHeading("UPDATE A PATIENT RECORD");
        if (patientIds.isEmpty()) {
            System.out.println("No patient records to update.");
            return;
        }

        int id = readInt("Enter the ID of the patient to update: ");
        int index = findIndexById(id);
        if (index == -1) {
            System.out.println("No patient found with ID " + id + ". Nothing was updated.");
            return;
        }

        System.out.println();
        printPatientDetails(index);
        System.out.println();
        System.out.println("  [1] Contact number");
        System.out.println("  [2] Reason for visit");
        System.out.println("  [3] Appointment time");
        System.out.println("  [0] Cancel");
        int field = readIntInRange("Field to update (0-3): ", 0, 3);

        if (field == 1) {
            contactNumbers.set(index, readContactNumber("New contact number: "));
        } else if (field == 2) {
            complaints.set(index, readNonEmpty("New reason for visit: "));
        } else if (field == 3) {
            appointmentTimes.set(index, readTime("New appointment time (HH:MM): "));
        } else {
            System.out.println("Update cancelled.");
            return;
        }
        System.out.println("Record of " + patientNames.get(index) + " updated successfully.");
    }

    static void removePatient() {
        printHeading("REMOVE A PATIENT RECORD");
        if (patientIds.isEmpty()) {
            System.out.println("No patient records to remove.");
            return;
        }

        int id = readInt("Enter the ID of the patient to remove: ");
        int index = findIndexById(id);
        if (index == -1) {
            System.out.println("No patient found with ID " + id + ". Nothing was removed.");
            return;
        }

        // A waiting patient cannot be deleted, or the queue would hold an ID
        // that points to no record.
        if (statuses.get(index).equals(STATUS_WAITING)) {
            System.out.println("Cannot remove " + patientNames.get(index)
                    + ": this patient is still waiting in the consultation queue.");
            return;
        }

        String name = patientNames.get(index);
        if (!readYesNo("Remove the record of " + name + "? (Y/N): ")) {
            System.out.println("Removal cancelled.");
            return;
        }

        // Remove from every parallel list at the same index.
        patientIds.remove(index);
        patientNames.remove(index);
        patientAges.remove(index);
        contactNumbers.remove(index);
        complaints.remove(index);
        appointmentTimes.remove(index);
        statuses.remove(index);

        System.out.println("Record of " + name + " (ID " + id + ") removed.");
    }

    // Linear search for a patient ID. Returns the list index, or -1 if not found.
    static int findIndexById(int id) {
        for (int i = 0; i < patientIds.size(); i++) {
            if (patientIds.get(i) == id) {
                return i;
            }
        }
        return -1;
    }

    // =================================================================
    // CONSULTATION QUEUE: ENQUEUE, DEQUEUE, PEEK, DISPLAY, ISEMPTY
    // =================================================================

    static void enqueuePatient() {
        printHeading("ADD PATIENT TO THE CONSULTATION QUEUE");
        if (patientIds.isEmpty()) {
            System.out.println("No patients registered yet. Register a patient first using option [1].");
            return;
        }

        int id = readInt("Enter the patient ID: ");
        int index = findIndexById(id);
        if (index == -1) {
            System.out.println("No patient found with ID " + id + ". Nothing was added to the queue.");
            return;
        }
        addToQueue(index);
    }

    // Enqueue: the patient goes to the BACK of the line.
    static void addToQueue(int index) {
        int id = patientIds.get(index);
        String name = patientNames.get(index);

        if (statuses.get(index).equals(STATUS_WAITING)) {
            System.out.println(name + " is already in the queue (position "
                    + getQueuePosition(id) + "). A patient cannot be queued twice.");
            return;
        }

        consultationQueue.offer(id);
        statuses.set(index, STATUS_WAITING);
        System.out.println(name + " (ID " + id + ") added to the queue. Queue position: "
                + consultationQueue.size());
    }

    // Dequeue: the patient at the FRONT of the line is called and removed.
    static void callNextPatient() {
        printHeading("CALL THE NEXT PATIENT");
        if (consultationQueue.isEmpty()) {
            System.out.println("The consultation queue is empty. There is no patient to call.");
            return;
        }

        int id = consultationQueue.poll();
        int index = findIndexById(id);
        statuses.set(index, STATUS_CONSULTED);
        consultationLog.add(id);

        System.out.println("NOW CALLING: " + patientNames.get(index) + " (ID " + id + ")");
        System.out.println("Please proceed to the consultation room.");
        System.out.println();
        printPatientDetails(index);
        System.out.println();
        if (consultationQueue.isEmpty()) {
            System.out.println("No more patients are waiting.");
        } else {
            int nextIndex = findIndexById(consultationQueue.peek());
            System.out.println("Next in line: " + patientNames.get(nextIndex)
                    + "   |   Still waiting: " + consultationQueue.size());
        }
    }

    // Peek: look at the FRONT of the line without removing anyone.
    static void peekNextPatient() {
        printHeading("NEXT PATIENT IN LINE");
        if (consultationQueue.isEmpty()) {
            System.out.println("The consultation queue is empty. No patient is waiting.");
            return;
        }

        int index = findIndexById(consultationQueue.peek());
        System.out.println("The next patient to be called is:");
        printPatientDetails(index);
        System.out.println();
        System.out.println("(Peek only - the patient is still in the queue.)");
    }

    // Display the queue from front to back. Iterating with a for-each loop
    // reads the queue without removing anything from it.
    static void displayQueue() {
        printHeading("CONSULTATION QUEUE (FRONT TO BACK)");
        if (consultationQueue.isEmpty()) {
            System.out.println("The consultation queue is empty.");
        } else {
            String line = "+-----+------+------------------------+-------+---------------------------+";
            System.out.println(line);
            System.out.println(String.format("| %-3s | %-4s | %-22s | %-5s | %-25s |",
                    "No.", "ID", "Name", "Time", "Reason for Visit"));
            System.out.println(line);
            int position = 1;
            for (int id : consultationQueue) {
                int index = findIndexById(id);
                System.out.println(String.format("| %3d | %-4d | %-22s | %-5s | %-25s |",
                        position, id,
                        shorten(patientNames.get(index), 22),
                        appointmentTimes.get(index),
                        shorten(complaints.get(index), 25)));
                position++;
            }
            System.out.println(line);
            System.out.println("Front of the queue: No. 1   |   Patients waiting: " + consultationQueue.size());
        }

        // Patients already called, in the order they were served.
        System.out.println();
        if (consultationLog.isEmpty()) {
            System.out.println("Already consulted: none yet.");
        } else {
            System.out.print("Already consulted (in order called): ");
            for (int i = 0; i < consultationLog.size(); i++) {
                int index = findIndexById(consultationLog.get(i));
                String name = (index == -1) ? "(record removed)" : patientNames.get(index);
                System.out.print((i + 1) + ". " + name);
                if (i < consultationLog.size() - 1) {
                    System.out.print(",  ");
                }
            }
            System.out.println();
        }
    }

    // Position of a patient in the queue (1 = front), or -1 if not in the queue.
    static int getQueuePosition(int id) {
        int position = 1;
        for (int queuedId : consultationQueue) {
            if (queuedId == id) {
                return position;
            }
            position++;
        }
        return -1;
    }

    // =================================================================
    // SORTING
    // Both algorithms are written by hand. They sort the parallel lists
    // together by swapping or moving whole records, so every field stays
    // with its own patient.
    // =================================================================

    static void sortMenu() {
        printHeading("SORT PATIENT RECORDS");
        if (patientIds.isEmpty()) {
            System.out.println("No patient records to sort.");
            return;
        }
        if (patientIds.size() == 1) {
            System.out.println("Only one record exists, so it is already sorted.");
            return;
        }

        System.out.println("Sort by:");
        System.out.println("  [1] Name (A to Z)");
        System.out.println("  [2] Age (youngest to oldest)");
        System.out.println("  [3] Appointment time (earliest to latest)");
        int field = readIntInRange("Choose a field (1-3): ", 1, 3);

        System.out.println();
        System.out.println("Sorting algorithm:");
        System.out.println("  [1] Selection Sort");
        System.out.println("  [2] Insertion Sort");
        int algorithm = readIntInRange("Choose an algorithm (1-2): ", 1, 2);

        String fieldName = (field == 1) ? "Name" : (field == 2) ? "Age" : "Appointment Time";
        String algorithmName = (algorithm == 1) ? "Selection Sort" : "Insertion Sort";

        System.out.println();
        System.out.println("BEFORE SORTING:");
        printPatientTable();

        int comparisons;
        if (algorithm == 1) {
            comparisons = selectionSort(field);
        } else {
            comparisons = insertionSort(field);
        }

        System.out.println();
        System.out.println("AFTER SORTING by " + fieldName + " using " + algorithmName + ":");
        printPatientTable();
        System.out.println(algorithmName + " made " + comparisons + " comparisons.");
        System.out.println("Note: the consultation queue is NOT affected. Patients are still called in arrival order.");
    }

    /*
     * Selection Sort
     * For each position i, scan the unsorted part (i+1 to the end) for the
     * smallest record, then swap it into position i. At most one swap per pass,
     * which keeps the number of times we touch all seven lists low.
     */
    static int selectionSort(int field) {
        int comparisons = 0;
        int n = patientIds.size();

        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                comparisons++;
                if (compareRecords(j, minIndex, field) < 0) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                swapRecords(i, minIndex);
            }
        }
        return comparisons;
    }

    /*
     * Insertion Sort
     * Take the record at position i and move it left, one step at a time,
     * while the record before it is larger. Each step is a swap of two
     * neighbouring records. Equal records are never swapped, so patients with
     * the same name, age, or time keep their original order (stable sort).
     */
    static int insertionSort(int field) {
        int comparisons = 0;
        int n = patientIds.size();

        for (int i = 1; i < n; i++) {
            int j = i;
            while (j > 0) {
                comparisons++;
                if (compareRecords(j - 1, j, field) > 0) {
                    swapRecords(j - 1, j);
                    j--;
                } else {
                    break; // record is already in its correct place
                }
            }
        }
        return comparisons;
    }

    // Compares the records at index a and b by the chosen field.
    // Negative: a comes first.  Zero: same.  Positive: b comes first.
    static int compareRecords(int a, int b, int field) {
        if (field == 1) {
            return patientNames.get(a).compareToIgnoreCase(patientNames.get(b));
        } else if (field == 2) {
            return patientAges.get(a) - patientAges.get(b);
        } else {
            // Times are always stored as zero-padded "HH:MM", so comparing the
            // text gives the correct time order ("08:45" < "09:30" < "13:00").
            return appointmentTimes.get(a).compareTo(appointmentTimes.get(b));
        }
    }

    // Swaps two whole records: the same two indexes in EVERY parallel list.
    static void swapRecords(int a, int b) {
        swapInt(patientIds, a, b);
        swapString(patientNames, a, b);
        swapInt(patientAges, a, b);
        swapString(contactNumbers, a, b);
        swapString(complaints, a, b);
        swapString(appointmentTimes, a, b);
        swapString(statuses, a, b);
    }

    static void swapInt(ArrayList<Integer> list, int a, int b) {
        int temp = list.get(a);
        list.set(a, list.get(b));
        list.set(b, temp);
    }

    static void swapString(ArrayList<String> list, int a, int b) {
        String temp = list.get(a);
        list.set(a, list.get(b));
        list.set(b, temp);
    }

    // =================================================================
    // SAMPLE DATA
    // =================================================================

    static void loadSamplePatients() {
        printHeading("LOAD SAMPLE PATIENTS");
        if (sampleDataLoaded) {
            System.out.println("Sample patients were already loaded. They were not added again.");
            return;
        }

        addPatientRecord("Reyes, Maria Clara", 34, "09171234567", "Fever and cough", "10:30");
        addPatientRecord("Santos, Juan", 8, "09281234567", "Vaccination", "09:00");
        addPatientRecord("Dela Cruz, Pedro", 67, "09391234567", "Blood pressure check", "08:15");
        addPatientRecord("Bautista, Ana", 25, "09451234567", "Follow-up check-up", "13:45");
        addPatientRecord("Garcia, Luis", 41, "09561234567", "Back pain", "11:00");
        sampleDataLoaded = true;

        System.out.println("5 sample patients added with status Registered (not yet in the queue).");
        System.out.println("Use option [6] to add them to the consultation queue.");
    }

    // =================================================================
    // INPUT HELPERS
    // Every read uses nextLine() and checks the text itself, so a wrong
    // entry prints a message and asks again instead of crashing.
    // =================================================================

    static String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return input.nextLine().trim();
        } catch (NoSuchElementException e) {
            // Input stream closed (e.g. Ctrl+Z / Ctrl+D). Exit cleanly.
            System.out.println();
            System.out.println("Input ended. Exiting the program.");
            System.exit(0);
            return "";
        }
    }

    static int readInt(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                System.out.println("  Nothing was entered. Please type a number.");
                continue;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input: \"" + text + "\" is not a whole number. Please try again.");
            }
        }
    }

    static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("  Out of range: please enter a number from " + min + " to " + max + ".");
        }
    }

    static String readNonEmpty(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("  This field cannot be empty. Please try again.");
        }
    }

    // Names may contain letters, spaces, periods, commas, hyphens and apostrophes.
    static String readName(String prompt) {
        while (true) {
            String name = readNonEmpty(prompt);
            boolean valid = true;
            boolean hasLetter = false;
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (Character.isLetter(c)) {
                    hasLetter = true;
                } else if (c != ' ' && c != '.' && c != ',' && c != '-' && c != '\'') {
                    valid = false;
                }
            }
            if (valid && hasLetter) {
                return name;
            }
            System.out.println("  Invalid name: use letters, spaces, periods, commas, hyphens or apostrophes only.");
        }
    }

    // Philippine mobile number: exactly 11 digits starting with 09.
    static String readContactNumber(String prompt) {
        while (true) {
            String number = readNonEmpty(prompt);
            boolean allDigits = true;
            for (int i = 0; i < number.length(); i++) {
                if (!Character.isDigit(number.charAt(i))) {
                    allDigits = false;
                }
            }
            if (allDigits && number.length() == 11 && number.startsWith("09")) {
                return number;
            }
            System.out.println("  Invalid contact number: enter 11 digits starting with 09 (e.g. 09171234567).");
        }
    }

    // Accepts H:MM or HH:MM (00:00 to 23:59) and always stores it as HH:MM.
    static String readTime(String prompt) {
        while (true) {
            String text = readNonEmpty(prompt);
            int colon = text.indexOf(':');
            if (colon > 0 && colon == text.lastIndexOf(':')) {
                try {
                    int hour = Integer.parseInt(text.substring(0, colon));
                    String minutePart = text.substring(colon + 1);
                    int minute = Integer.parseInt(minutePart);
                    if (minutePart.length() == 2 && hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                        return String.format("%02d:%02d", hour, minute);
                    }
                } catch (NumberFormatException e) {
                    // falls through to the error message below
                }
            }
            System.out.println("  Invalid time: use HH:MM in 24-hour format (e.g. 08:30 or 14:00).");
        }
    }

    static boolean readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt).toUpperCase();
            if (text.equals("Y") || text.equals("YES")) {
                return true;
            }
            if (text.equals("N") || text.equals("NO")) {
                return false;
            }
            System.out.println("  Please enter Y or N.");
        }
    }

    static void pause() {
        System.out.println();
        readLine("Press Enter to return to the main menu...");
    }

    // Cuts long text so the table columns stay aligned.
    static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
