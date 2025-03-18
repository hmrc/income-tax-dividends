/*
 * Copyright 2022 HM Revenue & Customs
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

import sbt.*

object AppDependencies {

  private val bootstrapPlay30Version = "8.6.0"
  private val hmrcMongoPlay30Version = "2.6.0"

  private val jacksonAndPlayExclusions: Seq[InclusionRule] = Seq(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
    ExclusionRule(organization = "com.fasterxml.jackson.module"),
    ExclusionRule(organization = "com.fasterxml.jackson.core:jackson-annotations"),
    ExclusionRule(organization = "com.typesafe.play")
  )

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"              % bootstrapPlay30Version,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"                     % hmrcMongoPlay30Version,
    "uk.gov.hmrc"                   %% "crypto-json-play-30"                    % "8.2.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                   % "2.18.3",
    "com.beachape"                  %% "enumeratum"                             % "1.7.5",
    "org.typelevel"                 %% "cats-core"                              % "2.13.0",
    "com.beachape"                  %% "enumeratum-play-json"                   % "1.8.2" excludeAll (jacksonAndPlayExclusions *)
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % bootstrapPlay30Version,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"  % hmrcMongoPlay30Version,
    "org.scalamock"           %% "scalamock"                % "5.2.0",
    "org.mockito"             %% "mockito-scala"            % "1.17.37"
  ).map(_ % Test)
}
