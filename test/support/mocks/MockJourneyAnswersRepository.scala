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

package support.mocks

import models.Done
import models.mongo.JourneyAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import repositories.JourneyAnswersRepository

import scala.concurrent.Future

trait MockJourneyAnswersRepository extends MockitoSugar {
  protected val mockJourneyAnswersRepo: JourneyAnswersRepository = mock[JourneyAnswersRepository]

  def mockKeepAliveJourneyAnswers(mtdItId: String, taxYear: Int, journey: String, result: Done): Unit =
    when(mockJourneyAnswersRepo.keepAlive(eqTo(mtdItId), eqTo(taxYear), eqTo(journey)))
      .thenReturn(Future.successful(result))

  def mockGetJourneyAnswers(mtdItId: String, taxYear: Int, journey: String, result: Option[JourneyAnswers]): Unit =
    when(mockJourneyAnswersRepo.get(eqTo(mtdItId), eqTo(taxYear), eqTo(journey)))
      .thenReturn(Future.successful(result))

  def mockGetJourneyAnswersException(mtdItId: String, taxYear: Int, journey: String, result: Throwable): Unit =
    when(mockJourneyAnswersRepo.get(eqTo(mtdItId), eqTo(taxYear), eqTo(journey)))
      .thenReturn(Future.failed(result))

  def mockSetJourneyAnswers(userData: JourneyAnswers, result: Done): Unit =
    when(mockJourneyAnswersRepo.set(eqTo(userData))).thenReturn(Future.successful(result))

  def mockClearJourneyAnswers(mtdItId: String, taxYear: Int, journey: String, result: Done): Unit =
    when(mockJourneyAnswersRepo.clear(eqTo(mtdItId), eqTo(taxYear), eqTo(journey)))
      .thenReturn(Future.successful(result))
}
