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

package models.logging

import models.logging.CorrelationId.{CorrelationIdHeaderKey, RequestOps}
import org.slf4j.MDC
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

object CorrelationIdMdc {

  /**
   * If correlation id exists in the request header reuse it. Otherwise generate a new one and add to header.
   * It also puts the correlationId to the MDC context for logging purpose.
   */
  def withEnrichedCorrelationId[A, B](originalRequest: Request[A])(block: Request[A] => Future[B])(implicit ec: ExecutionContext): Future[B] = {
    val (updatedRequest, correlationId) = RequestOps.withCorrelationId(originalRequest)
    MDC.put(CorrelationIdHeaderKey, correlationId)

    try {
      val result = block(updatedRequest)
      result.onComplete { _ =>
        MDC.remove(CorrelationIdHeaderKey)
      }
      result
    } catch {
      case e: Throwable =>
        MDC.remove(CorrelationIdHeaderKey)
        throw e
    }
  }
}