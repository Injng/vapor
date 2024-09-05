package org.vapor;

import java.util.ArrayList;

public class Benchmark {
    /** An ArrayList of timestamps for each stage of the pipeline. */
    private ArrayList<Long> timestamps;

    public Benchmark() {
        timestamps = new ArrayList<Long>();
    }

    /**
     * Marks the current time in milliseconds, adding it to the list of timestamps.
     */
    public void mark() {
        timestamps.add(System.currentTimeMillis());
    }

    /**
     * Print out the time taken for the latest stage of the pipeline.
     *
     * @param stage the name of the stage.
     */
    public void print(String stage) {
        long start = timestamps.get(timestamps.size() - 2);
        long end = timestamps.get(timestamps.size() - 1);
        System.out.println("Time taken for " + stage + " : " + (end - start) + " ms");
    }

    /*
     * Print out the total time taken for the pipeline.
     */
    public void total() {
        long start = timestamps.get(0);
        long end = timestamps.get(timestamps.size() - 1);
        System.out.println("Total time taken: " + (end - start) + " ms");
    }
}

