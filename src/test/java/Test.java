import io.github.justawaifuhunter.bluebird.async.Promise;
import io.github.justawaifuhunter.bluebird.async.PromiseArray;

public class Test {
    public static void main(String[] args) throws Exception {
        System.out.println("Running tests...");

        test1().await();
        test2().await();

        test3().catchException(() -> "Rejected").await();
        test4().catchException(() -> "Rejected").await();
        test5(10L).await();
        test5(200_000L).catchException(() -> 0L).await();
        test6(10L).await();
        test6(200_000L).catchException(() -> 0L).await();
        test7().await();
    }

    static void endTest(int n) {
        System.out.printf("Test %d finalized\n", n);
    }

    static PromiseArray<Integer> test1() {
        return (PromiseArray<Integer>) Promise.all(342434).then(System.out::println).onFinally(() -> endTest(1));
    }

    static PromiseArray<Integer> test2() {
        return (PromiseArray<Integer>) Promise.all(Promise.resolver(343)).then(System.out::println).onFinally(() -> endTest(2));
    }

    static Promise<Object> test3() {
        return Promise.rejected("Test").then(System.out::println).onFinally(() -> endTest(3));
    }

    static Promise<Object> test4() {
        return Promise.rejected(new Exception("Test")).then(System.out::println).onFinally(() -> endTest(4));
    }

    static Promise<Long> test5(long i) {
        return new Promise<Long>((resolve, reject) -> {
            if (i > 100_000L) {
                reject.accept(new Exception("TOO LONG"));
                return;
            }

            resolve.accept(i);
        }).then(System.out::println).onFinally(() -> endTest(5));
    }

    static Promise<Long> test6(long i) {
        Promise<Long> promise = new Promise<Long>().onFinally(() -> endTest(6));

        if (i > 100_000) {
            promise.reject(new Exception("TOO LONG"));
        } else {
            promise.resolve(i);
        }

        return promise;
    }

    static Promise<Long> test7() {
        return Promise.race(test5(4343L), test6(10L)).then(System.out::println).onFinally(() -> endTest(7));
    }
}