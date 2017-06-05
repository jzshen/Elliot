/**
 * Created by JenniferShen on 2017-06-04.
 */

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/*
Question :The times listed in the file are times when each user is busy, in the format above. A single user may have several events listed and some events may overlap.  The file is not necessarily sorted by any of the fields in it. Write a program to read the CSV file (called calendar.csv) and find the longest block of time in a single day that's free for all the users to meet. Only look for meetings between 8AM and 10PM, on days up to one week from the date the program is run.
Input: calendar.csv
100, 2017-04-03 13:30:00, 2017-04-03 14:30:00
101, 2017-04-15 00:00:00, 2017-04-15 23:59:59
Output:
2017-06-06 08:00:00
2017-06-06 22:00:00

Steps:
1. Read in csv file, only taking in start and end time putting them in a Calendar[] of size 2
2. Put these intervals in a list
    If there are no intervals return the next available full day
3. take intervals and sort them by starting time and merge overlapping ones and only keep the ones within the next week (between 8-22hrs)
    If there are no valid intervals return the next available full day
4. loop through these intervals finding the max available time
    currAvailableTime = BeginOfCurrBusyTime - EndOfPrevBusyTime // if this is overnight or more then a day adjust
    maxAvailableTime = Max(maxAvailableTime, currAvailableTime)
5. print start and end time that will give us the max available time

*/

public class ElliotQuestion {

    public static final int START_DAY = 800;
    public static final int END_DAY   = 2200;

   String friendly(int num) {
       return num < 10 ? String.format("%01", num) : Integer.toString(num);
   }
   void  printTime(int year, int month, int day, int hour, int min, int sec) {
       System.out.println(year + "-" + friendly(month) + "-" + friendly(day) + " " + friendly(hour) + ":" + friendly(min) + ":" + friendly(sec));
   }

   void printFullDay(Calendar day) {
       printTime(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DATE), 8, 0, 0);
       printTime(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DATE), 22, 0, 0);
   }

   void printFullDayTodayOrTomm(Calendar today) {
       // if 8 am hasnt hit today, we are free today!
       if (today.get(Calendar.HOUR_OF_DAY) * 100 + today.get(Calendar.MINUTE) < START_DAY) {
           printFullDay(today);
           return;
       }
       // Else everyone is free all day tomorrow
       Calendar tomorrow = today;
       tomorrow.add(Calendar.DATE, 1);
       printFullDay(tomorrow);
       return;
   }
    List<Calendar[]> flatten(List<Calendar[]> listOfIntervals, Calendar today) {
        Calendar nextWeek = today;
        nextWeek.add(Calendar.DATE, 7);
        // Sort list of intervals based off start time
        Collections.sort(listOfIntervals, Comparator.comparing(interval -> interval[0]));
        Stack<Calendar[]> stack = new Stack<>();
        stack.push(listOfIntervals.get(0)); // Push first interval onto stack
        for (int i = 1; i < listOfIntervals.size(); i++) {
            Calendar[] prevInterval = stack.pop();
            Calendar[] currInterval = listOfIntervals.get(i);
            // If start time of next interval occurs before the previous intervals finish time  merge them
            if (currInterval[0].before(prevInterval[1])) {
                prevInterval[1] = currInterval[1];
                stack.push(prevInterval);
            }
            // else push the next interval on the stack
            else stack.push(currInterval);
        }
        // Create list
        List<Calendar[]> flattenedList = new ArrayList<>();
        while (!stack.isEmpty()) {
            Calendar[] interval = stack.pop();
            if (interval[1].before(today)) continue; // We do not care if we have busy times before today
            if (interval[0].after(nextWeek)) continue; // We do not care if we are busy after a week from today
            // We do not care if they are busy between 10pm (2200) - 8am (800)
            if (interval[0].get(Calendar.HOUR_OF_DAY) > END_DAY/100 && interval[1].get(Calendar.HOUR_OF_DAY) < START_DAY/100) continue;
            flattenedList.add(interval);  // Else we can add to the list
        }
        return flattenedList;
    }

     Calendar formatDate(String rawFormat) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = (Date)formatter.parse(rawFormat);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public void main(String[] args) throws Exception {
        String csvFile = "calendar.csv";
        String line = "";
        String cvsSplitBy = ",";
        List<Calendar[]> listOfIntervals = new ArrayList<>();

        // Read info into a list of intervals of when people are busy
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                String[] timeInformation = line.split(cvsSplitBy);
                Calendar startBusy = formatDate(timeInformation[1]);
                Calendar endBusy   = formatDate(timeInformation[2]);
                Calendar[] interval = {startBusy, endBusy};
                listOfIntervals.add(interval);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Date currentDate = new Date();
        Calendar today    = Calendar.getInstance();
        today.setTime(currentDate);

        if (listOfIntervals.isEmpty())  {
            printFullDayTodayOrTomm(today);
            return;
        }

        // The flattenedListOfIntervals will be all the busy time intervals sorted (with overlapping ones merged together)
        // in the next week between 8am and 10pm
        List<Calendar[]> flattenedListOfIntervals = flatten(listOfIntervals, today);

        if (flattenedListOfIntervals.isEmpty()) {
            printFullDayTodayOrTomm(today);
            return;
        }

        Calendar prev = today;
        long prevTime = prev.getTimeInMillis(); // When the last busy time ends
        long maxTimeInterval = Long.MIN_VALUE, maxBeginTime = Long.MIN_VALUE, maxEndTime = Long.MIN_VALUE;

        // At each iteration we are comparing the time between CurrBusyStartTime - PrevBusyFinishTime to get the free time
        for (int i = 0; i < flattenedListOfIntervals.size(); i++) {

            Calendar curr = flattenedListOfIntervals.get(i)[0]; // assign curr to the the current busy intervals start time
            long currTime = curr.getTimeInMillis();

            Calendar prevEOD = prev;
            prev.set(Calendar.HOUR_OF_DAY, 22);

            Calendar currBOD = curr;
            curr.set(Calendar.HOUR_OF_DAY, 8);

            long currTimeInterval = currTime - prevTime;
            int daysApart = curr.get(Calendar.DATE) - prev.get(Calendar.DATE);

            if (daysApart > 1) { // we want to return the first full free day here.
                printFullDayTodayOrTomm(prev);
                return; // we have printed something
            }
            if (daysApart == 1) { // if the intervals are a day apart we have to take the max of prev time to EOD, or BOD to currTime
                currTimeInterval = Math.max(prevEOD.getTimeInMillis() - prevTime, currTime - currBOD.getTimeInMillis());
                if (currTimeInterval == prevEOD.getTimeInMillis() - prevTime) currTime = prevEOD.getTimeInMillis();
                else prevTime = currBOD.getTimeInMillis();
            }
            if (currTimeInterval > maxTimeInterval) { // update maxTimeInverval values if necessary, note we always get here if we did not return previously
                maxTimeInterval = currTimeInterval;
                maxBeginTime = prevTime;
                maxEndTime = currTime;
            }
            prev = flattenedListOfIntervals.get(i)[1]; // assign prev to the current busy interval end time
        }

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(maxBeginTime);
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(maxEndTime);
        printTime(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DATE),
                  start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), start.get(Calendar. SECOND));
        printTime(end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DATE),
                end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE), end.get(Calendar. SECOND));
        return;

    }
}
