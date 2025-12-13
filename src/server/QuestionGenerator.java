package server;

import java.util.Random;

public class QuestionGenerator {
    private static final Random rnd = new Random();

    public static class Q {
        public final int id;
        public final String text;
        public final int answer;

        public Q(int id, String text, int answer) {
            this.id = id;
            this.text = text;
            this.answer = answer;
        }
    }

    private static int nextId = 1;
    private static synchronized int newId() { return nextId++; }

    public static Q make(String op) {
        int a, b;
        switch (op) {
            case "+": a = rnd.nextInt(10); b = rnd.nextInt(10); return new Q(newId(), a + " + " + b, a + b);
            case "-": a = rnd.nextInt(10); b = rnd.nextInt(10); 
                      // jaga hasil non-negative supaya sederhana
                      if (a < b) { int t=a; a=b; b=t; }
                      return new Q(newId(), a + " - " + b, a - b);
            case "*": a = rnd.nextInt(10); b = rnd.nextInt(10); return new Q(newId(), a + " × " + b, a * b);
            case "/": // bikin a dan b sehingga a % b == 0, b != 0
                      b = rnd.nextInt(9) + 1; // 1..9
                      int mult = rnd.nextInt(10); // 0..9
                      a = b * mult;
                      return new Q(newId(), a + " ÷ " + b, mult);
            default:
                a = rnd.nextInt(10); b = rnd.nextInt(10);
                return new Q(newId(), a + " + " + b, a + b);
        }
    }
}
