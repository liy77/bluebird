# Bluebird
JavaScript-like promises for Java

## Usage Example
```java
import java.lang.Thread;
import io.github.justawaifuhunter.bluebird.async.Promise;

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

## Download
### Gradle
```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.justawaifuhunter:bluebird:1.0.0")
}
```

### Maven
```xml
<dependency>
    <groupId>io.github.justawaifuhunter</groupId>
    <artifactId>bluebird</artifactId>
    <version>1.0.0</version>
</dependency>
```
