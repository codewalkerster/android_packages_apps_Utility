package com.hardkernel.odroid;

public class CPU {
    enum Cluster {
        Big,
        Little
    }
    public Governor governor;
    public Frequency frequency;
    Cluster cluster;

    private CPU (String tag, Cluster cluster) {
        this.cluster = cluster;
        governor = new Governor(tag, cluster);
        frequency = new Frequency(tag, cluster);
    }

    private static CPU cpu_big = null;
    private static CPU cpu_little = null;

    public static CPU getCPU(String tag, Cluster cluster) {
        CPU cpu = null;
        switch (cluster) {
            case Big:
                if (cpu_big == null)
                    cpu_big = new CPU(tag, cluster);
                cpu = cpu_big;
                break;
            case Little:
                if (cpu_little == null)
                    cpu_little = new CPU(tag, cluster);
                cpu = cpu_little;
                break;
        }

        return cpu;
    }
}
