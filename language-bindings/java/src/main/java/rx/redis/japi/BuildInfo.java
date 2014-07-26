/*
 * Copyright 2014 Paul Horn
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

package rx.redis.japi;

import rx.redis.BuildInfo$;

import java.util.Properties;


public interface BuildInfo {

  public static final String ARTIFACT_ID =
      BuildInfo$.MODULE$.artifactId();

  public static final int BUILD_NUMBER =
      BuildInfo$.MODULE$.buildinfoBuildnumber();

  public static final String BUILD_TIME =
      BuildInfo$.MODULE$.buildTime();

  public static final long BUILD_TIME_MILLIS =
      BuildInfo$.MODULE$.buildTimeMillis();

  public static final String GIT_COMMIT =
      BuildInfo$.MODULE$.gitCommit();

  public static final String GIT_COMMIT_SHA1 =
      BuildInfo$.MODULE$.gitCommitSha1();

  public static final String GROUP_ID =
      BuildInfo$.MODULE$.groupId();

  public static final String SBT_VERSION =
      BuildInfo$.MODULE$.sbtVersion();

  public static final String SCALA_VERSION =
      BuildInfo$.MODULE$.scalaVersion();

  public static final String VERSION =
      BuildInfo$.MODULE$.version();

  public static Properties toProperties() {
    final Properties properties = new Properties();
    properties.setProperty("artifactId", ARTIFACT_ID);
    properties.setProperty("buildNumber", String.valueOf(BUILD_NUMBER));
    properties.setProperty("buildTime", BUILD_TIME);
    properties.setProperty("buildTimeMillis", String.valueOf(BUILD_TIME_MILLIS));
    properties.setProperty("gitCommit", GIT_COMMIT);
    properties.setProperty("gitCommitSha1", GIT_COMMIT_SHA1);
    properties.setProperty("groupId", GROUP_ID);
    properties.setProperty("sbtVersion", SBT_VERSION);
    properties.setProperty("scalaVersion", SCALA_VERSION);
    properties.setProperty("version", VERSION);
    return properties;
  }

}
