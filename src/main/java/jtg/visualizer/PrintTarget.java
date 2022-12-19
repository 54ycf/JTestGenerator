package jtg.visualizer;

public interface PrintTarget {
    void println(String str);

    void println();

    void print(String str);

    void print(char c);

    void flush();
}