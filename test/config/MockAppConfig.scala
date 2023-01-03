/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import org.scalamock.scalatest.MockFactory

class MockAppConfig extends AppConfig with MockFactory {

  override val authBaseUrl: String = "/auth"

  override val auditingEnabled: Boolean = true
  override val graphiteHost: String = "/graphite"
  override val desBaseUrl: String = "/des"

  override val environment: String = "dev"
  override val authorisationToken: String = "someToken"
  override val authorisationTokenKey: String = "someToken"
  override val ifBaseUrl: String = "/if"
  override val ifEnvironment: String = "dev"

  override def authorisationTokenFor(apiVersion: String): String = "someToken"
}
