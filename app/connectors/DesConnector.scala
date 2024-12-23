/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import com.typesafe.config.ConfigFactory
import config.AppConfig
import models.logging.CorrelationId.CorrelationIdHeaderKey
import uk.gov.hmrc.http.HeaderCarrier.Config
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import utils.HeaderCarrierSyntax.HeaderCarrierOps

import java.net.URL

trait DesConnector {

  val appConfig: AppConfig

  val headerCarrierConfig: Config = HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  private[connectors] def desHeaderCarrier(url : String)(implicit hc: HeaderCarrier): HeaderCarrier = {
    val isInternalHost = headerCarrierConfig.internalHostPatterns.exists(_.pattern.matcher(new URL(url).getHost).matches())

    val hcWithAuth = hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.authorisationToken}")))
    val correlationId: Seq[(String, String)] = hc.maybeCorrelationId.map(id => CorrelationIdHeaderKey -> id).toList
    val extraHeaders: Seq[(String, String)] = Seq("Environment" -> appConfig.desEnvironment) ++ correlationId

    if(isInternalHost) {
      hcWithAuth.withExtraHeaders(extraHeaders: _*)
    } else {
      hcWithAuth.withExtraHeaders(extraHeaders ++ hcWithAuth.toExplicitHeaders: _*)
    }
  }

}
