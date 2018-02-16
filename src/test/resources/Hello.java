import java.util.Optional;
import java.util.function.*;
import java.util.concurrent.atomic.*;

public class Hello {
  public void haha() {
    final AtomicInteger number = new AtomicInteger(25);
    Optional<Integer> iNum = hello(Optional.of(number), nnn -> nnn.getAndIncrement());
    System.out.println(iNum);
  }

  private <T extends Number, R> Optional<R> hello(final Optional<T> num, Function<T, R> f) {
    return num.map(f::apply);
  }
}