# Bluebird
JavaScript-like promises for Java

## Usage
```java
import java.lang.Thread;
import net.bluebird.async.Promise;

class Main() {
    public static void main(String[] args) {
        waitLong(1_000L).await(); // Wait 1_000 and returns 6_000L
        waitLong(11_000L).await(); // Throws Exception "Too Long"
    }
    
    public static Promise<Long> waitLong(long initialValue) {
        return new Promise((resolve, reject) -> {
            if (initialValue > 10_000L) {
                reject.accept(new Exception("Too Long"));
            } else {
                Thread.sleep(1_000);
                
                resolve.accept(initialValue + 5_000);
            }
        });
    }
}
```