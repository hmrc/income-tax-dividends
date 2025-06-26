
# income-tax-dividends

This is where we make API calls from users for creating, viewing and making changes to the dividends section of their income tax return.

## Running the service locally

You will need to have the following:
- Installed [MongoDB](https://docs.mongodb.com/manual/installation/)
- Installed/configured [service manager](https://github.com/hmrc/sm2).

This can be found in the [developer handbook](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/)

The service manager profile for this service is:

    sm2 --start INCOME_TAX_DIVIDENDS

Run the following command to start the remaining services locally:

sudo mongod (If not already running)
sm2 --start INCOME_TAX_SUBMISSION_ALL

This service runs on port: `localhost:9307`


### Running Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report using the script `./check.sh`

### Feature Switches
| Feature                         | Description                                                       |
|---------------------------------|-------------------------------------------------------------------|
| useEncryption	               | Enables SymmetricCryptoFactory instead of EncryptedValue          |
| sectionCompletedQuestionEnabled | Redirects user to Have you completed this section from CYA page   |


## Ninos with stubbed data for dividends

| Nino | Dividends data |
| ---  | ---            |
| AA123459A | User with dividends data |
| AA000001A | User with dividends data |
| AA000003A | User with dividends data |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
