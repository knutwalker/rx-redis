/*
 * Copyright 2014 – 2015 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.redis

import org.openjdk.jmh.profile.GCProfiler
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.options._
import org.openjdk.jmh.runner.{ Defaults, NoBenchmarksException, Runner, RunnerException }

import annotation.tailrec
import scala.util.Try

object Main {

  def run(options: Options): Unit = {
    val runner = new Runner(options)
    try {
      runner.run()
    } catch {
      case e: NoBenchmarksException ⇒
        Console.err.println("No matching benchmarks. Miss-spelled regexp?")
        if (options.verbosity.orElse(Defaults.VERBOSITY) ne VerboseMode.EXTRA) {
          Console.err.println("Use " + VerboseMode.EXTRA + " verbose mode to debug the pattern matching.")
        } else {
          runner.list()
        }
      case e: RunnerException ⇒
        Console.err.print("ERROR: ")
        e.printStackTrace(Console.err)
    }
  }

  def main(args: Array[String]): Unit = {
    Action(args) match {
      case NoAction ⇒
      case ListBenchmarks(opts) ⇒
        new Runner(opts).list()
      case RunBenchmarks(opts) ⇒
        run(opts)
    }
  }

  sealed trait Action
  case class ListBenchmarks(opts: Options) extends Action
  case class RunBenchmarks(opts: Options) extends Action
  case object NoAction extends Action

  object Action {
    def apply(args: Array[String]): Action = {
      loop(args.toList, None, OptionsParser()).map(_.run()).recover {
        case e: CommandLineOptionException ⇒
          Console.err.println("Error parsing command line:")
          Console.err.println(" " + e.getMessage)
          NoAction
      }.get
    }

    @tailrec
    private def loop(as: List[String], update: Option[String ⇒ OptionsParser], opts: OptionsParser): Try[CustomOptions] = as match {
      case Nil ⇒ opts.run()
      case x :: xs ⇒ update match {
        case Some(f) ⇒ loop(xs, None, f(x))
        case None ⇒ x match {
          case "-host" ⇒ loop(xs, Some(opts.withHost), opts)
          case "-port" ⇒ loop(xs, Some(opts.withPort), opts)
          case "-gc"   ⇒ loop(xs, None, opts.copy(enableProf = true))
          case unknown ⇒ loop(xs, None, opts.withArg(unknown))
        }
      }
    }
  }

  case class CustomOptions(options: Options, cmdOptions: CommandLineOptions) {
    def run(): Action =
      if (cmdOptions.shouldHelp) {
        cmdOptions.showHelp()
        Console.err.println(
          s"""
             |Redis options:
             |
             |  -host <string>              The redis host to connect to.
             |  -port <int>                 The redis port to connect to.
             |  -gc                         Enable GC profiling.
           """.stripMargin)
        NoAction
      } else if (cmdOptions.shouldList) {
        ListBenchmarks(cmdOptions)
      } else if (cmdOptions.shouldListProfilers) {
        cmdOptions.listProfilers()
        NoAction
      } else if (cmdOptions.shouldListResultFormats) {
        cmdOptions.listResultFormats()
        NoAction
      } else {
        RunBenchmarks(options)
      }
  }

  case class OptionsParser(
      remaining: Vector[String] = Vector(),
      enableProf: Boolean = false,
      host: Option[String] = None,
      port: Option[Int] = None) {

    def withHost(h: String): OptionsParser =
      copy(host = Some(h))

    def withPort(p: String): OptionsParser =
      copy(port = Try(p.toInt).toOption)

    def withArg(x: String): OptionsParser =
      copy(remaining = remaining :+ x)

    def run(): Try[CustomOptions] = {
      Try(new CommandLineOptions(remaining: _*)).map { cmdOptions ⇒
        val builder = new OptionsBuilder()
          .parent(cmdOptions)
          .resultFormat(ResultFormatType.SCSV)
          .shouldDoGC(true)

        if (enableProf) {
          builder.addProfiler(classOf[GCProfiler])
        }

        if (host.isDefined || port.isDefined) {
          builder.jvmArgsAppend(toJmh: _*)
        }

        val options = builder.build()
        CustomOptions(options, cmdOptions)
      }
    }

    private def toJmh = List(
      host.map(h ⇒ s"-Drx.redis.host=$h").toList,
      port.map(p ⇒ s"-Drx.redis.port=$p").toList
    ).flatten
  }
}
