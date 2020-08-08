package arrow.benchmarks

import arrow.fx.IO
import arrow.fx.IODispatchers
import arrow.fx.coroutines.*
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.CompilerControl
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

val env = Environment(ComputationPool)

@State(Scope.Thread)
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
open class Async {

  @Param("3000")
  var size: Int = 0

  private fun ioAsyncLoop(i: Int): IO<Int> =
    IO.unit.continueOn(IODispatchers.CommonPool).followedBy(
      if (i > size) IO.just(i) else ioAsyncLoop(i + 1)
    )

  tailrec suspend fun loop(i: Int): Int =
    if (i > size) i else loop(i + 1)

  suspend fun asyncLoop(i: Int): Int =
    evalOn(ComputationPool) { loop(i) }

  @Benchmark
  fun io(): Int =
    ioAsyncLoop(0).unsafeRunSync()

  @Benchmark
  fun fx(): Int = env.unsafeRunSync {
    evalOn(ComputationPool) {
      asyncLoop(0)
    }
  }

  @Benchmark
  fun catsIO(): Int =
    arrow.benchmarks.effects.scala.cats.`Async$`.`MODULE$`.unsafeIOAsyncLoop(size, 0)

  @Benchmark
  fun scalazZIO(): Int =
    arrow.benchmarks.effects.scala.zio.`Async$`.`MODULE$`.unsafeIOAsyncLoop(size, 0)
}
