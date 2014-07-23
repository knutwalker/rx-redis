name := "rx-redis"

libraryDependencies ++= List(
  "com.netflix.rxnetty"   %  "rx-netty"         % "0.3.9",
  "com.netflix.rxjava"    % "rxjava-scala"      % "0.19.1",
  "org.scalatest"        %%  "scalatest"        % "2.2.0" % "test"
)
