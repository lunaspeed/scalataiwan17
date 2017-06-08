import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Created by Lunaspeed on 05/06/2017.
 */
public class FutureTest {

    public static void main(String... args) throws Exception {

        MyThread t = new MyThread();
        t.start();

        t.getValue();//good luck with that, if you can `wait` you probably need a CountDownLatch or a Lock
        //what if we have 2 MyThread? need to wait on both


        // a bit better with Future
        final Future<String> f1 = CompletableFuture.supplyAsync(new Supplier<String>() {

            @Override
            public String get() {
                return "future number 1";
            }
        });

        Future<String> f2 = CompletableFuture.supplyAsync(new Supplier<String>() {

            @Override
            public String get() {
                try {
                    Future<String> a = anotherFuture();//is the order correct?
                    String s = f1.get();
                    return a.get() + s + ", future number two";
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });


        f1.get();
        f2.get();
        //>< still need to wait on all stage or give it to a thread to wait
        //or mix all future together and create unmanageable code


        //with Java8 a lot better
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "functional future");


        //map
        CompletableFuture<Integer> f4 = f3.thenApply(s -> s.length());

        //flatMap
        CompletableFuture<String> f5 = f3.thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> s + " rules"));

        //zip
        CompletableFuture<String> f6 = f4.thenCombine(f5, (i, s) -> s + " " + i + " times");

        //now we can create composable methods. everything in side can be extracted to reusable codes

    }

    static class MyThread extends Thread {

        private String value;
        public void run() {
            value = "hello";
        }

        public String getValue() {
            return value;
        }
    }


    private static Future<String> anotherFuture() {
        return CompletableFuture.completedFuture("hello");
    }

}
