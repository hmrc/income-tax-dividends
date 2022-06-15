
# income-tax-dividends

This is where we make API calls from users for creating, viewing and making changes to the dividends section of their income tax return.

## Running the service locally

You will need to have the following:
- Installed/configured [service manager](https://github.com/hmrc/service-manager).

The service manager profile for this service is:

    sm --start INCOME_TAX_DIVIDENDS
Run the following command to start the remaining services locally:

sudo mongod (If not already running)
sm --start INCOME_TAX_SUBMISSION_ALL -r

This service runs on port: `localhost:9307`

### Dividends endpoints:

- **GET     /income-tax/nino/:nino/income-source/dividends/annual/:taxYear** (Retrieves details for the dividends income source over the accounting period which matches the tax year provided)
- **POST    /income-tax/nino/:nino/income-source/dividends/annual/:taxYear** (Provides the ability for a user to submit periodic annual income for dividends)

### Downstream services:

All interest data is retrieved/updated via the downstream system.

- DES (Data Exchange Service)

### Dividends income source

<details>
<summary>Click here to see an example of a users dividends data (JSON)</summary>

```json
{
  "ukDividends": 293206807.99,
  "otherUkDividends": 170603870.99
}
```

</details>

## Ninos with stubbed data for dividends

| Nino | Dividends data |
| ---  | ---            |
| AA123459A | User with dividends data |
| AA000001A | User with dividends data |
| AA000003A | User with dividends data |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
